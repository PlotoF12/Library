package com.example.library.module.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.module.reservation.entity.ViolationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 爽约记录 MyBatis Plus Mapper
 * BaseMapper 提供基础CRUD
 */
@Mapper
public interface ViolationRecordMapper extends BaseMapper<ViolationRecord> {

    /** 插入爽约记录（手动映射参数，避免与BaseMapper.insert冲突） */
    int insertRecord(@Param("studentId") Long studentId,
                     @Param("reservationId") Long reservationId,
                     @Param("violationType") Integer violationType,
                     @Param("penaltyStart") LocalDate penaltyStart,
                     @Param("penaltyEnd") LocalDate penaltyEnd);

    /** 统计学生有效惩罚次数（惩罚期未过的） */
    int countActivePenalties(@Param("studentId") Long studentId,
                             @Param("today") LocalDate today);
}
