<template>
    <div class="consultation-container">
        <div class="sidebar">
            <!-- AI助手信息 -->
             <div class="ai-assistant-info">
                <div class="breathing-circle">
                    <el-image :src="iconUrl" style="width: 25px;height:25px" alt="AI助手" />
                </div>
                <h3 class="assistant-name">宁渡AI助手</h3>
                <div class="online-status">
                    <div class="status-dot"></div>
                    在线服务中
                </div>
             </div>
             <!-- 情绪花园 -->
             <div class="emotion-garden">
                <div class="garden-header">
                    <div class="garden-title"> 情绪花园 </div>
                    <div class="garden-subtitle">{{ currentEmotion.dataSource || '综合日记与对话动态评分' }}</div>
                </div>
                <div class="emotion-info">
                    <div class="emotion-name">{{ currentEmotion.primaryEmotion }}</div>
                    <div class="emotion-score">{{ currentEmotion.emotionScore }}</div>
                </div>
                <div class="warm-tips">
                    <div class="emotion-status-text">
                        <span class="status-label">今天感觉</span>
                        <span class="status-emotion">{{ currentEmotion.isNegative ? '需要关注' : '很不错' }}</span>
                    </div>
                    <div class="emotion-intensity">
                        <span class="intensity-dots">
                            <span v-for="dot in 3" :key="dot" class="dot" :class="{'active': getIntensityClass(currentEmotion.emotionScore) >= dot}"></span>
                        </span>
                        <span class="intensity-text">
                            {{ getRiskText(currentEmotion.riskLevel) }}
                        </span>
                    </div>
                    <!-- 温暖建议卡片 -->
                     <div class="warm-suggestion" v-if="currentEmotion.suggestion">
                        <div class="suggestion-icon">💝</div>
                        <div class="suggestion-content">
                            <div class="suggestion-title">给你的小建议</div>
                            <div class="suggestion-text">{{ currentEmotion.suggestion }}</div>
                        </div>
                     </div>
                     <!-- 治愈行动 -->
                      <div class="healing-actions" v-if="currentEmotion.improvementSuggestions.length > 0">
                        <div class="actions-title">治愈小行动</div>
                        <div class="actions-list">
                            <div v-for="action in currentEmotion.improvementSuggestions" :key="action" class="action-item">
                                <div class="action-icon">✨</div>
                                <div class="action-text">{{ action }}</div>
                            </div>
                        </div>
                      </div>
                      <!-- 风险提示 -->
                    <div class="risk-notice" v-if="currentEmotion.isNegative && currentEmotion.riskLevel > 1">
                        <div class="notice-icon">🤗</div>
                        <div class="notice-content">
                            <div class="notice-title">温馨提示</div>
                            <div class="notice-text">{{ currentEmotion.riskDescription }}</div>
                        </div>
                    </div>
                </div>
             </div>
             <!-- 会话列表 -->
             <div class="session-history">
                <h4 class="section-title">会话列表</h4>
                <div class="session-list">
                    <div v-for="session in sessionList" :key="session.id" class="session-item">
                        <div class="session-info" @click="handleSessionClick(session)">
                            <div class="session-title">
                                <span>{{ session.sessionTitle }}</span>
                                <div class="session-meta">
                                    <span class="session-time">{{ session.startedAt }}</span>
                                </div>
                                <div class="session-preview">
                                    {{ session.lastMessageContent }}
                                </div>
                                <div class="session-stats">
                                    <span>
                                        <el-icon>
                                            <ChatRound />
                                        </el-icon>
                                        {{ session.messageCount || 0 }}
                                    </span>
                                    <span>
                                        <el-icon>
                                            <Clock />
                                        </el-icon>
                                        {{ session.durationMinutes || 0 }} 分钟
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="session-actions">
                            <el-button text type="danger" size="small" @click.stop="handleDeleteSession(session.id)">
                                <el-icon><DeleteFilled /></el-icon>
                                删除
                            </el-button>
                        </div>
                    </div>
                </div>
             </div>
        </div>
        <div class="chat-main">
            <div class="chat-header">
                <div class="header-left">
                    <div class="chat-avatar">
                        <el-image :src="iconUrl1" style="width: 30px;height: 30px" />
                    </div>
                    <div class="chat-info">
                        <h2>宁渡AI助手</h2>
                        <p>您的贴心AI心理健康助手</p>
                    </div>
                </div>
                <el-button circle @click="createNewFrontendSession" title="新建会话">
                    <el-icon>
                        <Plus />
                    </el-icon>
                </el-button>
            </div>
            <!-- 聊天消息区域 -->
            <div class="chat-messages">
                <!-- 欢迎用语 -->
                <div class="message-item ai-message" v-if="messages.length === 0">
                    <div class="message-avatar">
                        <el-image :src="iconUrl" style="width: 18px;height: 18px" />
                    </div>
                    <div class="message-content">
                        <div class="message-bubble">
                            <p>您好！我是小暖，您的AI心理健康助手。很高兴陪伴您，为您提供温暖的心理支持。请告诉我，今天您感觉怎么样？有什么想要分享的吗？</p>
                        </div>
                        <div class="message-time">刚刚</div>
                    </div>
                </div>
                <!-- 消息列表 -->
                <div v-for="msg in messages" :key="msg.id" class="message-item" :class="msg.senderType === 1 ?  'user-message' : 'ai-message'">
                    <div class="message-avatar">
                        <el-image v-if="msg.senderType === 1" style="width: 18px; height:18px" :src="iconUrl2"></el-image>
                        <el-image v-if="msg.senderType === 2" style="width: 18px; height:18px" :src="iconUrl"></el-image>
                    </div>
                    <div class="message-content">
                        <div class="message-bubble">
                            <!-- AI正在思考中 -->
                            <div v-if="msg.senderType === 2 && isAiTyping && !msg.content" class="typing-indicator">
                                <div class="typing-dot"></div>
                                <div class="typing-dot"></div>
                                <div class="typing-dot"></div>
                            </div>
                            <!-- AI错误提示 -->
                            <div v-else-if="msg.isError" class="error-message">
                                <p>{{ msg.content }}</p>
                            </div>
                            <!-- AI正常返回消息 -->
                             <MarkdownRenderer v-else-if="msg.senderType === 2 && !msg.isError" :content="msg.content" :is-ai-message="true" />
                             <p v-else-if="msg.content" v-html="formatMessageContent(msg.content)"></p>
                        </div>
                        <div class="message-time">{{ msg.senderType === 2 && isAiTyping ? '正在输入中...' : msg.createdAt }}</div>
                    </div>
                </div>
            </div>
            <!-- 消息输入区域 -->
            <div class="chat-input">
                <div class="input-container">
                    <el-input
                        v-model="userMessage"
                        placeholder="请输入您想要分享的内容..."
                        type="textarea"
                        :rows="3"
                        :disabled="isAiTyping"
                        @keydown="handleKeyDown"
                        class="message-input"
                        clearable />
                        <div class="input-footer">
                            <div class="footer-left">
                                <span>按Enter发送，Shift+Enter换行</span>
                                <span class="char-count">{{ userMessage.length }}/500</span>
                            </div>
                            <div class="footer-right">
                                <el-select
                                    v-if="aiProvider.availableProviders && aiProvider.availableProviders.length > 0"
                                    v-model="aiProvider.provider"
                                    placeholder="选择 AI 供应商"
                                    size="small"
                                    class="provider-select"
                                    @change="handleProviderChange"
                                >
                                    <el-option
                                        v-for="p in aiProvider.availableProviders"
                                        :key="p"
                                        :label="providerLabel(p)"
                                        :value="p"
                                    />
                                </el-select>
                                <template v-if="aiProvider.provider === 'ollama'">
                                    <el-select
                                        v-model="aiProvider.model"
                                        placeholder="选择模型"
                                        size="small"
                                        class="model-select"
                                        @change="handleModelChange"
                                    >
                                        <el-option v-for="m in aiProvider.models" :key="m" :label="m" :value="m" />
                                    </el-select>
                                    <el-tag v-if="!aiProvider.ollamaOnline" type="danger" size="small" class="ollama-tag">离线</el-tag>
                                </template>
                            </div>
                        </div>
                </div>
                <el-button :disabled="!userMessage.trim() || userMessage.length > 500" type="primary" class="send-btn" @click="sendMessage">
                    <el-icon>
                        <Promotion />
                    </el-icon>
                </el-button>
            </div>
        </div>
    </div>
