-- ============================================================
-- V12: vip_order 表结构补全（旧表缺少新字段）
-- ============================================================

ALTER TABLE `vip_order`
    ADD COLUMN `order_no`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '系统订单号' AFTER `id`,
    ADD COLUMN `plan_name`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '套餐名称快照' AFTER `plan_id`,
    ADD COLUMN `duration_days`   INT           NOT NULL DEFAULT 30  COMMENT '套餐时长(天)' AFTER `plan_name`,
    ADD COLUMN `alipay_trade_no` VARCHAR(64)   DEFAULT NULL COMMENT '支付宝交易号' AFTER `status`,
    ADD COLUMN `pay_time`        DATETIME      DEFAULT NULL COMMENT '支付完成时间' AFTER `alipay_trade_no`,
    ADD COLUMN `notify_raw`      TEXT          DEFAULT NULL COMMENT '支付宝原始回调' AFTER `pay_time`,
    ADD COLUMN `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;

CREATE UNIQUE INDEX `uk_order_no` ON `vip_order` (`order_no`);
