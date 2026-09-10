<template>
    <div class="profile-page">
        <div class="profile-header">
            <h2>个人中心</h2>
            <p>管理你的账号信息、密码与情绪日记</p>
        </div>

        <el-tabs v-model="activeTab" class="profile-tabs">
            <!-- ========== 账号信息 ========== -->
            <el-tab-pane label="账号信息" name="info">
                <el-card class="info-card">
                    <div class="avatar-section">
                        <el-avatar :size="96" :src="userInfo.avatar || defaultAvatar" />
                        <div class="avatar-actions">
                            <el-upload
                                :show-file-list="false"
                                :before-upload="beforeAvatarUpload"
                                :http-request="handleAvatarUpload"
                                accept="image/*"
                            >
                                <el-button type="primary" size="small">更换头像</el-button>
                            </el-upload>
                            <p class="avatar-tip">支持 JPG/PNG，大小不超过 10MB</p>
                        </div>
                    </div>

                    <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="100px" class="profile-form">
                        <el-form-item label="用户名">
                            <el-input v-model="profileForm.username" disabled />
                        </el-form-item>
                        <el-form-item label="邮箱" prop="email">
                            <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                        </el-form-item>
                        <el-form-item label="昵称" prop="nickname">
                            <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="50" show-word-limit />
                        </el-form-item>
                        <el-form-item label="手机号" prop="phone">
                            <el-input v-model="profileForm.phone" placeholder="选填，11位手机号" />
                        </el-form-item>
                        <el-form-item label="性别" prop="gender">
                            <el-radio-group v-model="profileForm.gender">
                                <el-radio :value="0">保密</el-radio>
                                <el-radio :value="1">男</el-radio>
                                <el-radio :value="2">女</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="生日" prop="birthday">
                            <el-date-picker
                                v-model="profileForm.birthday"
                                type="date"
                                value-format="YYYY-MM-DD"
                                placeholder="选择出生日期"
                                style="width: 100%"
                            />
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" @click="submitProfile">保存修改</el-button>
                            <el-button @click="loadUserInfo">重置</el-button>
                        </el-form-item>
                    </el-form>
                </el-card>
            </el-tab-pane>

            <!-- ========== 修改密码 ========== -->
            <el-tab-pane label="修改密码" name="password">
                <el-card class="info-card">
                    <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="110px" class="pwd-form">
                        <el-form-item label="原密码" prop="oldPassword">
                            <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
                        </el-form-item>
                        <el-form-item label="新密码" prop="newPassword">
                            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-50位新密码" />
                        </el-form-item>
                        <el-form-item label="确认新密码" prop="confirmPassword">
                            <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" @click="submitPassword">确认修改</el-button>
                            <el-button @click="resetPwdForm">清空</el-button>
                        </el-form-item>
                    </el-form>
                </el-card>
            </el-tab-pane>

            <!-- ========== 历史日记 ========== -->
            <el-tab-pane label="历史日记" name="diary">
                <el-card class="info-card">
                    <div class="diary-filter">
                        <el-date-picker
                            v-model="diaryDateRange"
                            type="daterange"
                            range-separator="至"
                            start-placeholder="开始日期"
                            end-placeholder="结束日期"
                            value-format="YYYY-MM-DD"
                            style="width: 320px"
                        />
                        <el-button type="primary" @click="loadDiaryList(1)">查询</el-button>
                        <el-button @click="clearDiaryFilter">重置</el-button>
                    </div>

                    <div v-if="diaryList.length === 0" class="empty-tip">
                        <el-empty description="暂无情绪日记记录" />
                    </div>

                    <div v-else class="diary-list">
                        <div v-for="item in diaryList" :key="item.id" class="diary-item">
                            <div class="diary-item-header">
                                <span class="diary-date">{{ item.diaryDate }}</span>
                                <el-tag :type="moodTagType(item.moodScore)" effect="light">
                                    情绪评分 {{ item.moodScore }}/10
                                </el-tag>
                                <span v-if="item.dominantEmotion" class="diary-emotion">
                                    主导情绪：{{ item.dominantEmotion }}
                                </span>
                            </div>
                            <div v-if="item.emotionTriggers" class="diary-triggers">
                                <span class="label">触发因素：</span>{{ item.emotionTriggers }}
                            </div>
                            <div class="diary-content">{{ item.diaryContent }}</div>
                            <div v-if="item.aiEmotionAnalysis" class="diary-analysis">
                                <span class="label">AI 分析：</span>{{ item.aiEmotionAnalysis }}
                            </div>
                            <div class="diary-meta">
                                <span v-if="item.sleepQuality != null">睡眠质量 {{ item.sleepQuality }}/10</span>
                                <span v-if="item.stressLevel != null">压力水平 {{ item.stressLevel }}/10</span>
                            </div>
                        </div>
                    </div>

                    <div class="diary-pagination" v-if="diaryTotal > 0">
                        <el-pagination
                            background
                            layout="prev, pager, next, total"
                            :total="diaryTotal"
                            :page-size="diaryPageSize"
                            :current-page="diaryPage"
                            @current-change="loadDiaryList"
                        />
                    </div>
                </el-card>
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    getCurrentUser,
    updateProfile,
    updatePassword,
    uploadAvatar,
    getUserDiaryPage
} from '@/api/frontend'

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const activeTab = ref('info')

