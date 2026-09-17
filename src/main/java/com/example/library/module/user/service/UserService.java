package com.example.library.module.user.service;

import com.example.library.module.user.dto.StudentProfileVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /** 获取当前学生信息 */
    StudentProfileVO getProfile(Long studentId);

    /** 更新手机号 */
    void updatePhone(Long studentId, String phone, String password);

    /** 修改密码 */
    void changePassword(Long studentId, String oldPassword, String newPassword);
}
