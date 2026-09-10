<template>
  <div class="knowledge-container">
    <div class="header-section">
        <div class="header-content">
            <el-image :src="iconUrl" style="width: 60px;height: 60px"></el-image>
            <h1>知识库</h1>
            <div class="search-box">
                <el-input
                    v-model="searchTitle"
                    placeholder="搜索文章名"
                    clearable
                    size="large"
                    @keyup.enter="handleSearch"
                    @clear="handleSearch">
                    <template #prefix>
                        <el-icon><Search /></el-icon>
                    </template>
                </el-input>
                <el-button type="warning" size="large" @click="handleSearch">搜索</el-button>
            </div>
        </div>
    </div>
    <div class="content">
        <!-- 左侧菜单 -->
         <div class="recommend-section">
            <div class="section-title">推荐阅读</div>
            <div class="recommend-list">
                <div v-for="item in recommendList" :key="item.id" class="recommend-item" @click="goToArticle(item.id)">
                    <h4>{{item.title}}</h4>
                    <p class="read-count">
                        <el-icon><Histogram /></el-icon>
                            阅读量 {{ item.readCount }}
                    </p>
                </div>
            </div>
         </div>
         <!-- 右侧内容 -->
         <div class="article-list">
            <div v-for="item in articleList" :key="item.id" class="article-item" @click="goToArticle(item.id)">
                <el-image style="width: 240px; height: 150px" :src="getImage(item.coverImage)"></el-image>
                <div class="info">
                    <div class="title">
                        <h3>{{ item.title }}</h3>
                        <el-tag Plain type="primary">{{ item.categoryName }}</el-tag>
                    </div>
                    <div :style="{marginTop: '10px'}">
                        <div class="flex-box">
                            <el-icon><Avatar /></el-icon>
                            <span>{{ item.authorName }}</span>
                        </div>
                        <div class="flex-box">
                            <el-icon><List /></el-icon>
                            <span>{{ dayjs(item.updatedAt).format('YYYY-MM-DD') }}</span>
                        </div>
                    </div>
                    <div :style="{marginTop: '10px'}">
                        <div class="flex-box">
                            <el-icon><Platform /></el-icon>
                            <span>观看人数 {{ item.readCount }}</span>
                        </div>
                    </div>
                </div>
            </div>
         </div>
    </div>
    <!-- 分页 -->
    <div class="pagination-wrapper">
    <el-pagination 
        style="margin-top: 25px"
        :page-size="pagination.size"
        layout="prev, pager, next"
        :total="pagination.total"
        @change="handleChange" />
    </div>
  </div>
</template>
<script setup>
    import { dayjs, ElMessage } from 'element-plus'
    import { ref, reactive, onMounted } from 'vue'
    import { getKnowledgeList } from '@/api/frontend'
    import { useRouter } from 'vue-router'

    const router = useRouter()
    
    import iconUrl from '@/assets/images/book.png'
    import {Avatar, Histogram, List, Platform, Search} from '@element-plus/icons-vue'

    // 推荐阅读列表
    const recommendList = ref([])

    // 文章名搜索关键词
    const searchTitle = ref('')

    // 右侧列表数据
    const pagination = reactive({
        currentPage: 1,
        size: 10,
        total: 0
    })

    const articleList = ref([])
    // 获取列表数据
    const getPageList = () => {
        const params = {
            sortField: 'publishedAt',
            sortDirection: 'desc',
            ...pagination
        }
        // 文章名模糊搜索（后端 title 为 LIKE 条件）
        if (searchTitle.value.trim()) {
            params.title = searchTitle.value.trim()
        }
        getKnowledgeList(params).then(res => {
            articleList.value = res.records
            pagination.total = res.total
        })
    }

    // 点击搜索：回到第一页并按文章名过滤
    const handleSearch = () => {
        pagination.currentPage = 1
        getPageList()
    }
    // 获取封面图片
    const getImage = (url) => {
        if (!url) return 'https://file.itndedu.com/psychology_ai.png'
        // 开发环境走 vite 代理（/api → localhost:1236），生产环境部署时直接用后端域名
        // filePath 已经是 "/upload/xxx.png" 形式的相对路径，直接请求即可
        return url
    }

    const handleChange = (page) => {
        pagination.currentPage = page
        getPageList()
    }

    // 跳转到详情
    const goToArticle = (id) => {
        router.push(`/knowledge/article/${id}`)
    }

    onMounted(() => {
        // 获取推荐阅读列表
        const params = {
            sortField: 'readCount',
            sortDirection: 'desc',
            currentPage: 1,
            size: 5
        }
        getPageList()
        getKnowledgeList(params).then(res => {
            // console.log(res)
            recommendList.value = res.records
        })
    })
</script>
<style lang="scss" scoped>
    .knowledge-container {
    background: linear-gradient(135deg, #fafbfc 0%, #f7f9fc 50%, #f2f6fa 100%);
    .flex-box {
        display: flex;
        align-items: center;
        span {
            margin-left: 10px;
        }
    }
    .header-section {
        background: linear-gradient(135deg, #f59e0b 0%, #8b5cf6 100%);
        color: white;
        padding: 48px;
        .header-content {
            display: flex;
            align-items: center;
            gap: 12px;
            width: 1200px;
            margin: 0 auto;
            h1 {
                white-space: nowrap;
            }
            .search-box {
                margin-left: auto;
                display: flex;
                gap: 10px;
                width: 420px;
                :deep(.el-input__wrapper) {
                    border-radius: 8px;
                }
            }
        }
    }
    .content {
        display: flex;
        gap: 20px;
        margin: 0 auto;
        width: 1200px;
        padding: 20px;
        .recommend-section {
            width: 280px;
            background: white;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
            padding: 15px;
            height: 400px;
            .section-title {
                font-size: 12;
                font-weight: 600;
                color: #374151;
                margin-bottom: 10px;
                display: flex;
                align-items: center;
                gap: 5px;
            }
            .recommend-list {
                display: flex;
                flex-direction: column;
                gap: 1rem;
                .recommend-item {
                    border-left: 4px solid #f59e0b;
                    padding-left: 10px;
                    cursor: pointer;
                    .read-count {
                        margin-top: 15px;
                        font-size: 12px;
                        color: #6b7280;
                        display: flex;
                        align-items: center;
                        gap: 10px;
                    }
                }
            }
        }
        .article-list {
            flex: 1;
            .article-item {
                background: white;
                border-radius: 12px;
                box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
                padding: 15px;
                margin-bottom: 20px;
                display: flex;
                .info {
                    margin-left: 20px;
                    .title {
                        display: flex;
                        align-items: center;
                        gap: 10px;
                    }
                }
            }
        }
    }
    .pagination-wrapper {
        display: flex;
        justify-content: center;
        padding-bottom: 30px;
    }
}
</style>
