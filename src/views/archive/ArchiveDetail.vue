<template>
  <el-card>
    <div style="display:flex;justify-content:space-between;align-items:center;">
      <h2>档案明细</h2>
      <div>
        <el-button @click="goBack">返回</el-button>
        <el-button type="warning" v-if="canEdit" @click="toEdit" style="margin-left:8px;">编辑</el-button>
      </div>
    </div>

    <div v-if="archive" style="margin-top:12px;">
      <el-descriptions bordered column="2" style="margin-top:12px;">
        <el-descriptions-item label="档案编号">{{ archive.archiveCode }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ archive.name }}</el-descriptions-item>
        <el-descriptions-item label="职位">{{ archive.positionName }}</el-descriptions-item>
        <el-descriptions-item label="建档时间">{{ archive.registTime }}</el-descriptions-item>
        <el-descriptions-item label="一级机构">{{ archive.firstLevelOrgName }}</el-descriptions-item>
        <el-descriptions-item label="二级机构">{{ archive.secondLevelOrgName }}</el-descriptions-item>
        <el-descriptions-item label="三级机构">{{ archive.thirdLevelOrgName }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ archive.mobile }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ archive.email }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ archive.address }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div v-else style="text-align:center;padding:18px;">正在加载...</div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const id = route.params.id
const archive = ref(null)
const role = localStorage.getItem('role') || ''
const canEdit = (role === 'SPECIALIST')

onMounted(async () => {
  try {
    // 尝试 GET /api/archive/{id}，若不存在再尝试 /api/archive/detail/{id}
    let res = await request.get(`/api/archive/${id}`)
    if (!res || res.code === undefined) {
      res = await request.get(`/api/archive/detail/${id}`)
    }
    const data = res && res.code !== undefined ? res.data : res
    if (data) archive.value = data
    else ElMessage.error('未获取到档案数据')
  } catch (e) {
    console.error(e)
    ElMessage.error('获取档案失败')
  }
})

function goBack() {
  router.push({ name: 'ArchiveQuery' })
}
function toEdit() {
  router.push({ name: 'ArchiveEditPage', params: { id } })
}
</script>

<style scoped>
</style>