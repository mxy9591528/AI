# 宁渡 AI 心理健康咨询平台 —— UML 建模图

> 技术栈：Spring Boot 3 + MyBatis-Plus + Spring AI（阿里云百炼 / Ollama）+ Vue3 + Element Plus + SSE
> 以下图均依据项目真实代码绘制（7 实体、8 控制器、10 服务类）。Mermaid 源码可直接在支持 Mermaid 的 Markdown 查看器（VS Code 插件 / Typora / GitHub）中渲染。

---

## 1. 系统用例图（Use Case Diagram）

> Mermaid 无原生用例图，采用 flowchart 表示：圆角节点为用例，两侧为参与者。

```mermaid
flowchart LR
    User([👤 普通用户<br/>来访者])
    Admin([👑 管理员])
    AI([🤖 AI大模型<br/>百炼/Ollama])

    subgraph S1[账号与权限]
        U1(注册/登录·JWT)
        U2(查看当前用户信息)
    end
    subgraph S2[AI心理咨询]
        U3(流式心理对话·SSE)
        U4(查看会话历史)
        U5(删除咨询会话)
        U6(切换AI供应商/模型)
    end
    subgraph S3[情绪管理]
        U7(记录/编辑情绪日记)
        U8(查看今日日记)
        U9(情绪花园动态评分)
    end
    subgraph S4[知识库]
        U10(浏览/搜索文章)
        U11(阅读文章详情)
        U12(文章/分类CRUD与发布)
    end
    subgraph S5[系统管理]
        U13(数据看板·统计/趋势)
        U14(咨询记录管理)
        U15(情绪日记管理)
        U16(上传图片/文件)
    end

    User --- U1
    User --- U3
    User --- U4
    User --- U5
    User --- U7
    User --- U8
    User --- U9
    User --- U10
    User --- U11
    User --- U16

    Admin --- U1
    Admin --- U12
    Admin --- U13
    Admin --- U14
    Admin --- U15
    Admin --- U6
    Admin --- U2
    Admin --- U10

    U3 <-. 流式生成疏导回复 .-> AI
```

---

## 2. 领域模型类图（Domain Class Diagram）

> 仅保留核心业务字段（不含 getter/setter），关联标注多重性。

```mermaid
classDiagram
    class User {
        +Long id
        +String username
        +String email
        +String phone
        +String password
        +String nickname
        +String avatar
        +Integer gender
        +LocalDate birthday
        +Integer userType
        +Integer status
    }
    class ConsultationSession {
        +Long id
        +Long userId
        +String sessionTitle
        +LocalDateTime startedAt
        +String lastEmotionAnalysis
        +LocalDateTime lastEmotionUpdatedAt
    }
    class ConsultationMessage {
        +Long id
        +Long sessionId
        +Integer senderType
        +Integer messageType
        +String content
        +String emotionTag
        +String aiModel
        +LocalDateTime createdAt
    }
    class EmotionDiary {
        +Long id
        +Long userId
        +LocalDate diaryDate
        +Integer moodScore
        +String dominantEmotion
        +String emotionTriggers
        +String diaryContent
        +Integer sleepQuality
        +Integer stressLevel
        +String aiEmotionAnalysis
    }
    class KnowledgeCategory {
        +Long id
        +Long parentId
        +String categoryName
        +String categoryCode
        +String description
        +Integer sortOrder
        +Integer status
    }
    class KnowledgeArticle {
        +String id
        +Long categoryId
        +String title
        +String summary
        +String content
        +String coverImage
        +String tags
        +Long authorId
        +Integer readCount
        +Integer status
        +LocalDateTime publishedAt
    }
    class SysFileInfo {
        +Long id
        +String originalName
        +String filePath
        +Long fileSize
        +String fileType
        +String businessType
        +String businessId
        +Long uploadUserId
        +Integer isTemp
        +Integer status
    }

    User "1" --> "0..*" ConsultationSession : 拥有
    ConsultationSession "1" --> "1..*" ConsultationMessage : 包含
    User "1" --> "0..*" EmotionDiary : 记录
    User "1" --> "0..*" KnowledgeArticle : 创作(authorId)
    KnowledgeCategory "1" --> "0..*" KnowledgeArticle : 归类
    KnowledgeCategory "0..1" --> "0..*" KnowledgeCategory : 父子(parentId)
    User "1" --> "0..*" SysFileInfo : 上传
```

