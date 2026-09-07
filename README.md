# 心理健康 AI 助手 — 项目总结

> 最近更新：2026-09-07（Ollama SSE 自定义客户端、模型切换迁移至对话页、前端 UI 重构、权限开放、超时保护）

## 一、项目定位

面向大学生群体的 AI 心理健康陪伴平台。用户可随时与 AI 助手倾诉（SSE 流式对话）、记录情绪日记、浏览心理健康知识文章；管理员通过后台进行内容管理、数据看板分析、AI 模型运行时切换。

## 二、技术栈总览

| 层次 | 技术 | 说明 |
| --- | --- | --- |
| 前端框架 | Vue 3.5 + Vite 6 | Composition API、按路由懒加载 |
| 路由 / 状态 | Vue Router 4 + Pinia | 前台/认证/后台三区路由 + 登录守卫 |
| UI / 可视化 | Element Plus 2.9 + ECharts 5.6 | 全局中文语言包、图标全量注册 |
| 富文本 / Markdown | @wangeditor + marked(自封装渲染) | 文章编辑、AI 回复渲染 |
| 流式通信 | @microsoft/fetch-event-source (SSE) | AI 打字机效果 |
| HTTP | Axios 1.7 | 拦截器统一解包 / 401 处理 |
| 后端框架 | Spring Boot 3.5.15（JDK 17+，实际运行 JDK 26） | Maven 构建 |
| 安全 | Spring Security + JWT (auth0 java-jwt 4.4) | 无状态认证 + RBAC |
| ORM | MyBatis-Plus 3.5.17 + jsqlparser | 分页插件、Lambda 查询、审计字段自动填充 |
| AI 接入 | Spring AI 1.0.0（OpenAI 兼容模式）+ 自定义 Ollama SSE 客户端 | 百炼 / Ollama 双供应商 |
| 缓存 | Redis 7 + Spring Cache + Lettuce | 差异化 TTL 缓存体系 |
| 数据库 | MySQL 8 | ai_db |

## 三、系统架构

```
浏览器 (Vue 3 / 5173)
   │  /api /upload 代理
   ▼
Spring Boot (1236)
   ├─ JwtAuthenticationFilter ──> Redis (user:info 缓存)
   ├─ Controller 层 ────────────> Service 层 ──> MyBatis-Plus ──> MySQL
   │                                  │
   │                                  ├─> Redis Cache（@Cacheable/@CacheEvict）
   │                                  └─> AI 供应商分流
   │                                        ├─ bailian（阿里云百炼 qwen-plus，Spring AI ChatClient）
   │                                        └─ ollama（本地 qwen3:14b，自定义 OllamaSseClient）
   └─ uploads/ 本地静态文件（/upload/** 直出）
```

## 四、核心功能

1. **AI 咨询对话**：SSE 流式响应 + 50ms 节流打字机效果；多会话管理；首条消息懒落库去重；对话上下文从数据库加载（最多 10 条）；上游异常统一转为友好 error 事件降级；前端 60 秒超时保护 + isAiTyping 状态兜底复位。
2. **AI 供应商双轨 + 用户端切换**：`AiProviderManager` 维护百炼 / Ollama 双客户端，运行时切换并按模型懒重建 Ollama 客户端；**模型选择下拉框内嵌于用户对话页输入框底部**（从管理员看板迁移），支持百炼/Ollama 切换 + Ollama 模型选择；权限开放给所有已登录用户（非管理员独占）。
3. **Ollama 流式兼容**：qwen3 模型返回 Ollama 特有的 `reasoning` 思考链字段，Spring AI 的 OpenAiChatModel 无法解析导致 JSON parse error。自研 `OllamaSseClient` 直接调 Ollama `/v1/chat/completions` SSE 端点，逐行解析并**只提取 `delta.content`，完全忽略 `reasoning`**，绕过 Spring AI 的解析缺陷。
4. **情绪日记**：心情评分 / 睡眠 / 压力 / 触发因素，同日记录自动覆盖更新；AI 情绪分析字段预留。
5. **知识库**：分类树 + 文章 CRUD + 富文本 + 草稿/发布状态流转；普通用户仅可见已发布文章；浏览量原子自增。
6. **数据看板（管理员）**：总量四卡片 + 情绪趋势 / 咨询统计 / 用户活跃度三图表，全部 SQL 聚合 + Redis 缓存。
7. **用户体系**：注册 / 登录 / JWT；`SecurityConfig` 统一 `authenticated()`，管理员专属接口才加 `@PreAuthorize`。

## 五、Redis 缓存设计

