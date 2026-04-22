-- 创建数据库（如已存在可跳过）
CREATE DATABASE IF NOT EXISTS `carbonpulse` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `carbonpulse`;

-- ======================================================
-- 1. 用户表
-- ======================================================
CREATE TABLE `user`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`         VARCHAR(50)     NOT NULL COMMENT '用户名（手机号/邮箱）',
    `password`         VARCHAR(255)    NOT NULL COMMENT '加密密码',
    `nickname`         VARCHAR(50)     NOT NULL COMMENT '昵称',
    `avatar`           VARCHAR(255)             DEFAULT NULL COMMENT '头像URL',
    `total_carbon`     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '累计减排量（克）',
    `consecutive_days` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '连续打卡天数',
    `last_record_date` DATE                     DEFAULT NULL COMMENT '最后记录日期',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ======================================================
-- 2. 行为类型配置表
-- ======================================================
CREATE TABLE `behavior_type`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '行为类型ID',
    `name`          VARCHAR(50)     NOT NULL COMMENT '行为名称',
    `unit`          VARCHAR(20)     NOT NULL COMMENT '单位（次/公里/分钟等）',
    `carbon_factor` INT UNSIGNED    NOT NULL COMMENT '每单位减排系数（克）',
    `icon`          VARCHAR(255)             DEFAULT NULL COMMENT '图标URL',
    `sort`          INT             NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='行为类型配置表';

-- ======================================================
-- 3. 行为记录表
-- ======================================================
CREATE TABLE `behavior_record`
(
    `id`               BIGINT UNSIGNED         NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`          BIGINT UNSIGNED         NOT NULL COMMENT '用户ID',
    `behavior_type_id` BIGINT UNSIGNED         NOT NULL COMMENT '行为类型ID',
    `value`            DECIMAL(10, 2) UNSIGNED NOT NULL COMMENT '数量',
    `carbon_reduction` INT UNSIGNED            NOT NULL COMMENT '本次减排量（克）',
    `record_date`      DATE                    NOT NULL COMMENT '记录日期',
    `create_time`      DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `record_date`),
    KEY `idx_behavior_type` (`behavior_type_id`),
    CONSTRAINT `fk_br_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_br_behavior_type` FOREIGN KEY (`behavior_type_id`) REFERENCES `behavior_type` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='行为记录表';

