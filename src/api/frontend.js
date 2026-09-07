import request from '@/utils/request'

// 注册
export function register(data) {
    return request({ url: '/api/user/add', method: 'post', data })
}

// 知识库文章分页（前台）
export function getKnowledgeList(params) {
    return request({ url: '/api/knowledge/article/page', method: 'get', params })
}

// 知识库文章详情（前台）
export function getKnowledgeDetail(id) {
    return request({ url: `/api/knowledge/article/${id}`, method: 'get' })
}

// 新增情绪日记
export function addEmotionDiary(data) {
    return request({ url: '/api/emotion-diary', method: 'post', data })
}

// 开始新会话
export function startSession(data) {
    return request({ url: '/api/psychological-chat/session/start', method: 'post', data })
}

// 会话列表
export function getSessionList(params) {
    return request({ url: '/api/psychological-chat/sessions', method: 'get', params })
}

// 删除会话
export function deleteSession(id) {
    return request({ url: `/api/psychological-chat/sessions/${id}`, method: 'delete' })
}

// 会话消息详情
export function getSessionDetail(id) {
    return request({ url: `/api/psychological-chat/sessions/${id}/messages`, method: 'get' })
}

// 会话情绪分析
export function getSessionEmotion(sessionId) {
    return request({ url: `/api/psychological-chat/session/${sessionId}/emotion`, method: 'get' })
}
