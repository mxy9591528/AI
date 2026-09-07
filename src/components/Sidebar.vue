<template>
    <div class="sidebar-container">
        <div class="sidebar-logo">
            <div class="logo-avatar">
                <el-image style="width: 26px; height: 26px" :src="iconUrl" alt="心理AI助手" />
            </div>
            <span v-if="!isCollapse" class="logo-text">心理健康AI助手</span>
        </div>
        <el-menu
            :default-active="route.path"
            :collapse="isCollapse"
            router
            class="sidebar-menu"
        >
            <el-menu-item index="/back/dashboard">
                <el-icon><DataAnalysis /></el-icon>
                <template #title>数据看板</template>
            </el-menu-item>
            <el-menu-item index="/back/knowledge">
                <el-icon><Reading /></el-icon>
                <template #title>知识库管理</template>
            </el-menu-item>
            <el-menu-item index="/back/consultations">
                <el-icon><ChatDotRound /></el-icon>
                <template #title>咨询记录</template>
            </el-menu-item>
            <el-menu-item index="/back/emotional">
                <el-icon><Notebook /></el-icon>
                <template #title>情绪日记</template>
            </el-menu-item>
        </el-menu>
    </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAdminStore } from '@/stores/admin'
import {ChatDotRound, DataAnalysis, Notebook, Reading} from "@element-plus/icons-vue";
import iconUrl from '@/assets/images/robot-fill.png'

const route = useRoute()
const { isCollapse } = storeToRefs(useAdminStore())
</script>

<style lang="scss" scoped>
.sidebar-container {
    height: 100%;
    display: flex;
    flex-direction: column;

    .sidebar-logo {
        display: flex;
        align-items: center;
        gap: 10px;
        height: 74px;
        padding: 0 20px;
        border-bottom: 1px solid #e5e7eb;
        flex-shrink: 0;
        overflow: hidden;

        .logo-avatar {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            background: linear-gradient(135deg, #4a9c8c 0%, #3d8a7a 100%);
            display: flex;
            justify-content: center;
            align-items: center;
            flex-shrink: 0;
        }

        .logo-text {
            font-size: 16px;
            font-weight: bold;
            color: #4a9c8c;
            white-space: nowrap;
        }
    }

    .sidebar-menu {
        height: 100%;
        border-right: 1px solid #e5e7eb;

        &:not(.el-menu--collapse) {
            width: 220px;
        }
    }
}
</style>
