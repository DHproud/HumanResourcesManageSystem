<template>
  <div class="salary-standard-edit-page">
    <el-card>
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <h2>新增薪酬标准</h2>
        <div><el-button @click="goBack">返回</el-button></div>
      </div>

      <el-form :model="form" ref="formRef" label-width="140px" style="margin-top:12px;">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="薪酬标准编号">
              <el-input v-model="form.standardCode" disabled placeholder="系统生成"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="薪酬标准名称" prop="standardName" :rules="[{ required: true, message: '请输入名称', trigger: 'blur' }]">
              <el-input v-model="form.standardName" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="制定人" prop="author">
              <el-input v-model="form.author" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="登记人" prop="registrant">
              <el-input v-model="form.registrant" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="登记时间">
              <el-input v-model="form.registerTime" disabled />
            </el-form-item>
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
                <!-- 仅显示启用的项目 -->
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
        <div v-if="enabledProjects.length === 0" class="no-projects">没有可用的薪酬项目，请联系管理员添加或启用项目</div>

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
          <el-button type="primary" @click="handleSubmit">提交</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const router = useRouter()
const formRef = ref(null)
const form = reactive({
  standardCode: '',
  standardName: '',
  author: '',
  registrant: localStorage.getItem('username') || '',
  registerTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
  applicablePositionId: null,
  applicablePosition: '',
  includePension: false,
  includeMedical: false,
  includeUnemployment: false,
  includeHousing: false
})

const projects = ref([]) // will load all and we filter enabled
const positions = ref([])
const basicProjectId = ref(null)

const enabledProjects = computed(() => projects.value.filter(p => p.status == null ? true : Number(p.status) === 1))

function buildHeaders() { return { Username: localStorage.getItem('username') || '' } }

onMounted(() => {
  fetchProjects()
  fetchPositions()
})

async function fetchProjects() {
  try {
    const res = await request.get('/api/salary/project/listAll', { headers: buildHeaders() })
    if (res && res.code === 200) {
      projects.value = (res.data || []).map(p => ({ ...p, id: p.id != null ? String(p.id) : null, selected: true, amount: '0.00' }))
    } else {
      ElMessage.error(res?.msg || '获取薪酬项目失败')
    }
  } catch (e) { console.error(e); ElMessage.error('获取薪酬项目失败') }
}

async function fetchPositions() {
  try {
    const res = await request.get('/api/position/listAll', { headers: buildHeaders() })
    if (res && res.code === 200) {
      positions.value = (res.data || []).map(p => ({ id: p.id, name: p.name || (p.orgNamePath ? p.orgNamePath + '/' + p.name : p.name) }))
    } else {
      ElMessage.error(res?.msg || '获取职位列表失败')
    }
  } catch (e) { console.error(e); ElMessage.error('获取职位列表失败') }
}

function goBack() { router.push({ path: '/salary/standard' }) }

async function handleSubmit() {
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
    const res = await request.post('/api/salary/standard/add', payload, { headers: buildHeaders() })
    if (res && res.code === 200) {
      ElMessage.success('提交成功，等待管理员复核')
      router.push({ path: '/salary/standard' })
    } else {
      ElMessage.error(res?.msg || '新增失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('提交失败：网络或后端错误')
  }
}
</script>

<style scoped>
.salary-standard-edit-page { padding: 16px; }
.no-projects { padding: 12px; color: #999; }
</style>