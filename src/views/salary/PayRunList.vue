<template>
  <div class="payrun-list">
    <el-card>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h2>薪酬发放登记 - 待登记发放单</h2>
        <div>
          <el-button type="primary" @click="fetchList">刷新</el-button>
        </div>
      </div>

      <div style="margin-top:12px;">
        <el-form :inline="true">
          <el-form-item label="机构关键字">
            <el-input v-model="kw" placeholder="机构名称/编号" style="width:220px;" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSearch">搜索</el-button>
            <el-button @click="resetSearch" style="margin-left:8px;">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="runs" v-loading="loading" style="width:100%;margin-top:12px;">
        <el-table-column prop="runCode" label="薪酬单号" width="240" />
        <el-table-column prop="orgName" label="机构" />
        <el-table-column prop="totalCount" label="总人数" width="110" />
        <el-table-column prop="totalBasic" label="基本薪酬总额" width="160">
          <template #default="scope">
            {{ formatMoney(scope.row.totalBasic) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <el-button type="primary" size="mini" @click="goToRegister(scope.row.id)">登记</el-button>
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
const runs = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const kw = ref('')

function formatMoney(v) {
  if (v == null) return '¥0.00'
  return '¥' + Number(v).toFixed(2)
}

async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (kw.value && kw.value.trim()) params.name = kw.value.trim()
    const res = await request.get('/api/pay/run/page', { params })
    if (res && res.code === 200) {
      runs.value = res.data?.records || []
      total.value = res.data?.total || 0
      page.value = res.data?.current || page.value
      size.value = res.data?.size || size.value
    } else {
      ElMessage.error(res?.msg || '获取发放单列表失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取发放单列表失败')
  } finally {
    loading.value = false
  }
}

function onSearch() { page.value = 1; fetchList() }
function resetSearch() { kw.value = ''; page.value = 1; fetchList() }
function onPageChange(p) { page.value = p; fetchList() }

function goToRegister(runId) {
  router.push({ path: `/salary/payrun/register/${encodeURIComponent(String(runId))}` })
}

onMounted(() => fetchList())
</script>

<style scoped>
.payrun-list { padding: 16px; }
</style>