// ===== 用户信息 =====
const userInfo = ref({})
const profileFormRef = ref(null)
const profileForm = reactive({
    username: '',
    email: '',
    nickname: '',
    phone: '',
    gender: 0,
    birthday: '',
    avatar: ''
})

const profileRules = {
    email: [
        { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
    ],
    phone: [
        { pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
    ]
}

const loadUserInfo = () => {
    getCurrentUser().then(res => {
        userInfo.value = res || {}
        profileForm.username = res.username || ''
        profileForm.email = res.email || ''
        profileForm.nickname = res.nickname || ''
        profileForm.phone = res.phone || ''
        profileForm.gender = res.gender ?? 0
        // birthday：后端 LocalDate 序列化为 "YYYY-MM-DD"，与 el-date-picker value-format 匹配
        profileForm.birthday = res.birthday || null
        profileForm.avatar = res.avatar || ''
        // 统一更新 localStorage，使顶栏昵称/头像立即同步
        localStorage.setItem('userInfo', JSON.stringify(res || {}))
        window.dispatchEvent(new Event('userinfo-updated'))
    })
}

const submitProfile = () => {
    profileFormRef.value.validate(valid => {
        if (!valid) return
        updateProfile({
            email: profileForm.email,
            nickname: profileForm.nickname,
            phone: profileForm.phone,
            gender: profileForm.gender,
            birthday: profileForm.birthday,
            avatar: profileForm.avatar
        }).then(res => {
            ElMessage.success('资料更新成功')
            // 用后端返回的完整数据覆盖本地状态（避免 Object.assign 漏字段）
            userInfo.value = res || {}
            localStorage.setItem('userInfo', JSON.stringify(res || {}))
            // 通知顶栏立即刷新昵称/头像
            window.dispatchEvent(new Event('userinfo-updated'))
            // 重新拉取一次最新数据确保完全同步
            loadUserInfo()
        })
    })
}

// ===== 头像上传 =====
const beforeAvatarUpload = (file) => {
    const isImage = file.type.startsWith('image/')
    const isLt10M = file.size / 1024 / 1024 < 10
    if (!isImage) {
        ElMessage.error('只能上传图片文件')
        return false
    }
    if (!isLt10M) {
        ElMessage.error('图片大小不能超过 10MB')
        return false
    }
    return true
}

const handleAvatarUpload = (options) => {
    uploadAvatar(options.file).then(res => {
        const newAvatar = res.filePath || res.url || ''
        if (!newAvatar) {
            ElMessage.error('头像上传返回为空')
            return
        }
        // 先更新到前端表单显示
        profileForm.avatar = newAvatar
        userInfo.value.avatar = newAvatar
        // 自动保存到后端，无需用户再点"保存修改"
        updateProfile({ avatar: newAvatar }).then(saved => {
            ElMessage.success('头像已更新')
            userInfo.value = saved || {}
            localStorage.setItem('userInfo', JSON.stringify(saved || {}))
            window.dispatchEvent(new Event('userinfo-updated'))
        }).catch(() => {
            // 后端保存失败，回滚
            profileForm.avatar = userInfo.value.avatar || ''
        })
    })
}

// ===== 修改密码 =====
const pwdFormRef = ref(null)
const pwdForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
    if (value !== pwdForm.newPassword) {
        callback(new Error('两次输入的新密码不一致'))
    } else {
        callback()
    }
}

const pwdRules = {
    oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
    newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, max: 50, message: '密码长度 6-50 位', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请再次输入新密码', trigger: 'blur' },
        { validator: validateConfirm, trigger: 'blur' }
    ]
}

