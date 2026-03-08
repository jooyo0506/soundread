-- ==========================================
-- AI 情感配音指令库库表设计 (动态化)
-- ==========================================

-- 1. 指令大类表 (例如: 广播剧、短视频等)
CREATE TABLE IF NOT EXISTS `ai_prompt_category` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称 (如: 广播剧)',
  `icon` VARCHAR(50) NOT NULL COMMENT '分类图标 (如: fas fa-headphones-alt)',
  `sort_order` INT DEFAULT 0 COMMENT '排序权重',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI指令库-分类表';

-- 2. 指令具体的角色设定表 (例如: 悬疑惊悚、傲娇毒舌等)
CREATE TABLE IF NOT EXISTS `ai_prompt_role` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `category_id` BIGINT NOT NULL COMMENT '归属分类的ID',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称 (如: 悬疑惊悚)',
  `description` VARCHAR(255) COMMENT '前端UI展示的短描述 (如: 压低声音、带点气声、放缓、偶尔颤音)',
  `tags` VARCHAR(255) NOT NULL COMMENT '实际传给大模型的详细Prompt Tags',
  `sort_order` INT DEFAULT 0 COMMENT '排序权重',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_category_id (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI指令库-角色设定表';


-- ==========================================
-- 初始数据填充 (基于原有前端硬编码数据)
-- ==========================================

-- 插入: 广播剧
INSERT INTO `ai_prompt_category` (`id`, `name`, `icon`, `sort_order`) VALUES (1, '广播剧', 'fas fa-headphones-alt', 10);
INSERT INTO `ai_prompt_role` (`category_id`, `name`, `description`, `tags`, `sort_order`) VALUES
(1, '悬疑惊悚', '压低声音、带点气声、放缓、偶尔颤音', '压低声音、带点气声、语速放缓、偶尔颤音', 10),
(1, '傲娇毒舌', '上扬、不屑冷笑、稍快、偶尔哼声', '语气上扬、带不屑和冷笑、语速稍快、偶尔哼声', 20),
(1, '绝望崩溃', '哭腔加重、呼吸急促、声音颤抖', '哭腔加重、呼吸急促、声音颤抖、断断续续', 30),
(1, '绿茶白月光', '极度温柔、委屈无辜感、细语', '极度温柔、带委屈和无辜感、语速慢、轻声细语', 40),
(1, '霸道总裁', '低沉、命令式、咬字干脆、慵懒', '语气低沉、命令式、咬字干脆、略带慵懒', 50),
(1, '腹黑反派', '阴险冷笑、平稳拖长、偶尔轻哼', '阴险冷笑、语速慢而平稳、尾音拖长、偶尔轻哼', 60),
(1, '病娇', '忽冷忽热、甜蜜中带威胁、诡异', '忽冷忽热、甜蜜中带威胁、语调诡异、笑声渗人', 70),
(1, '御姐女王', '强势、咬字清晰、慵懒命令', '语气强势、咬字清晰、带点慵懒或命令感', 80);

-- 插入: 领域专家
INSERT INTO `ai_prompt_category` (`id`, `name`, `icon`, `sort_order`) VALUES (2, '领域专家', 'fas fa-user-md', 20);
INSERT INTO `ai_prompt_role` (`category_id`, `name`, `description`, `tags`, `sort_order`) VALUES
(2, '温柔安抚师', '轻柔、关怀、共情倾听', '语速慢、声音轻柔、带关怀、偶尔停顿倾听', 10),
(2, '专业科普', '咬字清晰、平稳自信、逻辑强', '咬字清晰、语气平稳自信、语速适中、逻辑性强', 20),
(2, '严厉督导', '加重、严肃、强调关键词', '语气加重、严肃、语速变快、强调关键词', 30),
(2, '心理咨询', '温和、共情、开放式、舒缓', '温和、共情、多用开放式提问、语速舒缓', 40),
(2, '健身教练', '坚定有力、语速有节奏', '坚定有力、语速有节奏、必要时严厉', 50);

-- 插入: 教育助手
INSERT INTO `ai_prompt_category` (`id`, `name`, `icon`, `sort_order`) VALUES (3, '教育助手', 'fas fa-chalkboard-teacher', 30);
INSERT INTO `ai_prompt_role` (`category_id`, `name`, `description`, `tags`, `sort_order`) VALUES
(3, '元气鼓励', '充满活力、语调上扬、语速快', '充满活力、语调上扬、语速快', 10),
(3, '恨铁不成钢', '微微叹气、无奈笑、重复重点', '微微叹气、语速变快、带点无奈的笑、重复重点', 20),
(3, '循循善诱', '停顿多、启发性、慢慢引导', '停顿较多、语气充满启发性、语速慢', 30),
(3, '严厉教师', '严肃、咬字重、停顿施压', '严肃、语速适中、咬字重、停顿施加压力', 40),
(3, '幽默讲师', '调侃轻松、语速多变、适当笑声', '调侃、轻松、语速多变、适时笑声', 50),
(3, '考前押题', '制造紧张感、重点突出、神秘', '语速快、重点突出、带神秘感', 60);

-- 插入: 短视频
INSERT INTO `ai_prompt_category` (`id`, `name`, `icon`, `sort_order`) VALUES (4, '短视频', 'fas fa-video', 40);
INSERT INTO `ai_prompt_role` (`category_id`, `name`, `description`, `tags`, `sort_order`) VALUES
(4, '悬念开场', '夸张、神秘、慢速带气声', '夸张、神秘、语速慢、带气声', 10),
(4, '激情带货', '语速极快、煽动性、语调高昂', '语速极快、充满煽动性、语调高昂', 20),
(4, '影视解说', '节奏固定、冷面滑稽幽默', '语速适中、节奏固定、冷面幽默', 30),
(4, '阴阳怪气', '语调扭曲、故意拖长、不屑冷笑', '语调扭曲、故意拖长、带不屑冷笑', 40),
(4, '情感语录', '慢速温柔、沙哑饱满、适当停顿', '语速慢、声音温柔带沙哑、情感饱满、适当停顿', 50),
(4, '科技评测', '语速快、专业兴奋、数据加重', '语速快、专业、带兴奋、关键数据加重音', 60);

-- 插入: 游戏 NPC
INSERT INTO `ai_prompt_category` (`id`, `name`, `icon`, `sort_order`) VALUES (5, '游戏 NPC', 'fas fa-gamepad', 50);
INSERT INTO `ai_prompt_role` (`category_id`, `name`, `description`, `tags`, `sort_order`) VALUES
(5, '谄媚商人', '语速快、笑声、语调上扬、讨好', '语速快、带笑声、语调上扬', 10),
(5, '濒死战士', '呼吸急促、断续、虚弱喘息', '呼吸急促、断断续续、虚弱喘息、带咳嗽', 20),
(5, '酒馆老板', '豪爽热情、方言感、压低声音', '豪爽、热情、带方言感、偶尔低声', 30),
(5, '神秘先知', '低沉缓慢、回音效果、双关', '低沉、缓慢、带回音效果、语带双关', 40),
(5, '恶魔反派', '邪恶低沉、威胁、笑声渗人', '邪恶、低沉、威胁、语速慢、笑声渗人', 50);