---

## 3. 分层架构类图（Layered Architecture）

> Controller → Service → Mapper（MyBatis-Plus BaseMapper）→ 数据库；AI 能力经 AiProviderManager 统一适配双供应商。

```mermaid
classDiagram
    direction LR

    namespace ControllerLayer {
        class UserController
        class PsychologicalChatController
        class ConsultationController
        class EmotionDiaryController
        class KnowledgeController
        class AiProviderController
        class DataAnalyticsController
        class FileController
    }
    namespace ServiceLayer {
        class UserService
        class PsychologicalSupportService
        class ConsultationSessionService
        class ConsultationMessageService
        class EmotionDiaryService
        class EmotionGardenService
        class KnowledgeService
        class DataAnalyticsService
        class FileService
        class AiProviderManager
        class OllamaSseClient
    }
    namespace MapperLayer {
        class UserMapper
        class ConsultationSessionMapper
        class ConsultationMessageMapper
        class EmotionDiaryMapper
        class KnowledgeArticleMapper
        class KnowledgeCategoryMapper
        class SysFileInfoMapper
    }

    UserController ..> UserService
    PsychologicalChatController ..> PsychologicalSupportService
    ConsultationController ..> ConsultationSessionService
    ConsultationController ..> ConsultationMessageService
    ConsultationController ..> EmotionGardenService
    EmotionDiaryController ..> EmotionDiaryService
    KnowledgeController ..> KnowledgeService
    DataAnalyticsController ..> DataAnalyticsService
    FileController ..> FileService
    AiProviderController ..> AiProviderManager

    PsychologicalSupportService ..> ConsultationSessionService
    PsychologicalSupportService ..> ConsultationMessageService
    PsychologicalSupportService ..> AiProviderManager
    PsychologicalSupportService ..> OllamaSseClient
    EmotionGardenService ..> EmotionDiaryMapper
    EmotionGardenService ..> ConsultationSessionMapper
    EmotionGardenService ..> ConsultationMessageMapper

    UserService ..> UserMapper
    ConsultationSessionService ..> ConsultationSessionMapper
    ConsultationSessionService ..> ConsultationMessageMapper
    ConsultationMessageService ..> ConsultationMessageMapper
    EmotionDiaryService ..> EmotionDiaryMapper
    KnowledgeService ..> KnowledgeArticleMapper
    KnowledgeService ..> KnowledgeCategoryMapper
    FileService ..> SysFileInfoMapper
    AiProviderManager ..> OllamaSseClient
```

---

## 4. 实体关系图（ER Diagram）

```mermaid
erDiagram
    USER ||--o{ CONSULTATION_SESSION : "拥有"
    CONSULTATION_SESSION ||--|{ CONSULTATION_MESSAGE : "包含"
    USER ||--o{ EMOTION_DIARY : "记录"
    USER ||--o{ KNOWLEDGE_ARTICLE : "创作"
    KNOWLEDGE_CATEGORY ||--o{ KNOWLEDGE_ARTICLE : "归类"
    KNOWLEDGE_CATEGORY ||--o{ KNOWLEDGE_CATEGORY : "父子"
    USER ||--o{ SYS_FILE_INFO : "上传"

    USER {
        bigint id PK
        varchar username
        varchar email
        varchar phone
        varchar password
        varchar nickname
        varchar avatar
        int user_type
        int status
        datetime created_at
    }
    CONSULTATION_SESSION {
        bigint id PK
        bigint user_id FK
        varchar session_title
        datetime started_at
        text last_emotion_analysis
        datetime last_emotion_updated_at
    }
    CONSULTATION_MESSAGE {
        bigint id PK
        bigint session_id FK
        tinyint sender_type
        tinyint message_type
        text content
        varchar emotion_tag
        varchar ai_model
        datetime created_at
    }
    EMOTION_DIARY {
        bigint id PK
        bigint user_id FK
        date diary_date
        int mood_score
        varchar dominant_emotion
        varchar emotion_triggers
        text diary_content
        int sleep_quality
        int stress_level
        text ai_emotion_analysis
    }
    KNOWLEDGE_CATEGORY {
        bigint id PK
        bigint parent_id FK
        varchar category_name
        varchar category_code
        int sort_order
        int status
    }
    KNOWLEDGE_ARTICLE {
        varchar id PK
        bigint category_id FK
        varchar title
        text summary
        longtext content
        varchar cover_image
        varchar tags
        bigint author_id FK
        int read_count
        int status
        datetime published_at
    }
    SYS_FILE_INFO {
        bigint id PK
        varchar original_name
        varchar file_path
        bigint file_size
        varchar file_type
        varchar business_type
        varchar business_id
        bigint upload_user_id FK
        tinyint is_temp
        int status
    }
```

