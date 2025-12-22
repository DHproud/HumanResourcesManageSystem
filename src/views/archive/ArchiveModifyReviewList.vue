<template>
  <div class="archive-modify-review-page">
    <el-card>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h2>已存档修改复核（仅针对已存在档案的修改）</h2>
        <div>
          <el-button type="primary" @click="fetchList">刷新</el-button>
        </div>
      </div>

      <div style="margin-top:12px;">
        <el-form :inline="true">
          <el-form-item label="状态">
            <el-select v-model="filter.status" placeholder="状态" style="width:160px;">
              <el-option :label="'待复核(0)'" :value="0" />
              <el-option :label="'通过(1)'" :value="1" />
              <el-option :label="'拒绝(2)'" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onFilter">查询</el-button>
            <el-button @click="resetFilter" style="margin-left:8px;">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
          :data="requests"
          style="width:100%;margin-top:12px;"
          v-loading="loading"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="archiveId" label="档案ID" width="120" />
        <el-table-column prop="requester" label="申请人" width="160" />
        <el-table-column prop="requestTime" label="申请时间" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <span v-if="scope.row.status === 0">待复核</span>
            <span v-else-if="scope.row.status === 1">通过</span>
            <span v-else-if="scope.row.status === 2">拒绝</span>
            <span v-else>未知</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="320">
          <template #default="scope">
            <el-button size="mini" @click="goToDetail(scope.row)">预览</el-button>

            <el-button
                v-if="scope.row.status === 0"
                type="success"
                size="mini"
                style="margin-left:8px;"
                @click="confirmApprove(scope.row.id)"
            >通过</el-button>

            <el-button
                v-if="scope.row.status === 0"
                type="danger"
                size="mini"
                style="margin-left:8px;"
                @click="openRejectDialog(scope.row.id)"
            >拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:12px;display:flex;justify-content:space-between;align-items:center;">
        <div>第 {{ page }} 页 / 每页 {{ size }} 条</div>
        <el-pagination
            background
            layout="prev, pager, next, jumper"
            :current-page="page"
            :page-size="size"
            :total="total"
            @current-change="onPageChange"
        />
      </div>
    </el-card>

    <!-- 拒绝原因对话框（使用 v-model，更兼容） -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝修改申请">
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const role = localStorage.getItem('role') || ''

const loading = ref(false)
const requests = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filter = reactive({ status: 0 }) // 默认看待复核

const rejectDialogVisible = ref(false)
const rejectRemark = ref('')
let currentRejectId = null

onMounted(() => {
  console.log('[ArchiveModifyReviewList] mounted, role=', role)
  fetchList()
})

function buildHeaders() {
  return { Role: role }
}

async function fetchList() {
  if (role !== 'MANAGER') {
    requests.value = []
    total.value = 0
    ElMessage.warning('权限不足：只有人事经理可以查看此页面')
    return
  }

  loading.value = true
  try {
    const params = { page: page.value, size: size.value, status: filter.status }
    const res = await request.get('/api/archive/editRequest/list', { params, headers: buildHeaders() })
    if (res && res.code === 200) {
      // 仅保留针对已存在档案的修改申请（archiveId 非空/非0）
      const all = res.data.records || []
      const filtered = all.filter(r => r.archiveId !== null && r.archiveId !== 0)
      requests.value = filtered
      // total: 如果后端返回总数且你要分页，请使用后端的 total；这里因前端过滤，我们取 filtered.length
      total.value = (res.data.total != null) ? res.data.total : filtered.length
    } else {
      ElMessage.error(res?.msg || '获取复核列表失败')
      requests.value = []
      total.value = 0
    }
  } catch (e) {
    console.error('fetchList error', e)
    ElMessage.error('获取复核列表失败：网络或后端错误')
    requests.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onFilter() {
  page.value = 1
  fetchList()
}
function resetFilter() {
  filter.status = 0
  page.value = 1
  fetchList()
}
function onPageChange(p) {
  page.value = p
  fetchList()
}

function goToDetail(row) {
  try {
    sessionStorage.setItem(`archiveModifyPreview_${row.id}`, JSON.stringify(row))
  } catch (e) {
    console.warn('sessionStorage 写入失败', e)
  }
  router.push({ name: 'ArchiveModifyReviewDetail', params: { id: row.id } })
}

async function confirmApprove(id) {
  try {
    await ElMessageBox.confirm('确认通过该修改申请？通过后档案将被更新。', '确认通过', { type: 'warning' })
    await doApprove(id)
  } catch (e) {
    // user cancelled
  }
}

async function doApprove(id) {
  try {
    console.log('doApprove called id=', id)
    const res = await request.post(`/api/archive/editRequest/approve/${id}`, null, { headers: buildHeaders() })
    if (res && (res.code === 200 || res.status === 200)) {
      ElMessage.success('审批通过，档案已更新')
      fetchList()
    } else {
      ElMessage.error(res?.msg || '审批失败')
    }
  } catch (e) {
    console.error('approve error', e)
    ElMessage.error('审批失败：网络或后端错误')
  }
}

function openRejectDialog(id) {
  console.log('openRejectDialog id=', id)
  currentRejectId = id
  rejectRemark.value = ''
  rejectDialogVisible.value = true
}

async function doReject() {
  if (!currentRejectId) {
    ElMessage.warning('目标申请未选择')
    return
  }
  // 防止重复点击
  if (!rejectDialogVisible.value) {
    console.log('reject dialog not visible, abort')
    return
  }
  try {
    console.log('doReject called id=', currentRejectId, 'remark=', rejectRemark.value)
    const body = { remark: rejectRemark.value || '' }
    const res = await request.post(`/api/archive/editRequest/reject/${currentRejectId}`, body, { headers: buildHeaders() })
    if (res && (res.code === 200 || res.status === 200)) {
      ElMessage.success('已拒绝该申请')
      rejectDialogVisible.value = false
      currentRejectId = null
      fetchList()
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
.archive-modify-review-page { padding: 16px; }
</style>