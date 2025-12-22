<template>
  <div class="payrun-search">
    <el-card>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h2>薪酬发放查询</h2>
        <div>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </div>

      <div style="margin-top:12px;">
        <el-form :inline="true" :model="form">
          <el-form-item label="薪酬单号">
            <el-input v-model="form.runCode" placeholder="输入发放单号" />
          </el-form-item>

          <el-form-item label="关键字">
            <el-input v-model="form.name" placeholder="机构名称/单号/关键词" />
          </el-form-item>

          <el-form-item label="发放时间">
            <el-date-picker
                v-model="form.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="yyyy-MM-dd"
            />
          </el-form-item>
        </el-form>
      </div>

      <el-divider />

      <el-table :data="rows" v-loading="loading" style="width:100%; margin-top:12px;">
        <el-table-column prop="runCode" label="薪酬单号" width="260" />
        <el-table-column prop="orgName" label="机构" />
        <el-table-column prop="totalCount" label="总人数" width="110" />
        <el-table-column prop="totalBasic" label="基本薪酬总额" width="160">
          <template #default="scope">{{ formatMoney(scope.row.totalBasic) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <el-button type="primary" size="mini" @click="gotoDetail(scope.row.id)">查看</el-button>
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
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const form = reactive({
  runCode: '',
  name: '',
  dateRange: [] // [start, end] formatted yyyy-MM-dd
})

function formatMoney(v) {
  if (v == null) return '¥0.00'
  return '¥' + Number(v).toFixed(2)
}

async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (form.runCode && form.runCode.trim()) params.runCode = form.runCode.trim()
    if (form.name && form.name.trim()) params.name = form.name.trim()
    if (Array.isArray(form.dateRange) && form.dateRange.length === 2) {
      params.startDate = form.dateRange[0]
      params.endDate = form.dateRange[1]
    }
    const res = await request.get('/api/pay/run/page', { params })
    if (res && res.code === 200) {
      rows.value = res.data?.records || []
      total.value = res.data?.total || 0
      page.value = res.data?.current || page.value
      size.value = res.data?.size || size.value
    } else {
      ElMessage.error(res?.msg || '查询失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  fetchList()
}

function reset() {
  form.runCode = ''
  form.name = ''
  form.dateRange = []
  page.value = 1
  fetchList()
}

function onPageChange(p) {
  page.value = p
  fetchList()
}

function gotoDetail(id) {
  if (!id) {
    ElMessage.warning('无效的发放单ID')
    return
  }
  // 进入只读的明细查看（使用 pushreview 路径或通用查看路径）
  router.push({ path: `/salary/query/${encodeURIComponent(String(id))}` })
}

onMounted(() => { fetchList() })
</script>

<style scoped>
.payrun-search { padding: 16px; }
</style>