</template>
<script setup>
import {nextTick, onMounted, ref} from 'vue'
import {deleteSession, getEmotionGarden, getSessionDetail, getSessionList, startSession} from '@/api/frontend'
import {getAiProvider, switchAiProvider} from '@/api/admin'
import {ElMessage, ElMessageBox} from 'element-plus'
import {ChatRound, Clock, DeleteFilled, Plus, Promotion} from '@element-plus/icons-vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import {fetchEventSource} from '@microsoft/fetch-event-source'
import iconUrl from '@/assets/images/robot-fill.png'
import iconUrl1 from '@/assets/images/like.png'
import iconUrl2 from '@/assets/images/users.png'

// AI 供应商配置（从后端拉取）
const aiProvider = ref({
    provider: 'bailian',
    model: 'qwen-plus',
    models: [],
    ollamaOnline: false,
    availableProviders: ['bailian', 'ollama']
})

const providerLabel = (p) => p === 'bailian' ? '阿里云百炼' : '本地 Ollama'

const loadAiProvider = () => {
    getAiProvider().then(res => {
        aiProvider.value.provider = res.currentProvider
        aiProvider.value.model = res.ollamaModel || res.bailianModel || ''
        aiProvider.value.models = res.ollamaModels || []
        aiProvider.value.ollamaOnline = res.ollamaOnline
        aiProvider.value.availableProviders = res.availableProviders || ['bailian', 'ollama']
    }).catch(() => {})
}

