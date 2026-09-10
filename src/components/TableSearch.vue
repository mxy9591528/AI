<template>
    <el-form :model="formData" inline class="table-search">
        <el-form-item
            v-for="item in formItem"
            :key="item.prop"
            :label="item.label"
        >
            <el-input
                v-if="item.comp === 'input'"
                v-model="formData[item.prop]"
                :placeholder="item.placeholder"
                clearable
            />
            <el-select
                v-else-if="item.comp === 'select'"
                v-model="formData[item.prop]"
                :placeholder="item.placeholder"
                clearable
                style="width: 180px"
            >
                <el-option
                    v-for="opt in item.options || []"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                />
            </el-select>
            <el-date-picker
                v-else-if="item.comp === 'daterange'"
                v-model="formData[item.prop]"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 260px"
            />
        </el-form-item>
        <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
        </el-form-item>
    </el-form>
</template>
<script setup>
import { reactive, watch } from 'vue'

const props = defineProps({
    formItem: {
        type: Array,
        default: () => []
    }
})

const emit = defineEmits(['search'])

const buildFormData = () => {
    const data = {}
    props.formItem.forEach(item => {
        data[item.prop] = ''
    })
    return data
}

const formData = reactive(buildFormData())

// 表单项定义变化时重建表单数据
watch(() => props.formItem, () => {
    Object.assign(formData, buildFormData())
})

const handleSearch = () => {
    emit('search', { ...formData })
}

const handleReset = () => {
    Object.keys(formData).forEach(key => {
        formData[key] = ''
    })
    emit('search', { ...formData })
}
</script>
<style scoped lang="scss">
.table-search {
    margin-top: 10px;
}
</style>
