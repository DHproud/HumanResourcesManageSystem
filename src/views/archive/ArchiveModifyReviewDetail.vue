<template>
  <div class="archive-modify-review-detail">
    <el-card>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h2>修改申请明细（已存档修改）</h2>
        <div>
          <el-button @click="goBack">返回</el-button>
        </div>
      </div>

      <div v-if="loading" style="text-align:center;padding:20px;">加载中...</div>

      <div v-else-if="!requestRow" style="text-align:center;padding:20px;">未找到该申请</div>

      <div v-else style="margin-top:12px;">
        <el-descriptions bordered column="2">
          <el-descriptions-item label="申请ID">{{ requestRow.id }}</el-descriptions-item>
          <el-descriptions-item label="档案ID">{{ requestRow.archiveId || '新建' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ requestRow.requester }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ requestRow.requestTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <span v-if="requestRow.status === 0">待复核</span>
            <span v-else-if="requestRow.status === 1">通过</span>
            <span v-else-if="requestRow.status === 2">拒绝</span>
            <span v-else>未知</span>
          </el-descriptions-item>
          <el-descriptions-item label="复核人/时间">{{ requestRow.reviewer || '-' }} / {{ requestRow.reviewTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ requestRow.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h3 style="margin-top:12px;">修改后数据预览</h3>
        <el-card style="margin-top:8px;">
          <pre style="white-space:pre-wrap; max-height:420px; overflow:auto;">{{ prettyPreview }}</pre>
        </el-card>

        <div style="text-align:right;margin-top:12px;">
          <el-button v-if="canApprove" type="success" @click="confirmApprove">通过</el-button>
          <el-button v-if="canApprove" type="danger" style="margin-left:8px;" @click="openRejectDialog">拒绝</el-button>
        </div>

        <!-- 临时显示状态便于调试（发布时可删除） -->
        <div style="margin-top:8px;color:#888;font-size:12px;">
          Debug role: {{ role }} | rejectDialogVisible: {{ rejectDialogVisible }}
        </div>
      </div>
    </el-card>

    <!-- 使用 v-model 更兼容 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝修改申请" :destroy-on-close="false" :close-on-click-modal="false">
      <el-form>
        <el-form-item label="原因" label-width="80px">
          <el-input type="textarea" v-model="rejectRemark" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doReject">提交拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const id = route.params.id

const role = localStorage.getItem('role') || ''
const username = localStorage.getItem('username') || ''

const loading = ref(true)
const requestRow = ref(null)
const previewData = ref(null)

const rejectDialogVisible = ref(false)
const rejectRemark = ref('')

const canApprove = computed(() => role === 'MANAGER')

onMounted(async () => {
  console.log('[ArchiveModifyReviewDetail] mounted, role=', role, 'id=', id)
  await loadData()
})

function buildHeaders() {
  return { Role: role }
}

async function loadData() {
  loading.value = true
  try {
    const key = `archiveModifyPreview_${id}`
    const raw = sessionStorage.getItem(key)
    if (raw) {
      requestRow.value = JSON.parse(raw)
      try { previewData.value = JSON.parse(requestRow.value.newData || '{}') } catch (e) { previewData.value = requestRow.value.newData || {} }
      loading.value = false
      return
    }

    const res = await request.get('/api/archive/editRequest/list', { params: { page: 1, size: 1000, status: 0 }, headers: buildHeaders() })
    if (res && res.code === 200) {
      const found = (res.data.records || []).find(r => String(r.id) === String(id))
      if (found) {
        requestRow.value = found
        try { previewData.value = JSON.parse(found.newData || '{}') } catch (e) { previewData.value = found.newData || {} }
      } else {
        requestRow.value = null
      }
    } else {
      requestRow.value = null
    }
  } catch (e) {
    console.error('loadData error', e)
    requestRow.value = null
  } finally {
    loading.value = false
  }
}

const prettyPreview = computed(() => {
  try { return JSON.stringify(previewData.value, null, 2) } catch (e) { return String(previewData.value) }
})

function goBack() {
  router.push({ name: 'ArchiveModifyReview' })
}

async function confirmApprove() {
  try {
    await ElMessageBox.confirm('确认通过该修改申请？通过后档案将被更新。', '确认通过', { type: 'warning' })
    await doApprove()
  } catch (e) {
    // cancelled
  }
}

async function doApprove() {
  try {
    console.log('doApprove called, id=', id)
    const res = await request.post(`/api/archive/editRequest/approve/${id}`, null, { headers: buildHeaders() })
    if (res && (res.code === 200 || res.status === 200)) {
      ElMessage.success('审批通过，档案已更新')
      try { sessionStorage.removeItem(`archiveModifyPreview_${id}`) } catch (e) {}
      router.push({ name: 'ArchiveModifyReview' })
    } else {
      ElMessage.error(res?.msg || '审批失败')
    }
  } catch (e) {
    console.error('approve error', e)
    ElMessage.error('审批失败：网络或后端错误')
  }
}

function openRejectDialog() {
  console.log('openRejectDialog called')
  rejectRemark.value = ''
  rejectDialogVisible.value = true
}

async function doReject() {
  if (!rejectDialogVisible.value) {
    console.log('reject dialog not visible, abort')
    return
  }
  try {
    console.log('doReject called, id=', id, 'remark=', rejectRemark.value)
    const body = { remark: rejectRemark.value || '' }
    const res = await request.post(`/api/archive/editRequest/reject/${id}`, body, { headers: buildHeaders() })
    if (res && (res.code === 200 || res.status === 200)) {
      ElMessage.success('已拒绝该申请')
      try { sessionStorage.removeItem(`archiveModifyPreview_${id}`) } catch (e) {}
      rejectDialogVisible.value = false
      router.push({ name: 'ArchiveModifyReview' })
    } else {
      ElMessage.error(res?.msg || '操作失败')
    }
  } catch (e) {
    console.error('reject error', e)
    ElMessage.error('操作失败：网络或后端错误')
  }
}
</script>

<style scoped>
.archive-modify-review-detail { padding: 16px; }
</style>