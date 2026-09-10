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

// 修改文章状态（data 为 { status: 1 } 结构，直接作为请求体，避免双层包装）
export function changeArticleStatus(id, data) {
    return request({ url: `/api/knowledge/article/${id}/status`, method: 'put', data })
}

// 删除文章
export function deleteArticle(id) {
    return request({ url: `/api/knowledge/article/${id}`, method: 'delete' })
}

// ========== 文件 ==========
// 文件上传（FormData 由本函数构造；不要手动设置 Content-Type，
// 浏览器会自动追加 multipart boundary，否则后端报 no multipart boundary）
export function uploadFile(file, businessInfo) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'ARTICLE')
    formData.append('businessId', businessInfo?.businessId ?? '')
    formData.append('businessField', 'cover')
    return request({
        url: '/api/file/upload',
        method: 'post',
        data: formData
    })
}

// ========== 数据看板 ==========
// 数据分析总览
export function getAnalyticsOverview() {
    return request({ url: '/api/data-analytics/overview', method: 'get' })
}

// ========== 咨询记录 ==========
// 咨询会话分页（支持管理员按 userId / username 搜索）
export function getConsultationPage(params) {
    return request({ url: '/api/psychological-chat/sessions', method: 'get', params })
}

// 会话消息详情
export function getSessionDetail(id) {
    return request({ url: `/api/psychological-chat/sessions/${id}/messages`, method: 'get' })
}

// 删除咨询会话（级联删除其所有消息）
export function deleteSession(id) {
    return request({ url: `/api/psychological-chat/sessions/${id}`, method: 'delete' })
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
