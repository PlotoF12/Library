package com.example.library.module.reservation.service.impl;

import com.example.library.common.exception.BizException;
import com.example.library.common.response.PageResult;
import com.example.library.common.util.IdempotencyUtil;
import com.example.library.module.notification.event.ReservationEvent;
import com.example.library.module.reservation.dto.*;
import com.example.library.module.reservation.entity.Reservation;
import com.example.library.module.reservation.entity.ViolationRecord;
import com.example.library.module.reservation.mapper.ReservationMapper;
import com.example.library.module.reservation.mapper.ViolationRecordMapper;
import com.example.library.module.reservation.repository.ReservationRepository;
import com.example.library.module.reservation.service.ReservationService;
import com.example.library.module.user.entity.Student;
import com.example.library.module.user.mapper.StudentMapper;
import com.example.library.module.user.repository.StudentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 预约服务实现 — 系统最核心的业务逻辑。
 *
 * <p><b>并发安全设计（多层防御）</b>：
 * <ol>
 *   <li>幂等键检查（Redis SETNX）— 防止重复提交</li>
 *   <li>Redis 分布式锁（Redisson RLock）— 同一座位同时段互斥</li>
 *   <li>DB UNIQUE KEY uk_seat_time — 数据库层面兜底防重</li>
 *   <li>乐观锁 version — 签到/离座/爽约的并发控制</li>
 * </ol>
 *
 * <p><b>爽约处理流程</b>：
 * <ol>
 *   <li>预约创建时 → 投递延迟队列消息（15分钟后触发）</li>
 *   <li>延迟消息到达 → 检查 status 是否为 1（待签到）→ 乐观锁更新为 4（爽约）</li>
 *   <li>@Scheduled 每秒扫描 → 兜底检查（防止延迟队列丢消息）</li>
 *   <li>爽约确认后 → SELECT FOR UPDATE 递增违规计数 → 达到阈值则禁约10天</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;
    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final ViolationRecordMapper violationRecordMapper;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final RBloomFilter<Long> seatBloomFilter;
    private final RDelayedQueue<Long> noShowDelayedQueue;
    private final IdempotencyUtil idempotencyUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final com.example.library.module.seat.repository.SeatRepository seatRepository;
    private final com.example.library.module.seat.repository.StudyRoomRepository roomRepository;
    private final EntityManager entityManager;

    @Value("${app.reservation.no-show-timeout-minutes:15}")
    private int noShowTimeoutMinutes;

    @Value("${app.reservation.violation-threshold:3}")
    private int violationThreshold;

    @Value("${app.reservation.violation-ban-days:10}")
    private int violationBanDays;

    @Value("${app.reservation.max-end-time:22:00}")
    private String maxEndTime;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ==================== 创建预约 ====================

    /**
     * 创建预约 — 并发安全的多层防御。
     *
     * <p>完整流程（8步）：
     * <pre>
     * 1. 幂等键检查 → 防重复提交
     * 2. 日期校验 → 仅允许当天
     * 3. 惩罚期校验 → 被禁约的学生拒绝
     * 4. 时段冲突校验 → 同一学生同时段只能约1个
     * 5. 座位可用性校验 → 布隆过滤器 + Redis + DB
     * 6. Redis分布式锁 → 座位+时段粒度的互斥
     * 7. INSERT → DB UNIQUE KEY 兜底
     * 8. 投递延迟队列 + 发布异步事件
     * </pre>
     */
    @Override
    @Transactional
    public ReservationVO createReservation(Long studentId, CreateReservationRequest request,
                                            String idempotencyKey) {
        // 1. 幂等键检查
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String existing = idempotencyUtil.get(idempotencyKey);
            if (existing != null) {
                log.info("幂等返回: idempotencyKey={}", idempotencyKey);
                try {
                    return objectMapper.readValue(existing, ReservationVO.class);
                } catch (JsonProcessingException e) {
                    log.error("幂等缓存反序列化失败", e);
                }
            }
        }

        // 2. 解析参数
        LocalDate reserveDate = LocalDate.parse(request.getReserveDate());
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalTime endTime = LocalTime.parse(request.getEndTime());
        LocalTime maxEnd = LocalTime.parse(maxEndTime);
        LocalDateTime now = LocalDateTime.now();

        // 仅允许当天预约
        if (!reserveDate.equals(LocalDate.now())) {
            throw new BizException(20001, "只能预约当天的座位");
        }
        // 开始时间不早于当前
        if (startTime.isBefore(now.toLocalTime())) {
            throw new BizException(20007, "预约开始时间不能早于当前时间");
        }
        // 结束时间不晚于22:00
        if (endTime.isAfter(maxEnd)) {
            throw new BizException(20008, "预约结束时间不能晚于" + maxEndTime);
        }
        // 结束晚于开始
        if (!endTime.isAfter(startTime)) {
            throw new BizException(20002, "结束时间必须晚于开始时间");
        }

        // 3. 检查学生是否在惩罚期
        checkBanned(studentId);

        // 4. 检查当日无时段冲突
        if (reservationMapper.existsConflict(studentId, reserveDate, startTime, endTime)) {
            throw new BizException(20004, "同一时段只能预约一个座位");
        }

        // 5. 检查座位可用
        checkSeatAvailable(request.getSeatId());

        // 5.5 检查座位时段冲突（快速失败）
        // 查询该座位在目标时段内是否有重叠的预约（status=1待签到 / status=2使用中）
        List<Reservation> seatConflicts = reservationMapper.findBySeatAndTimeRange(
                request.getSeatId(), reserveDate, startTime, endTime);
        if (!seatConflicts.isEmpty()) {
            throw new BizException(20006, "该座位此时段已被预约");
        }

        // 6. Redis 分布式锁（同一座位同时段互斥）
        String lockKey = buildLockKey(request.getSeatId(), request.getReserveDate(),
                request.getStartTime(), request.getEndTime());
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                throw new BizException(20006, "该座位此时段已被预约");
            }

            // 6.5 锁内二次校验（SELECT ... FOR UPDATE）
            //    跳过快照读，直接读取最新已提交数据；Next-Key Lock 阻塞并发 INSERT
            seatConflicts = reservationMapper.findBySeatAndTimeRangeForUpdate(
                    request.getSeatId(), reserveDate, startTime, endTime);
            if (!seatConflicts.isEmpty()) {
                throw new BizException(20006, "该座位此时段已被预约");
            }

            // 7. 双重校验 + INSERT（DB UNIQUE KEY 兜底）
            Reservation reservation = Reservation.builder()
                    .studentId(studentId)
                    .seatId(request.getSeatId())
                    .roomId(getRoomIdBySeat(request.getSeatId()))
                    .reserveDate(reserveDate)
                    .startTime(startTime)
                    .endTime(endTime)
                    .status(1) // 待签到
                    .signToken(UUID.randomUUID().toString())
                    .version(0)
                    .build();

            try {
                reservationMapper.insert(reservation);
            } catch (DuplicateKeyException e) {
                throw new BizException(20006, "该座位此时段已被预约");
            }

            // 8. 缓存清理 + 投递延迟队列 + 异步事件
            redisTemplate.delete("seat_status:" + request.getSeatId());
            offerDelayedNoShowCheck(reservation.getId(), startTime);
            eventPublisher.publishEvent(new ReservationEvent(this,
                    reservation.getId(), ReservationEvent.EventType.RESERVATION_CREATED));

            log.info("预约创建成功: reservationId={}, studentId={}, seatId={}",
                    reservation.getId(), studentId, request.getSeatId());

            ReservationVO vo = toReservationVO(reservation, null);
            // 缓存幂等结果
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                try {
                    idempotencyUtil.checkAndSet(idempotencyKey, objectMapper.writeValueAsString(vo));
                } catch (JsonProcessingException e) {
                    log.error("幂等缓存序列化失败", e);
                }
            }
            return vo;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(500, "系统繁忙，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ==================== 签到（模拟） ====================

    /**
     * 签到确认 — 模拟二维码扫描。
     *
     * <p>学生点击"确认到达座位"按钮 → 前端弹窗"已完成二维码扫描确认" → 后端签到。
     *
     * <p>并发安全：乐观锁更新，version 字段防止并发重复签到。
     * 如果 affected_rows=0，说明已被爽约系统标记或已签到。
     */
    @Override
    @Transactional
    public ReservationVO signIn(Long reservationId, Long studentId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BizException(10004, "预约不存在"));

        // 归属校验
        if (!reservation.getStudentId().equals(studentId)) {
            throw new BizException(10003, "只能签到自己的预约");
        }

        // 乐观锁更新 status: 1(待签到) → 2(使用中)
        int affected = reservationMapper.updateStatusToSignedIn(
                reservationId, reservation.getVersion());

        if (affected == 0) {
            // 并发冲突，检查当前状态
            Reservation current = reservationRepository.findById(reservationId).orElse(null);
            if (current != null && current.getStatus() == 4) {
                throw new BizException(20010, "签到超时，该预约已被标记为爽约，座位已释放");
            }
            throw new BizException(20011, "您已签到，无需重复操作");
        }

        // 解管实体：MyBatis 已直接更新 DB，防止 Hibernate 提交时 version 冲突
        entityManager.detach(reservation);
        log.info("签到成功: reservationId={}, studentId={}", reservationId, studentId);
        reservation.setStatus(2);
        reservation.setActualSignTime(LocalDateTime.now());
        return toReservationVO(reservation, null);
    }

    // ==================== 离座 ====================

    /**
     * 确认离座 — 释放座位。
     *
     * <p>并发安全：乐观锁更新 status: 2(使用中) → 3(已完成)。
     */
    @Override
    @Transactional
    public ReservationVO leave(Long reservationId, Long studentId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BizException(10004, "预约不存在"));

        if (!reservation.getStudentId().equals(studentId)) {
            throw new BizException(10003, "只能操作自己的预约");
        }

        int affected = reservationMapper.updateStatusToCompleted(
                reservationId, reservation.getVersion());

        if (affected == 0) {
            throw new BizException(20012, "该预约已完成，请勿重复操作");
        }

        // 解管实体：MyBatis 已直接更新 DB，防止 Hibernate 提交时 version 冲突
        entityManager.detach(reservation);
        // 删除座位缓存（座位恢复可用）
        redisTemplate.delete("seat_status:" + reservation.getSeatId());

        reservation.setStatus(3);
        reservation.setActualLeaveTime(LocalDateTime.now());
        log.info("离座确认成功: reservationId={}", reservationId);
        return toReservationVO(reservation, null);
    }

    // ==================== 取消预约 ====================

    @Override
    @Transactional
    public ReservationVO cancel(Long reservationId, Long studentId, String reason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BizException(10004, "预约不存在"));

        if (!reservation.getStudentId().equals(studentId)) {
            throw new BizException(10003, "只能取消自己的预约");
        }

        // 已开始的预约不能取消
        LocalDateTime startDateTime = LocalDateTime.of(
                reservation.getReserveDate(), reservation.getStartTime());
        if (LocalDateTime.now().isAfter(startDateTime)) {
            throw new BizException(20013, "预约已开始，无法取消");
        }

        int affected = reservationMapper.updateStatusToCancelled(
                reservationId, reservation.getVersion());

        if (affected == 0) {
            throw new BizException(20011, "该预约状态已变更，无法取消");
        }

        // 解管实体：MyBatis 已直接更新 DB，防止 Hibernate 提交时 version 冲突
        entityManager.detach(reservation);
        redisTemplate.delete("seat_status:" + reservation.getSeatId());
        log.info("预约取消: reservationId={}, reason={}", reservationId, reason);

        reservation.setStatus(5);
        return toReservationVO(reservation, null);
    }

    // ==================== 查询 ====================

    @Override
    public PageResult<ReservationVO> listMyReservations(Long studentId, Integer status,
                                                         String date, int page, int size) {
        LocalDate queryDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : null;
        int offset = (page - 1) * size;

        List<Reservation> reservations = reservationMapper.findByConditions(
                queryDate, status, null, studentId, null, null, offset, size);
        long total = reservationMapper.countByConditions(
                queryDate, status, null, studentId, null, null);

        List<ReservationVO> vos = reservations.stream()
                .map(r -> toReservationVO(r, null))
                .collect(Collectors.toList());

        return PageResult.of(vos, page, size, total);
    }

    @Override
    public ReservationVO getDetail(Long reservationId, Long userId, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BizException(10004, "预约不存在"));

        if (!isAdmin && !reservation.getStudentId().equals(userId)) {
            throw new BizException(10003, "只能查看自己的预约");
        }

        Student student = studentRepository.findById(reservation.getStudentId()).orElse(null);
        return toReservationVO(reservation, student);
    }

    @Override
    public PageResult<ReservationVO> listAllReservations(String date, Integer status,
                                                          Long roomId, String studentNo,
                                                          String phone, Boolean onlyNoShow,
                                                          int page, int size) {
        LocalDate queryDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : null;
        int offset = (page - 1) * size;

        List<Reservation> reservations = reservationMapper.findByConditions(
                queryDate, status, roomId, null, studentNo, onlyNoShow, offset, size);
        long total = reservationMapper.countByConditions(
                queryDate, status, roomId, null, studentNo, onlyNoShow);

        List<ReservationVO> vos = reservations.stream()
                .map(r -> {
                    Student s = studentRepository.findById(r.getStudentId()).orElse(null);
                    return toReservationVO(r, s);
                })
                .collect(Collectors.toList());

        return PageResult.of(vos, page, size, total);
    }

    // ==================== 管理员按学生搜索 ====================

    /**
     * 管理员按学号或手机号搜索学生，返回其"待签到"和"使用中"的预约。
     */
    @Override
    public PageResult<ReservationVO> searchByStudent(String keyword, int page, int size) {
        int offset = (page - 1) * size;

        List<Reservation> reservations = reservationMapper.findByStudentNoOrPhone(
                keyword, offset, size);
        long total = reservationMapper.countByStudentNoOrPhone(keyword);

        List<ReservationVO> vos = reservations.stream()
                .map(r -> {
                    Student s = studentRepository.findById(r.getStudentId()).orElse(null);
                    return toReservationVO(r, s);
                })
                .collect(Collectors.toList());

        return PageResult.of(vos, page, size, total);
    }

    // ==================== 管理员签到/离座 ====================

    /**
     * 管理员确认学生签到 — 跳过学生归属校验。
     * 乐观锁更新 status: 1(待签到) → 2(使用中)。
     */
    @Override
    @Transactional
    public ReservationVO adminSignIn(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BizException(10004, "预约不存在"));

        if (reservation.getStatus() != 1) {
            throw new BizException(20011, "该预约不是待签到状态，当前状态：" +
                    switch (reservation.getStatus()) {
                        case 2 -> "使用中";
                        case 3 -> "已完成";
                        case 4 -> "已爽约";
                        case 5 -> "已取消";
                        default -> "未知";
                    });
        }

        int affected = reservationMapper.updateStatusToSignedIn(
                reservationId, reservation.getVersion());

        if (affected == 0) {
            throw new BizException(20010, "操作失败，预约状态已被其他操作变更，请刷新后重试");
        }

        entityManager.detach(reservation);
        reservation.setStatus(2);
        reservation.setActualSignTime(LocalDateTime.now());
        log.info("管理员确认签到: reservationId={}", reservationId);
        return toReservationVO(reservation, null);
    }

    /**
     * 管理员确认学生离座 — 跳过学生归属校验。
     * 乐观锁更新 status: 2(使用中) → 3(已完成)。
     */
    @Override
    @Transactional
    public ReservationVO adminLeave(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BizException(10004, "预约不存在"));

        if (reservation.getStatus() != 2) {
            throw new BizException(20012, "该预约不是使用中状态，当前状态：" +
                    switch (reservation.getStatus()) {
                        case 1 -> "待签到";
                        case 3 -> "已完成";
                        case 4 -> "已爽约";
                        case 5 -> "已取消";
                        default -> "未知";
                    });
        }

        int affected = reservationMapper.updateStatusToCompleted(
                reservationId, reservation.getVersion());

        if (affected == 0) {
            throw new BizException(20010, "操作失败，预约状态已被其他操作变更，请刷新后重试");
        }

        entityManager.detach(reservation);
        redisTemplate.delete("seat_status:" + reservation.getSeatId());

        reservation.setStatus(3);
        reservation.setActualLeaveTime(LocalDateTime.now());
        log.info("管理员确认离座: reservationId={}, seatId={}",
                reservationId, reservation.getSeatId());
        return toReservationVO(reservation, null);
    }

    // ==================== 定时任务 ====================

    /**
     * 爽约扫描 — 每秒执行，查询超过15分钟未签到的预约并标记为爽约。
     *
     * <p>执行流程：
     * <ol>
     *   <li>查询 status=1 且 start_time 距今超过15分钟的预约</li>
     *   <li>逐条乐观锁更新 status: 1→4</li>
     *   <li>更新失败（affected=0）则跳过（可能已被延迟队列处理）</li>
     *   <li>递增学生违规计数（SELECT FOR UPDATE + CASE WHEN 判断阈值）</li>
     *   <li>达到阈值则设置 banned_until</li>
     *   <li>插入爽约记录</li>
     *   <li>删除座位缓存</li>
     *   <li>发布爽约通知事件</li>
     * </ol>
     */
    @Override
    @Transactional
    public void checkNoShowReservations() {
        LocalDate today = LocalDate.now();
        LocalTime deadline = LocalTime.now().minusMinutes(noShowTimeoutMinutes);
        int batchSize = 50;

        List<Reservation> expired = reservationMapper.findPendingAndExpired(
                today, deadline, batchSize);

        for (Reservation r : expired) {
            int affected = reservationMapper.updateStatusToNoShow(r.getId(), r.getVersion());
            if (affected == 0) { entityManager.detach(r); continue; } // 已被处理

            // 解管实体：MyBatis 已直接更新 DB，防止 Hibernate 提交时 version 冲突
            entityManager.detach(r);

            // 递增违规计数 + 判断是否惩罚
            studentMapper.incrementViolationAndBan(
                    r.getStudentId(), violationBanDays, violationThreshold);

            // 插入爽约记录
            Student student = studentRepository.findById(r.getStudentId()).orElse(null);
            int currentCount = student != null ? student.getViolationCount() + 1 : 1;
            violationRecordMapper.insertRecord(
                    r.getStudentId(), r.getId(), 1,
                    LocalDate.now(),
                    currentCount >= violationThreshold
                            ? LocalDate.now().plusDays(violationBanDays)
                            : LocalDate.now());

            // 释放座位
            redisTemplate.delete("seat_status:" + r.getSeatId());

            // 异步通知
            eventPublisher.publishEvent(new ReservationEvent(this,
                    r.getId(), ReservationEvent.EventType.NO_SHOW));

            log.info("爽约标记: reservationId={}, studentId={}", r.getId(), r.getStudentId());
        }
    }

    /**
     * 超时释放扫描 — 每分钟执行，释放已超过预约结束时间但未离座的座位。
     */
    @Override
    @Transactional
    public void checkTimeoutReservations() {
        LocalTime now = LocalTime.now();
        int batchSize = 50;

        List<Reservation> expired = reservationMapper.findInUseAndExpired(now, batchSize);

        for (Reservation r : expired) {
            int affected = reservationMapper.updateStatusToCompleted(r.getId(), r.getVersion());
            if (affected == 0) { entityManager.detach(r); continue; }

            // 解管实体：MyBatis 已直接更新 DB，防止 Hibernate 提交时 version 冲突
            entityManager.detach(r);
            redisTemplate.delete("seat_status:" + r.getSeatId());

            eventPublisher.publishEvent(new ReservationEvent(this,
                    r.getId(), ReservationEvent.EventType.TIMEOUT));

            log.info("超时释放: reservationId={}, seatId={}", r.getId(), r.getSeatId());
        }
    }

    // ==================== 私有辅助方法 ====================

    /** 构建分布式锁 Key */
    private String buildLockKey(Long seatId, String date, String start, String end) {
        return "seat_lock:" + seatId + ":" + date + ":" + start + ":" + end;
    }

    /** 检查学生是否在惩罚期内 */
    private void checkBanned(Long studentId) {
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student != null && student.getBannedUntil() != null) {
            if (student.getBannedUntil().isAfter(LocalDateTime.now())) {
                throw new BizException(20003,
                        "您因累计爽约已被限制预约，解禁时间：" +
                                student.getBannedUntil().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
        }
    }

    /**
     * 检查座位可用性 — 布隆过滤器 → Redis → DB 三级校验。
     */
    private void checkSeatAvailable(Long seatId) {
        // 第一层：布隆过滤器快速过滤不存在的ID
        if (!seatBloomFilter.contains(seatId)) {
            throw new BizException(20005, "该座位不存在或不可预约");
        }

        // 第二层：Redis 缓存检查（查座位当前状态）
        String cacheKey = "seat_status:" + seatId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null && cached.toString().contains("IN_USE")) {
            throw new BizException(20005, "该座位已被占用");
        }

        // 第三层：DB 最终校验座位状态
        var seatOpt = seatRepository.findById(seatId);
        if (seatOpt.isEmpty()) {
            throw new BizException(20005, "该座位不存在");
        }
        if (seatOpt.get().getStatus() != 1) {
            throw new BizException(20005,
                    seatOpt.get().getStatus() == 2 ? "该座位正在维修中" : "该座位已被禁用");
        }
    }

    /** 根据座位ID获取自习室ID */
    private Long getRoomIdBySeat(Long seatId) {
        return seatRepository.findById(seatId)
                .map(s -> s.getRoomId())
                .orElseThrow(() -> new BizException(10004, "座位不存在"));
    }

    /** 投递延迟队列消息 */
    private void offerDelayedNoShowCheck(Long reservationId, LocalTime startTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = LocalDateTime.of(LocalDate.now(), startTime)
                .plusMinutes(noShowTimeoutMinutes);
        long delaySeconds = ChronoUnit.SECONDS.between(now, deadline);
        if (delaySeconds > 0) {
            noShowDelayedQueue.offer(reservationId, delaySeconds, TimeUnit.SECONDS);
            log.debug("延迟队列投递: reservationId={}, delay={}s", reservationId, delaySeconds);
        }
    }

    /** 将实体转为VO */
    private ReservationVO toReservationVO(Reservation r, Student s) {
        String statusText = switch (r.getStatus()) {
            case 1 -> "待签到";
            case 2 -> "使用中";
            case 3 -> "已完成";
            case 4 -> "已爽约";
            case 5 -> "已取消";
            default -> "未知";
        };

        // 计算使用时长
        String duration = null;
        if (r.getActualSignTime() != null && r.getActualLeaveTime() != null) {
            long minutes = ChronoUnit.MINUTES.between(
                    r.getActualSignTime(), r.getActualLeaveTime());
            duration = (minutes / 60) + "小时" + (minutes % 60) + "分钟";
        }

        // 计算签到截止时间
        LocalDateTime deadline = LocalDateTime.of(r.getReserveDate(), r.getStartTime())
                .plusMinutes(noShowTimeoutMinutes);

        // 查询座位编号和自习室名称
        String seatCode = seatRepository.findById(r.getSeatId())
                .map(com.example.library.module.seat.entity.Seat::getSeatCode).orElse(null);
        var room = roomRepository.findById(r.getRoomId()).orElse(null);
        String roomName = room != null ? room.getName() : null;
        String roomCode = room != null ? room.getRoomCode() : null;

        return ReservationVO.builder()
                .reservationId(r.getId())
                .studentId(r.getStudentId())
                .studentNo(s != null ? s.getStudentNo() : null)
                .studentName(s != null ? s.getName() : null)
                .phone(s != null ? maskPhone(s.getPhone()) : null)
                .seatId(r.getSeatId())
                .seatCode(seatCode)
                .roomId(r.getRoomId())
                .roomCode(roomCode)
                .roomName(roomName)
                .reserveDate(r.getReserveDate())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .status(r.getStatus())
                .statusText(statusText)
                .signToken(r.getSignToken())
                .actualSignTime(r.getActualSignTime())
                .actualLeaveTime(r.getActualLeaveTime())
                .duration(duration)
                .deadline(deadline)
                .deadlineNote("请在" + deadline.format(DateTimeFormatter.ofPattern("HH:mm")) +
                        "前点击确认到达，否则将标记为爽约")
                .violationCount(s != null ? s.getViolationCount() : 0)
                .isNoShow(r.getStatus() == 4)
                .createdAt(r.getCreatedAt())
                .build();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