const handleProviderChange = (val) => {
    switchAiProvider({ provider: val, model: aiProvider.value.model }).then(res => {
        aiProvider.value.model = res.ollamaModel || aiProvider.value.model
        aiProvider.value.models = res.ollamaModels || []
        aiProvider.value.ollamaOnline = res.ollamaOnline
        ElMessage.success(`已切换到${providerLabel(val)}`)
    })
}

const handleModelChange = (val) => {
    switchAiProvider({ provider: 'ollama', model: val }).then(() => {
        ElMessage.success(`模型已切换为 ${val}`)
    })
}

// 新建会话
const createNewFrontendSession = () => {
    currentSession.value = {
      sessionId: `temp_${Date.now()}`,
      status: 'TEMP',
      sessionTitle: '新对话'
    }
    // 清空旧消息，让欢迎语重新出现
    messages.value = []
    userMessage.value = ''
    isAiTyping.value = false
}

// 定义一个当前会话对象
const currentSession = ref(null)
const sessionList = ref([])

// 定义对话消息
const messages = ref([])
// 定义用户输入消息
const userMessage = ref('')
// 定义AI助手是否正在输入
const isAiTyping = ref(false)
// 标记流式响应是否已正常收到 done 事件（用于区分"正常结束 abort"与"真错误"）
const streamCompleted = ref(false)

// 情绪花园
const currentEmotion = ref({
    primaryEmotion: '中性',
    emotionScore: 50,
    isNegative: false,
    riskLevel: 0,
    suggestion: '情绪状态平稳',
    improvementSuggestions: [],
    dataSource: '综合日记与对话动态评分'
})

// 加载情绪花园动态评分（后端综合近14天情绪日记 + AI咨询对话内容计算）
const loadEmotionGarden = () => {
    getEmotionGarden().then(res => {
        if (res) {
            currentEmotion.value = {
                primaryEmotion: res.primaryEmotion || '中性',
                emotionScore: res.emotionScore ?? 50,
                isNegative: !!res.isNegative,
                riskLevel: res.riskLevel ?? 0,
                suggestion: res.suggestion || '',
                riskDescription: res.riskDescription || '',
                improvementSuggestions: res.improvementSuggestions || [],
                dataSource: res.dataSource || '综合日记与对话动态评分'
            }
        }
    }).catch(() => {})
}

const getIntensityClass = (score) => {
    if (score >= 61) {
        return 3
    }
    if (score >= 31) {
        return 2
    }
    return 1
}

const getRiskText = (level) => {
    switch (level) {
        case 0:
            return '正常'
        case 1:
            return '关注'
        case 2:
            return '预警'
        case 3:
            return '危机'
        default:
            return '正常'
    }
}

