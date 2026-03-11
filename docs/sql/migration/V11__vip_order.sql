-- ============================================================
-- V11: 支付宝订单表
-- ============================================================

CREATE TABLE IF NOT EXISTS `vip_order` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`        VARCHAR(64)  NOT NULL COMMENT '系统订单号，全局唯一',
    `user_id`         BIGINT       NOT NULL COMMENT '购买用户 ID',
    `plan_id`         VARCHAR(32)  NOT NULL COMMENT '套餐 ID（vip_month/vip_year/vip_lifetime）',
    `plan_name`       VARCHAR(64)  NOT NULL COMMENT '套餐名称（快照）',
    `amount`          DECIMAL(10,2) NOT NULL COMMENT '应付金额（元）',
    `duration_days`   INT          NOT NULL COMMENT '套餐时长（天，9999=永久）',
    `status`          VARCHAR(16)  NOT NULL DEFAULT 'pending'
                      COMMENT '状态: pending/paid/failed/refunded',
    `alipay_trade_no` VARCHAR(64)  DEFAULT NULL COMMENT '支付宝交易号',
    `pay_time`        DATETIME     DEFAULT NULL COMMENT '支付完成时间',
    `notify_raw`      TEXT         DEFAULT NULL COMMENT '支付宝原始回调报文',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP 购买订单';
