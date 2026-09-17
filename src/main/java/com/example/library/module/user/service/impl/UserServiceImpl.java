package com.example.library.module.user.service.impl;

import com.example.library.common.exception.BizException;
import com.example.library.module.user.dto.StudentProfileVO;
import com.example.library.module.user.entity.Student;
import com.example.library.module.user.repository.StudentRepository;
import com.example.library.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现 — 个人中心相关的查询与修改操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int ERR_CREDENTIALS = 11007;
    private static final int ERR_PHONE_EXISTS = 11002;

    /**
     * 获取当前学生信息 — 脱敏手机号 + 展示爽约状态。
     */
    @Override
    public StudentProfileVO getProfile(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BizException(10004, "用户不存在"));

        return StudentProfileVO.builder()
                .studentId(student.getId())
                .studentNo(student.getStudentNo())
                .name(student.getName())
                .phone(maskPhone(student.getPhone()))
                .violationCount(student.getViolationCount())
                .bannedUntil(student.getBannedUntil())
                .canReserve(isCanReserve(student))
                .todayReservationCount(0) // 可在调用方查询填充
                .build();
    }

    /**
     * 更新手机号 — 需要输入密码验证身份。
     */
    @Override
    @Transactional
    public void updatePhone(Long studentId, String phone, String password) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BizException(10004, "用户不存在"));

        // 验证密码
        if (!passwordEncoder.matches(password, student.getPasswordHash())) {
            throw new BizException(ERR_CREDENTIALS, "原密码错误");
        }

        // 校验手机号唯一性
        if (studentRepository.existsByPhone(phone)) {
            throw new BizException(ERR_PHONE_EXISTS, "手机号已被使用");
        }

        student.setPhone(phone);
        studentRepository.save(student);
        log.info("学生手机号更新: id={}", studentId);
    }

    /**
     * 修改密码 — 验证旧密码后更新。
     */
    @Override
    @Transactional
    public void changePassword(Long studentId, String oldPassword, String newPassword) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BizException(10004, "用户不存在"));

        if (!passwordEncoder.matches(oldPassword, student.getPasswordHash())) {
            throw new BizException(ERR_CREDENTIALS, "原密码错误");
        }

        student.setPasswordHash(passwordEncoder.encode(newPassword));
        studentRepository.save(student);
        log.info("学生密码修改: id={}", studentId);
    }

    private boolean isCanReserve(Student s) {
        return s.getBannedUntil() == null ||
                s.getBannedUntil().isBefore(java.time.LocalDateTime.now());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
