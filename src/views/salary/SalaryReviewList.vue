<template>
  <div class="salary-review-list">
    <el-card>
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <h2>薪酬标准复核（管理员）</h2>
        <div><el-button type="primary" @click="fetchList">刷新</el-button></div>
      </div>

      <div style="margin-top:12px;">
        <el-form :inline="true">
          <el-form-item label="关键字">
            <el-input v-model="kw" placeholder="名称/编号" style="width:220px;" />
          </el-form-item>
          <el-form-item><el-button type="primary" @click="onSearch">搜索</el-button></el-form-item>
        </el-form>
      </div>

      <el-table :data="rows" v-loading="loading" style="width:100%;margin-top:12px;">
        <el-table-column label="编号" width="220">
          <template #default="scope">{{ scope.row.standard.standardCode }}</template>
        </el-table-column>
        <el-table-column label="名称"><template #default="scope">{{ scope.row.standard.standardName }}</template></el-table-column>
        <el-table-column label="制定人" width="120"><template #default="scope">{{ scope.row.standard.author }}</template></el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="mini" type="primary" @click="goDetail(scope.row)">复核</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:12px; display:flex; justify-content:flex-end;">
        <el-pagination background layout="prev, pager, next, jumper" :current-page="page" :page-size="size" :total="total" @current-change="onPageChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const kw = ref('')

function buildHeaders() { return { Username: localStorage.getItem('username') || '' } }

onMounted(() => fetchList())

async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (kw.value && kw.value.trim()) params.name = kw.value.trim()
    const res = await request.get('/api/salary/standard/review/list', { params, headers: buildHeaders() })
    if (res && res.code === 200) {
      rows.value = res.data?.records || []
      total.value = res.data?.total || 0
      page.value = res.data?.current || page.value
      size.value = res.data?.size || size.value
    } else ElMessage.error(res?.msg || '获取失败')
  } catch (e) { console.error(e); ElMessage.error('获取失败') } finally { loading.value = false }
}

function onSearch() { page.value = 1; fetchList() }
function onPageChange(p) { page.value = p; fetchList() }

function goDetail(row) {
  const code = row.standard?.standardCode
  if (!code) { ElMessage.error('无法获取记录编号'); return }
  router.push({ path: `/salary/standard/review/${encodeURIComponent(code)}` })
}
</script>

<style scoped>
.salary-review-list { padding: 16px; }
</style>