// 定义处理键盘事件：Enter 发送，Shift+Enter 换行
const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
        e.preventDefault()
        sendMessage()
    }
}

// 自动滚动到最新消息
const scrollToBottom = () => {
    nextTick(() => {
        const el = document.querySelector('.chat-messages')
        if (el) el.scrollTop = el.scrollHeight
    })
}

// 用户发送消息
const sendMessage = () => {
    if (!userMessage.value.trim()) return

    if (isAiTyping.value) {
        ElMessage.error('AI助手正在输入中，请稍后')
        return
    }

    const message = userMessage.value.trim()
    userMessage.value = ''

    // 如果没有会话或者是临时会话，就需要创建一个新的会话
    if (currentSession.value.status === 'TEMP') {
       startNewSession(message)
    } else {
        // 继续现有会话
        messages.value.push({
            id: Date.now(),
            senderType: 1,
            content: message,
            createAt: new Date().toISOString()
        })
        scrollToBottom()
        startAIResponse(currentSession.value.sessionId, message)
    }
}

const startNewSession = (message) => {
    // 构建会话参数
    const sessionParams = {
        initialMessage: message
    }
    if (currentSession.value.sessionTitle === '新对话') {
        sessionParams.sessionTitle = `宁渡AI助手 - ${new Date().toLocaleString()}`
    } else {
        // 如果历史会话记录
        sessionParams.sessionTitle = currentSession.value.sessionTitle
    }
    // 调用后端接口创建新会话
    startSession(sessionParams).then(res => {
        console.log(res)
       // 将后端返回的数据转为前端会话格式
       const sessionData = {
            sessionId: res.sessionId,
            status: res.status,
            sessionTitle: sessionParams.sessionTitle
       }
       // 如果当前是临时会话，更新数据
       if (currentSession.value && currentSession.value.status === 'TEMP') {
            // 更新为正式会话 
            Object.assign(currentSession.value, sessionData)
       } else {
            // 否则，创建一个新的会话
            currentSession.value = sessionData
       }
       // 更新会话列表
       getSessionPage()

       // 添加初始用户消息
       messages.value.push({
        id: Date.now(),
        senderType: 1,
        content: message,
        createAt: new Date().toISOString()
       })
       scrollToBottom()

       // 开始流式对话
       startAIResponse(currentSession.value.sessionId, message)
    })
}