-- ======================================================
-- 4. 社区动态表
-- ======================================================
CREATE TABLE `post`
(
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '动态ID',
    `user_id`             BIGINT UNSIGNED NOT NULL COMMENT '发布用户ID',
    `content`             VARCHAR(500)    NOT NULL COMMENT '文字内容',
    `images`              JSON                     DEFAULT NULL COMMENT '图片URL列表（JSON数组）',
    `related_behavior_id` BIGINT UNSIGNED          DEFAULT NULL COMMENT '关联的行为记录ID',
    `like_count`          INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count`       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '评论数',
    `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_user` (`user_id`),
    CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_post_behavior` FOREIGN KEY (`related_behavior_id`) REFERENCES `behavior_record` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='社区动态表';

-- ======================================================
-- 5. 评论表
-- ======================================================
CREATE TABLE `comment`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `post_id`     BIGINT UNSIGNED NOT NULL COMMENT '动态ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '评论用户ID',
    `content`     VARCHAR(200)    NOT NULL COMMENT '评论内容',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_post` (`post_id`),
    KEY `idx_user` (`user_id`),
    CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='评论表';

-- ======================================================
-- 6. 点赞记录表
-- ======================================================
CREATE TABLE `like_record`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `post_id`     BIGINT UNSIGNED NOT NULL COMMENT '动态ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '点赞用户ID',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
    KEY `idx_post` (`post_id`),
    KEY `idx_user` (`user_id`),
    CONSTRAINT `fk_like_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='点赞记录表';

-- ======================================================
-- 7. 挑战活动表
-- ======================================================
CREATE TABLE `challenge`
(
    `id`          BIGINT UNSIGNED                         NOT NULL AUTO_INCREMENT COMMENT '挑战ID',
    `title`       VARCHAR(100)                            NOT NULL COMMENT '挑战标题',
    `description` VARCHAR(500)                            NOT NULL COMMENT '挑战描述',
    `start_time`  DATETIME                                NOT NULL COMMENT '开始时间',
    `end_time`    DATETIME                                NOT NULL COMMENT '结束时间',
    `goal_type`   ENUM ('total_carbon', 'behavior_count') NOT NULL COMMENT '目标类型（总减排量/特定行为次数）',
    `goal_value`  INT UNSIGNED                            NOT NULL COMMENT '目标值',
    `reward`      VARCHAR(100)                                     DEFAULT NULL COMMENT '奖励描述',
    `create_time` DATETIME                                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='挑战活动表';

-- ======================================================
-- 8. 用户参与挑战表
-- ======================================================
CREATE TABLE `user_challenge`
(
    `id`            BIGINT UNSIGNED                        NOT NULL AUTO_INCREMENT COMMENT '参与记录ID',
    `user_id`       BIGINT UNSIGNED                        NOT NULL COMMENT '用户ID',
    `challenge_id`  BIGINT UNSIGNED                        NOT NULL COMMENT '挑战ID',
    `progress`      INT UNSIGNED                           NOT NULL DEFAULT 0 COMMENT '当前完成量',
    `status`        ENUM ('active', 'completed', 'failed') NOT NULL DEFAULT 'active' COMMENT '状态',
    `join_time`     DATETIME                               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    `complete_time` DATETIME                                        DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_challenge` (`user_id`, `challenge_id`),
    KEY `idx_challenge` (`challenge_id`),
    CONSTRAINT `fk_uc_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_uc_challenge` FOREIGN KEY (`challenge_id`) REFERENCES `challenge` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户参与挑战表';

-- ======================================================
-- 9. 通知表
-- ======================================================
CREATE TABLE `notification`
(
    `id`          BIGINT UNSIGNED                                 NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id`     BIGINT UNSIGNED                                 NOT NULL COMMENT '接收用户ID',
    `type`        ENUM ('like', 'comment', 'challenge', 'system') NOT NULL COMMENT '通知类型',
    `title`       VARCHAR(100)                                    NOT NULL COMMENT '通知标题',
    `content`     VARCHAR(500)                                    NOT NULL COMMENT '通知内容',
    `related_id`  BIGINT UNSIGNED                                          DEFAULT NULL COMMENT '关联ID（如动态ID、挑战ID）',
    `is_read`     TINYINT(1)                                      NOT NULL DEFAULT 0 COMMENT '是否已读',
    `create_time` DATETIME                                        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`, `create_time`),
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='通知表';



-- ======================================================
-- ======================================================
-- ======================================================
-- ======================================================


-- 密码统一使用 BCrypt 加密后的 "123456"，这里用占位符表示，实际使用时需替换
INSERT INTO `user` (`username`, `password`, `nickname`, `avatar`, `total_carbon`, `consecutive_days`,
                    `last_record_date`)
VALUES ('alex.chen@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '绿行侠',
        'https://example.com/avatar1.png', 12500, 7, '2026-03-20'),
       ('bella.wang@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '低碳生活家',
        'https://example.com/avatar2.png', 8700, 4, '2026-03-20'),
       ('chris.li@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '骑行风',
        'https://example.com/avatar3.png', 15600, 12, '2026-03-20'),
       ('diana.zhao@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '零废弃达人',
        'https://example.com/avatar4.png', 6200, 2, '2026-03-19'),
       ('ethan.liu@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '地球守卫者',
        'https://example.com/avatar5.png', 23400, 21, '2026-03-20'),
       ('fiona.sun@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '小绿芽',
        'https://example.com/avatar6.png', 4100, 3, '2026-03-18'),
       ('george.zhou@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '环保工程师',
        'https://example.com/avatar7.png', 19800, 9, '2026-03-20'),
       ('hannah.wu@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '森林之友',
        'https://example.com/avatar8.png', 9500, 6, '2026-03-20'),
       ('ian.xu@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '碳中和先生',
        'https://example.com/avatar9.png', 5200, 1, '2026-03-20'),
       ('julia.chen@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '绿色梦想家',
        'https://example.com/avatar10.png', 14300, 8, '2026-03-19');

INSERT INTO `behavior_type` (`name`, `unit`, `carbon_factor`, `icon`, `sort`)
VALUES ('步行', '公里', 100, 'https://example.com/icons/walk.png', 1),
       ('骑行', '公里', 50, 'https://example.com/icons/bike.png', 2),
       ('自带杯', '次', 20, 'https://example.com/icons/cup.png', 3),
       ('关灯一小时', '次', 30, 'https://example.com/icons/light.png', 4),
       ('无纸化办公', '次', 150, 'https://example.com/icons/paperless.png', 5);


-- 日期：2026-03-15 至 2026-03-21，随机分配
INSERT INTO `behavior_record` (`user_id`, `behavior_type_id`, `value`, `carbon_reduction`, `record_date`)
VALUES
-- 用户1 (id=1)
(1, 1, 3.5, 350, '2026-03-20'),
(1, 2, 5.2, 260, '2026-03-19'),
(1, 3, 2, 40, '2026-03-18'),
(1, 1, 2.0, 200, '2026-03-16'),
-- 用户2
(2, 1, 2.8, 280, '2026-03-20'),
(2, 3, 1, 20, '2026-03-20'),
(2, 4, 1, 30, '2026-03-17'),
(2, 2, 3.0, 150, '2026-03-15'),
-- 用户3
(3, 2, 8.0, 400, '2026-03-20'),
(3, 1, 4.2, 420, '2026-03-19'),
(3, 2, 6.5, 325, '2026-03-18'),
(3, 3, 1, 20, '2026-03-17'),
(3, 5, 1, 150, '2026-03-16'),
-- 用户4
(4, 3, 3, 60, '2026-03-19'),
(4, 4, 2, 60, '2026-03-18'),
(4, 1, 1.5, 150, '2026-03-15'),
-- 用户5
(5, 1, 6.0, 600, '2026-03-20'),
(5, 2, 12.0, 600, '2026-03-20'),
(5, 3, 2, 40, '2026-03-19'),
(5, 4, 1, 30, '2026-03-18'),
(5, 1, 3.2, 320, '2026-03-17'),
-- 用户6
(6, 1, 1.2, 120, '2026-03-18'),
(6, 2, 2.0, 100, '2026-03-18'),
(6, 3, 1, 20, '2026-03-15'),
-- 用户7
(7, 2, 9.5, 475, '2026-03-20'),
(7, 1, 5.0, 500, '2026-03-19'),
(7, 3, 3, 60, '2026-03-19'),
(7, 5, 2, 300, '2026-03-18'),
(7, 2, 4.0, 200, '2026-03-17'),
-- 用户8
(8, 1, 2.5, 250, '2026-03-20'),
(8, 2, 3.8, 190, '2026-03-20'),
(8, 3, 2, 40, '2026-03-19'),
(8, 4, 1, 30, '2026-03-16'),
-- 用户9
(9, 1, 1.0, 100, '2026-03-20'),
(9, 2, 2.5, 125, '2026-03-20'),
(9, 3, 1, 20, '2026-03-19'),
-- 用户10
(10, 1, 4.0, 400, '2026-03-19'),
(10, 2, 7.0, 350, '2026-03-18'),
(10, 3, 2, 40, '2026-03-18'),
(10, 4, 2, 60, '2026-03-17'),
(10, 1, 3.0, 300, '2026-03-16');

-- 注意：related_behavior_id 可选，这里用 NULL 或实际已存在的记录ID
INSERT INTO `post` (`user_id`, `content`, `images`, `related_behavior_id`, `like_count`, `comment_count`, `create_time`)
VALUES (3, '今天骑行8公里上班，空气好心情也好！', '[
  "https://example.com/img1.jpg"
]', (SELECT id FROM behavior_record WHERE user_id = 3 AND behavior_type_id = 2 AND record_date = '2026-03-20' LIMIT 1),
        12, 3, '2026-03-20 08:30:00'),
       (5, '连续一周低碳出行，累计减排超过2kg！', '[
         "https://example.com/img2.jpg"
       ]', NULL, 24, 5, '2026-03-20 20:15:00'),
       (1, '自带杯买咖啡，减碳20g，小事也能汇聚成海。', NULL, (SELECT id
                                                            FROM behavior_record
                                                            WHERE user_id = 1
                                                              AND behavior_type_id = 3
                                                              AND record_date = '2026-03-18'
                                                            LIMIT 1), 8, 2, '2026-03-18 10:00:00'),
       (7, '今天在公司推行无纸化办公，大家都很支持！', '[
         "https://example.com/img3.jpg"
       ]', (SELECT id
            FROM behavior_record
            WHERE user_id = 7
              AND behavior_type_id = 5
              AND record_date = '2026-03-18'
            LIMIT 1), 35, 7, '2026-03-18 14:20:00'),
       (2, '晚上散步3公里，顺便捡起路边垃圾，环保又健康。', NULL, (SELECT id
                                                                FROM behavior_record
                                                                WHERE user_id = 2
                                                                  AND behavior_type_id = 1
                                                                  AND record_date = '2026-03-20'
                                                                LIMIT 1), 15, 4, '2026-03-20 19:45:00'),
       (8, '关灯一小时活动，我和室友一起参与，省电又浪漫。', NULL, (SELECT id
                                                                 FROM behavior_record
                                                                 WHERE user_id = 8
                                                                   AND behavior_type_id = 4
                                                                   AND record_date = '2026-03-16'
                                                                 LIMIT 1), 6, 1, '2026-03-16 21:00:00'),
       (10, '周末骑行去郊外，单程12公里，感觉太棒了！', '[
         "https://example.com/img4.jpg"
       ]', (SELECT id
            FROM behavior_record
            WHERE user_id = 10
              AND behavior_type_id = 2
              AND record_date = '2026-03-18'
            LIMIT 1), 18, 2, '2026-03-18 17:30:00'),
       (4, '今天开始记录碳足迹，希望坚持100天！', NULL, NULL, 5, 0, '2026-03-19 09:00:00'),
       (6, '发现附近新开了一家素食餐厅，健康又环保。', '[
         "https://example.com/img5.jpg"
       ]', NULL, 9, 2, '2026-03-18 12:10:00'),
       (9, '用APP记录一周，发现步行最多，要继续保持。', NULL, NULL, 3, 1, '2026-03-20 22:00:00');

INSERT INTO `comment` (`post_id`, `user_id`, `content`, `create_time`)
VALUES (1, 2, '骑车上班好棒！我也想试试。', '2026-03-20 09:15:00'),
       (1, 5, '下次一起组队啊！', '2026-03-20 10:00:00'),
       (1, 8, '我也在坚持骑行，加油！', '2026-03-20 11:30:00'),
       (2, 1, '太厉害了，向你学习！', '2026-03-20 20:30:00'),
       (2, 3, '我这一周也减排了1.5kg，继续努力。', '2026-03-20 21:00:00'),
       (2, 9, '大家一起来打卡！', '2026-03-21 08:00:00'),
       (2, 7, '求带！', '2026-03-21 09:20:00'),
       (2, 10, '这个活动我也有参与，一起加油！', '2026-03-21 10:15:00'),
       (3, 4, '自带杯确实方便，我也常带。', '2026-03-18 11:00:00'),
       (3, 6, '我每次买咖啡都自带杯，还能优惠。', '2026-03-18 14:00:00'),
       (4, 1, '无纸化办公值得推广！', '2026-03-18 15:00:00'),
       (4, 5, '我们公司也在推行，节约纸张。', '2026-03-18 16:30:00'),
       (4, 2, '赞一个！', '2026-03-19 09:00:00'),
       (5, 3, '散步还能捡垃圾，好主意！', '2026-03-20 20:00:00'),
       (5, 8, '向你致敬！', '2026-03-21 07:30:00'),
       (7, 4, '郊外风景一定很美吧？', '2026-03-18 18:00:00'),
       (7, 9, '下次带上我！', '2026-03-19 10:00:00'),
       (9, 1, '素食确实环保，我也喜欢。', '2026-03-18 13:00:00'),
       (10, 5, '坚持记录，数据可视化很有成就感。', '2026-03-20 22:30:00');

-- 为避免重复，使用 INSERT IGNORE 或子查询去重
INSERT IGNORE INTO `like_record` (`post_id`, `user_id`)
VALUES (1, 3),
       (1, 6),
       (1, 9),
       (2, 4),
       (2, 6),
       (2, 8),
       (2, 10),
       (2, 1),
       (3, 2),
       (3, 7),
       (4, 3),
       (4, 5),
       (4, 8),
       (4, 10),
       (5, 1),
       (5, 4),
       (5, 9),
       (6, 2),
       (6, 7),
       (7, 3),
       (7, 5),
       (7, 8),
       (8, 6),
       (8, 9),
       (9, 4),
       (9, 10),
       (10, 2),
       (10, 7);

INSERT INTO `challenge` (`title`, `description`, `start_time`, `end_time`, `goal_type`, `goal_value`, `reward`)
VALUES ('绿色出行周', '一周内骑行或步行累计减排达到2000克', '2026-03-15 00:00:00', '2026-03-21 23:59:59',
        'total_carbon', 2000, '“低碳先锋”勋章'),
       ('无塑生活挑战', '一周内自带杯次数达到5次', '2026-03-18 00:00:00', '2026-03-25 23:59:59', 'behavior_count', 5,
        '“减塑达人”称号'),
       ('无纸化办公月', '记录无纸化办公行为10次', '2026-03-01 00:00:00', '2026-03-31 23:59:59', 'behavior_count', 10,
        '“绿色办公之星”证书');

-- 绿色出行周 部分用户参与
INSERT INTO `user_challenge` (`user_id`, `challenge_id`, `progress`, `status`, `join_time`, `complete_time`)
VALUES (1, 1, 1800, 'active', '2026-03-14 10:00:00', NULL),
       (3, 1, 2750, 'completed', '2026-03-14 09:30:00', '2026-03-20 20:00:00'),
       (5, 1, 2100, 'completed', '2026-03-15 08:00:00', '2026-03-20 19:00:00'),
       (7, 1, 1500, 'active', '2026-03-16 11:00:00', NULL),
       (10, 1, 900, 'active', '2026-03-17 14:00:00', NULL),
-- 无塑生活挑战
       (1, 2, 3, 'active', '2026-03-18 12:00:00', NULL),
       (2, 2, 4, 'active', '2026-03-18 09:00:00', NULL),
       (3, 2, 5, 'completed', '2026-03-18 08:30:00', '2026-03-22 10:00:00'),
       (5, 2, 2, 'active', '2026-03-19 16:00:00', NULL),
-- 无纸化办公月
       (4, 3, 6, 'active', '2026-03-01 09:00:00', NULL),
       (7, 3, 12, 'completed', '2026-03-01 08:00:00', '2026-03-20 09:00:00'),
       (8, 3, 4, 'active', '2026-03-05 10:00:00', NULL);

INSERT INTO `notification` (`user_id`, `type`, `title`, `content`, `related_id`, `is_read`)
VALUES (2, 'like', '有人赞了你的动态', '用户 绿行侠 赞了你的动态“骑行8公里上班”', 1, 0),
       (2, 'comment', '有人评论了你的动态', '用户 骑行风 评论：“一起组队吧！”', 1, 0),
       (5, 'like', '有人赞了你的动态', '用户 低碳生活家 赞了你的动态“连续一周低碳出行”', 2, 1),
       (5, 'comment', '有人评论了你的动态', '用户 地球守卫者 评论：“太厉害了”', 2, 0),
       (1, 'like', '有人赞了你的动态', '用户 骑行风 赞了你的动态“自带杯买咖啡”', 3, 0),
       (7, 'challenge', '挑战进度提醒', '你的“绿色出行周”挑战已完成75%，加油！', 1, 0),
       (3, 'challenge', '挑战完成', '恭喜你完成“绿色出行周”挑战，获得勋章！', 1, 0),
       (1, 'challenge', '挑战报名成功', '你已成功报名“无塑生活挑战”，开始行动吧！', 2, 1),
       (4, 'system', '欢迎加入碳足迹社区', '记录生活，为地球减碳。', NULL, 0),
       (8, 'comment', '有人评论了你的动态', '用户 零废弃达人 评论：“我也在坚持骑行”', 1, 0),
       (10, 'like', '有人赞了你的动态', '用户 绿色梦想家 赞了你的动态“周末骑行”', 7, 0),
       (9, 'challenge', '挑战完成', '恭喜你完成“无纸化办公月”挑战，获得证书！', 3, 0);
-- 分界线
-- 额外的测试数据插入语句
-- 这些数据可以用于开发和测试

-- 1. 添加更多行为类型
INSERT INTO `behavior_type` (`name`, `unit`, `carbon_factor`, `icon`, `sort`)
VALUES ('公共交通', '公里', 30, 'https://example.com/icons/bus.png', 6),
       ('素食一餐', '次', 500, 'https://example.com/icons/vegetarian.png', 7),
       ('垃圾分类', '次', 50, 'https://example.com/icons/recycle.png', 8),
       ('节水淋浴', '分钟', 10, 'https://example.com/icons/shower.png', 9),
       ('二手购物', '次', 200, 'https://example.com/icons/secondhand.png', 10);

-- 2. 添加更多用户（密码都是加密后的"123456"）
INSERT INTO `user` (`username`, `password`, `nickname`, `avatar`, `total_carbon`, `consecutive_days`,
                    `last_record_date`)
VALUES ('kevin.zhang@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '环保小卫士',
        'https://example.com/avatar11.png', 3200, 5, '2026-03-20'),
       ('lily.wang@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '绿色生活家',
        'https://example.com/avatar12.png', 8900, 14, '2026-03-20'),
       ('mike.li@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '骑行达人',
        'https://example.com/avatar13.png', 15200, 8, '2026-03-19'),
       ('nina.chen@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '零碳先锋',
        'https://example.com/avatar14.png', 21000, 25, '2026-03-20'),
       ('oscar.zhao@example.com', '$2a$10$NkZ9XxHjY7qQ8rS9tUvW.eXfGcVbNnMmKjHhGgFfDdSsAaPpOoIiUu', '地球守护者',
        'https://example.com/avatar15.png', 17800, 19, '2026-03-20');

-- 3. 添加更多行为记录（2026-03-15 至 2026-03-21）
INSERT INTO `behavior_record` (`user_id`, `behavior_type_id`, `value`, `carbon_reduction`, `record_date`)
VALUES
-- 用户11 (id=11)
(11, 1, 2.5, 250, '2026-03-20'),
(11, 6, 5.0, 150, '2026-03-20'),  -- 公共交通
(11, 7, 1, 500, '2026-03-19'),    -- 素食一餐
-- 用户12 (id=12)
(12, 2, 8.5, 425, '2026-03-20'),
(12, 3, 2, 40, '2026-03-20'),
(12, 8, 3, 150, '2026-03-19'),    -- 垃圾分类
-- 用户13 (id=13)
(13, 2, 15.0, 750, '2026-03-20'),
(13, 1, 3.8, 380, '2026-03-19'),
(13, 6, 12.0, 360, '2026-03-18'), -- 公共交通
-- 用户14 (id=14)
(14, 1, 8.0, 800, '2026-03-20'),
(14, 2, 20.0, 1000, '2026-03-20'),
(14, 7, 2, 1000, '2026-03-19'),   -- 素食两餐
(14, 9, 10, 100, '2026-03-18'),   -- 节水淋浴10分钟
-- 用户15 (id=15)
(15, 2, 10.5, 525, '2026-03-20'),
(15, 3, 3, 60, '2026-03-20'),
(15, 10, 1, 200, '2026-03-19');
-- 二手购物

-- 4. 添加更多社区动态
INSERT INTO `post` (`user_id`, `content`, `images`, `related_behavior_id`, `like_count`, `comment_count`, `create_time`)
VALUES (11, '今天尝试公共交通出行，感觉很不错！', '[
  "https://example.com/img6.jpg"
]', NULL, 7, 2, '2026-03-20 09:15:00'),
       (12, '坚持垃圾分类一个月了，环境变得更美好！', NULL, NULL, 15, 4, '2026-03-19 14:30:00'),
       (13, '周末骑行50公里，挑战成功！', '[
         "https://example.com/img7.jpg",
         "https://example.com/img8.jpg"
       ]',
        (SELECT id
         FROM behavior_record
         WHERE user_id = 13 AND behavior_type_id = 2 AND record_date = '2026-03-20'
         LIMIT 1),
        28, 6, '2026-03-20 18:45:00'),
       (14, '尝试一周素食，感觉身体更轻盈了！', '[
         "https://example.com/img9.jpg"
       ]',
        (SELECT id
         FROM behavior_record
         WHERE user_id = 14 AND behavior_type_id = 7 AND record_date = '2026-03-19'
         LIMIT 1),
        22, 5, '2026-03-19 12:20:00'),
       (15, '在二手市场淘到宝贝，既环保又省钱！', '[
         "https://example.com/img10.jpg"
       ]',
        (SELECT id
         FROM behavior_record
         WHERE user_id = 15 AND behavior_type_id = 10 AND record_date = '2026-03-19'
         LIMIT 1),
        12, 3, '2026-03-19 16:40:00');

-- 5. 添加更多评论
INSERT INTO `comment` (`post_id`, `user_id`, `content`, `create_time`)
VALUES (11, 1, '公共交通确实环保，我也经常坐地铁！', '2026-03-20 10:00:00'),
       (11, 3, '支持绿色出行！', '2026-03-20 10:30:00'),
       (12, 5, '垃圾分类从我做起！', '2026-03-19 15:00:00'),
       (12, 7, '向你学习！', '2026-03-19 16:20:00'),
       (13, 2, '50公里太厉害了！', '2026-03-20 19:30:00'),
       (13, 4, '下次一起骑行！', '2026-03-20 20:15:00'),
       (14, 6, '素食对健康和环境都好！', '2026-03-19 13:00:00'),
       (14, 8, '我也在尝试素食！', '2026-03-19 14:30:00'),
       (15, 9, '二手购物很有意义！', '2026-03-19 17:20:00'),
       (15, 10, '我也喜欢逛二手市场！', '2026-03-19 18:00:00');

-- 6. 添加更多点赞记录
INSERT IGNORE INTO `like_record` (`post_id`, `user_id`)
VALUES (11, 1),
       (11, 2),
       (11, 3),
       (11, 4),
       (12, 5),
       (12, 6),
       (12, 7),
       (12, 8),
       (12, 9),
       (13, 10),
       (13, 11),
       (13, 12),
       (13, 13),
       (13, 14),
       (14, 15),
       (14, 1),
       (14, 2),
       (14, 3),
       (14, 4),
       (15, 5),
       (15, 6),
       (15, 7),
       (15, 8);

-- 7. 添加更多挑战
INSERT INTO `challenge` (`title`, `description`, `start_time`, `end_time`, `goal_type`, `goal_value`, `reward`)
VALUES ('公共交通周', '一周内使用公共交通累计减排达到1500克', '2026-03-22 00:00:00', '2026-03-28 23:59:59',
        'total_carbon', 1500, '"绿色出行达人"徽章'),
       ('素食挑战', '一周内完成5次素食餐', '2026-03-22 00:00:00', '2026-03-28 23:59:59', 'behavior_count', 5,
        '"素食先锋"称号'),
       ('节水行动', '记录节水行为20次', '2026-03-01 00:00:00', '2026-03-31 23:59:59', 'behavior_count', 20,
        '"节水卫士"证书');

-- 8. 添加更多用户参与挑战记录
INSERT INTO `user_challenge` (`user_id`, `challenge_id`, `progress`, `status`, `join_time`, `complete_time`)
VALUES
-- 公共交通周
(11, 4, 400, 'active', '2026-03-21 09:00:00', NULL),
(12, 4, 150, 'active', '2026-03-21 10:30:00', NULL),
-- 素食挑战
(14, 5, 2, 'active', '2026-03-21 11:00:00', NULL),
(1, 5, 1, 'active', '2026-03-21 14:00:00', NULL),
-- 节水行动
(11, 6, 5, 'active', '2026-03-01 09:00:00', NULL),
(12, 6, 8, 'active', '2026-03-05 10:00:00', NULL),
(13, 6, 12, 'active', '2026-03-10 11:00:00', NULL);

-- 9. 添加更多通知
INSERT INTO `notification` (`user_id`, `type`, `title`, `content`, `related_id`, `is_read`)
VALUES (11, 'like', '有人赞了你的动态', '用户 绿行侠 赞了你的动态"今天尝试公共交通出行"', 11, 0),
       (11, 'comment', '有人评论了你的动态', '用户 骑行风 评论："支持绿色出行！"', 11, 0),
       (12, 'like', '有人赞了你的动态', '用户 地球守卫者 赞了你的动态"坚持垃圾分类一个月"', 12, 1),
       (13, 'challenge', '挑战进度提醒', '你的"公共交通周"挑战已完成25%，加油！', 4, 0),
       (14, 'challenge', '挑战报名成功', '你已成功报名"素食挑战"，开始行动吧！', 5, 1),
       (15, 'system', '新挑战上线', '"节水行动"挑战已开始，快来参与！', 6, 0);

-- 10. 更新用户累计减排量（基于实际行为记录）
UPDATE `user` u
SET u.total_carbon = (SELECT COALESCE(SUM(br.carbon_reduction), 0)
                      FROM behavior_record br
                      WHERE br.user_id = u.id)
WHERE u.id IN (11, 12, 13, 14, 15);

-- 显示数据统计
SELECT '用户总数' as 统计项,
       COUNT(*)   as 数量
FROM `user`
UNION ALL
SELECT '行为记录总数',
       COUNT(*)
FROM `behavior_record`
UNION ALL
SELECT '社区动态总数',
       COUNT(*)
FROM `post`
UNION ALL
SELECT '评论总数',
       COUNT(*)
FROM `comment`
UNION ALL
SELECT '挑战总数',
       COUNT(*)
FROM `challenge`;
