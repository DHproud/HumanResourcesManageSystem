<template>
  <div class="org-manage">
    <el-alert title="操作提示：请从左至右依次选择机构，选中父级后方可添加子级。" type="info" show-icon style="margin-bottom: 20px;" />

    <el-row :gutter="20">

      <!-- ================== 一级机构管理 ================== -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>一级机构 (集团/中心)</span>
              <el-tag v-if="current.l1" type="success">当前选中: {{ current.l1.orgName }}</el-tag>
            </div>
          </template>

          <!-- 添加框 -->
          <div class="add-box">
            <el-input v-model="input.l1" placeholder="输入一级机构名称" size="small">
              <template #append>
                <el-button @click="handleAdd(0)">添加</el-button>
              </template>
            </el-input>
          </div>

          <!-- 列表 -->
          <div class="list-box">
            <div
                v-for="item in l1List"
                :key="item.id"
                class="list-item"
                :class="{ active: current.l1 && current.l1.id === item.id }"
                @click="handleSelectL1(item)"
            >
              <span>{{ item.orgName }}</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- ================== 二级机构管理 ================== -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>二级机构 (部门)</span>
              <el-tag v-if="current.l2" type="success">当前选中: {{ current.l2.orgName }}</el-tag>
            </div>
          </template>

          <div v-if="current.l1">
            <div class="add-box">
              <el-input v-model="input.l2" :placeholder="`在 [${current.l1.orgName}] 下添加`" size="small">
                <template #append>
                  <el-button @click="handleAdd(current.l1.id)">添加</el-button>
                </template>
              </el-input>
            </div>
            <div class="list-box">
              <div
                  v-for="item in l2List"
                  :key="item.id"
                  class="list-item"
                  :class="{ active: current.l2 && current.l2.id === item.id }"
                  @click="handleSelectL2(item)"
              >
                <span>{{ item.orgName }}</span>
                <el-icon><ArrowRight /></el-icon>
              </div>
              <el-empty v-if="l2List.length === 0" description="暂无二级机构" :image-size="60" />
            </div>
          </div>
          <el-empty v-else description="请先选择左侧一级机构" />
        </el-card>
      </el-col>

      <!-- ================== 三级机构管理 ================== -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>三级机构 (小组/科室)</span>
            </div>
          </template>

          <div v-if="current.l2">
            <div class="add-box">
              <el-input v-model="input.l3" :placeholder="`在 [${current.l2.orgName}] 下添加`" size="small">
                <template #append>
                  <el-button @click="handleAdd(current.l2.id)">添加</el-button>
                </template>
              </el-input>
            </div>
            <div class="list-box">
              <div v-for="item in l3List" :key="item.id" class="list-item">
                <span>{{ item.orgName }}</span>
              </div>
              <el-empty v-if="l3List.length === 0" description="暂无三级机构" :image-size="60" />
            </div>
          </div>
          <el-empty v-else description="请先选择左侧二级机构" />
        </el-card>
      </el-col>

    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'

// 数据列表
const l1List = ref([])
const l2List = ref([])
const l3List = ref([])

// 当前选中的对象
const current = reactive({
  l1: null,
  l2: null
})

// 输入框内容
const input = reactive({
  l1: '',
  l2: '',
  l3: ''
})

onMounted(() => {
  loadOrg(0) // 加载一级机构
})

// 通用加载方法
const getList = async (parentId) => {
  const res = await request.get(`/api/org/list/${parentId}`)
  return res.code === 200 ? res.data : []
}

const loadOrg = async (pid) => {
  l1List.value = await getList(pid)
}

// 选择一级
const handleSelectL1 = async (item) => {
  current.l1 = item
  current.l2 = null // 重置二级选中
  l2List.value = [] // 清空二级列表
  l3List.value = [] // 清空三级列表

  // 加载该一级下的二级
  l2List.value = await getList(item.id)
}

// 选择二级
const handleSelectL2 = async (item) => {
  current.l2 = item
  // 加载该二级下的三级
  l3List.value = await getList(item.id)
}

// 添加机构 (核心逻辑)
const handleAdd = async (parentId) => {
  let name = ''
  let targetLevel = 0

  if (parentId === 0) { name = input.l1; targetLevel = 1; }
  else if (parentId === current.l1?.id) { name = input.l2; targetLevel = 2; }
  else { name = input.l3; targetLevel = 3; }

  if (!name) return ElMessage.warning('请输入机构名称')

  // 根据级别设置orgCode
  let orgCode = ''
  if (targetLevel === 1) orgCode = '01'
  else if (targetLevel === 2) orgCode = '02'
  else if (targetLevel === 3) orgCode = '03'
  else return ElMessage.warning('添加失败')

  try {
    const res = await request.post('/api/org/add', {
      orgName: name,
      parentId: parentId,
      orgCode: orgCode,
      sort: 1
    })

    if (res.code === 200) {
      ElMessage.success('添加成功')
      // 清空输入并刷新列表
      if (targetLevel === 1) { input.l1 = ''; loadOrg(0); }
      if (targetLevel === 2) { input.l2 = ''; handleSelectL1(current.l1); }
      if (targetLevel === 3) { input.l3 = ''; handleSelectL2(current.l2); }
    } else {
      ElMessage.error(res.msg)
    }
  } catch (e) {
    ElMessage.error('添加失败')
  }
}
</script>

<style scoped>
.org-manage { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: bold; }
.add-box { margin-bottom: 15px; }
.list-box { height: 400px; overflow-y: auto; border: 1px solid #ebeef5; border-radius: 4px; }
.list-item {
  padding: 10px 15px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s;
  border-bottom: 1px solid #f0f2f5;
}
.list-item:hover { background-color: #f5f7fa; }
.list-item.active { background-color: #ecf5ff; color: #409eff; font-weight: bold; }
</style>