const submitPassword = () => {
    pwdFormRef.value.validate(valid => {
        if (!valid) return
        updatePassword({ ...pwdForm }).then(() => {
            ElMessage.success('密码修改成功，请重新登录')
            ElMessageBox.confirm('密码已修改，需要重新登录', '提示', {
                confirmButtonText: '去登录',
                showCancelButton: false,
                type: 'success'
            }).then(() => {
                localStorage.removeItem('token')
                localStorage.removeItem('userInfo')
                location.href = '/auth/login'
            })
        })
    })
}

const resetPwdForm = () => {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdFormRef.value?.clearValidate()
}

// ===== 历史日记 =====
const diaryList = ref([])
const diaryTotal = ref(0)
const diaryPage = ref(1)
const diaryPageSize = ref(10)
const diaryDateRange = ref([])

const moodTagType = (score) => {
    if (score == null) return 'info'
    if (score >= 8) return 'success'
    if (score >= 5) return 'warning'
    return 'danger'
}

const loadDiaryList = (page = 1) => {
    diaryPage.value = page
    const params = { pageNum: page, pageSize: diaryPageSize.value }
    if (diaryDateRange.value && diaryDateRange.value.length === 2) {
        params.diaryDateStart = diaryDateRange.value[0]
        params.diaryDateEnd = diaryDateRange.value[1]
    }
    getUserDiaryPage(params).then(res => {
        diaryList.value = res.records || []
        diaryTotal.value = res.total || 0
    })
}

const clearDiaryFilter = () => {
    diaryDateRange.value = []
    loadDiaryList(1)
}

onMounted(() => {
    loadUserInfo()
    loadDiaryList(1)
})
</script>

<style scoped lang="scss">
.profile-page {
    max-width: 900px;
    margin: 0 auto;
    padding: 24px 20px;

    .profile-header {
        margin-bottom: 20px;
        h2 { margin: 0 0 6px; color: #1f2937; font-size: 24px; }
        p { margin: 0; color: #6b7280; font-size: 14px; }
    }

    .info-card {
        border-radius: 12px;
    }

    .avatar-section {
        display: flex;
        align-items: center;
        gap: 20px;
        margin-bottom: 24px;
        padding-bottom: 20px;
        border-bottom: 1px solid #f0f0f0;

        .avatar-actions {
            .avatar-tip {
                margin: 8px 0 0;
                font-size: 12px;
                color: #9ca3af;
            }
        }
    }

    .profile-form, .pwd-form {
        max-width: 480px;
    }
}

/* 历史日记 */
.diary-filter {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
    flex-wrap: wrap;
}

.empty-tip {
    padding: 40px 0;
}

.diary-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.diary-item {
    border: 1px solid #eef0f3;
    border-radius: 10px;
    padding: 14px 16px;
    background: #fafbfc;
    transition: box-shadow 0.2s;

    &:hover {
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
    }

    .diary-item-header {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
        margin-bottom: 8px;

        .diary-date {
            font-weight: 600;
            color: #1f5d54;
            font-size: 15px;
        }

        .diary-emotion {
            font-size: 13px;
            color: #6b7280;
        }
    }

    .diary-triggers, .diary-analysis {
        font-size: 13px;
        color: #4b5563;
        margin: 4px 0;
        .label { color: #9ca3af; }
    }

    .diary-content {
        font-size: 14px;
        color: #1f2937;
        line-height: 1.6;
        margin: 6px 0;
        white-space: pre-wrap;
    }

    .diary-meta {
        display: flex;
        gap: 16px;
        margin-top: 8px;
        font-size: 12px;
        color: #9ca3af;
    }
}

.diary-pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;
}
</style>
