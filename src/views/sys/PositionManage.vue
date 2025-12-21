<template>
  <div class="position-manage">
    <el-card>
      <template #header>
        <div class="header">
          <span>职位管理</span>
          <el-button type="primary" @click="dialogVisible = true">+ 添加职位</el-button>
        </div>
      </template>

      <!-- 职位列表 -->
      <el-table :data="tableData" style="width: 100%" border stripe>
        <!-- 【修正1】prop="name" 对应实体类的 name 字段 -->
        <el-table-column prop="name" label="职位名称" width="180" />

        <!-- 【修正2】prop="orgNamePath" 对应实体类的 orgNamePath 字段 -->
        <el-table-column prop="orgNamePath" label="所属机构 (一级/二级/三级)" />

        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 添加职位弹窗 -->
      <el-dialog v-model="dialogVisible" title="添加职位" width="500px">
        <el-form :model="form" label-width="80px">
          <el-form-item label="一级机构">
            <el-select v-model="form.l1" @change="loadL2" placeholder="选择一级">
              <el-option v-for="i in l1List" :key="i.id" :label="i.orgName" :value="i.id"/>
            </el-select>
          </el-form-item>
          <el-form-item label="二级机构">
            <el-select v-model="form.l2" @change="loadL3" placeholder="选择二级" :disabled="!form.l1">
              <el-option v-for="i in l2List" :key="i.id" :label="i.orgName" :value="i.id"/>
            </el-select>
          </el-form-item>
          <el-form-item label="三级机构">
            <el-select v-model="form.l3" placeholder="选择三级" :disabled="!form.l2">
              <el-option v-for="i in l3List" :key="i.id" :label="i.orgName" :value="i.id"/>
            </el-select>
          </el-form-item>
          <el-form-item label="职位名称">
            <el-input v-model="form.positionName" placeholder="例如：前端工程师"></el-input>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAdd">确定</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const dialogVisible = ref(false)

const l1List = ref([])
const l2List = ref([])
const l3List = ref([])

const form = reactive({
  l1: '', l2: '', l3: '',
  positionName: ''
})

// 加载数据 (对应 Controller 的 listAll)
const loadData = async () => {
  const res = await request.get('/api/position/listAll')
  if(res.code === 200) tableData.value = res.data
}

onMounted(() => {
  loadData()
  getOrgList(0).then(data => l1List.value = data)
})

// 级联加载逻辑
const getOrgList = async (pid) => {
  const res = await request.get(`/api/org/list/${pid}`)
  return res.code === 200 ? res.data : []
}
const loadL2 = async (val) => {
  form.l2 = ''; form.l3 = ''; l2List.value = await getOrgList(val)
}
const loadL3 = async (val) => {
  form.l3 = ''; l3List.value = await getOrgList(val)
}

// 提交添加
const submitAdd = async () => {
  if(!form.l3 || !form.positionName) return ElMessage.warning('请补全信息')

  // 获取机构名称用于拼接
  const n1 = l1List.value.find(i=>i.id===form.l1)?.orgName || ''
  const n2 = l2List.value.find(i=>i.id===form.l2)?.orgName || ''
  const n3 = l3List.value.find(i=>i.id===form.l3)?.orgName || ''

  // 【核心参数发送】
  const res = await request.post('/api/position/add', {
    positionName: form.positionName,        // 对应后端 map.get("positionName")
    orgId: form.l3,                         // 对应后端 map.get("orgId")
    orgNameChain: `${n1} / ${n2} / ${n3}`   // 对应后端 map.get("orgNameChain")
  })

  if(res.code === 200) {
    ElMessage.success('添加成功')
    dialogVisible.value = false
    // 清空输入
    form.positionName = ''
    form.l3 = ''
    // 重新加载列表，确保最新数据（包括机构名称）能显示出来
    loadData()
  } else {
    ElMessage.error(res.msg || '添加失败')
  }
}

// 删除
const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除吗?', '提示').then(async () => {
    const res = await request.delete(`/api/position/delete/${id}`)
    if(res.code === 200) {
      ElMessage.success('已删除')
      loadData()
    }
  })
}
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
.position-manage { padding: 20px; }
</style>