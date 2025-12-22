<template>
  <div class="salary-review-detail">
    <el-card>
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <h2>薪酬标准复核 - {{ standard.standardCode || '' }}</h2>
        <div><el-button @click="goBack">返回</el-button></div>
      </div>

      <el-descriptions border :column="2" style="margin-top:12px;">
        <el-descriptions-item label="编号">{{ standard.standardCode }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ standard.standardName }}</el-descriptions-item>
        <el-descriptions-item label="制定人">{{ standard.author }}</el-descriptions-item>
        <el-descriptions-item label="登记人">{{ standard.registrant }}</el-descriptions-item>
        <el-descriptions-item label="适用职位">{{ standard.applicablePosition }}</el-descriptions-item>
        <el-descriptions-item label="登记时间">{{ formatDate(standard.registerTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>薪酬明细</el-divider>
      <el-table :data="items" style="width:100%" v-loading="loading">
        <el-table-column prop="projectCode" label="项目编号" width="140" />
        <el-table-column prop="projectName" label="项目名称" />
        <el-table-column prop="amount" label="金额" width="160" />
      </el-table>

      <el-divider>复核意见</el-divider>
      <el-form :model="reviewForm" ref="formRef">
        <el-form-item label="复核意见"><el-input type="textarea" v-model="reviewForm.comment" :rows="6" /></el-form-item>
        <div style="display:flex; justify-content:flex-end; gap:12px;">
          <el-button type="danger" @click="doReview('reject')">拒绝</el-button>
          <el-button type="primary" @click="doReview('approve')">通过</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request, { encodeId } from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const code = route.params.code ? String(route.params.code) : null

const loading = ref(false)
const standard = ref({})
const items = ref([])
const reviewForm = ref({ comment: '' })

function buildHeaders() { return { Username: localStorage.getItem('username') || '' } }

onMounted(() => {
  if (!code) { ElMessage.error('缺少记录标识'); return }
  loadByCode(code)
})

async function findStandardIdByCode(code) {
  if (!code) return null
  try {
    const res = await request.get('/api/salary/standard/page', { params: { page: 1, size: 20, name: code }, headers: buildHeaders() })
    if (res && res.code === 200) {
      const list = res.data?.records || []
      for (const r of list) {
        const s = r.standard || {}
        if (s.standardCode === code) return r.standardIdStr || (s.id != null ? String(s.id) : null)
      }
    }
  } catch (e) { console.error(e) }
  return null
}

async function loadByCode(code) {
  const id = await findStandardIdByCode(code)
  if (!id) { ElMessage.error('未找到该记录'); return }
  try {
    const res = await request.get(`/api/salary/standard/${encodeId(id)}`, { headers: buildHeaders() })
    if (res && res.code === 200) {
      standard.value = res.data?.standard || {}
      items.value = res.data?.items || []
    } else ElMessage.error(res?.msg || '获取详情失败')
  } catch (e) { console.error(e); ElMessage.error('获取详情失败') }
}

function goBack() { router.push({ path: '/salary/review' }) }
function formatDate(v) { return v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '' }

async function doReview(action) {
  if (!reviewForm.value.comment || reviewForm.value.comment.trim() === '') { ElMessage.warning('请填写复核意见'); return }
  await ElMessageBox.confirm(`确认要 ${action === 'approve' ? '通过' : '拒绝'} 该薪酬标准？`, '复核确认', { type: 'warning' })
  const id = await findStandardIdByCode(code)
  if (!id) { ElMessage.error('找不到记录ID，无法复核'); return }
  try {
    const res = await request.post(`/api/salary/standard/review/${encodeId(id)}`, { action, comment: reviewForm.value.comment }, { headers: buildHeaders() })
    if (res && res.code === 200) {
      ElMessage.success(res.msg || (action === 'approve' ? '复核通过' : '复核拒绝'))
      router.push({ path: '/salary/review' })
    } else ElMessage.error(res?.msg || '复核失败')
  } catch (e) { console.error(e); ElMessage.error('复核失败') }
}
</script>

<style scoped>
.salary-review-detail { padding: 16px; }
</style>