-- =============================================================
-- 校园自习室座位预约管理系统 — 数据库初始化脚本
-- 用于首次部署时手动执行建表
-- =============================================================

CREATE DATABASE IF NOT EXISTS seat_reservation
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
USE seat_reservation;

-- ---- 学生表 ----
CREATE TABLE IF NOT EXISTS student (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    student_no      VARCHAR(20)     NOT NULL UNIQUE             COMMENT '学号',
    name            VARCHAR(50)     NOT NULL                    COMMENT '姓名',
    phone           VARCHAR(20)     NOT NULL UNIQUE             COMMENT '手机号',
    password_hash   VARCHAR(255)    NOT NULL                    COMMENT '密码(BCrypt)',
    violation_count INT             NOT NULL DEFAULT 0          COMMENT '累计爽约次数',
    banned_until    DATETIME       DEFAULT NULL                COMMENT '禁止预约截止时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_banned_until (banned_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生用户表';

-- ---- 管理员表 ----
CREATE TABLE IF NOT EXISTS admin (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    username        VARCHAR(50)     NOT NULL UNIQUE             COMMENT '用户名',
    password_hash   VARCHAR(255)    NOT NULL                    COMMENT '密码(BCrypt)',
    name            VARCHAR(50)     NOT NULL                    COMMENT '真实姓名',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- ---- 自习室表 ----
CREATE TABLE IF NOT EXISTS study_room (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    room_code       VARCHAR(20)     NOT NULL UNIQUE             COMMENT '自习室编号',
    name            VARCHAR(100)    NOT NULL                    COMMENT '自习室名称',
    floor           INT             NOT NULL                    COMMENT '楼层',
    total_seats     INT             NOT NULL DEFAULT 0          COMMENT '总座位数',
    status          TINYINT         NOT NULL DEFAULT 1          COMMENT '1-开放 0-关闭',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_room_code (room_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自习室表';

-- ---- 座位表 ----
CREATE TABLE IF NOT EXISTS seat (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    room_id         BIGINT          NOT NULL                    COMMENT '自习室ID',
    seat_code       VARCHAR(20)     NOT NULL                    COMMENT '座位编号',
    status          TINYINT         NOT NULL DEFAULT 1          COMMENT '1-可用 2-维修中 3-禁用',
    has_power       TINYINT         NOT NULL DEFAULT 0          COMMENT '是否有电源',
    is_window       TINYINT         NOT NULL DEFAULT 0          COMMENT '是否靠窗',
    is_single       TINYINT         NOT NULL DEFAULT 0          COMMENT '是否单座',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_room_seat (room_id, seat_code),
    INDEX idx_room_status (room_id, status),
    INDEX idx_preferences (has_power, is_window, is_single),
    CONSTRAINT fk_seat_room FOREIGN KEY (room_id) REFERENCES study_room(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位表';

-- ---- 预约记录表 ----
CREATE TABLE IF NOT EXISTS reservation (
    id                  BIGINT      PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    student_id          BIGINT      NOT NULL                    COMMENT '学生ID',
    seat_id             BIGINT      NOT NULL                    COMMENT '座位ID',
    room_id             BIGINT      NOT NULL                    COMMENT '自习室ID(冗余)',
    reserve_date        DATE        NOT NULL                    COMMENT '预约日期',
    start_time          TIME        NOT NULL                    COMMENT '开始时间',
    end_time            TIME        NOT NULL                    COMMENT '结束时间',
    status              TINYINT     NOT NULL DEFAULT 1          COMMENT '1-待签到 2-使用中 3-已完成 4-已爽约 5-已取消',
    sign_token          VARCHAR(64) DEFAULT NULL                COMMENT '签到Token',
    actual_sign_time    DATETIME   DEFAULT NULL                COMMENT '实际签到时间',
    actual_leave_time   DATETIME   DEFAULT NULL                COMMENT '实际离座时间',
    version             INT         NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_seat_time (seat_id, reserve_date, start_time, end_time),
    INDEX idx_student_id (student_id),
    INDEX idx_status (status),
    INDEX idx_reserve_datetime (reserve_date, start_time),
    INDEX idx_sign_token (sign_token),
    INDEX idx_student_date (student_id, reserve_date),
    INDEX idx_room_date (room_id, reserve_date),
    CONSTRAINT fk_resv_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_resv_seat FOREIGN KEY (seat_id) REFERENCES seat(id),
    CONSTRAINT fk_resv_room FOREIGN KEY (room_id) REFERENCES study_room(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

-- ---- 爽约惩罚记录表 ----
CREATE TABLE IF NOT EXISTS violation_record (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    student_id      BIGINT          NOT NULL                    COMMENT '学生ID',
    reservation_id  BIGINT          NOT NULL                    COMMENT '关联预约ID',
    violation_type  TINYINT         NOT NULL                    COMMENT '1-未签到爽约 2-超时未离座',
    penalty_start   DATE            NOT NULL                    COMMENT '惩罚开始日期',
    penalty_end     DATE            NOT NULL                    COMMENT '惩罚结束日期',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_penalty (student_id, penalty_end),
    INDEX idx_reservation (reservation_id),
    CONSTRAINT fk_vio_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_vio_resv FOREIGN KEY (reservation_id) REFERENCES reservation(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爽约惩罚记录表';

-- ---- 短信通知记录表 ----
CREATE TABLE IF NOT EXISTS notification_log (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    student_id      BIGINT          NOT NULL                    COMMENT '学生ID',
    phone           VARCHAR(20)     NOT NULL                    COMMENT '手机号',
    type            TINYINT         NOT NULL                    COMMENT '1-预约成功 2-签到提醒 3-爽约 4-超时提醒 5-惩罚通知',
    content         VARCHAR(500)    NOT NULL                    COMMENT '短信内容',
    status          TINYINT         NOT NULL DEFAULT 0          COMMENT '0-待发送 1-已发送 2-失败',
    sent_at         DATETIME       DEFAULT NULL                COMMENT '发送时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_type (student_id, type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信通知记录表';

-- ---- 管理员初始数据 ----
INSERT INTO admin (username, password_hash, name) VALUES
('admin_lib', '$2a$12$c/.sE62W3pTAdolYAmwRQO.T2zhNpO2Xl24klWnD0hMRdWe4IkeQK', '李老师');
-- 默认密码: Admin@456