| 缓存名 | TTL | 内容 | 淘汰策略 |
| --- | --- | --- | --- |
| `analytics:overview` | 5 分钟 | 看板全部统计与趋势 | 短 TTL 自然过期（允许统计延迟） |
| `user:info` | 30 分钟 | JWT 过滤器每请求的用户信息 | 无用户写接口，TTL 兜底 |
| `knowledge:article` | 30 分钟 | 文章详情（含作者/分类名） | 创建/更新/改状态/删除时精确 Evict |
| `knowledge:category` | 1 小时 | 分类树 | 极少变化，TTL 兜底 |

技术要点：

- `RedisConfig` 统一配置：String key + Jackson JSON value（含 JavaTimeModule 支持 LocalDateTime）；
- `RedisCacheManager` 按缓存名差异化 TTL，`transactionAware` 保证回滚不污染缓存；
- 序列化开启 default typing 以保留泛型类型信息，`FAIL_ON_UNKNOWN_PROPERTIES=false` 提升前向兼容；
- Lettuce 连接池（max-active 8）+ 3s 超时，Redis 未启动时仅缓存失效不阻塞业务。

## 六、性能优化亮点

1. **数据看板全表扫描消除**：原实现 `selectList(null)` 将用户/会话/日记三张表全量载入内存统计；现全部下推 SQL（`COUNT / AVG / DATE_FORMAT + GROUP BY`），10+ 次内存遍历降为 8 条聚合 SQL，结果整体 Redis 缓存 5 分钟。
2. **认证链路缓存**：JWT 过滤器每个受保护请求都查库校验用户状态，现走 `user:info` Redis 缓存，热路径零 SQL。
3. **会话列表 N+1 修复**：`sessionPage` 原来逐条 `selectById` 查用户昵称，现改为 `selectBatchIds` 批量补齐。
4. **文章浏览量原子化**：原"读出整行 → 内存 +1 → 整行 update"存在并发丢失且放大锁开销，现改为 `read_count = IFNULL(read_count,0) + 1` 单语句原子自增。
5. **前端分包优化**：Vite `manualChunks` 将 element-plus / echarts / wangeditor / vue 全家桶拆为独立 vendor chunk，业务代码从 ~1.2MB 降至 10~18KB/页，vendor 长缓存命中，构建提速至 20s。

## 七、健壮性与代码质量

- 清除全部反编译痕迹：冗余强转、`@Generated` 日志、`value={...}` 注解写法、匿名 lambda 伪变量名等，全项目统一 `@Slf4j`、构造注入 / `@Resource` 规范。
- **敏感信息不入日志**：移除登录时打印完整 token、注册时 JSON 序列化明文密码等隐患。
- **空指针防护**：`startSession` 用户不存在时抛业务异常（原返回 null 导致 NPE）；会话 ID 解析统一捕获 `NumberFormatException`。
- **查询语义修正**：登录用户名/邮箱 OR 查询用 `and(w -> ...)` 包裹，避免多条件时逻辑漂移。
- **异常分类细化**：过滤器区分"无 token（401 未授权）/ token 无效（401）/ 用户被禁用（403）"，业务层 `BusinessException` 统一由 `GlobalExceptionHandler` 兜底。
- **参数拼写修正**：`moodScreRange → moodScoreRange`，前后端同步修正。
- Prompt 常量集中至 `PromptManage`（text block），与代码解耦。
- **MySQL ONLY_FULL_GROUP_BY 适配**：`GROUP BY DATE(started_at)` 改为 `GROUP BY DATE_FORMAT(started_at, '%Y-%m-%d')`，确保 SELECT 字段与分组字段一致。
- **Netty 版本冲突修复**：删除 pom.xml 中手动引入的 `netty-common:4.2.17.Final`，由 Spring Boot 统一管理版本。

## 八、前端 UI 重构亮点

- **主色调统一**：从橙黄系（#fb923c）全面切换为青绿系（#4a9c8c），与心理健康平台调性一致。
- **消息气泡布局**：AI 消息左对齐白色气泡 + 用户消息右对齐青绿填充气泡，圆角差异化（AI 左下小圆角 / 用户右下小圆角）。
- **输入框交互**：Enter 发送（修复原 handleKeyDown 缺失 sendMessage 调用的 Bug）+ Shift+Enter 换行 + 中文输入法 `isComposing` 防误触；底部左侧提示文字 + 字数统计，右侧内嵌 AI 模型选择下拉框。
- **自动滚动**：`nextTick` + `scrollToBottom` 在用户发送、AI 入队、流式追加 5 个节点触发，确保最新消息可见。
- **动画体系**：呼吸圆（AI 头像 3.5s 呼吸）、消息入站（0.3s fadeInUp）、输入指示器（三点跳动）、在线状态脉冲。
- **全屏布局**：`min-height: calc(100vh - 140px)` + flex 布局，消除白边；全局 `box-sizing: border-box` + `margin: 0` 重置。
- **侧边栏 Logo**：机器人头像加青绿圆形背景容器，解决白色图标在白色侧边栏不可见问题（适配浅色/深色模式）。