---

## 5. 时序图：用户登录（JWT 鉴权）

```mermaid
sequenceDiagram
    autonumber
    participant V as 前端 Vue
    participant UC as UserController
    participant US as UserService
    participant DB as 数据库
    participant JWT as JwtTokenUtil

    V->>UC: POST /api/user/login {username,password}
    UC->>US: login(commandDTO)
    US->>DB: 按 username 查询 User
    DB-->>US: User(含密文密码/状态)
    US->>US: 校验密码(BCrypt)与账号状态
    US->>JWT: 生成 Token(userId, userType)
    JWT-->>US: token
    US-->>UC: UserLoginResponseDTO(token + 用户信息)
    UC-->>V: Result.ok(token, userInfo)
    V->>V: localStorage 保存 token
```

---

## 6. 时序图：AI 流式心理咨询（SSE 打字机）

```mermaid
sequenceDiagram
    autonumber
    participant V as 前端 Vue<br/>(consultation.vue)
    participant PC as PsychologicalChatController
    participant PS as PsychologicalSupportService
    participant MS as ConsultationMessageService
    participant AM as AiProviderManager
    participant LLM as 大模型<br/>(百炼 ChatClient / OllamaSseClient)
    participant DB as 数据库

    Note over V: 首条消息：先 POST /session/start 创建会话
    V->>PC: POST /session/start {initialMessage,title}
    PC->>PS: startSession(userId, dto)
    PS->>DB: 插入 ConsultationSession + 首条用户消息
    DB-->>PS: sessionId
    PS-->>V: StreamChatSession(session_xxx)

    V->>PC: POST /stream (SSE) {sessionId,userMessage}
    PC->>PS: streamPsychologicalChat(sessionId,msg)
    PS->>MS: saveUserMessage(...)
    MS->>DB: 持久化用户消息
    PS->>DB: 加载最近10条历史消息
    PS->>AM: 获取当前供应商 ChatClient
    alt 百炼
        AM->>LLM: ChatClient.prompt().stream()
    else Ollama
        AM->>LLM: OllamaSseClient.streamChat(过滤reasoning)
    end
    loop 逐片段
        LLM-->>PS: token 片段
        PS-->>PC: Flux 片段
        PC-->>V: SSE event=message {content}
        V->>V: 追加渲染(打字机)
    end
    LLM-->>PS: 完成
    PS->>MS: saveAiMessage(完整回复,模型)
    MS->>DB: 持久化 AI 消息
    PC-->>V: SSE event=done
    V->>PC: GET /emotion/garden
    PC-->>V: 刷新情绪花园评分
```

---

## 7. 时序图：情绪花园动态评分

> 近 14 天「情绪日记评分(权重0.6) + 对话关键词情感(权重0.4)」，含危机词风险分级。

