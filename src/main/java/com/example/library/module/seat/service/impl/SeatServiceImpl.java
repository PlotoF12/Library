package com.example.library.module.seat.service.impl;

import com.example.library.common.exception.BizException;
import com.example.library.common.response.PageResult;
import com.example.library.module.reservation.mapper.ReservationMapper;
import com.example.library.module.seat.dto.*;
import com.example.library.module.seat.entity.Seat;
import com.example.library.module.seat.entity.StudyRoom;
import com.example.library.module.seat.repository.SeatRepository;
import com.example.library.module.seat.repository.StudyRoomRepository;
import com.example.library.module.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 座位服务实现 — 自习室查询、座位查询/管理。
 *
 * <p>缓存架构（三级）：
 * <ol>
 *   <li>L1 Caffeine：自习室列表（低频变更，TTL=10分钟）</li>
 *   <li>L2 Redis：座位实时状态（高频变更，TTL=5分钟+随机）</li>
 *   <li>L3 MySQL：数据库兜底</li>
 * </ol>
 *
 * <p>缓存一致性：采用 Cache-Aside 模式，写操作先更新DB再删除缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final StudyRoomRepository roomRepository;
    private final ReservationMapper reservationMapper;
    private final RedissonClient redissonClient;
    private final RBloomFilter<Long> seatBloomFilter;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String CACHE_SEAT_STATUS = "seat_status:";
    private static final String CACHE_ROOM_LIST = "room_list";

    /**
     * 应用启动时加载所有可用座位ID到布隆过滤器。
     * 防止因布隆过滤器未初始化导致已有座位被误判为"不存在"。
     */
    @PostConstruct
    public void initSeatBloomFilter() {
        List<Seat> allSeats = seatRepository.findAll();
        for (Seat seat : allSeats) {
            seatBloomFilter.add(seat.getId());
        }
        log.info("布隆过滤器初始化完成: 已加载 {} 个座位ID", allSeats.size());
    }

    // ---- 自习室 ----

    @Override
    public PageResult<RoomVO> listRooms(Integer floor, Integer status, int page, int size) {
        // 直接从 DB 查询（自习室数量少，无需缓存）
        List<StudyRoom> rooms;
        if (floor != null) {
            rooms = roomRepository.findByFloorAndStatus(floor, status != null ? status : 1);
        } else {
            rooms = roomRepository.findByStatus(status != null ? status : 1);
        }

        // 组装 VO（含可用座位数）
        List<RoomVO> roomVOs = new ArrayList<>();
        for (StudyRoom room : rooms) {
            long available = seatRepository.countByRoomIdAndStatus(room.getId(), 1);
            roomVOs.add(RoomVO.builder()
                    .roomId(room.getId())
                    .roomCode(room.getRoomCode())
                    .name(room.getName())
                    .floor(room.getFloor())
                    .totalSeats(room.getTotalSeats())
                    .availableSeats((int) available)
                    .status(room.getStatus())
                    .statusText(room.getStatus() == 1 ? "开放中" : "已关闭")
                    .build());
        }

        int start = (page - 1) * size;
        int end = Math.min(start + size, roomVOs.size());
        return PageResult.of(
                start < roomVOs.size() ? roomVOs.subList(start, end) : List.of(),
                page, size, roomVOs.size());
    }

    @Override
    public RoomVO getRoomDetail(Long roomId) {
        StudyRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BizException(10004, "自习室不存在"));

        List<Seat> seats = seatRepository.findByRoomId(roomId);
        long available = seatRepository.countByRoomIdAndStatus(roomId, 1);

        List<SeatVO> seatVOs = seats.stream()
                .map(this::toSeatVO)
                .collect(Collectors.toList());

        return RoomVO.builder()
                .roomId(room.getId())
                .roomCode(room.getRoomCode())
                .name(room.getName())
                .floor(room.getFloor())
                .totalSeats(room.getTotalSeats())
                .availableSeats((int) available)
                .status(room.getStatus())
                .statusText(room.getStatus() == 1 ? "开放中" : "已关闭")
                .seats(seatVOs)
                .build();
    }

    // ---- 座位查询 ----

    @Override
    public PageResult<SeatVO> listAvailableSeats(Long roomId, String date,
                                                  String startTime, String endTime,
                                                  Integer hasPower, Integer isWindow,
                                                  Integer isSingle, int page, int size) {
        // 解析参数（处理空字符串，前端清空时段时会传 ""）
        LocalDate reserveDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
        LocalTime start = (startTime != null && !startTime.isBlank()) ? LocalTime.parse(startTime) : null;
        LocalTime end = (endTime != null && !endTime.isBlank()) ? LocalTime.parse(endTime) : null;

        // 从DB查询所有符合条件的座位
        List<Seat> allSeats;
        if (roomId != null) {
            allSeats = seatRepository.findByRoomIdAndStatus(roomId, 1);
        } else {
            allSeats = seatRepository.findAll(); // 需要优化：大数量时改用分页
            allSeats = allSeats.stream()
                    .filter(s -> s.getStatus() == 1)
                    .collect(Collectors.toList());
        }

        // 偏好筛选
        if (hasPower != null) {
            allSeats = allSeats.stream()
                    .filter(s -> s.getHasPower().equals(hasPower))
                    .collect(Collectors.toList());
        }
        if (isWindow != null) {
            allSeats = allSeats.stream()
                    .filter(s -> s.getIsWindow().equals(isWindow))
                    .collect(Collectors.toList());
        }
        if (isSingle != null) {
            allSeats = allSeats.stream()
                    .filter(s -> s.getIsSingle().equals(isSingle))
                    .collect(Collectors.toList());
        }

        // 时段冲突校验：查询已有预约，过滤已占用的座位
        List<Long> occupiedSeatIds = List.of();
        if (start != null && end != null) {
            occupiedSeatIds = reservationMapper
                    .findBySeatAndTimeRange(null, reserveDate, start, end)
                    .stream()
                    .map(r -> r.getSeatId())
                    .collect(Collectors.toList());
        }

        // 组装 VO
        List<Long> finalOccupiedSeatIds = occupiedSeatIds;
        List<SeatVO> seatVOs = allSeats.stream()
                .map(s -> {
                    SeatVO vo = toSeatVO(s);
                    if (finalOccupiedSeatIds.contains(s.getId())) {
                        vo.setAvailable(false);
                        vo.setCurrentStatus("RESERVED");
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        // 内存分页
        int startIdx = (page - 1) * size;
        int endIdx = Math.min(startIdx + size, seatVOs.size());
        return PageResult.of(
                startIdx < seatVOs.size() ? seatVOs.subList(startIdx, endIdx) : List.of(),
                page, size, seatVOs.size());
    }

    // ---- 管理员：座位CRUD ----

    @Override
    @Transactional
    @CacheEvict(value = "rooms", allEntries = true)
    public SeatVO createSeat(SeatCreateRequest request) {
        // 校验自习室存在
        roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new BizException(10004, "自习室不存在"));

        // 校验座位编号唯一性
        if (seatRepository.existsByRoomIdAndSeatCode(request.getRoomId(), request.getSeatCode())) {
            throw new BizException(30002, "该自习室中座位编号已存在");
        }

        Seat seat = Seat.builder()
                .roomId(request.getRoomId())
                .seatCode(request.getSeatCode())
                .hasPower(request.getHasPower() != null ? request.getHasPower() : 0)
                .isWindow(request.getIsWindow() != null ? request.getIsWindow() : 0)
                .isSingle(request.getIsSingle() != null ? request.getIsSingle() : 0)
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .build();
        seat = seatRepository.save(seat);

        // 更新自习室座位计数
        StudyRoom room = roomRepository.findById(request.getRoomId()).get();
        room.setTotalSeats(room.getTotalSeats() + 1);
        roomRepository.save(room);

        // 更新布隆过滤器
        seatBloomFilter.add(seat.getId());

        // 清理缓存
        redisTemplate.delete(CACHE_ROOM_LIST + ":" + room.getFloor() + ":" + room.getStatus());

        log.info("座位添加: roomId={}, seatCode={}, id={}",
                request.getRoomId(), request.getSeatCode(), seat.getId());
        return toSeatVO(seat);
    }

    @Override
    @Transactional
    @CacheEvict(value = "rooms", allEntries = true)
    public SeatVO updateSeat(Long seatId, SeatUpdateRequest request) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new BizException(10004, "座位不存在"));

        // 如果要改为维修/禁用，检查是否有进行中的预约
        if (request.getStatus() != null && request.getStatus() != 1) {
            boolean hasActive = reservationMapper.findBySeatAndTimeRange(
                    seatId, LocalDate.now(), LocalTime.MIN, LocalTime.MAX)
                    .stream()
                    .anyMatch(r -> r.getStatus() == 1 || r.getStatus() == 2);
            if (hasActive) {
                throw new BizException(30003, "该座位有进行中的预约，无法修改为不可用状态");
            }
        }

        if (request.getStatus() != null) seat.setStatus(request.getStatus());
        if (request.getHasPower() != null) seat.setHasPower(request.getHasPower());
        if (request.getIsWindow() != null) seat.setIsWindow(request.getIsWindow());
        if (request.getIsSingle() != null) seat.setIsSingle(request.getIsSingle());

        seat = seatRepository.save(seat);

        // 删除Redis座位缓存
        redisTemplate.delete(CACHE_SEAT_STATUS + seatId);

        StudyRoom room = roomRepository.findById(seat.getRoomId()).orElse(null);
        if (room != null) {
            redisTemplate.delete(CACHE_ROOM_LIST + ":" + room.getFloor() + ":" + room.getStatus());
        }

        log.info("座位更新: id={}, status={}", seatId, seat.getStatus());
        return toSeatVO(seat);
    }

    @Override
    @Transactional
    public void deleteSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new BizException(10004, "座位不存在"));

        // 有历史预约记录的建议禁用而非物理删除
        if (seat.getStatus() != 3) {
            seat.setStatus(3); // 禁用
            seatRepository.save(seat);
            log.info("座位已禁用（不建议物理删除）: id={}", seatId);
        }
    }

    // ==================== 辅助方法 ====================

    /** 将实体转为VO */
    private SeatVO toSeatVO(Seat seat) {
        return SeatVO.builder()
                .seatId(seat.getId())
                .seatCode(seat.getSeatCode())
                .roomId(seat.getRoomId())
                .status(seat.getStatus())
                .statusText(statusText(seat.getStatus()))
                .hasPower(seat.getHasPower())
                .isWindow(seat.getIsWindow())
                .isSingle(seat.getIsSingle())
                .available(seat.getStatus() == 1)
                .currentStatus(seat.getStatus() == 1 ? "AVAILABLE" :
                        seat.getStatus() == 2 ? "MAINTENANCE" : "DISABLED")
                .build();
    }

    private String statusText(int status) {
        return switch (status) {
            case 1 -> "可预约";
            case 2 -> "维修中";
            case 3 -> "已禁用";
            default -> "未知";
        };
    }
}
