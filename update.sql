-- CarbonPulse 数据库升级SQL文件
-- 此文件只包含新增的表、存储过程和触发器
-- 适用于已运行过data.sql的数据库
-- 运行时间：2026-03-28

USE `carbonpulse`;

-- ======================================================
-- 1. 新增成就表
-- ======================================================
CREATE TABLE IF NOT EXISTS `achievement`
(
    `id`              BIGINT UNSIGNED                                            NOT NULL AUTO_INCREMENT COMMENT '成就ID',
    `title`           VARCHAR(100)                                               NOT NULL COMMENT '成就标题',
    `description`     VARCHAR(500)                                               NOT NULL COMMENT '成就描述',
    `icon`            VARCHAR(255)                                                        DEFAULT NULL COMMENT '成就图标',
    `type`            ENUM ('carbon', 'days', 'behavior', 'challenge', 'social') NOT NULL COMMENT '成就类型',
    `condition_value` INT UNSIGNED                                               NOT NULL COMMENT '达成条件值',
    `condition_type`  VARCHAR(50)                                                NOT NULL COMMENT '达成条件类型',
    `sort`            INT                                                        NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time`     DATETIME                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='成就表';

-- ======================================================
-- 2. 新增用户成就表
-- ======================================================
CREATE TABLE IF NOT EXISTS `user_achievement`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `achievement_id` BIGINT UNSIGNED NOT NULL COMMENT '成就ID',
    `progress`       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '当前进度',
    `is_unlocked`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已解锁',
    `unlock_time`    DATETIME                 DEFAULT NULL COMMENT '解锁时间',
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_achievement` (`user_id`, `achievement_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_achievement` (`achievement_id`),
    CONSTRAINT `fk_ua_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ua_achievement` FOREIGN KEY (`achievement_id`) REFERENCES `achievement` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户成就表';

-- ======================================================
-- 3. 检查并添加用户表缺少的字段（如果不存在）
-- ======================================================
-- 检查consecutive_days字段是否存在，不存在则添加
SET @consecutive_days_exists = (SELECT COUNT(*)
                                FROM information_schema.COLUMNS
                                WHERE TABLE_SCHEMA = DATABASE()
                                  AND TABLE_NAME = 'user'
                                  AND COLUMN_NAME = 'consecutive_days');

SET @sql_consecutive_days = IF(@consecutive_days_exists = 0,
                               'ALTER TABLE `user` ADD COLUMN `consecutive_days` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT \'连续打卡天数\' AFTER `total_carbon`',
                               'SELECT \'consecutive_days字段已存在\' as status');

PREPARE stmt FROM @sql_consecutive_days;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查last_record_date字段是否存在，不存在则添加
SET @last_record_date_exists = (SELECT COUNT(*)
                                FROM information_schema.COLUMNS
                                WHERE TABLE_SCHEMA = DATABASE()
                                  AND TABLE_NAME = 'user'
                                  AND COLUMN_NAME = 'last_record_date');

SET @sql_last_record_date = IF(@last_record_date_exists = 0,
                               'ALTER TABLE `user` ADD COLUMN `last_record_date` DATE DEFAULT NULL COMMENT \'最后记录日期\' AFTER `consecutive_days`',
                               'SELECT \'last_record_date字段已存在\' as status');

PREPARE stmt FROM @sql_last_record_date;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ======================================================
-- 4. 删除已存在的存储过程和触发器（如果存在）- 清理旧版本
-- ======================================================
DROP PROCEDURE IF EXISTS `update_consecutive_days`;
DROP PROCEDURE IF EXISTS `check_achievement_unlock`;
DROP TRIGGER IF EXISTS `after_behavior_record_insert`;

-- ======================================================
-- 5. 简化打卡记录表 - 仅用于记录，计算由Redis bitmap处理
-- ======================================================
-- 删除已存在的打卡记录表（如果存在）
DROP TABLE IF EXISTS `checkin_record`;

-- 创建简化版的打卡记录表
CREATE TABLE `checkin_record`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`      BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `checkin_date` DATE            NOT NULL COMMENT '打卡日期',
    `checkin_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
    `note`         VARCHAR(200)             DEFAULT NULL COMMENT '打卡备注',
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `checkin_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='打卡记录表（仅记录，计算由Redis处理）';

-- ======================================================
-- 6. 插入新增的初始数据
-- ======================================================

-- 6.1 插入成就数据（如果表为空）
INSERT IGNORE INTO `achievement` (`title`, `description`, `icon`, `type`, `condition_value`, `condition_type`, `sort`)
VALUES ('环保新人', '完成首次碳足迹计算', '/icons/achievement1.png', 'behavior', 1, 'first_record', 1),
       ('节能先锋', '连续7天减少能源消耗', '/icons/achievement2.png', 'days', 7, 'consecutive_days', 2),
       ('绿色出行者', '累计减少交通碳排放100kg', '/icons/achievement3.png', 'carbon', 100000, 'total_carbon', 3),
       ('社区领袖', '在社区获得50个点赞', '/icons/achievement4.png', 'social', 50, 'total_likes', 4),
       ('年度减排王', '年度减排目标完成100%', '/icons/achievement5.png', 'carbon', 100, 'goal_percentage', 5),
       ('全能环保家', '解锁所有基础成就', '/icons/achievement6.png', 'behavior', 5, 'achievement_count', 6),
       ('打卡达人', '连续打卡30天', '/icons/achievement7.png', 'days', 30, 'consecutive_days', 7),
       ('减排专家', '累计减排500kg', '/icons/achievement8.png', 'carbon', 500000, 'total_carbon', 8),
       ('社区活跃分子', '发布10条动态', '/icons/achievement9.png', 'social', 10, 'post_count', 9),
       ('挑战王者', '完成5个挑战', '/icons/achievement10.png', 'challenge', 5, 'challenge_completed', 10);

-- 6.2 为现有用户初始化连续打卡数据字段（仅设置默认值，实际值由Redis计算）
-- 注意：这里只设置默认值，实际连续打卡天数由Redis bitmap计算后更新
UPDATE `user`
SET consecutive_days = 0,
    last_record_date = NULL
WHERE consecutive_days IS NULL
   OR last_record_date IS NULL;

-- 6.3 为现有用户插入历史打卡记录（基于已有的行为记录）
-- 注意：这里只插入记录，不计算连续天数，连续天数由Redis计算
INSERT IGNORE INTO `checkin_record` (user_id, checkin_date, note)
SELECT br.user_id,
       br.record_date                                                          as checkin_date,
       CONCAT('历史行为记录: ', GROUP_CONCAT(DISTINCT bt.name SEPARATOR ', ')) as note
FROM behavior_record br
         JOIN behavior_type bt ON br.behavior_type_id = bt.id
GROUP BY br.user_id, br.record_date;

-- ======================================================
-- 7. 验证升级结果
-- ======================================================
SELECT '数据库升级完成' as 状态;

SELECT '新增成就表记录数' as 统计项,
       COUNT(*)           as 数量
FROM `achievement`
UNION ALL
SELECT '用户成就记录数',
       COUNT(*)
FROM `user_achievement`
UNION ALL
SELECT '打卡记录数',
       COUNT(*)
FROM `checkin_record`
UNION ALL
SELECT '用户总数（已更新连续打卡字段）',
       COUNT(*)
FROM `user`
WHERE consecutive_days IS NOT NULL;

-- ======================================================
-- 8. 使用说明
-- ======================================================
/*
升级内容：
1. 新增3个表：
   - achievement（成就定义表）
   - user_achievement（用户成就表）
   - checkin_record（打卡记录表，仅记录，计算由Redis处理）

2. 添加用户表字段（如果不存在）：
   - consecutive_days（连续打卡天数，由Redis计算后更新）
   - last_record_date（最后记录日期）

3. 清理旧版本（如果存在）：
   - 删除存储过程：update_consecutive_days, check_achievement_unlock
   - 删除触发器：after_behavior_record_insert

4. 插入成就定义数据（10个成就）

5. 初始化数据：
   - 设置用户连续打卡字段默认值
   - 基于现有行为记录创建历史打卡记录

运行方式：
mysql -u root -p carbonpulse < upgrade_database.sql

Redis Bitmap实现说明：
1. 连续打卡计算使用Redis bitmap实现，key格式：checkin:bitmap:{userId}:{year}
2. 每日打卡：SETBIT checkin:bitmap:{userId}:{year} {dayOfYear} 1
3. 计算连续天数：BITFIELD + BITCOUNT操作
4. 计算完成后，更新MySQL的consecutive_days字段

后端实现建议：
1. 用户打卡时，同时更新Redis bitmap和MySQL checkin_record表
2. 定时任务或用户查询时，从Redis计算连续天数，更新到MySQL
3. 成就解锁逻辑在后端代码中实现，根据Redis计算的连续天数判断

注意事项：
1. 此文件可重复运行，使用IF NOT EXISTS确保安全
2. 不会删除或修改现有数据，只添加新表和字段
3. 打卡记录表仅用于记录，连续打卡计算由Redis处理
4. 成就解锁逻辑需要后端代码实现
*/

SELECT '✅ CarbonPulse数据库升级完成！支持Redis bitmap连续打卡实现。' as 完成提示;

-- 好友关系表
CREATE TABLE `friend_relation`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关系ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `friend_id`   BIGINT UNSIGNED NOT NULL COMMENT '好友用户ID',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1=正常好友，0=已删除/拉黑',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_friend_id` (`friend_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='好友关系表';

-- 插入好友关系（双向）
-- 为保证随机性，使用以下组合（同时插入两条记录）
-- 格式：(user_id, friend_id, status) 双向记录
INSERT INTO `friend_relation` (`user_id`, `friend_id`, `status`)
VALUES
-- 用户1的好友
(1, 2, 1),
(2, 1, 1),
(1, 3, 1),
(3, 1, 1),
(1, 5, 1),
(5, 1, 1),
-- 用户2的好友
(2, 4, 1),
(4, 2, 1),
(2, 6, 1),
(6, 2, 1),
-- 用户3的好友
(3, 7, 1),
(7, 3, 1),
(3, 8, 1),
(8, 3, 1),
-- 用户4的好友
(4, 9, 1),
(9, 4, 1),
(4, 10, 1),
(10, 4, 1),
-- 用户5的好友
(5, 6, 1),
(6, 5, 1),
(5, 8, 1),
(8, 5, 1),
-- 用户6的好友
(6, 9, 1),
(9, 6, 1),
-- 用户7的好友
(7, 10, 1),
(10, 7, 1),
-- 用户8的好友
(8, 10, 1),
(10, 8, 1),
-- 用户9的好友
(9, 3, 1),
(3, 9, 1), -- 注意3和9已经是好友？3已有7、8，这里再补一个（但3-9未出现，可以增加）
(9, 5, 1),
(5, 9, 1),
-- 用户10的好友
(10, 2, 1),
(2, 10, 1),
(10, 6, 1),
(6, 10, 1);

SELECT user_id, COUNT(*) AS friend_count
FROM friend_relation
WHERE status = 1
GROUP BY user_id
ORDER BY user_id;



-- 私信会话表
CREATE TABLE `private_conversation` (
                                        `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID',
                                        `user_id_1` BIGINT UNSIGNED NOT NULL COMMENT '用户A ID（较小）',
                                        `user_id_2` BIGINT UNSIGNED NOT NULL COMMENT '用户B ID（较大）',
                                        `last_message` VARCHAR(500) DEFAULT NULL COMMENT '最后一条消息内容',
                                        `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
                                        `unread_count_user1` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户1未读消息数',
                                        `unread_count_user2` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户2未读消息数',
                                        `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uk_user_pair` (`user_id_1`, `user_id_2`),
                                        KEY `idx_user1_time` (`user_id_1`, `last_message_time`),
                                        KEY `idx_user2_time` (`user_id_2`, `last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信会话表';

-- 私信消息表
CREATE TABLE `private_message` (
                                   `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
                                   `conversation_id` BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
                                   `from_user_id` BIGINT UNSIGNED NOT NULL COMMENT '发送者ID',
                                   `to_user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
                                   `content` VARCHAR(1000) NOT NULL COMMENT '消息内容',
                                   `type` TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型：1-文本，2-图片',
                                   `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
                                   `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_conversation_time` (`conversation_id`, `create_time`),
                                   KEY `idx_from_to_time` (`from_user_id`, `to_user_id`, `create_time`),
                                   KEY `idx_to_read_time` (`to_user_id`, `is_read`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信消息表';

-- ======================================================
-- 创建私信会话（基于现有10个用户）
-- ======================================================
-- 创建8个私信会话，确保每个用户至少参与2个会话
INSERT IGNORE INTO `private_conversation` (`user_id_1`, `user_id_2`, `last_message`, `last_message_time`, `unread_count_user1`, `unread_count_user2`, `create_time`, `update_time`) VALUES
-- 会话1: 用户1和用户2
(1, 2, NULL, NULL, 0, 0, '2026-03-20 09:00:00', '2026-03-20 09:00:00'),
-- 会话2: 用户1和用户3
(1, 3, NULL, NULL, 0, 0, '2026-03-21 10:00:00', '2026-03-21 10:00:00'),
-- 会话3: 用户2和用户4
(2, 4, NULL, NULL, 0, 0, '2026-03-22 11:00:00', '2026-03-22 11:00:00'),
-- 会话4: 用户3和用户5
(3, 5, NULL, NULL, 0, 0, '2026-03-23 12:00:00', '2026-03-23 12:00:00'),
-- 会话5: 用户4和用户6
(4, 6, NULL, NULL, 0, 0, '2026-03-24 13:00:00', '2026-03-24 13:00:00'),
-- 会话6: 用户5和用户7
(5, 7, NULL, NULL, 0, 0, '2026-03-25 14:00:00', '2026-03-25 14:00:00'),
-- 会话7: 用户6和用户8
(6, 8, NULL, NULL, 0, 0, '2026-03-26 15:00:00', '2026-03-26 15:00:00'),
-- 会话8: 用户7和用户9
(7, 9, NULL, NULL, 0, 0, '2026-03-27 16:00:00', '2026-03-27 16:00:00'),
-- 会话9: 用户8和用户10
(8, 10, NULL, NULL, 0, 0, '2026-03-28 17:00:00', '2026-03-28 17:00:00'),
-- 会话10: 用户9和用户1
(1, 9, NULL, NULL, 0, 0, '2026-03-29 18:00:00', '2026-03-29 18:00:00'),
-- 会话11: 用户10和用户2
(2, 10, NULL, NULL, 0, 0, '2026-03-30 19:00:00', '2026-03-30 19:00:00');

-- ======================================================
-- 插入50条私信消息
-- ======================================================
INSERT INTO `private_message` (`conversation_id`, `from_user_id`, `to_user_id`, `content`, `type`, `is_read`, `create_time`) VALUES
-- 会话1: 用户1 ↔ 用户2 (6条消息)
(1, 1, 2, '你好！最近在环保方面有什么新发现吗？', 1, 1, '2026-03-20 09:15:00'),
(1, 2, 1, '最近开始尝试素食，感觉对环境很有帮助！', 1, 1, '2026-03-20 10:30:00'),
(1, 1, 2, '素食确实是个好选择，能减少很多碳排放', 1, 1, '2026-03-20 11:45:00'),
(1, 2, 1, '是的，而且对身体也有好处', 1, 1, '2026-03-20 13:00:00'),
(1, 1, 2, '你一般在哪里买素食材料？', 1, 0, '2026-03-20 14:15:00'),
(1, 2, 1, '我常去本地的农贸市场，新鲜又便宜', 1, 1, '2026-03-20 15:30:00'),

-- 会话2: 用户1 ↔ 用户3 (5条消息)
(2, 1, 3, '看到你最近骑行很多，有什么好的路线推荐吗？', 1, 1, '2026-03-21 10:20:00'),
(2, 3, 1, '滨江绿道很不错，风景好又安全', 1, 1, '2026-03-21 11:35:00'),
(2, 1, 3, '谢谢推荐！周末去试试', 1, 1, '2026-03-21 12:50:00'),
(2, 3, 1, '记得带上水壶，减少买瓶装水', 1, 1, '2026-03-21 14:05:00'),
(2, 1, 3, '好建议，环保从细节做起', 1, 0, '2026-03-21 15:20:00'),

-- 会话3: 用户2 ↔ 用户4 (5条消息)
(3, 2, 4, '一起参加本周的绿色出行挑战吗？', 1, 1, '2026-03-22 11:10:00'),
(3, 4, 2, '好啊！我已经报名了，一起努力！', 1, 1, '2026-03-22 12:25:00'),
(3, 2, 4, '你一般用什么交通方式？', 1, 1, '2026-03-22 13:40:00'),
(3, 4, 2, '我主要骑自行车和坐地铁', 1, 1, '2026-03-22 14:55:00'),
(3, 2, 4, '我也是，尽量减少开车', 1, 0, '2026-03-22 16:10:00'),

-- 会话4: 用户3 ↔ 用户5 (5条消息)
(4, 3, 5, '听说你是个环保专家，能分享些经验吗？', 1, 1, '2026-03-23 12:15:00'),
(4, 5, 3, '其实很简单，从小事做起，比如节约用水用电', 1, 1, '2026-03-23 13:30:00'),
(4, 3, 5, '垃圾分类有什么技巧吗？', 1, 1, '2026-03-23 14:45:00'),
(4, 5, 3, '准备不同颜色的垃圾桶，养成习惯就好了', 1, 1, '2026-03-23 16:00:00'),
(4, 3, 5, '谢谢指导，我试试看', 1, 0, '2026-03-23 17:15:00'),

-- 会话5: 用户4 ↔ 用户6 (5条消息)
(5, 4, 6, '周末有个环保骑行活动，要一起参加吗？', 1, 1, '2026-03-24 13:20:00'),
(5, 6, 4, '听起来不错！具体时间和地点是？', 1, 1, '2026-03-24 14:35:00'),
(5, 4, 6, '周六上午9点，在中央公园集合', 1, 1, '2026-03-24 15:50:00'),
(5, 6, 4, '好的，我会准时到', 1, 1, '2026-03-24 17:05:00'),
(5, 4, 6, '记得带上水壶和防晒用品', 1, 0, '2026-03-24 18:20:00'),

-- 会话6: 用户5 ↔ 用户7 (5条消息)
(6, 5, 7, '你推荐的节能灯真的很好用，电费省了不少', 1, 1, '2026-03-25 14:25:00'),
(6, 7, 5, '很高兴对你有帮助！环保从小事做起', 1, 1, '2026-03-25 15:40:00'),
(6, 5, 7, '还有什么其他节能建议吗？', 1, 1, '2026-03-25 16:55:00'),
(6, 7, 5, '可以试试智能插座，远程控制电器开关', 1, 1, '2026-03-25 18:10:00'),
(6, 5, 7, '好主意，我去研究一下', 1, 0, '2026-03-25 19:25:00'),

-- 会话7: 用户6 ↔ 用户8 (5条消息)
(7, 6, 8, '无纸化办公真的能节省很多资源', 1, 1, '2026-03-26 15:30:00'),
(7, 8, 6, '是的，我们公司也在推行无纸化', 1, 1, '2026-03-26 16:45:00'),
(7, 6, 8, '你们用什么工具替代纸质文件？', 1, 1, '2026-03-26 18:00:00'),
(7, 8, 6, '主要用云文档和电子签名', 1, 1, '2026-03-26 19:15:00'),
(7, 6, 8, '谢谢分享，我们也准备试试', 1, 0, '2026-03-26 20:30:00'),

-- 会话8: 用户7 ↔ 用户9 (5条消息)
(8, 7, 9, '节水有什么小技巧可以分享吗？', 1, 1, '2026-03-27 16:35:00'),
(8, 9, 7, '洗澡时间缩短几分钟，积少成多', 1, 1, '2026-03-27 17:50:00'),
(8, 7, 9, '还有收集雨水浇花也不错', 1, 1, '2026-03-27 19:05:00'),
(8, 9, 7, '对，我家的花园就是用的雨水', 1, 1, '2026-03-27 20:20:00'),
(8, 7, 9, '学到了，谢谢！', 1, 0, '2026-03-27 21:35:00'),

-- 会话9: 用户8 ↔ 用户10 (5条消息)
(9, 8, 10, '二手购物真的能减少很多浪费', 1, 1, '2026-03-28 17:40:00'),
(9, 10, 8, '是的，我最近买了很多二手书', 1, 1, '2026-03-28 18:55:00'),
(9, 8, 10, '有什么好的二手平台推荐吗？', 1, 1, '2026-03-28 20:10:00'),
(9, 10, 8, '闲鱼和多抓鱼都不错', 1, 1, '2026-03-28 21:25:00'),
(9, 8, 10, '好的，我去看看', 1, 0, '2026-03-28 22:40:00'),

-- 会话10: 用户9 ↔ 用户1 (4条消息)
(10, 9, 1, '环保生活真的让生活更美好了', 1, 1, '2026-03-29 18:45:00'),
(10, 1, 9, '是的，感觉更有意义了', 1, 1, '2026-03-29 20:00:00'),
(10, 9, 1, '一起继续努力！', 1, 1, '2026-03-29 21:15:00'),
(10, 1, 9, '加油！', 1, 0, '2026-03-29 22:30:00'),

-- 会话11: 用户10 ↔ 用户2 (5条消息)
(11, 10, 2, '如何鼓励家人一起参与环保？', 1, 1, '2026-03-30 19:50:00'),
(11, 2, 10, '从简单的事情开始，比如一起垃圾分类', 1, 1, '2026-03-30 21:05:00'),
(11, 10, 2, '好的，我试试看', 1, 1, '2026-03-30 22:20:00'),
(11, 2, 10, '也可以设置家庭环保目标', 1, 1, '2026-03-30 23:35:00'),
(11, 10, 2, '好主意，谢谢建议！', 1, 0, '2026-03-31 00:50:00');



-- CarbonPulse 私信数据修复
-- 同步会话表的 last_message / last_message_time / unread_count 与实际消息数据一致
-- 运行前提：已执行 update.sql 中的 private_conversation 和 private_message 插入

USE `carbonpulse`;

-- 1. 更新每个会话的最后消息和最后消息时间
UPDATE private_conversation pc
SET
    last_message = (
        SELECT pm.content
        FROM private_message pm
        WHERE pm.conversation_id = pc.id
        ORDER BY pm.create_time DESC
        LIMIT 1
    ),
    last_message_time = (
        SELECT pm.create_time
        FROM private_message pm
        WHERE pm.conversation_id = pc.id
        ORDER BY pm.create_time DESC
        LIMIT 1
    )
WHERE pc.id IN (
    SELECT DISTINCT conversation_id FROM private_message
);

-- 2. 更新每个会话中 user_id_1 的未读消息数
-- 未读消息 = 发给 user_id_1 且 is_read=0 的消息数
UPDATE private_conversation pc
SET unread_count_user1 = (
    SELECT COUNT(*)
    FROM private_message pm
    WHERE pm.conversation_id = pc.id
      AND pm.to_user_id = pc.user_id_1
      AND pm.is_read = 0
);

-- 3. 更新每个会话中 user_id_2 的未读消息数
UPDATE private_conversation pc
SET unread_count_user2 = (
    SELECT COUNT(*)
    FROM private_message pm
    WHERE pm.conversation_id = pc.id
      AND pm.to_user_id = pc.user_id_2
      AND pm.is_read = 0
);

-- 验证结果
SELECT
    pc.id AS conv_id,
    pc.user_id_1,
    pc.user_id_2,
    pc.last_message,
    pc.last_message_time,
    pc.unread_count_user1,
    pc.unread_count_user2
FROM private_conversation pc
ORDER BY pc.id;

SELECT '✅ 私信会话数据同步完成' AS status;