```mermaid
sequenceDiagram
    autonumber
    participant V as 前端 Vue
    participant CC as ConsultationController
    participant G as EmotionGardenService
    participant DM as EmotionDiaryMapper
    participant SM as ConsultationSessionMapper
    participant MM as ConsultationMessageMapper

    V->>CC: GET /api/psychological-chat/emotion/garden
    CC->>G: buildGarden(currentUserId)
    G->>DM: 查近14天 EmotionDiary 列表
    DM-->>G: 日记(moodScore/内容/情绪/触发因素)
    G->>G: moodScore 均值×10 → 日记分(0.6)<br/>统计积极/消极/危机词
    G->>SM: 查近14天会话 id 集合
    SM-->>G: sessionIds
    G->>MM: 查这些会话中用户消息(≤100条)
    MM-->>G: 用户对话内容
    G->>G: 50 + 积极词×4 − 消极词×7 → 对话分(0.4)
    G->>G: 加权融合 + 危机词命中判定风险等级
    G-->>CC: {emotionScore,primaryEmotion,<br/>riskLevel,suggestion,dataSource...}
    CC-->>V: Result.ok(花园状态)
    V->>V: 更新情绪名/分数/建议/治愈行动
```

---

## 8. 时序图：情绪日记每日记录（同日唯一、再次编辑）

```mermaid
sequenceDiagram
    autonumber
    participant V as 前端 Vue<br/>(emotionDiary.vue)
    participant EC as EmotionDiaryController
    participant ES as EmotionDiaryService
    participant DB as 数据库

    Note over V: 进入页面 onMounted 自动加载今日记录
    V->>EC: GET /api/emotion-diary/today
    EC->>ES: getTodayDiary(userId)
    ES->>DB: 按 (userId, 今天) 查询
    DB-->>ES: 已有记录 / null
    ES-->>V: 有则回填(编辑态)，无则空白

    V->>EC: POST /api/emotion-diary {diaryDate,moodScore,...}
    EC->>ES: saveDiary(userId, dto)
    ES->>DB: 按 (userId, diaryDate) 查询
    alt 当日无记录
        ES->>DB: insert 新日记
    else 当日已有记录
        ES->>DB: update 覆盖更新
    end
    ES-->>V: Result.ok()
```

---

## 9. 时序图：知识库文章浏览与模糊搜索

```mermaid
sequenceDiagram
    autonumber
    participant V as 前端 Vue<br/>(frontendKnowledge.vue)
    participant KC as KnowledgeController
    participant KS as KnowledgeService
    participant DB as 数据库

    V->>KC: GET /article/page?sortField=publishedAt&...
    KC->>KS: articlePage(...,status=已发布)
    KS->>DB: 分页查询(仅 status=1)
    DB-->>V: 文章分页列表

    V->>KC: GET /article/page?title=情绪(模糊搜索)
    KC->>KS: articlePage(title, ...)
    KS->>DB: title LIKE '%情绪%' AND status=1
    DB-->>V: 匹配文章

    V->>KC: GET /article/{id}
    KC->>KS: getArticleDetail(id)
    KS->>DB: 查详情 + readCount+1
    DB-->>V: 文章正文/封面/作者
```

---

## 10. 状态图：知识库文章生命周期

```mermaid
stateDiagram-v2
    [*] --> 草稿 : 管理员创建(0)
    草稿 --> 已发布 : 发布(1)
    已发布 --> 已下线 : 下线(2)
    已下线 --> 已发布 : 重新发布(1)
    已发布 --> [*] : 删除
    草稿 --> [*] : 删除
    note right of 已发布
        前台 /article/page 仅返回
        status=已发布 的文章
    end note
```

---

## 11. 活动图：AI 供应商运行时切换（管理员）

```mermaid
flowchart TD
    A[管理员打开AI配置] --> B[GET /api/ai-config/provider]
    B --> C{Ollama 在线?}
    C -- 是 --> D[返回 百炼/Ollama + 模型列表]
    C -- 否 --> E[仅返回百炼, ollamaOnline=false]
    D --> F[选择供应商/模型]
    E --> F
    F --> G[PUT /api/ai-config/provider]
    G --> H{provider}
    H -- bailian --> I[currentProvider=bailian]
    H -- ollama --> J{模型变更?}
    J -- 是 --> K[按模型懒重建 Ollama ChatClient]
    J -- 否 --> L[currentProvider=ollama]
    K --> L
    I --> M[后续对话即时生效]
    L --> M
```
