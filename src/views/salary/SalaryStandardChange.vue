<template>
  <div class="salary-standard-change-page">
    <el-card>
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <h2>编辑薪酬标准</h2>
        <div><el-button @click="goBack">返回</el-button></div>
      </div>

      <el-form :model="form" ref="formRef" label-width="140px" style="margin-top:12px;">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="薪酬标准编号">
              <el-input v-model="form.standardCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="薪酬标准名称" prop="standardName">
              <el-input v-model="form.standardName" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="制定人"><el-input v-model="form.author" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="登记人"><el-input v-model="form.registrant" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="登记时间"><el-input v-model="form.registerTime" disabled /></el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="适用职位">
              <el-select v-model="form.applicablePositionId" placeholder="请选择适用职位" filterable clearable>
                <el-option v-for="p in positions" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="基本工资项目（可选）">
              <el-select v-model="basicProjectId" placeholder="选择基本工资项目（可选）" clearable>
                <el-option v-for="p in enabledProjects" :key="p.id" :label="p.projectCode + ' - ' + p.projectName" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider>三险一金</el-divider>
        <el-row :gutter="16" style="margin-bottom:12px;">
          <el-col :span="6"><el-checkbox v-model="form.includePension">包含养老保险（8%）</el-checkbox></el-col>
          <el-col :span="6"><el-checkbox v-model="form.includeMedical">包含医疗保险（2%+3）</el-checkbox></el-col>
          <el-col :span="6"><el-checkbox v-model="form.includeUnemployment">包含失业保险（0.5%）</el-checkbox></el-col>
          <el-col :span="6"><el-checkbox v-model="form.includeHousing">包含住房公积金（8%）</el-checkbox></el-col>
        </el-row>

        <el-divider>薪酬项目与金额（仅显示启用的项目）</el-divider>
        <div v-for="proj in enabledProjects" :key="proj.id" style="margin-bottom:12px; border:1px solid #ebeef5; padding:12px; border-radius:4px;">
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <div style="display:flex; align-items:center; gap:12px;">
              <el-checkbox v-model="proj.selected" />
              <div>{{ proj.projectCode }} - {{ proj.projectName }}</div>
            </div>
            <div style="width:320px;">
              <el-input v-model="proj.amount" placeholder="0.00" />
            </div>
          </div>
        </div>

        <div style="margin-top:18px; display:flex; justify-content:flex-end; gap:12px;">
          <el-button @click="goBack">取消</el-button>
          <el-button type="primary" @click="handleUpdate">提交</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request, { encodeId } from '@/utils/request'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const codeParam = route.params.code ? String(route.params.code) : null

const formRef = ref(null)
const form = reactive({
  id: null,
  standardCode: '',
  standardName: '',
  author: '',
  registrant: '',
  registerTime: '',
  applicablePositionId: null,
  applicablePosition: '',
  includePension: false,
  includeMedical: false,
  includeUnemployment: false,
  includeHousing: false
})

const projects = ref([])
const positions = ref([])
const basicProjectId = ref(null)
const enabledProjects = computed(() => projects.value.filter(p => p.status == null ? true : Number(p.status) === 1))

function buildHeaders() { return { Username: localStorage.getItem('username') || '' } }

onMounted(() => {
  fetchProjects()
  fetchPositions()
  loadInitial()
})

async function fetchProjects() {
  try {
    const res = await request.get('/api/salary/project/listAll', { headers: buildHeaders() })
    if (res && res.code === 200) {
      projects.value = (res.data || []).map(p => ({ ...p, id: p.id != null ? String(p.id) : null, selected: false, amount: '0.00' }))
    } else ElMessage.error(res?.msg || '获取薪酬项目失败')
  } catch (e) { console.error(e); ElMessage.error('获取薪酬项目失败') }
}

async function fetchPositions() {
  try {
    const res = await request.get('/api/position/listAll', { headers: buildHeaders() })
    if (res && res.code === 200) positions.value = (res.data || []).map(p => ({ id: p.id, name: p.name || (p.orgNamePath ? p.orgNamePath + '/' + p.name : p.name) }))
    else ElMessage.error(res?.msg || '获取职位列表失败')
  } catch (e) { console.error(e); ElMessage.error('获取职位列表失败') }
}

