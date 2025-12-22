<template>
  <div class="payrun-query-detail">
    <el-card>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h2>薪酬发放明细 - 单号：{{ run?.runCode || '' }}</h2>
        <div>
          <el-button @click="goBack">返回</el-button>
        </div>
      </div>

      <div v-if="run" style="margin-top:12px;">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="机构">{{ runSummary.orgName }}</el-descriptions-item>
          <el-descriptions-item label="总人数">{{ runSummary.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="基本薪酬总额">{{ formatMoney(runSummary.totalBasic) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <el-divider style="margin-top:12px;">发放明细（按员工）</el-divider>

      <div v-if="loading" style="padding:24px 0;">
        <el-skeleton rows="6" animated/>
      </div>

      <div v-else>
        <div v-if="records.length">
          <el-table :data="records" border :row-key="r => r.id" style="width:100%">
            <el-table-column prop="employeeName" label="姓名" width="160" />
            <el-table-column prop="position" label="职位" width="160" />

            <el-table-column
                v-for="col in columns"
                :key="col.key"
                :label="col.title"
                :width="140"
            >
              <template #default="{ row }">
                <div v-if="row.itemsMap && row.itemsMap[col.key] !== undefined">
                  {{ formatMoney(row.itemsMap[col.key]) }}
                </div>
                <div v-else>-</div>
              </template>
            </el-table-column>

            <el-table-column prop="totalPayable" label="合计应发" width="160">
              <template #default="{ row }">
                {{ formatMoney(row.totalPayable) }}
              </template>
            </el-table-column>
          </el-table>

          <div style="margin-top:12px; display:flex; justify-content:flex-end; align-items:center;">
            合计应发： <strong style="margin-left:8px;">{{ formatMoney(totalAll) }}</strong>
          </div>
        </div>

        <div v-else>
          <el-empty description="未找到发放记录"></el-empty>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const rawId = route.params.id || route.query.id
const runId = rawId != null ? String(rawId) : null

const loading = ref(false)
const run = ref(null)
const runSummary = ref({ orgName: '', totalCount: 0, totalBasic: 0 })
const records = ref([])
const columns = ref([])

function formatMoney(v) { return v == null ? '¥0.00' : '¥' + Number(v).toFixed(2) }

async function loadRun() {
  if (!runId) {
    ElMessage.error('未提供发放单ID')
    return
  }
  loading.value = true
  try {
    const res = await request.get(`/api/pay/run/${encodeURIComponent(String(runId))}`)
    if (!(res && (res.code === 200 || res.run))) {
      ElMessage.error(res?.msg || '获取发放单失败')
      loading.value = false
      return
    }
    const data = (res.code === 200 ? res.data : res)
    run.value = data.run || data || null

    runSummary.value = {
      orgName: (run.value?.firstLevelOrgName || '') + (run.value?.secondLevelOrgName ? '/' + run.value.secondLevelOrgName : '') + (run.value?.thirdLevelOrgName ? '/' + run.value.thirdLevelOrgName : ''),
      totalCount: run.value?.totalCount || run.value?.total_count || 0,
      totalBasic: run.value?.totalBasic || run.value?.total_basic || 0
    }

    const recordsArr = data.records || data.payRecords || []
    const projectsArr = data.projects || []

    const recs = (Array.isArray(recordsArr) ? recordsArr : []).map(r => {
      const items = r.items || r.recordItems || r.itemsList || []
      const itemsMap = {}
      items.forEach(it => {
        const key = it.projectCode || it.project_code || String(it.projectId || it.project_id || '')
        itemsMap[key] = Number(it.amount || it.value || 0)
      })
      const basicSum = Object.values(itemsMap).reduce((s, v) => s + Number(v || 0), 0)
      return {
        id: r.id || r.recordId || r.record_id || null,
        employeeId: r.employeeId || r.employee_id || null,
        employeeName: r.employeeName || r.employee_name || r.name || '',
        position: r.position || r.positionName || r.position_name || '',
        items,
        itemsMap,
        totalPayable: Number(((basicSum || 0) + (r.reward || 0) - (r.deduction || 0)).toFixed(2))
      }
    })
    records.value = recs

    if (Array.isArray(projectsArr) && projectsArr.length > 0) {
      columns.value = projectsArr.map(p => {
        const key = p.projectCode || p.project_code || String(p.projectId || p.project_id || '')
        const title = (p.projectCode ? p.projectCode + ' - ' : '') + (p.projectName || p.project_name || '')
        return { key, title }
      })
    } else {
      const keys = new Set()
      records.value.forEach(r => Object.keys(r.itemsMap || {}).forEach(k => keys.add(k)))
      columns.value = Array.from(keys).map(k => ({ key: k, title: k }))
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取发放单失败')
  } finally {
    loading.value = false
  }
}

const totalAll = computed(() => {
  return records.value.reduce((s, r) => s + Number(r.totalPayable || 0), 0)
})

function goBack() { router.back() }

onMounted(() => { loadRun() })
</script>

<style scoped>
.payrun-query-detail { padding: 16px; }
</style>