<template>
  <div class="archive-search-page">
    <el-card>
      <div class="header" style="display:flex;justify-content:space-between;align-items:center;">
        <h2>人力资源档案查询</h2>
        <div>当前用户：<strong>{{ username }}</strong>（角色：{{ role || '未知' }}）</div>
      </div>

      <el-form :model="form" label-width="100px" class="search-form" style="margin-top:12px;">
        <el-row :gutter="16">
          <el-col :span="5">
            <el-form-item label="一级机构">
              <el-select v-model="selected.first" placeholder="选择一级机构" clearable @change="onFirstChange">
                <el-option v-for="o in orgsFirst" :key="o.id" :label="o.orgName" :value="o.id" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="5">
            <el-form-item label="二级机构">
              <el-select v-model="selected.second" placeholder="选择二级机构" clearable @change="onSecondChange">
                <el-option v-for="o in orgsSecond" :key="o.id" :label="o.orgName" :value="o.id" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="5">
            <el-form-item label="三级机构">
              <el-select v-model="selected.third" placeholder="选择三级机构" clearable>
                <el-option v-for="o in orgsThird" :key="o.id" :label="o.orgName" :value="o.id" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="4">
            <el-form-item label="职位名称">
              <el-select v-model="form.positionName" placeholder="选择职位" clearable>
                <el-option v-for="p in positionOptions" :key="p" :label="p" :value="p" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="5">
            <el-form-item label="状态">
              <el-select v-model="form.status" placeholder="选择状态" clearable>
                <el-option :label="'全部'" :value="null" />
                <el-option :label="'待复核（0）'" :value="0" />
                <el-option :label="'正常（1）'" :value="1" />
                <!-- 已删除仅对非 SPECIALIST 可见 -->
                <el-option v-if="role !== 'SPECIALIST'" :label="'已删除（2）'" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16" style="margin-top:8px;">
          <el-col :span="6">
            <el-form-item label="建档起始">
              <el-date-picker v-model="form.startTime" type="date" placeholder="起始日期" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="建档结束">
              <el-date-picker v-model="form.endTime" type="date" placeholder="结束日期" style="width:100%;" />
            </el-form-item>
          </el-col>

          <el-col :span="12" class="buttons-col" style="display:flex;align-items:center;">
            <el-button type="primary" @click="onSearch">查询</el-button>
            <el-button @click="onReset" style="margin-left:10px;">重置条件</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card style="margin-top:12px;">
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h3>查询结果（共 {{ total }} 条）</h3>
        <el-button type="primary" @click="doSearch">刷新</el-button>
      </div>

      <el-table :data="records" stripe style="width:100%;margin-top:12px;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="archiveCode" label="档案编号" width="180" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="positionName" label="职位" width="140" />
        <el-table-column prop="firstLevelOrgName" label="一级机构" width="160" />
        <el-table-column prop="registTime" label="建档时间" width="180" />
        <el-table-column label="操作" width="320">
          <template #default="scope">
            <el-button size="mini" @click="toDetail(scope.row.id)">查看</el-button>

            <el-button size="mini" type="warning" v-if="role === 'SPECIALIST'" @click="toEdit(scope.row.id)" style="margin-left:8px;">编辑</el-button>

            <el-button
                v-if="role === 'MANAGER' && scope.row.status === 1"
                size="mini"
                type="danger"
                style="margin-left:8px;"
                @click="confirmMarkDeleted(scope.row.id)"
            >标记删除</el-button>

            <el-button
                v-if="role === 'MANAGER' && scope.row.status === 2"
                size="mini"
                type="success"
                style="margin-left:8px;"
                @click="confirmRecover(scope.row.id)"
            >恢复</el-button>

            <el-tooltip v-if="role === 'MANAGER' && scope.row.status === 0" content="待复核档案不可删除" placement="top">
              <el-button size="mini" disabled style="margin-left:8px;">删除</el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:12px;display:flex;justify-content:space-between;align-items:center;">
        <div>当前第 {{ page }} 页 / 共 {{ totalPages }} 页</div>
        <el-pagination
            background
            layout="prev, pager, next"
            :current-page="page"
            :page-size="size"
            :total="total"
            @current-change="onPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const role = localStorage.getItem('role') || ''
const username = localStorage.getItem('username') || ''
const canEdit = computed(() => role === 'SPECIALIST')

const orgsFirst = ref([])
const orgsSecond = ref([])
const orgsThird = ref([])
const positionOptions = ref([])

const selected = reactive({ first: null, second: null, third: null })
const form = reactive({ positionName: '', startTime: null, endTime: null, status: null })

const page = ref(1)
const size = ref(10)
const total = ref(0)
const records = ref([])

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

onMounted(() => {
  loadOrgChildren(0, orgsFirst)
  loadPositions()
})

async function loadOrgChildren(parentId, targetRef) {
  try {
    const res = await request.get(`/api/org/list/${parentId}`)
    if (res && res.code === 200 && Array.isArray(res.data)) {
      targetRef.value = res.data
      return
    }
    targetRef.value = []
  } catch (e) {
    targetRef.value = []
    console.error('loadOrgChildren error', e)
  }
}