const startAIResponse = (sessionId, userMessage) => {
    // 防止重复发送
    if (isAiTyping.value) {
        ElMessage.error('AI助手正在输入中，请稍后')
        return
    }

    isAiTyping.value = true
    streamCompleted.value = false

    const aiMessage = {
        id: `ai_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        senderType: 2,
        content: '',
        createAt: new Date().toISOString()
    }
    messages.value.push(aiMessage)
    scrollToBottom()

    let ctrl = new AbortController() // 用来中止fetch请求

    // AI 响应超时保护（60 秒）
    const timeoutId = setTimeout(() => {
        if (isAiTyping.value) {
            isAiTyping.value = false
            ctrl.abort()
            if (!aiMessage.content) {
                aiMessage.content = 'AI 响应超时，请重试'
                aiMessage.isError = true
            }
            ElMessage.warning('AI 响应超时')
        }
    }, 60000)

    // 调用流式接口
    fetchEventSource('/api/psychological-chat/stream', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Token': localStorage.getItem('token'),
            'Accept': 'text/event-stream'
        },
        body: JSON.stringify({
            sessionId,
            userMessage
        }),
        signal: ctrl.signal,
        onopen: (response) => {
            console.log(response)
            const ct = response.headers.get('Content-Type') || ''
            if (!ct.includes('text/event-stream')) {
                ElMessage.error('服务器返回非流式数据')
            }
        },
        onmessage: (event) => {
            const raw = event.data.trim()
            if (!raw) return
            const eventName = event.event
            // 当前会话的AI消息
            const aiMessage = messages.value[messages.value.length - 1]

            if (eventName === 'done') {
                streamCompleted.value = true
                isAiTyping.value = false
                clearTimeout(timeoutId)
                // 对话完成后刷新情绪花园评分
                loadEmotionGarden()
                return
            }
            const payload = JSON.parse(raw)
            const ok = String(payload.code) === '200'
            if (ok && payload.data && payload.data.content) {
                aiMessage.content += payload.data.content
                scrollToBottom()
            } else if (!ok) {
                // 错误回复的显示
                handleError(payload.message || 'AI回复失败')
            }
        },
        onerror: (err) => {
            clearTimeout(timeoutId)
            // done 事件后连接自然断开会触发 abort，属于正常结束，不报错
            if (streamCompleted.value) return
            handleError(err || 'AI回复失败')
            throw err
        },
        onclose: () => {
            clearTimeout(timeoutId)
            isAiTyping.value = false
            // 对话结束后刷新情绪花园评分
            loadEmotionGarden()
        }
    })

}

// 错误处理函数
const handleError = (error) => {
    // 当前会话的AI消息
    const aiMessage = messages.value[messages.value.length - 1]
    if (aiMessage) {
        // 已有内容说明AI至少回复了一部分，不要覆盖
        if (!aiMessage.content) {
            aiMessage.content = 'AI回复失败，请重试'
        }
    }
    isAiTyping.value = false
    ElMessage.error('AI回复失败，请重试')
}

const getSessionPage = () => {
    getSessionList({
        pageNum: 1,
        pageSize: 10
    }).then(res => {
        console.log(res)
        sessionList.value = res.records || []
    }).catch(err => {
        console.error('获取会话列表失败', err)
    })
}

// 获取会话数据
const handleSessionClick = (session) => {
    console.log(session, 'session')
    // 点击会话时，获取会话详情
    getSessionDetail(session.id).then(res => {
        console.log(res)
        messages.value = res
    })
    // 刷新情绪花园动态评分
    loadEmotionGarden()
    // 更新当前会话对象数据
  currentSession.value = {
      sessionId: "session_" + session.id,
      status: 'ACTIVE',
      sessionTitle: session.sessionTitle
    }
}

const handleDeleteSession = (sessionId) => {
    ElMessageBox.confirm('确定删除该会话吗？相关聊天记录将一并清除', '删除确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
    }).then(() => {
        deleteSession(sessionId).then(() => {
            ElMessage.success('删除成功')
            // 如果删的是当前会话，重置为空/新会话状态
            const prefix = 'session_' + sessionId
            if (currentSession.value?.sessionId === prefix || !currentSession.value || currentSession.value.status === 'ACTIVE') {
                const wasActive = currentSession.value?.sessionId === prefix
                getSessionPage()
                if (wasActive) {
                    currentSession.value = {
                        sessionId: `temp_${Date.now()}`,
                        status: 'TEMP',
                        sessionTitle: '新对话'
                    }
                    messages.value = []
                }
            } else {
                getSessionPage()
            }
        })
    }).catch(() => {})
}

// 简单的换行逻辑
const formatMessageContent = (content) => {
    return content.replace(/\n/g, '<br>')
}

onMounted(() => {
    // 初始化时获取会话列表
    getSessionPage()
    // 初始化时加载 AI 供应商配置
    loadAiProvider()
    // 初始化时创建一个新会话
    createNewFrontendSession()
    // 初始化时加载情绪花园动态评分
    loadEmotionGarden()
})
</script>
<style scoped lang="scss">
/* 项目主题色 */
$brand: #4a9c8c;
$brand-dark: #3d8a7a;
$brand-light: #6ab8a8;
$brand-bg: #e8f4f1;
$user-bubble: linear-gradient(135deg, #4a9c8c 0%, #3d8a7a 100%);
$ai-bubble: #ffffff;

.consultation-container {
    width: 100%;
    max-width: 1400px;
    /* 固定一屏高度，让内部滚动条生效，而不是页面整体变长 */
    height: calc(100vh - 120px);
    overflow: hidden;
    display: flex;
    gap: 20px;
    padding: 20px;
    margin: 0 auto;

    /* ========== 左侧边栏 ========== */
    .sidebar {
        width: 300px;
        flex-shrink: 0;
        display: flex;
        flex-direction: column;
        gap: 16px;
        /* 关键：允许 Flex 子项在列方向收缩，否则内容会把容器撑爆 */
        min-height: 0;

        /* AI 助手卡片 */
        .ai-assistant-info {
            background: linear-gradient(135deg, #ffffff 0%, #f6faf9 100%);
            border-radius: 18px;
            padding: 20px 16px;
            box-shadow: 0 2px 12px rgba(74, 156, 140, 0.08);
            border: 1px solid rgba(74, 156, 140, 0.1);
            text-align: center;

            .breathing-circle {
                width: 56px;
                height: 56px;
                background: $user-bubble;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                margin: 0 auto 12px;
                animation: breathe 3.5s ease-in-out infinite;
                box-shadow: 0 6px 20px rgba(74, 156, 140, 0.3);
            }
            .assistant-name {
                font-size: 16px;
                font-weight: 700;
                color: #1f5d54;
                margin: 0 0 8px;
            }
            .online-status {
                display: flex;
                align-items: center;
                justify-content: center;
                color: #059669;
                font-size: 12px;
                font-weight: 600;
                gap: 6px;
                .status-dot {
                    width: 8px;
                    height: 8px;
                    background: #059669;
                    border-radius: 50%;
                    animation: pulse 2s infinite;
                    box-shadow: 0 0 8px rgba(5, 150, 105, 0.5);
                }
            }
        }

        /* 情绪花园 */
        .emotion-garden {
            background: linear-gradient(135deg, #f0fbf8 0%, #e4f5f0 100%);
            border-radius: 18px;
            padding: 18px;
            box-shadow: 0 2px 12px rgba(74, 156, 140, 0.08);
            border: 1px solid rgba(74, 156, 140, 0.1);
            /* 限制高度：不超过 sidebar 可视区的 45%，给 session-history 留足空间 */
            max-height: 48%;
            overflow-y: auto;
            flex-shrink: 0;

            .garden-header {
                display: flex;
                align-items: center;
                justify-content: space-between;
                margin-bottom: 16px;
                .garden-title {
                    font-size: 15px;
                    font-weight: 600;
                    color: $brand-dark;
                }
                .garden-subtitle {
                    font-size: 11px;
                    color: #6b8e88;
                    max-width: 170px;
                    text-align: right;
                    line-height: 1.3;
                }
            }
            .emotion-info {
                width: 72px;
                height: 72px;
                border-radius: 50%;
                margin: 0 auto 12px;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                background: linear-gradient(135deg, $brand-light 0%, $brand 100%);
                color: #fff;
                box-shadow: 0 4px 14px rgba(74, 156, 140, 0.3);
                .emotion-name { font-size: 14px; font-weight: 600; }
                .emotion-score { font-size: 12px; font-weight: 700; opacity: 0.9; }
            }
            .warm-tips {
                text-align: center;
                .emotion-status-text {
                    margin-bottom: 10px;
                    .status-label { font-size: 13px; color: #6b8e88; }
                    .status-emotion {
                        font-size: 14px; font-weight: 600;
                        padding: 2px 10px; border-radius: 12px;
                        background: rgba(74,156,140,0.15); color: $brand-dark;
                    }
                }
                .emotion-intensity {
                    display: flex; align-items: center; justify-content: center; gap: 8px; margin-bottom: 10px;
                    .intensity-dots {
                        display: flex; gap: 4px;
                        .dot {
                            width: 8px; height: 8px; border-radius: 50%; background: #d1e0dc;
                            transition: all 0.3s;
                            &.active { background: $brand; transform: scale(1.2); box-shadow: 0 2px 6px rgba(74,156,140,0.4); }
                        }
                    }
                    .intensity-text { font-size: 12px; color: #6b8e88; font-weight: 500; }
                }
                .warm-suggestion {
                    background: #fff; border-radius: 12px; padding: 10px 12px;
                    display: flex; gap: 10px; align-items: flex-start;
                    border: 1px solid rgba(74,156,140,0.1); margin-bottom: 10px;
                    .suggestion-icon { font-size: 18px; }
                    .suggestion-content {
                        text-align: left; .suggestion-title { font-size: 13px; font-weight: 600; color: $brand-dark; margin-bottom: 4px; }
                        .suggestion-text { font-size: 12px; color: #5c7b75; line-height: 1.5; }
                    }
                }
                .healing-actions {
                    .actions-title { font-size: 13px; font-weight: 600; color: $brand-dark; margin-bottom: 8px; }
                    .actions-list { display: flex; flex-direction: column; gap: 6px; }
                    .action-item {
                        background: #fff; border-radius: 10px; padding: 8px 10px;
                        display: flex; gap: 8px; align-items: center;
                        border: 1px solid rgba(74,156,140,0.08);
                        .action-icon { font-size: 12px; }
                        .action-text { font-size: 12px; color: #5c7b75; line-height: 1.4; }
                    }
                }
                .risk-notice {
                    background: #fff9e6; border-radius: 12px; padding: 10px 12px;
                    display: flex; gap: 10px; border: 1px solid rgba(234, 179, 8, 0.2);
                    .notice-icon { font-size: 18px; }
                    .notice-content {
                        .notice-title { font-size: 13px; font-weight: 600; color: #b45309; margin-bottom: 4px; }
                        .notice-text { font-size: 12px; color: #92400e; line-height: 1.5; }
                    }
                }
            }
        }

        /* 会话历史 */
        .session-history {
            background: #fff;
            border-radius: 18px;
            padding: 16px;
            box-shadow: 0 2px 12px rgba(74, 156, 140, 0.08);
            border: 1px solid rgba(74, 156, 140, 0.1);
            flex: 1;
            display: flex;
            flex-direction: column;
            /* 至少保证 220px 让 2-3 条会话可见；emotion-garden 已限制 max-height */
            min-height: 220px;

            .section-title {
                font-size: 15px; font-weight: 600; color: #1f5d54; margin: 0 0 12px;
            }
            .session-list {
                overflow-y: auto; flex: 1; min-height: 0;
                scrollbar-width: thin; scrollbar-color: $brand-light transparent;
                .session-item {
                    padding: 10px 12px; border-radius: 12px;
                    transition: all 0.2s ease; margin-bottom: 6px;
                    display: flex; align-items: flex-start; gap: 8px;
                    background: transparent;
                    &:hover { background: $brand-bg; }
                    .session-info {
                        flex: 1; min-width: 0; cursor: pointer;
                        .session-title {
                            font-size: 13px; font-weight: 500; color: #1f2937; margin-bottom: 2px;
                            .session-meta .session-time { font-size: 11px; color: #9ca3af; }
                        }
                        .session-preview {
                            font-size: 12px; color: #6b7280; white-space: nowrap;
                            overflow: hidden; text-overflow: ellipsis;
                        }
                    }
                    .session-actions {
                        flex-shrink: 0; opacity: 0.5; transition: opacity 0.2s;
                    }
                    &:hover .session-actions { opacity: 1; }
                }
            }
        }
    }

    /* ========== 右侧聊天主区 ========== */
    .chat-main {
        flex: 1;
        background: #ffffff;
        border-radius: 18px;
        box-shadow: 0 4px 24px rgba(74, 156, 140, 0.1);
        border: 1px solid rgba(74, 156, 140, 0.1);
        display: flex;
        flex-direction: column;
        overflow: hidden;
        height: 100%;      /* 撑满父容器固定高度 */
        min-height: 0;     /* Flex 子项允许收缩，给 chat-messages 留出生效空间 */

        .chat-header {
            background: $user-bubble;
            color: white;
            padding: 16px 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-shrink: 0;

            .header-left {
                display: flex; align-items: center;
                .chat-avatar {
                    width: 40px; height: 40px;
                    background: rgba(255,255,255,0.25);
                    border-radius: 50%;
                    display: flex; align-items: center; justify-content: center;
                    margin-right: 12px;
                }
                .chat-info {
                    h2 { font-size: 17px; font-weight: 700; margin: 0 0 2px; }
                    p { font-size: 12px; opacity: 0.85; margin: 0; }
                }
            }
        }

        .chat-messages {
            flex: 1;
            min-height: 0;       /* 关键！Flex 子项默认 min-height:auto 会阻止收缩，加上后 overflow-y 才能生效 */
            overflow-y: auto;
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 18px;
            background: linear-gradient(180deg, #fafefc 0%, #f4f9f7 100%);
            scrollbar-width: thin; scrollbar-color: $brand-light transparent;

            .message-item {
                display: flex;
                align-items: flex-start;
                gap: 10px;
                animation: msgIn 0.3s ease-out;

                .message-avatar {
                    width: 32px; height: 32px; border-radius: 50%;
                    display: flex; align-items: center; justify-content: center;
                    flex-shrink: 0;
                }

                &.ai-message {
                    flex-direction: row;
                    .message-avatar {
                        background: $user-bubble;
                        box-shadow: 0 2px 8px rgba(74, 156, 140, 0.25);
                    }
                    .message-content {
                        max-width: 70%;
                        .message-bubble {
                            background: $ai-bubble;
                            border-radius: 16px 16px 16px 4px;
                            padding: 12px 16px;
                            border: 1px solid rgba(74,156,140,0.08);
                            box-shadow: 0 2px 8px rgba(0,0,0,0.04);
                            color: #1f2937;
                            line-height: 1.7;
                            font-size: 14px;
                        }
                    }
                }

                &.user-message {
                    flex-direction: row-reverse;
                    .message-avatar {
                        background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
                    }
                    .message-content {
                        max-width: 70%;
                        display: flex; flex-direction: column; align-items: flex-end;
                        .message-bubble {
                            background: $user-bubble;
                            border-radius: 16px 16px 4px 16px;
                            padding: 12px 16px;
                            color: #fff;
                            line-height: 1.7;
                            font-size: 14px;
                            box-shadow: 0 2px 8px rgba(74,156,140,0.2);
                        }
                        .message-time { color: #9ca3af; }
                    }
                }

                .message-time {
                    font-size: 11px;
                    color: #9ca3af;
                    margin-top: 4px;
                }
            }

            .typing-indicator {
                display: flex; gap: 4px; padding: 4px 0;
                .typing-dot {
                    width: 7px; height: 7px; border-radius: 50%;
                    background: $brand-light;
                    animation: typing 1.4s ease-in-out infinite;
                    &:nth-child(2) { animation-delay: 0.2s; }
                    &:nth-child(3) { animation-delay: 0.4s; }
                }
            }

            .error-message {
                background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
                border: 1px solid #fca5a5;
                border-radius: 12px;
                padding: 10px 14px;
                color: #b91c1c;
                font-size: 13px;
            }
        }

        .chat-input {
            padding: 16px 20px;
            display: flex;
            gap: 12px;
            align-items: flex-end;
            background: #fff;
            border-top: 1px solid rgba(74,156,140,0.1);
            flex-shrink: 0;

            .input-container {
                flex: 1;
                .message-input {
                    :deep(.el-textarea__inner) {
                        border-radius: 14px;
                        border: 1.5px solid #e5e7eb;
                        padding: 12px 14px;
                        font-size: 14px;
                        resize: none;
                        transition: border-color 0.2s, box-shadow 0.2s;
                        &:focus {
                            border-color: $brand;
                            box-shadow: 0 0 0 3px rgba(74,156,140,0.12);
                        }
                    }
                }
            }

            .input-footer {
                display: flex; justify-content: space-between; align-items: center;
                font-size: 12px; color: #6b7280; margin-top: 6px;

                .footer-left { display: flex; gap: 12px; align-items: center; }
                .footer-right { display: flex; gap: 8px; align-items: center; }
                .provider-select { width: 130px; }
                .model-select { width: 140px; }
            }

            .send-btn {
                width: 48px; height: 48px; border-radius: 14px;
                background: $user-bubble !important;
                border: none !important; color: #fff !important;
                box-shadow: 0 4px 14px rgba(74,156,140,0.3);
                transition: transform 0.2s, box-shadow 0.2s;
                &:hover:not(:disabled) {
                    transform: scale(1.06);
                    box-shadow: 0 6px 20px rgba(74,156,140,0.4);
                }
                &:active:not(:disabled) { transform: scale(0.96); }
                &:disabled { opacity: 0.5; }
            }
        }
    }
}

/* ========== 动画 ========== */
@keyframes breathe {
    0%, 100% { transform: scale(1); box-shadow: 0 6px 20px rgba(74,156,140,0.3); }
    50% { transform: scale(1.08); box-shadow: 0 10px 28px rgba(74,156,140,0.45); }
}
@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
}
@keyframes typing {
    0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
    30% { transform: translateY(-4px); opacity: 1; }
}
@keyframes msgIn {
    from { opacity: 0; transform: translateY(8px); }
    to { opacity: 1; transform: translateY(0); }
}

/* 滚动条 */
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #c5d9d4; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: $brand-light; }
</style>
