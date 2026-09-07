import request from '@/utils/request'

// ========== 用户 ==========
// 登录
export function login(data) {
    return request({ url: '/api/user/login', method: 'post', data })
}

// 退出登录
export function logout() {
    return request({ url: '/api/user/logout', method: 'post' })
}

// ========== 知识库 ==========
// 分类树
export function categoryTree() {
    return request({ url: '/api/knowledge/category/tree', method: 'get' })
}

// 文章分页
export function articlePage(params) {
    return request({ url: '/api/knowledge/article/page', method: 'get', params })
}

// 文章详情
export function getArticleDetail(id) {
    return request({ url: `/api/knowledge/article/${id}`, method: 'get' })
}

// 创建文章
export function createArticle(data) {
    return request({ url: '/api/knowledge/article', method: 'post', data })
}

// 更新文章
export function updateArticle(id, data) {
    return request({ url: `/api/knowledge/article/${id}`, method: 'put', data })
}

// 修改文章状态
export function changeArticleStatus(id, status) {
    return request({ url: `/api/knowledge/article/${id}/status`, method: 'put', data: { status } })
}

// 删除文章
export function deleteArticle(id) {
    return request({ url: `/api/knowledge/article/${id}`, method: 'delete' })
}

// ========== 文件 ==========
// 文件上传
export function uploadFile(formData) {
    return request({
        url: '/api/file/upload',
        method: 'post',
        data: formData,
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

// ========== 数据看板 ==========
// 数据分析总览
export function getAnalyticsOverview() {
    return request({ url: '/api/data-analytics/overview', method: 'get' })
}

// ========== 咨询记录 ==========
// 咨询会话分页
export function getConsultationPage(params) {
    return request({ url: '/api/psychological-chat/sessions', method: 'get', params })
}

// 会话消息详情
export function getSessionDetail(id) {
    return request({ url: `/api/psychological-chat/sessions/${id}/messages`, method: 'get' })
}

// ========== 情绪日记 ==========
// 情绪日记分页
export function getEmotionalPage(params) {
    return request({ url: '/api/emotion-diary/admin/page', method: 'get', params })
}

// 删除情绪日记
export function deleteEmotional(id) {
    return request({ url: `/api/emotion-diary/admin/${id}`, method: 'delete' })
}

// ========== AI 模型设置 ==========
// 获取 AI 供应商状态
export function getAiProvider() {
    return request({ url: '/api/ai-config/provider', method: 'get' })
}

// 切换 AI 供应商
export function switchAiProvider(data) {
    return request({ url: '/api/ai-config/provider', method: 'put', data })
}
