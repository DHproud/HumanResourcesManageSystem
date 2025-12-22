<template>
  <div class="salary-standard-page">
    <el-card>
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <h2>薪酬标准管理</h2>
        <div>
          <el-button type="primary" @click="goToCreate">新增薪酬标准</el-button>
          <el-button style="margin-left:8px;" @click="fetchList">刷新</el-button>
        </div>
      </div>

      <!-- 搜索区域 -->
      <div style="margin-top:12px;">
        <el-form :inline="true" class="search-form">
          <el-form-item label="标准编号">
            <el-input v-model="q.standardCode" placeholder="输入标准编号（支持模糊）" style="width:220px;" />
          </el-form-item>

          <el-form-item label="关键字">
            <el-input v-model="q.keyword" placeholder="名称/制定人/变更人/复核人（支持模糊）" style="width:320px;" />
          </el-form-item>

          <el-form-item label="登记时间起">
            <el-date-picker v-model="q.startDate" type="date" placeholder="开始日期" value-format="yyyy-MM-dd" />
          </el-form-item>

          <el-form-item label="登记时间止">
            <el-date-picker v-model="q.endDate" type="date" placeholder="结束日期" value-format="yyyy-MM-dd" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="onSearch">查询</el-button>
            <el-button @click="resetSearch" style="margin-left:8px;">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="rows" v-loading="loading" style="width:100%;margin-top:12px;">
        <el-table-column label="编号" width="220">
          <template #default="scope">{{ scope.row.standard?.standardCode || scope.row.standardCode || '' }}</template>
        </el-table-column>
        <el-table-column label="名称"><template #default="scope">{{ scope.row.standard?.standardName || scope.row.standardName }}</template></el-table-column>
        <el-table-column label="制定人" width="120"><template #default="scope">{{ scope.row.standard?.author || scope.row.author }}</template></el-table-column>
        <el-table-column label="适用职位" width="180"><template #default="scope">{{ scope.row.standard?.applicablePosition || scope.row.applicablePosition }}</template></el-table-column>
        <el-table-column label="登记时间" width="160"><template #default="scope">{{ formatDate(scope.row.standard?.registerTime || scope.row.registerTime) }}</template></el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="scope">
            <el-button size="mini" @click="openViewModal(scope.row)">查看</el-button>
            <el-button size="mini" type="primary" @click="goToEdit(scope.row)" style="margin-left:8px;">编辑</el-button>
            <el-button size="mini" type="danger" style="margin-left:8px;" @click="confirmDeleteByCode(getCodeFromRow(scope.row))">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:12px; display:flex; justify-content:flex-end;">
        <el-pagination background layout="prev, pager, next, jumper" :current-page="page" :page-size="size" :total="total" @current-change="onPageChange" />
      </div>
    </el-card>

    <!-- 查看弹窗 -->
    <el-dialog v-model="viewVisible" width="820px" :before-close="() => (viewVisible = false)">
      <template #title>
        <span>薪酬标准详情</span>
        <el-button style="float:right;" type="text" @click="viewVisible = false">关闭</el-button>
      </template>

      <div v-if="viewData">
        <el-descriptions border :column="2">
          <el-descriptions-item label="编号">{{ viewData.standard?.standardCode || viewData.standardCode }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ viewData.standard?.standardName || viewData.standardName }}</el-descriptions-item>
          <el-descriptions-item label="制定人">{{ viewData.standard?.author || viewData.author }}</el-descriptions-item>
          <el-descriptions-item label="登记人">{{ viewData.standard?.registrant || viewData.registrant }}</el-descriptions-item>
          <el-descriptions-item label="适用职位">{{ viewData.standard?.applicablePosition || viewData.applicablePosition }}</el-descriptions-item>
          <el-descriptions-item label="登记时间">{{ formatDate(viewData.standard?.registerTime || viewData.registerTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>薪酬明细</el-divider>
        <el-table :data="viewItems" style="width:100%">
          <el-table-column prop="projectCode" label="项目编号" width="140" />
          <el-table-column prop="projectName" label="项目名称" />
          <el-table-column prop="amount" label="金额" width="160" />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request, { encodeId } from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

// 查询条件对象
const q = ref({
  standardCode: '',
  keyword: '',
  startDate: null, // 'YYYY-MM-DD'
  endDate: null
})

const viewVisible = ref(false)
const viewData = ref(null)
const viewItems = ref([])

function buildHeaders() { return { Username: localStorage.getItem('username') || '' } }

onMounted(() => fetchList())

function formatDate(v) { return v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '' }
function getCodeFromRow(row) { return row?.standard?.standardCode || row?.standardCode || '' }

// 构造请求参数：仅加入用户填写的条件
function buildQueryParams() {
  const params = { page: page.value, size: size.value }
  if (q.value.standardCode && q.value.standardCode.trim()) {
    params.standardCode = q.value.standardCode.trim()
  }
  if (q.value.keyword && q.value.keyword.trim()) {
    params.keyword = q.value.keyword.trim()
    // 兼容旧后端的 name 参数（回退）
    params.name = q.value.keyword.trim()
  }
  if (q.value.startDate) {
    // start of day
    params.startTime = dayjs(q.value.startDate).startOf('day').format('YYYY-MM-DD HH:mm:ss')
  }
  if (q.value.endDate) {
    // end of day
    params.endTime = dayjs(q.value.endDate).endOf('day').format('YYYY-MM-DD HH:mm:ss')
  }
  return params
}

async function fetchList() {
  loading.value = true
  try {
    const params = buildQueryParams()
    const res = await request.get('/api/salary/standard/page', { params, headers: buildHeaders() })
    if (res && res.code === 200) {
      rows.value = (res.data?.records || []).map(r => (r.standard ? r : { standard: r }))
      total.value = res.data?.total || 0
      page.value = res.data?.current || page.value
      size.value = res.data?.size || size.value
    } else {
      ElMessage.error(res?.msg || '获取列表失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取列表失败：网络或后端错误')
  } finally {
    loading.value = false
  }
}

// 搜索（含时间校验）
function onSearch() {
  // validate time range
  if (q.value.startDate && q.value.endDate) {
    const s = dayjs(q.value.startDate)
    const e = dayjs(q.value.endDate)
    if (s.isAfter(e, 'day')) {
      ElMessage.warning('起始日期不能晚于结束日期')
      return
    }
  }
  page.value = 1
  fetchList()
}

function resetSearch() {
  q.value.standardCode = ''
  q.value.keyword = ''
  q.value.startDate = null
  q.value.endDate = null
  page.value = 1
  fetchList()
}

function onPageChange(p) { page.value = p; fetchList() }

// 查看：优先使用行数据；若没有 items 则按 code 去查 id -> detail
async function openViewModal(row) {
  viewData.value = row
  viewItems.value = row.items || row.standard?.items || []
  if ((viewItems.value && viewItems.value.length > 0) || (row.standard && row.standard.standardCode && row.standard.standardName)) {
    viewVisible.value = true
    return
  }
  const code = getCodeFromRow(row)
  if (!code) { ElMessage.error('记录缺少编号，无法查看'); return }
  const id = await findStandardIdByCode(code)
  if (!id) { ElMessage.error('未找到该标准'); return }
  try {
    const res = await request.get(`/api/salary/standard/${encodeId(id)}`, { headers: buildHeaders() })
    if (res && res.code === 200) {
      viewData.value = res.data || {}
      viewItems.value = res.data?.items || []
      viewVisible.value = true
    } else ElMessage.error(res?.msg || '获取详情失败')
  } catch (e) {
    console.error(e)
    ElMessage.error('获取详情失败')
  }
}

// 编辑：把行缓存到 sessionStorage 以便编辑页优先使用
function goToEdit(row) {
  const code = getCodeFromRow(row)
  if (!code) { ElMessage.error('记录没有编号，无法编辑'); return }
  try { sessionStorage.setItem(`salary_standard_record_${code}`, JSON.stringify(row)) } catch (e) { console.warn('sessionStorage set error', e) }
  router.push({ path: `/salary/standard/change/${encodeURIComponent(code)}` })
}

function goToCreate() { router.push({ path: '/salary/standard/edit' }) }

// 查 id 的逻辑（增强匹配）
async function findStandardIdByCode(code) {
  if (!code) return null
  try {
    const res = await request.get('/api/salary/standard/page', { params: { page: 1, size: 50, name: code }, headers: buildHeaders() })
    if (res && res.code === 200) {
      const list = res.data?.records || []
      for (const r of list) {
        const s = r.standard || r
        if (s.standardCode === code || String(s.id) === code || (r.standardIdStr && String(r.standardIdStr) === code)) {
          return r.standardIdStr || (s.id != null ? String(s.id) : null)
        }
      }
    }
  } catch (e) {
    console.error('findStandardIdByCode page error', e)
  }
  return null
}

async function confirmDeleteByCode(code) {
  try {
    await ElMessageBox.confirm('确认删除该记录？', '确认删除', { type: 'warning' })
    const id = await findStandardIdByCode(code)
    if (!id) { ElMessage.error('未找到该记录'); return }
    const res = await request.delete(`/api/salary/standard/delete/${encodeId(id)}`, { headers: buildHeaders() })
    if (res && res.code === 200) {
      ElMessage.success('删除成功'); fetchList()
    } else ElMessage.error(res?.msg || '删除失败')
  } catch (e) { /* cancel */ }
}
</script>

<style scoped>
.salary-standard-page { padding: 16px; }
.search-form { margin-bottom: 8px; }
</style>