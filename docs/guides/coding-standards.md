# SoundRead 编码规范

> **基准规范**：本规范以 [阿里巴巴 Java 开发手册（黄山版）](https://github.com/alibaba/p3c) 为基准，并在其基础上结合项目实际进行补充约定。
> 适用范围：server（Java）、web（Vue 3）、数据库、Git 提交。
> 所有 AI 辅助代码生成必须遵循本规范。

---

## 一、Java 后端规范（遵循阿里巴巴 Java 开发手册）

### 1.1 【命名规范】

> **阿里规范摘录**（强制项）

| 规则 | 说明 | 示例 |
|------|------|------|
| 类名 | UpperCamelCase，避免下划线 | `StudioService`, `TtsV2Controller` |
| 方法名 | lowerCamelCase | `generateContent()`, `publishProject()` |
| 参数名 | lowerCamelCase，禁止单字符（循环除外）| `projectId`, `userId` |
| 常量 | 全大写 + 下划线，在常量类或接口中定义 | `MAX_RETRY_COUNT`, `TTS_ENDPOINT` |
| 包名 | 全小写，点分隔，不含大写或下划线 | `com.soundread.service` |
| 接口名 | **不加 I 前缀** | `VoiceService`（非 `IVoiceService`）|
| 实现类 | 接口名 + `Impl` | `VoiceServiceImpl` |
| POJO 类 | 不含 BO/VO/DTO 等作类型后缀（推荐用语义命名）| `CreateRequest`, `ProgressInfo` |
| 布尔变量 | **禁止** `isXxx` 命名（Lombok 生成 getter 会去掉 is 导致序列化异常）| 用 `enable`, `published`, `deleted` |
| 枚举类 | 类名 UpperCamelCase，成员名 UPPER_SNAKE_CASE | `StatusEnum.DRAFT` |
| 抽象类 | 以 `Abstract` 开头 | `AbstractTtsAdapter` |

**补充约定（项目级）：**

| 类型 | 规范 |
|------|------|
| 数据库字段（snake_case） | MyBatis-Plus 自动映射驼峰，无需手动配置 |
| 状态枚举值（字符串）| 全小写，如 `"draft"`, `"creating"`, `"completed"` |
| 日志字段 key | 统一小驼峰，如 `projectId`, `userId`, `textLen` |

### 1.2 【常量规范】

> 【强制】不允许出现魔法值（未经预先定义的常量）直接出现在代码中。

```java
// ✅ 正确：定义常量
private static final int MAX_TEXT_LENGTH = 5000;
private static final String STATUS_DRAFT = "draft";
private static final double AUDIO_SPEED_CPS = 4.5; // 字/秒，TTS 标准时长估算

// ❌ 错误：魔法值
if (text.length() > 5000) { ... }
if ("draft".equals(project.getStatus())) { ... }
long duration = totalChars / 4;  // 4 是什么意思？❌
```

### 1.3 【注释规范】

> **阿里规范**：
> - 【强制】类、类属性、类方法的注释必须使用 Javadoc 规范，使用 `/** 内容 */` 格式，不得使用行注释 `//`。
> - 【强制】所有的抽象方法（包括接口中的方法）必须要用 Javadoc 注释、除了返回值、参数、异常说明外，还必须指出该方法做什么事情，实现什么功能。
> - 【推荐】方法内部单行注释，在被注释语句上方另起一行，使用 `//` 注释。
> - 【推荐】注释的双斜线与注释内容之间有且仅有一个空格。

**本项目补充：所有注释必须使用中文。**

```java
/**
 * 将 AI 生成的原始文本持久化为 StudioSection 段落记录
 *
 * <p>
 * 若 targetSectionIndex 指定了目标段落，则更新现有段落；
 * 否则追加新段落，sectionIndex 自动递增。
 * 方法内会同步更新 AI 交互历史（AiInteraction）。
 * </p>
 *
 * @param project            目标创作工坊项目
 * @param content            AI 生成的原始文本内容
 * @param user               当前操作用户
 * @param userInput          用户的原始输入（用于历史记录）
 * @param targetSectionIndex 目标段落序号（null 时追加新段落）
 */
private void saveGeneratedContent(StudioProject project, String content,
        User user, String userInput, Integer targetSectionIndex) { ... }
```

**日志规范（阿里规范 + 项目约定）：**

```java
// 【强制】不允许直接使用日志系统的 API，必须使用日志框架的 API
// 使用 @Slf4j（Lombok），不手动 new Logger

// 格式：[模块名] 操作描述: key1={} key2={}
log.info("[StudioService] 内容生成完成: projectId={} typeCode={}", id, typeCode);
log.warn("[TTS1] 响应码异常，将降级重试: code={} message={}", code, msg);
log.error("[NovelPipeline] 流水线处理失败: projectId={}", projectId, e);

// 【强制】日志中禁止用占位符拼接字符串（禁止 + 拼接）
log.debug("用户 ID 为 " + userId);           // ❌
log.debug("用户 ID 为: userId={}", userId);  // ✅

// 【强制】禁止使用无意义的占位日志
log.info("info", param);       // ❌ 占位符无意义
log.error("error", e);         // ❌
```

### 1.4 【OOP 规范】

> **阿里规范摘录（强制项）：**

```java
// 【强制】所有的覆写方法，必须加 @Override 注解
@Override
public List<SysVoice> getSupportedVoices(String engine) { ... }

// 【强制】相同参数类型，相同业务含义，才可以使用 Java 的可变参数
// 避免使用可变参数编程，外部接口的入参不允许使用可变参数

// 【强制】不能使用过时的类或方法
// 【强制】Object 的 equals 方法容易抛空指针异常，应使用常量或确定有值的对象来调用
"draft".equals(project.getStatus());  // ✅
project.getStatus().equals("draft");  // ❌ status 可能为 null

// 【强制】所有的 POJO 类属性必须使用包装数据类型（Integer 非 int）
// 【强制】RPC 方法返回值和参数必须使用包装数据类型
// 【推荐】局部变量使用基本数据类型

// 【强制】定义 DO/PO/DTO/VO 等 POJO 类时，不要设定任何属性默认值
// 不在字段上直接 = defaultValue
private Integer progress;       // ✅
private Integer progress = 0;   // ❌
```

### 1.5 【依赖注入规范】

> 阿里规范要求避免循环依赖，本项目统一使用构造函数注入：

```java
// ✅ 推荐（Lombok @RequiredArgsConstructor）
@Service
@RequiredArgsConstructor
public class StudioService {
    private final ProjectMapper projectMapper;
    private final SectionMapper sectionMapper;
    private final LlmRouter llmRouter;
}

// ❌ 禁止（@Autowired 字段注入，不利于测试和循环依赖检测）
@Service
public class StudioService {
    @Autowired
    private ProjectMapper projectMapper;
}
```

### 1.6 【集合处理规范】

> **阿里规范摘录（强制项）：**

```java
// 【强制】只要覆写 equals，就必须覆写 hashCode（Lombok @Data 已自动处理）

// 【强制】不要在 foreach 循环里进行元素的 remove/add 操作
// 如需删除，使用 Iterator 或 Stream filter
List<NovelSegment> segments = new ArrayList<>(allSegments);
segments.removeIf(s -> s.getAudioUrl() == null);  // ✅

// 【强制】集合初始化时，指定集合初始值大小，避免频繁扩容
Map<String, Object> requestBody = new LinkedHashMap<>(8);  // ✅
Map<String, Object> requestBody = new HashMap<>();          // ❌（初始容量未知）

// 【强制】使用 entrySet 遍历 Map，而非 keySet + get
for (Map.Entry<String, Object> entry : map.entrySet()) {   // ✅
    String key = entry.getKey();
    Object value = entry.getValue();
}

// 【强制】Collections.emptyList() 返回的是不可变集合，判断是否为空用 isEmpty()
if (chapters.isEmpty()) { ... }           // ✅
if (chapters.size() == 0) { ... }         // ❌（阿里规范不推荐）

// 【推荐】返回值为集合时，不允许返回 null，应返回空集合
public List<NovelChapter> listChapters(Long projectId) {
    List<NovelChapter> result = chapterMapper.selectList(wrapper);
    return result != null ? result : Collections.emptyList();  // ✅
}
```

### 1.7 【异常处理规范】

> **阿里规范摘录（强制项）：**

```java
// 【强制】不要捕获 Java 类库中定义的继承自 RuntimeException 的运行时异常类
// 例如：IndexOutOfBoundsException, NullPointerException，应从代码逻辑中预防

// 【强制】catch 时要区分稳定代码和非稳定代码
// 对大段代码 try-catch 是不规范的

// 【强制】不能在 finally 块中使用 return（覆盖 try 中的 return）

// 【强制】捕获异常后必须处理，不允许空 catch 块
try {
    emitter.complete();
} catch (Exception e) {
    log.warn("[StudioService] SSE 完成信号发送失败: {}", e.getMessage());  // ✅
}
// 禁止：
} catch (Exception e) { }   // ❌ 静默吞异常

// 【强制】异常信息必须具体，不能泛化
throw new RuntimeException("TTS1 合成失败: voiceId=" + voiceId);  // ✅
throw new RuntimeException("operation failed");                      // ❌
throw new RuntimeException("error");                                 // ❌

// 【推荐】定义业务异常时，区分用户侧错误（BusinessException）和系统侧错误
throw new BusinessException("项目不存在或无权访问");   // 用户可见，前端展示
throw new RuntimeException("TTS HTTP 请求超时");       // 系统错误，记录日志

// 【强制】事务场景中，捕获异常后如果需要回滚，要手动设置标记
@Transactional(rollbackFor = Exception.class)
public void deleteProject(Long projectId) {
    // rollbackFor = Exception.class 确保非 RuntimeException 也能回滚
}
```

### 1.8 【Controller 层规范】

```java
// Controller 职责：鉴权（解析身份）、参数校验、调用 Service、封装响应
// 禁止在 Controller 写业务逻辑、操作 Mapper、调第三方接口

@PostMapping("/create")
public Result<NovelProject> create(@RequestBody CreateRequest req) {
    // 1. 解析身份（Sa-Token）
    User user = authService.getCurrentUser();
    // 2. 功能权限校验（等级策略）
    if (!tierPolicyService.hasFeature(user.getTierCode(), "ai_novel")) {
        return Result.fail("当前会员等级不支持 AI 有声书功能");
    }
    // 3. 调用 Service（业务逻辑完全在 Service 内）
    NovelProject project = novelService.create(user, req.getTitle(),
            req.getVoiceId(), req.getRawText());
    // 4. 封装响应
    return Result.ok(project);
}

// 统一响应封装
Result.ok(data)           // 成功，带数据
Result.ok(null)           // 成功，无数据（void 操作）
Result.fail("错误消息")   // 失败，带用户可见消息
```

### 1.9 【Service 层规范】

```java
// 写操作加事务（rollbackFor 必须指定 Exception.class）
@Transactional(rollbackFor = Exception.class)
public void deleteProject(Long projectId) { ... }

// 读操作不加事务（减少数据库连接占用）
public List<NovelChapter> listChapters(Long projectId) { ... }

// 异步任务：@Async + try-catch（异步任务的异常不会向上传播）
@Async
public void startPipeline(Long projectId, String rawText) {
    try {
        // ...
    } catch (Exception e) {
        log.error("[NovelPipeline] 流水线处理失败: projectId={}", projectId, e);
        novelService.updateProgress(projectId, -1, "failed");
    }
}
```

### 1.10 【实体类规范】

```java
@Data
@TableName("novel_project")
public class NovelProject {

    /**
     * 项目 ID（雪花算法自动生成）
     * <p>序列化为字符串，防止 JavaScript Number 精度损失。</p>
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    // 【强制】POJO 类属性不设置默认值
    private String status;      // ✅
    private String status = "draft";  // ❌ 不在字段上设默认值

    // 【强制】Long 类型 ID 必须加 @JsonSerialize，防止 JS 精度丢失
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 逻辑删除标记（0=正常，1=已删除） */
    @TableLogic
    private Integer deleted;

    /** 创建时间（INSERT 时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（INSERT/UPDATE 时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

### 1.11 【禁止事项汇总】

| 禁止行为 | 阿里规范出处 | 替代方案 |
|---------|------------|---------|
| 硬编码密钥/密码 | 安全规范 | 环境变量注入 |
| `System.out.println` | 日志规范 | `log.info/warn/error` |
| 空 catch 块 | 异常规范 | 至少打 warn 日志 |
| `@Autowired` 字段注入 | OOP 规范 | 构造函数注入 |
| `log.info("info", ...)` 占位 | 日志规范 | 描述性日志消息 |
| `isXxx` 布尔字段命名 | 命名规范 | `enable`, `published` |
| POJO 字段设默认值 | OOP 规范 | 在构造器或初始化方法中设置 |
| foreach 内 remove/add | 集合规范 | `removeIf` 或 Stream |
| `keySet()` 遍历 Map | 集合规范 | `entrySet()` |
| 未定义魔法值 | 常量规范 | 定义有意义常量 |
| Controller 内写业务逻辑 | 分层规范 | 下沉到 Service |
| `transaction` 方法内捕获后不回滚 | 异常规范 | 使用 `TransactionAspectSupport` 或重新抛出 |

---

## 二、Vue 前端规范

### 2.1 文件组织

```
src/
├── api/           # 接口定义（按业务域分文件）
│   ├── tts.js, novel.js, studio.js ...
├── components/    # 全局可复用组件（PascalCase 命名）
├── composables/   # 可复用 Composition 函数（use 前缀）
├── stores/        # Pinia 状态（auth/player/toast）
├── router/        # 路由定义（懒加载）
└── views/         # 页面组件（与路由一一对应）
```

### 2.2 组件规范

```vue
<!-- 统一使用 <script setup> Composition API -->
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { fetchNovelList } from '@/api/novel'

const props = defineProps({
  projectId: { type: Number, required: true },
  status: { type: String, default: 'draft' }
})

const emit = defineEmits(['update', 'delete'])

const loading = ref(false)
const projects = ref([])

onMounted(() => loadData())

async function loadData() {
  try {
    loading.value = true
    const res = await fetchNovelList()
    projects.value = res.data
  } catch (e) {
    console.error('[Novel] 加载列表失败:', e)
  } finally {
    loading.value = false
  }
}
</script>
```

### 2.3 API 调用规范

```js
// ✅ 在 api/ 目录统一封装，组件内只调用封装好的函数
import request from './request'
export const createNovel = (data) => request.post('/api/novel/create', data)

// ❌ 禁止在组件内直接写 axios
import axios from 'axios'
axios.get('/api/novel/list')
```

### 2.4 样式规范

```vue
<!-- 优先使用 Tailwind CSS utility classes -->
<div class="flex items-center gap-4 px-6 py-4 bg-white rounded-lg shadow-sm">

<!-- 禁止内联 style（动态值除外）-->
<div style="display: flex; padding: 16px;">     <!-- ❌ -->
<div :style="{ color: dynamicColor }">           <!-- ✅ 仅动态值 -->
```

---

## 三、数据库规范

### 3.1 表命名

- 系统配置：`sys_` 前缀（`sys_voice`, `sys_tier_policy`）
- 用户数据：`user_` 前缀（`user_creation`, `user_storage`）
- 业务数据：语义命名（`novel_project`, `work`）

### 3.2 字段约定

```sql
-- 所有业务表必须字段
id          BIGINT          PRIMARY KEY COMMENT '主键（雪花算法）'
created_at  DATETIME        NOT NULL    COMMENT '创建时间'
updated_at  DATETIME                    COMMENT '更新时间（有更新需求时添加）'
deleted     TINYINT(1)      DEFAULT 0   COMMENT '逻辑删除（0正常，1已删除）'

-- 状态字段：用 VARCHAR 存字符串枚举值，不用 TINYINT 数字
status      VARCHAR(32)     COMMENT '状态：draft/creating/completed/failed'

-- 金额字段：DECIMAL(10,2)，禁止 FLOAT/DOUBLE
price       DECIMAL(10,2)   COMMENT '价格（元）'
```

### 3.3 MyBatis-Plus 规范

```java
// 【强制】使用 LambdaQueryWrapper（类型安全，防止字段名拼写错误）
new LambdaQueryWrapper<NovelProject>()
    .eq(NovelProject::getUserId, userId)
    .orderByDesc(NovelProject::getCreatedAt);

// ❌ 禁止字符串字段名（运行期才发现错误）
new QueryWrapper<NovelProject>().eq("user_id", userId);

// 分页查询必须使用 Page 对象
Page<UserCreation> page = new Page<>(pageNum, pageSize);
return creationMapper.selectPage(page, wrapper);
```

---

## 四、Git 提交规范（遵循 Conventional Commits）

### 4.1 Commit Message 格式

```
<type>(<scope>): <subject>

[body - 可选，描述为何这样改]
```

**type 类型：**

| type | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `refactor` | 重构（非 feat/fix）|
| `docs` | 文档更新 |
| `style` | 代码格式（不影响逻辑）|
| `perf` | 性能优化 |
| `chore` | 构建/配置/依赖变更 |
| `revert` | 回滚 commit |

**示例：**

```bash
feat(studio): 新增 AI 有声书 Pipeline 三阶段处理
fix(tts): 修复 TTS 1.0 base64 为空时的空指针异常
refactor(novel): 将章节切割逻辑从 Controller 下沉到 Service
docs: 新增编码规范和安全检查文档
chore(deps): 升级 LangChain4j 到 0.30.0
```

### 4.2 禁止提交的内容

```
application.yml      # 含真实密钥
.env / *.local       # 环境变量文件
node_modules/        # 前端依赖
target/              # Maven 构建产物
dist/                # Vite 构建产物
*.log / logs/        # 日志文件
.idea/ / .vscode/    # IDE 配置
```

---

## 五、参考资料

- [阿里巴巴 Java 开发手册（黄山版）](https://github.com/alibaba/p3c)
- [阿里巴巴 p3c 代码规范插件（IntelliJ IDEA）](https://plugins.jetbrains.com/plugin/10046-alibaba-java-coding-guidelines)
- [Conventional Commits 规范](https://www.conventionalcommits.org/zh-hans/)
- [Vue 3 官方风格指南](https://cn.vuejs.org/style-guide/)

---

*Last updated: 2026-03-09*
