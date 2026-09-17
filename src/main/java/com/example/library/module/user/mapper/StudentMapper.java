package com.example.library.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.module.user.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 学生 MyBatis Plus Mapper — 行级锁更新爽约计数
 * BaseMapper 提供基础CRUD
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /** 行级锁读取爽约计数 */
    Integer selectViolationCountForUpdate(@Param("studentId") Long studentId);

    /** 递增爽约计数并设置惩罚截止时间 */
    int incrementViolationAndBan(@Param("studentId") Long studentId,
                                 @Param("banDays") int banDays,
                                 @Param("threshold") int threshold);

    /** 重置爽约计数 */
    int resetViolationCount(@Param("studentId") Long studentId);
}
