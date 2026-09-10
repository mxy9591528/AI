<template>
    <div class="frontend-layout">
        <div class="navbar-container">
            <div class="brand-section">
                <el-image style="width: 50px; height: 50px" :src="iconUrl" alt="品牌logo" class="brand-logo" />
                <h1 class="brand-name">心理健康AI助手</h1>
            </div>
            <div class="nav-section">
                <router-link to="/" class="nav-link">首页</router-link>
                <router-link to="/consultation" class="nav-link" v-if="isLoggedIn">AI咨询</router-link>
                <router-link to="/emotion-diary" class="nav-link" v-if="isLoggedIn">情绪日记</router-link>
                <router-link to="/knowledge" class="nav-link">知识库</router-link>
                <el-dropdown v-if="isLoggedIn" @command="handleCommand" trigger="click">
                    <div class="user-dropdown">
                        <el-avatar :size="34" :src="userAvatar" />
                        <span class="user-name">{{ userNickname }}</span>
                        <el-icon><ArrowDown /></el-icon>
                    </div>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
                <template v-else>
                    <router-link to="/auth/login" class="nav-link">登录</router-link>
                    <router-link to="/auth/register" class="nav-link">
                        <el-button type="primary">注册</el-button>
                    </router-link>
                </template>
            </div>
        </div>
        <div class="main-content">
            <router-view></router-view>
        </div>
        <div class="footer-container">
            <div class="footer-bottom">
                <p>&copy; 2026 心理健康AI助手. All rights reserved.</p>
            </div>
        </div>
    </div>
</template>
<script setup>
import { ref, computed, onMounted, onUnmounted, watchEffect } from 'vue'
import { logout } from '@/api/admin'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import iconUrl from '@/assets/images/机器人.png'

const router = useRouter()

const isLoggedIn = ref(false)
// 用户信息用 ref 存储（localStorage 非响应式，需手动刷新）
const userInfo = ref(null)

const readUserInfo = () => {
    try {
        userInfo.value = JSON.parse(localStorage.getItem('userInfo') || 'null')
    } catch (e) {
        userInfo.value = null
    }
}

const userNickname = computed(() => (userInfo.value && (userInfo.value.nickname || userInfo.value.username)) || '用户')
const userAvatar = computed(() => (userInfo.value && userInfo.value.avatar) || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png')

// 个人中心保存资料后派发该事件，顶栏立即同步昵称/头像
const onUserInfoUpdated = () => readUserInfo()

// 每次路由变化时重新检查登录状态并刷新用户信息
watchEffect(() => {
    router.currentRoute.value // 依赖路由，触发重算
    isLoggedIn.value = localStorage.getItem('token') !== null
    readUserInfo()
})

const handleCommand = (command) => {
    if (command === 'profile') {
        router.push('/profile')
    } else if (command === 'logout') {
        ElMessageBox.confirm('确定退出登录吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        }).then(() => {
            logout().then(() => {
                localStorage.removeItem('token')
                localStorage.removeItem('userInfo')
                isLoggedIn.value = false
                userInfo.value = null
                router.push('/')
            })
        })
    }
}

onMounted(() => {
   isLoggedIn.value = localStorage.getItem('token') !== null
   readUserInfo()
   window.addEventListener('userinfo-updated', onUserInfoUpdated)
})

onUnmounted(() => {
   window.removeEventListener('userinfo-updated', onUserInfoUpdated)
})
</script>
<style scoped lang="scss">
.frontend-layout {
    background-color: #fff;
    display: flex;
    flex-direction: column;
    min-height: 100vh;

    .navbar-container {
        max-width: 1200px;
        width: 100%;
        height: 100%;
        margin: 0 auto;
        padding: 10px 20px;
        display: flex;
        align-items: center;
        justify-content: space-between;

        .brand-section {
            display: flex;
            align-items: center;

            .brand-name {
                margin-left: 10px;
                font-size: 24px;
                font-weight: 600;
                color: #333;
            }
        }

        .nav-section {
            display: flex;
            align-items: center;
            gap: 40px;

            .nav-link {
                color: #4b5563;
                font-size: 16px;
                font-weight: 500;

                &:hover {
                    color: #4A90E2;
                }
            }

            .user-dropdown {
                display: flex;
                align-items: center;
                gap: 8px;
                cursor: pointer;
                padding: 4px 10px;
                border-radius: 20px;
                transition: background 0.2s;

                &:hover {
                    background: #f3f4f6;
                }

                .user-name {
                    font-size: 14px;
                    color: #374151;
                    max-width: 100px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }
            }
        }
    }

    .main-content {
        flex: 1;
        display: flex;
        flex-direction: column;
    }

    .footer-container {
        background: #1f2937;
        color: white;
        padding: 15px 0;
        margin-top: auto;

        .footer-bottom {
            max-width: 1200px;
            margin: 0 auto;
            padding: 0 10px;
            text-align: center;
        }
    }
}
</style>
