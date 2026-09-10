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

// 查询今天的情绪日记（null 表示还没记录）
export function getTodayEmotionDiary() {
    return request({ url: '/api/emotion-diary/today', method: 'get' })
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

// 情绪花园动态评分（综合近14天情绪日记 + AI咨询对话内容）
export function getEmotionGarden() {
    return request({ url: '/api/psychological-chat/emotion/garden', method: 'get' })
}

// ========== 个人中心 ==========

// 获取当前登录用户信息
export function getCurrentUser() {
    return request({ url: '/api/user/current', method: 'get' })
}

// 更新个人资料（邮箱/昵称/手机号/性别/生日/头像）
export function updateProfile(data) {
    return request({ url: '/api/user', method: 'put', data })
}

// 修改密码（需原密码 + 两次新密码）
export function updatePassword(data) {
    return request({ url: '/api/user/password', method: 'put', data })
}

// 上传头像（通用文件上传）
export function uploadAvatar(file) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'USER')
    formData.append('businessField', 'avatar')
    return request({ url: '/api/file/upload', method: 'post', data: formData })
}

// 当前用户的历史情绪日记分页（支持日期范围筛选）
export function getUserDiaryPage(params) {
    return request({ url: '/api/emotion-diary/page', method: 'get', params })
}
