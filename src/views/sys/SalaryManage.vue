<template>
  <div class="salary-manage-page">
    <el-card>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h2>薪酬项目管理（仅管理员可见）</h2>
        <div>
          <el-button type="primary" @click="openAddDialog">新增薪酬项目</el-button>
          <el-button style="margin-left:8px;" @click="fetchList">刷新</el-button>
        </div>
      </div>

      <div style="margin-top:12px;">
        <el-form :inline="true">
          <el-form-item label="关键字">
            <el-input v-model="kw" placeholder="编号/名称" style="width:220px;"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSearch">搜索</el-button>
            <el-button @click="resetSearch" style="margin-left:8px;">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="projects" v-loading="loading" style="width:100%;margin-top:12px;">
        <el-table-column prop="projectCode" label="项目编号" width="140" />
        <el-table-column prop="projectName" label="项目名称" />
        <el-table-column prop="description" label="说明" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 1" type="success">启用</el-tag>
            <el-tag v-else type="info">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="scope">
            <el-button size="mini" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" style="margin-left:8px;" @click="confirmDeleteByCode(scope.row.projectCode)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:12px; display:flex; justify-content:flex-end;">
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

    <!-- 新增 / 编辑 对话框 -->
    <el-dialog :title="isEditing ? '编辑薪酬项目' : '新增薪酬项目'" v-model="dialogVisible" width="540px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="项目编号" prop="projectCode">
          <el-input v-model="form.projectCode" :disabled="isEditing" placeholder="例如 S001" />
        </el-form-item>

        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="例如 基本工资" />
        </el-form-item>

        <el-form-item label="说明" prop="description">
          <el-input type="textarea" v-model="form.description" placeholder="可选" />
        </el-form-item>

        <el-form-item label="状态">
          <el-switch v-model="form.status" active-value="1" inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request, { encodeId } from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const projects = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const kw = ref('')

const dialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null, // optional cached id (string) but we won't rely on it for submit
  projectCode: '',
  projectName: '',
  description: '',
  status: 1
})

const rules = {
  projectCode: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }]
}

onMounted(() => { fetchList() })

function buildHeaders() { return { Role: localStorage.getItem('role') || '' } }

async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (kw.value && kw.value.trim() !== '') params.name = kw.value.trim()
    const res = await request.get('/api/salary/project/page', { params, headers: buildHeaders() })
    if (res && res.code === 200) {
      const d = res.data || {}
      projects.value = (d.records || []).map(r => { if (r && r.id != null) r.id = String(r.id); return r })
      total.value = d.total || projects.value.length
      page.value = d.current || page.value
      size.value = d.size || size.value
    } else {
      ElMessage.error(res?.msg || '获取数据失败')
    }
  } catch (e) {
    console.error('fetchList error', e)
    ElMessage.error('获取数据失败：网络或后端错误')
  } finally {
    loading.value = false
  }
}

function onSearch() { page.value = 1; fetchList() }
function resetSearch() { kw.value = ''; page.value = 1; fetchList() }
function onPageChange(p) { page.value = p; fetchList() }

function openAddDialog() {
  isEditing.value = false
  form.id = null
  form.projectCode = ''
  form.projectName = ''
  form.description = ''
  form.status = 1
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEditing.value = true
  // 仍填充 UI，但提交时将通过 projectCode 查 id 再提交
  form.id = row.id != null ? String(row.id) : null
  form.projectCode = row.projectCode
  form.projectName = row.projectName
  form.description = row.description
  form.status = row.status != null ? row.status : 1
  dialogVisible.value = true
}

// 按 projectCode 去 page 查找精确匹配并返回 id（字符串）
async function findProjectIdByCode(projectCode) {
  if (!projectCode) return null
  try {
    const res = await request.get('/api/salary/project/page', { params: { page: 1, size: 50, name: projectCode }, headers: buildHeaders() })
    if (res && res.code === 200) {
      const records = res.data?.records || []
      for (const r of records) {
        if (String(r.projectCode) === String(projectCode)) {
          return r.id != null ? String(r.id) : null
        }
      }
    }
  } catch (e) {
    console.error('findProjectIdByCode error', e)
  }
  return null
}

async function submitForm() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  try {
    if (isEditing.value) {
      // 不再直接信任 form.id：始终通过 projectCode 查真实 id（更稳健）
      const targetId = await findProjectIdByCode(form.projectCode)
      if (!targetId) {
        ElMessage.error('无法定位要更新的记录（编号未找到），请刷新列表后重试')
        return
      }
      const res = await request.put(`/api/salary/project/${encodeId(targetId)}`, {
        projectCode: form.projectCode,
        projectName: form.projectName,
        description: form.description,
        status: Number(form.status)
      }, { headers: buildHeaders() })
      if (res && res.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        fetchList()
      } else {
        ElMessage.error(res?.msg || '更新失败')
      }
    } else {
      const res = await request.post('/api/salary/project/add', {
        projectCode: form.projectCode,
        projectName: form.projectName,
        description: form.description,
        status: Number(form.status)
      }, { headers: buildHeaders() })
      if (res && res.code === 200) {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        fetchList()
      } else {
        ElMessage.error(res?.msg || '新增失败')
      }
    }
  } catch (e) {
    console.error('submitForm error', e)
    ElMessage.error('提交失败：网络或后端错误')
  }
}

async function confirmDeleteByCode(projectCode) {
  try {
    await ElMessageBox.confirm('确认删除该薪酬项目？', '确认删除', { type: 'warning' })
    const id = await findProjectIdByCode(projectCode)
    if (!id) {
      ElMessage.error('未找到该记录，请刷新后重试')
      return
    }
    const res = await request.delete(`/api/salary/project/delete/${encodeId(id)}`, { headers: buildHeaders() })
    if (res && res.code === 200) {
      ElMessage.success('删除成功')
      fetchList()
    } else {
      ElMessage.error(res?.msg || '删除失败')
    }
  } catch (e) { /* cancel */ }
}
</script>

<style scoped>
.salary-manage-page { padding: 16px; }
</style>