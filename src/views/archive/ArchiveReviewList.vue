<template>
  <div class="review-list">
    <el-card>
      <div slot="header"><span>待复核档案列表</span></div>
      <el-table :data="tableData" border style="width: 100%">
        <el-table-column prop="archiveCode" label="档案编号" width="180" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="firstLevelOrgName" label="一级机构" />
        <el-table-column prop="secondLevelOrgName" label="二级机构" />
        <el-table-column prop="thirdLevelOrgName" label="三级机构" />
        <el-table-column prop="positionName" label="职位" />
        <el-table-column prop="registTime" label="登记时间" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleReview(scope.row)">复核</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 复核详情弹窗 -->
    <el-dialog v-model="reviewDialogVisible" title="档案复核" width="80%" top="5vh">
      <archive-review-detail :archive-data="currentArchive" @close="closeReview" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import ArchiveReviewDetail from './ArchiveReviewDetail.vue' // 下面会创建这个组件

const tableData = ref([])
const reviewDialogVisible = ref(false)
const currentArchive = ref({})

const loadData = async () => {
  const res = await request.get('/api/archive/list/pending')
  if(res.code === 200) tableData.value = res.data
}

const handleReview = (row) => {
  currentArchive.value = JSON.parse(JSON.stringify(row)) // 深拷贝防止直接污染
  reviewDialogVisible.value = true
}

const closeReview = (isSuccess) => {
  reviewDialogVisible.value = false
  if(isSuccess) loadData() // 刷新列表
}

onMounted(() => {
  loadData()
})
</script>
<style scoped> .review-list { padding: 20px; } </style>