## 九、关键技术点实现

### 9.1 OllamaSseClient（绕过 Spring AI reasoning 字段缺陷）

```java
// OllamaSseClient.java 核心逻辑
fragmentFlux = ollamaSseClient.streamChat(model,
        PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT, history, userMessage);

// 内部：逐行解析 SSE，只取 delta.content，忽略 reasoning
JsonNode delta = choices.get(0).path("delta");
String content = delta.path("content").asText("");  // ← 只取 content
if (!content.isBlank()) { sink.next(content); }      // ← reasoning 被丢弃
```

### 9.2 AI 供应商运行时切换

```java
// AiProviderManager.java
public synchronized void switchTo(String provider, String model) {
    case PROVIDER_OLLAMA -> {
        if (model != null && !model.equals(ollamaModel)) {
            ollamaModel = model;
            ollamaClient = buildOllamaClient(model);  // 懒重建
        }
        currentProvider = PROVIDER_OLLAMA;
    }
}
```

### 9.3 前端模型选择 + 超时保护

```javascript
// consultation.vue
const timeoutId = setTimeout(() => {
    if (isAiTyping.value) {
        isAiTyping.value = false
        ctrl.abort()
        ElMessage.warning('AI 响应超时')
    }
}, 60000)  // 60s 超时兜底
```

## 十、目录结构

```
求职项目/AI/
├── ai-springboot/                # 后端（端口 1236）
│   ├── pom.xml                   # web/security/validation/webflux + redis + spring-ai + mybatis-plus
│   └── src/main/
│       ├── resources/application.yml   # 数据源 / Redis / AI / JWT / 日志
│       └── java/com/example/aispringboot/
│           ├── ai/               # AiProviderManager、OllamaSseClient、PromptManage
│           ├── common/           # Result、ResultCode、GlobalExceptionHandler
│           ├── config/           # SecurityConfig、RedisConfig、ChatClientConfig、MybatisPlusConfig…
│           ├── controller/       # user / knowledge / emotion-diary / psychological-chat / data-analytics / ai-config / file
│           ├── dto/              # command / response
│           ├── entity/           # 审计字段 @TableField(fill) 自动填充
│           ├── mapper/           # MyBatis-Plus mapper
│           ├── service/          # consultation / knowledge / emotion / system
│           └── util/             # JwtAuthenticationFilter、JwtTokenUtil、ResponseUtil
├── ai-vue/                       # 前端（端口 5173，代理 → 1236）
│   ├── vite.config.js            # vendor 分包 + 代理
│   └── src/
│       ├── api/                  # admin.js / frontend.js
│       ├── components/           # layouts / Navbar / Sidebar / ArticleDialog / RichTextEditor / MarkdownRenderer…
│       ├── router/               # 前台(/) + 认证(/auth) + 后台(/back) 三区路由 + 登录守卫
│       ├── stores/               # Pinia
│       ├── utils/request.js      # Token 头 + Result 解包 + 401 去重处理
│       └── views/                # 前台 5 页 + 后台 4 页
└── 项目总结.md
```

## 十一、启动方式

```bash
# 0) 前置：MySQL(ai_db) 与 Redis(6379) 本地已启动；Ollama 可选（qwen3:14b）

# 后端
cd ai-springboot && mvn spring-boot:run        # http://localhost:1236

# 前端
cd ai-vue && npm install && npm run dev        # http://localhost:5173
```

测试账号：`admin_test / 123456`（管理员）、`test / 123456`（普通用户）。

支持环境变量覆盖：`DB_PASSWORD`、`DASHSCOPE_API_KEY`、`JWT_SECRET`。

## 十二、验证记录（2026-09-07）

- 后端 `mvn compile` 通过（含 OllamaSseClient + 全部重构代码）；
- 前端 `vite build` 通过，vendor 分包生效（业务 chunk ≤ 18KB）；
- Redis 本地 `redis-cli ping` 返回 PONG；
- **Ollama 流式**：`OllamaSseClient` 正常返回纯文本 content，reasoning 字段被过滤；
- **百炼流式**：Spring AI ChatClient 正常；
- **模型切换**：用户对话页下拉框切换百炼/Ollama 均即时生效；
- **超时保护**：60s 后自动 abort + 输入框恢复；
- 建议冒烟路径：登录 → 后台看板（观察 Redis 键 `analytics:overview` 生成与 5 分钟过期）→ 知识库文章详情（键 `knowledge:article::id`）→ AI 对话流式回复 → 切换 ollama 供应商。
