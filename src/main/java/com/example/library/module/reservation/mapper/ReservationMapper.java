package com.example.library.module.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.module.reservation.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 预约 MyBatis Plus Mapper — 复杂查询和原子化SQL
 * BaseMapper 提供基础CRUD：insert / deleteById / updateById / selectById / selectList
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /** 乐观锁更新状态为"已签到" */
    int updateStatusToSignedIn(@Param("id") Long id,
                               @Param("expectedVersion") Integer expectedVersion);

    /** 乐观锁更新状态为"已爽约" */
    int updateStatusToNoShow(@Param("id") Long id,
                             @Param("expectedVersion") Integer expectedVersion);

    /** 乐观锁更新状态为"已完成" */
    int updateStatusToCompleted(@Param("id") Long id,
                                @Param("expectedVersion") Integer expectedVersion);

    /** 乐观锁更新状态为"已取消" */
    int updateStatusToCancelled(@Param("id") Long id,
                                @Param("expectedVersion") Integer expectedVersion);

    /** 查询待签到的超时预约（爽约扫描） */
    List<Reservation> findPendingAndExpired(@Param("reserveDate") LocalDate date,
                                            @Param("deadlineTime") LocalTime deadlineTime,
                                            @Param("limit") int limit);

    /** 查询超时未离座的预约 */
    List<Reservation> findInUseAndExpired(@Param("deadlineTime") LocalTime deadlineTime,
                                          @Param("limit") int limit);

    /** 按条件分页查询（学生传 studentId 只看自己，管理员传 null 看全部） */
    List<Reservation> findByConditions(@Param("date") LocalDate date,
                                       @Param("status") Integer status,
                                       @Param("roomId") Long roomId,
                                       @Param("studentId") Long studentId,
                                       @Param("studentNo") String studentNo,
                                       @Param("onlyNoShow") Boolean onlyNoShow,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    /** 按条件统计总数 */
    long countByConditions(@Param("date") LocalDate date,
                           @Param("status") Integer status,
                           @Param("roomId") Long roomId,
                           @Param("studentId") Long studentId,
                           @Param("studentNo") String studentNo,
                           @Param("onlyNoShow") Boolean onlyNoShow);

    /** 检查学生当日是否有冲突预约 */
    boolean existsConflict(@Param("studentId") Long studentId,
                           @Param("reserveDate") LocalDate date,
                           @Param("startTime") LocalTime startTime,
                           @Param("endTime") LocalTime endTime);

    /** 查询某座位在指定时段的预约（用于只读校验：座位列表、管理后台） */
    List<Reservation> findBySeatAndTimeRange(@Param("seatId") Long seatId,
                                             @Param("reserveDate") LocalDate date,
                                             @Param("startTime") LocalTime startTime,
                                             @Param("endTime") LocalTime endTime);

    /**
     * 查询座位时段冲突并锁定 — 用于创建预约的锁内二次校验。
     * SELECT ... FOR UPDATE 跳过快照读，直接读取最新已提交数据，
     * 并在匹配行（或间隙）上加 Next-Key Lock，阻塞并发 INSERT，
     * 从根本上杜绝 REPEATABLE READ 快照盲区。
     */
    List<Reservation> findBySeatAndTimeRangeForUpdate(@Param("seatId") Long seatId,
                                                       @Param("reserveDate") LocalDate date,
                                                       @Param("startTime") LocalTime startTime,
                                                       @Param("endTime") LocalTime endTime);

    /** 按学号或手机号搜索学生，返回其待签到和使用中的预约 */
    List<Reservation> findByStudentNoOrPhone(@Param("keyword") String keyword,
                                             @Param("offset") int offset,
                                             @Param("size") int size);

    /** 按学号或手机号搜索结果总数 */
    long countByStudentNoOrPhone(@Param("keyword") String keyword);
}
