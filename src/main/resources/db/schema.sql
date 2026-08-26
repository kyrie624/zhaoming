-- 初始化数据库。执行该脚本的 MySQL 账号需要具备 CREATE DATABASE 权限。
CREATE DATABASE IF NOT EXISTS zhaoming
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE zhaoming;

-- 设备每分钟采样明细。执行一次即可；生产环境建议由 DBA 纳入正式迁移流程。
CREATE TABLE IF NOT EXISTS device_measurement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(255) NULL,
    floor_name VARCHAR(255) NULL,
    collected_at DATETIME(3) NOT NULL,
    energy_kwh DECIMAL(20, 6) NULL,
    current_a DECIMAL(20, 6) NULL,
    current_b DECIMAL(20, 6) NULL,
    current_c DECIMAL(20, 6) NULL,
    working TINYINT(1) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_measurement_time (device_id, collected_at),
    KEY idx_measurement_query (device_id, collected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 工作区间：end_at 为空表示当前仍在工作。
CREATE TABLE IF NOT EXISTS device_work_period (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(255) NULL,
    floor_name VARCHAR(255) NULL,
    start_at DATETIME(3) NOT NULL,
    end_at DATETIME(3) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_period_query (device_id, start_at, end_at),
    KEY idx_period_open (device_id, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
