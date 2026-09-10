<template>
    <div>
        <PageHead title="咨询记录" />
        <TableSearch :formItem="formItem" @search="handleSearch" />
        <el-table :data="tableData" style="width: 100%">
            <el-table-column prop="userId" label="用户ID" width="90" />
            <el-table-column prop="username" label="用户名" width="120">
                <template #default="scope">
                    {{ scope.row.username || '-' }}
                </template>
            </el-table-column>
            <el-table-column prop="userNickname" label="昵称" width="120">
                <template #default="scope">
                    {{ scope.row.userNickname || '-' }}
                </template>
            </el-table-column>
            <el-table-column label="会话" min-width="220">
                <template #default="scope">
                    <div class="session-title">{{ scope.row.sessionTitle }}</div>
                    <div class="session-preview">{{ scope.row.lastMessageContent }}</div>
                </template>
            </el-table-column>
            <el-table-column prop="messageCount" label="消息数" width="80" />
            <el-table-column label="开始时间" width="170">
                <template #default="scope">
                    {{ scope.row.startedAt }}
                </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
                <template #default="scope">
                    <el-button type="primary" text @click="viewSessionDetail(scope.row)">详情</el-button>
                    <el-button type="danger" text @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
         style="margin-top: 25px"
         :current-page="pagination.currentPage"
         :page-size="pagination.size"
         layout="prev, pager, next, jumper, total"
         :total="pagination.total"
         @current-change="handleChange"
         />
         <el-dialog
            v-model="showDetailDialog"
            title="咨询会话详情"
            width="70%"
            :close-on-click-modal="false"
        >
            <div class="session-detail">
                <div class="detail-header">
                    <div class="detail-row"><div class="detail-label">用户：</div><div class="detail-value">{{ sessionDetail.userNickname || sessionDetail.username || '-' }}</div></div>
                    <div class="detail-row"><div class="detail-label">用户ID：</div><div class="detail-value">{{ sessionDetail.userId }}</div></div>
                    <div class="detail-row"><div class="detail-label">开始时间：</div><div class="detail-value">{{ sessionDetail.startedAt }}</div></div>
                    <div class="detail-row"><div class="detail-label">消息数：</div><div class="detail-value">{{ sessionDetail.messageCount }}</div></div>
                </div>
                <div class="messages-container">
                    <div class="messages-header"><h4>对话记录</h4></div>
                    <div class="messages-list" v-loading="loadingMessages">
                        <div v-for="message in sessionMessages" :key="message.id" class="message-item" :class="message.senderType === 1 ? 'user-message' : 'ai-message'">
                            <div class="message-header">
                               <span class="sender">{{ message.senderType === 1 ? '用户' : 'AI助手' }}</span>
                               <span class="time">{{ message.createdAt }}</span>
                            </div>
                            <div class="message-content">{{ message.content }}</div>
                        </div>
                    </div>
                </div>
            </div>
            <template #footer>
                <el-button @click="showDetailDialog = false">关闭</el-button>
            </template>
        </el-dialog>
    </div>
</template>
<script setup>
import { onMounted, ref, reactive } from 'vue'
import PageHead from '@/components/PageHead.vue'
import TableSearch from '@/components/TableSearch.vue'
import { getConsultationPage, getSessionDetail, deleteSession } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

// 搜索表单（传什么字段就给后端什么字段；不传就查全部）
const formItem = [
    { comp: 'input', prop: 'userId', label: '用户ID', placeholder: '请输入用户ID' },
    { comp: 'input', prop: 'username', label: '用户名', placeholder: '支持用户名/昵称模糊搜索' }
]

const tableData = ref([])
const pagination = reactive({
    currentPage: 1,
    size: 10,
    total: 0
})
const lastSearch = reactive({})  // 保存最近一次搜索条件，翻页时沿用

// 会话详情
const sessionDetail = ref({})
const sessionMessages = ref([])
const loadingMessages = ref(false)
const showDetailDialog = ref(false)

const viewSessionDetail = (row) => {
    loadingMessages.value = true
    showDetailDialog.value = true
    getSessionDetail(row.id).then(res => {
        loadingMessages.value = false
        sessionMessages.value = res
        sessionDetail.value = row
    })
}

const handleDelete = (row) => {
    ElMessageBox.confirm(
        `确认删除 ${row.username || row.userNickname || row.userId} 的会话「${row.sessionTitle}」吗？其下所有消息将一并删除，操作不可恢复。`,
        '删除会话',
        { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    ).then(() => {
        deleteSession(row.id).then(() => {
            ElMessage.success('会话已删除')
            handleSearch()
        })
    })
}

const handleChange = (page) => {
    pagination.currentPage = page
    handleSearch()
}

const handleSearch = (formData = {}) => {
    // 记录搜索条件（翻页时 handleSearch() 无参，沿用上次）
    if (Object.keys(formData).length > 0) {
        Object.assign(lastSearch, formData)
        pagination.currentPage = 1
    }
    const params = {
        currentPage: pagination.currentPage,
        pageNum: pagination.currentPage,
        size: pagination.size,
        pageSize: pagination.size,
        ...lastSearch
    }
    // 过滤掉空字符串，避免把空值传给后端
    Object.keys(params).forEach(k => { if (params[k] === '' || params[k] === null || params[k] === undefined) delete params[k] })

    getConsultationPage(params).then(res => {
        const { records, total } = res
        tableData.value = records
        pagination.total = total
    })
}

onMounted(() => { handleSearch() })
</script>

<style lang="scss" scoped>
.session-title { font-weight: 500; color: #333; margin-bottom: 4px; }
.session-preview {
    font-size: 13px; color: #666; margin-bottom: 4px;
    display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2;
    -webkit-box-orient: vertical; overflow: hidden;
}
.session-detail {
    max-height: 70vh; overflow-y: auto;
    .detail-header { margin-bottom: 20px; padding: 16px; background: #f8f9fa; border-radius: 8px; border: 1px solid #e9ecef; }
    .detail-row { display: flex; align-items: center; margin-bottom: 8px; :last-child { margin-bottom: 0; }
        .detail-label { font-weight: 500; color: #495057; min-width: 80px; margin-right: 8px; }
        .detail-value { color: #333; }
    }
}
.messages-container {
    margin-top: 20px;
    .messages-header { margin-bottom: 16px; h4 { margin: 0; color: #333; font-size: 16px; font-weight: 500; } }
    .messages-list {
        max-height: 400px; overflow-y: auto; border: 1px solid #e9ecef; border-radius: 8px; padding: 16px; background: #fff;
        .message-item { margin-bottom: 12px; padding: 12px; border-radius: 8px; background: #f8f9fa; border: 1px solid #e9ecef;
            :last-child { margin-bottom: 0; }
            &.user-message { background: #e8f4fd; }
            &.ai-message { background: #f0f9f0; }
        }
        .message-header {
            display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;
            .sender { font-weight: 500; color: #333; }
            .time { font-size: 12px; color: #999; }
        }
        .message-content { color: #333; line-height: 1.6; white-space: pre-wrap; margin-top: 8px; font-size: 14px; }
    }
}
</style>