// 优先从 sessionStorage 获取 row（避免再次请求）；否则按 code 查 id 并请求详情
async function loadInitial() {
  if (!codeParam) {
    form.registerTime = dayjs().format('YYYY-MM-DD HH:mm:ss')
    form.registrant = localStorage.getItem('username') || ''
    return
  }
  // try sessionStorage first
  try {
    const raw = sessionStorage.getItem(`salary_standard_record_${codeParam}`)
    if (raw) {
      const r = JSON.parse(raw)
      // normalize: r may be wrapper or direct standard
      const std = r.standard || r
      fillFormFromStd(std)
      // fill items if present
      const items = r.items || r.standard?.items || []
      applyItemsToProjects(items)
      return
    }
  } catch (e) {
    console.warn('sessionStorage read error', e)
  }

  // fallback: find id by code then GET detail
  const id = await findStandardIdByCode(codeParam)
  if (!id) {
    ElMessage.warning('未找到该标准，可能已被删除')
    return
  }
  try {
    const res = await request.get(`/api/salary/standard/${encodeId(id)}`, { headers: buildHeaders() })
    if (res && res.code === 200) {
      const d = res.data || {}
      const std = d.standard || {}
      fillFormFromStd(std)
      applyItemsToProjects(d.items || [])
    } else ElMessage.error(res?.msg || '获取详情失败')
  } catch (e) { console.error(e); ElMessage.error('获取详情失败') }
}

function fillFormFromStd(std) {
  form.id = std.id || null
  form.standardCode = std.standardCode || ''
  form.standardName = std.standardName || ''
  form.author = std.author || ''
  form.registrant = std.registrant || (localStorage.getItem('username') || '')
  form.registerTime = std.registerTime || ''
  form.applicablePositionId = std.applicablePositionId || null
  form.applicablePosition = std.applicablePosition || ''
  form.includePension = !!std.includePension
  form.includeMedical = !!std.includeMedical
  form.includeUnemployment = !!std.includeUnemployment
  form.includeHousing = !!std.includeHousing
}

function applyItemsToProjects(items) {
  const map = {}
  items.forEach(it => { if (it.projectId) map[it.projectId] = it })
  projects.value.forEach(p => {
    if (map[p.id]) {
      p.selected = true
      p.amount = Number(map[p.id].amount || 0).toFixed(2)
    } else {
      p.selected = false
      p.amount = '0.00'
    }
  })
}

// find id by code (same robust logic)
async function findStandardIdByCode(code) {
  if (!code) return null
  try {
    const res = await request.get('/api/salary/standard/page', { params: { page:1, size:50, name:code }, headers: buildHeaders() })
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
    console.error('findStandardIdByCode error', e)
  }
  return null
}

function goBack() { router.push({ path: '/salary/standard' }) }

async function handleUpdate() {
  if (!form.standardName || form.standardName.trim() === '') { ElMessage.warning('请输入名称'); return }

  const items = enabledProjects.value.filter(p => p.selected).map(p => ({
    projectId: p.id,
    projectCode: p.projectCode,
    projectName: p.projectName,
    amount: p.amount && p.amount.toString().trim() !== '' ? Number(parseFloat(p.amount).toFixed(2)) : 0.00
  }))

  const payload = {
    standardName: form.standardName,
    author: form.author,
    registrant: form.registrant,
    applicablePositionId: form.applicablePositionId,
    applicablePosition: positions.value.find(p => p.id === form.applicablePositionId)?.name || form.applicablePosition || '',
    includePension: form.includePension ? 1 : 0,
    includeMedical: form.includeMedical ? 1 : 0,
    includeUnemployment: form.includeUnemployment ? 1 : 0,
    includeHousing: form.includeHousing ? 1 : 0,
    items
  }

  try {
    const id = await findStandardIdByCode(form.standardCode)
    if (!id) { ElMessage.error('无法定位记录 id，更新失败'); return }
    const res = await request.put(`/api/salary/standard/${encodeId(id)}`, payload, { headers: buildHeaders() })
    if (res && res.code === 200) {
      ElMessage.success('提交成功，等待管理员复核'); router.push({ path: '/salary/standard' })
    } else ElMessage.error(res?.msg || '更新失败')
  } catch (e) { console.error(e); ElMessage.error('提交失败：网络或后端错误') }
}
</script>

<style scoped>
.salary-standard-change-page { padding: 16px; }
</style>