function onFirstChange(val) {
  orgsSecond.value = []
  orgsThird.value = []
  selected.second = null
  selected.third = null
  if (val != null) loadOrgChildren(val, orgsSecond)
}
function onSecondChange(val) {
  orgsThird.value = []
  selected.third = null
  if (val != null) loadOrgChildren(val, orgsThird)
}

async function loadPositions() {
  try {
    let res = await request.get('/api/position/listAll')
    if (res && res.code === 200 && Array.isArray(res.data)) {
      positionOptions.value = res.data.map(p => p.name || p.positionName || String(p))
      return
    }
    res = await request.get('/api/position/list')
    if (res && res.code === 200 && Array.isArray(res.data)) {
      positionOptions.value = res.data.map(p => p.name || p.positionName || String(p))
      return
    }
  } catch (e) {
    console.error('loadPositions error', e)
  }
  positionOptions.value = []
}

function formatDate(dateObj, isStart) {
  if (!dateObj) return ''
  const d = new Date(dateObj)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return isStart ? `${yyyy}-${mm}-${dd} 00:00:00` : `${yyyy}-${mm}-${dd} 23:59:59`
}

function hasAnyCondition() {
  return !!(selected.first || selected.second || selected.third || form.positionName || form.startTime || form.endTime || form.status !== null)
}

function buildPayload() {
  // 前端二次保护：SPECIALIST 不允许请求 status=2
  let statusValue = form.status === null || form.status === undefined ? null : Number(form.status)
  if (role === 'SPECIALIST' && statusValue === 2) {
    console.warn('[SECURITY] SPECIALIST attempted to request status=2; overriding to status=1 on frontend')
    statusValue = 1
  }

  return {
    page: page.value,
    size: size.value,
    firstLevelOrgId: selected.first || null,
    secondLevelOrgId: selected.second || null,
    thirdLevelOrgId: selected.third || null,
    positionName: form.positionName || '',
    startTime: form.startTime ? formatDate(form.startTime, true) : '',
    endTime: form.endTime ? formatDate(form.endTime, false) : '',
    status: statusValue
  }
}

async function onSearch() {
  if (!hasAnyCondition()) {
    ElMessage.warning('请先选择查询条件')
    return
  }
  page.value = 1
  await doSearch()
}

async function doSearch() {
  const payload = buildPayload()
  try {
    const res = await request.post('/api/archive/search', payload)
    if (res && res.code === 200) {
      applyPageData(res.data)
      return
    } else {
      console.warn('[doSearch] 非200返回', res)
    }
  } catch (e) {
    console.error('[doSearch] 请求异常', e)
  }
  records.value = []
  total.value = 0
}

function applyPageData(pageData) {
  records.value = pageData.records || []
  total.value = pageData.total || (records.value.length)
  page.value = pageData.current || page.value
  size.value = pageData.size || size.value
}

function onReset() {
  selected.first = null
  selected.second = null
  selected.third = null
  form.positionName = ''
  form.startTime = null
  form.endTime = null
  form.status = null
  page.value = 1
  records.value = []
  total.value = 0
}

function onPageChange(p) {
  page.value = p
  doSearch()
}

/* 页面跳转 */
function toDetail(id) {
  if (!id) return
  router.push({ name: 'ArchiveDetail', params: { id } })
}
function toEdit(id) {
  if (!id) return
  if (role !== 'SPECIALIST') {
    ElMessage.warning('只有人事专员可以变更档案')
    return
  }
  router.push({ name: 'ArchiveEditPage', params: { id } })
}

/* 删除 / 恢复：仅 MANAGER */
async function confirmMarkDeleted(id) {
  if (role !== 'MANAGER') {
    ElMessage.warning('权限不足')
    return
  }
  try {
    await ElMessageBox.confirm('确认要将此档案标记为已删除吗？此操作可恢复。', '确认删除', { type: 'warning' })
    await markDeleted(id)
  } catch (e) {}
}

async function markDeleted(id) {
  try {
    const headers = { Role: role }
    const res = await request.post(`/api/archive/markDeleted/${id}`, null, { headers })
    if (res && res.code === 200) {
      ElMessage.success('标记删除成功')
      form.status = 2
      page.value = 1
      await doSearch()
    } else {
      ElMessage.error(res?.msg || '删除失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('删除失败：网络或服务器错误')
  }
}

async function confirmRecover(id) {
  if (role !== 'MANAGER') {
    ElMessage.warning('权限不足')
    return
  }
  try {
    await ElMessageBox.confirm('确认要恢复此档案为正常状态吗？', '确认恢复', { type: 'info' })
    await recover(id)
  } catch (e) {}
}

async function recover(id) {
  try {
    const headers = { Role: role }
    const res = await request.post(`/api/archive/recover/${id}`, null, { headers })
    if (res && res.code === 200) {
      ElMessage.success('恢复成功')
      form.status = 1
      page.value = 1
      await doSearch()
    } else {
      ElMessage.error(res?.msg || '恢复失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('恢复失败：网络或服务器错误')
  }
}
</script>

<style scoped>
.archive-search-page { padding: 16px; }
.buttons-col { display:flex;align-items:center; }
.search-form { margin-top: 8px; }
</style>