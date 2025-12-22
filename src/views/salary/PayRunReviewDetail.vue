<template>
  <div class="payrun-review-detail">
    <el-card>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h2>薪酬发放复核 - 单号：{{ run?.runCode || '' }}</h2>
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

      <el-divider style="margin-top:12px;">待复核发放记录</el-divider>

      <div v-if="loading" style="padding:24px 0;">
        <el-skeleton rows="6" animated />
      </div>

      <div v-else>
        <div v-if="records.length">
          <el-table :data="records" border :row-key="r => r.id" style="width:100%">
            <el-table-column prop="employeeName" label="姓名" width="160" />
            <el-table-column prop="position" label="职位" width="180" />

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

            <el-table-column label="奖励金额" width="220">
              <template #default="{ row }">
                <el-input-number
                    v-model="row.reward"
                    :min="0"
                    :step="100"
                    :precision="2"
                    size="large"
                    controls
                    controls-position="right"
                    @change="onRowChange(row)"
                    :style="{ width: '160px' }"
                />
              </template>
            </el-table-column>

            <el-table-column label="应扣金额" width="220">
              <template #default="{ row }">
                <el-input-number
                    v-model="row.deduction"
                    :min="0"
                    :step="100"
                    :precision="2"
                    size="large"
                    controls
                    controls-position="right"
                    @change="onRowChange(row)"
                    :style="{ width: '160px' }"
                />
              </template>
            </el-table-column>

            <el-table-column prop="totalPayable" label="合计应发" width="160">
              <template #default="{ row }">
                {{ formatMoney(row.totalPayable) }}
              </template>
            </el-table-column>
          </el-table>

          <div style="margin-top:12px; display:flex; justify-content:space-between; align-items:center;">
            <div>
              <el-button type="primary" @click="approve" :loading="actioning" v-if="!isReadonly">通过并标记已发放</el-button>
              <el-button type="danger" @click="reject" :loading="actioning" v-if="!isReadonly" style="margin-left:8px;">拒绝（退回专员）</el-button>
              <el-button @click="saveDraft" style="margin-left:8px;">保存草稿</el-button>
            </div>
            <div>
              合计应发： <strong>{{ formatMoney(totalAll) }}</strong>
            </div>
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
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const rawId = route.params.id || route.query.id
const runId = rawId != null ? String(rawId) : null
const isReadonly = route.query.readonly === '1'

const loading = ref(false)
const actioning = ref(false)
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
        reward: r.reward != null ? Number(r.reward) : 0,
        deduction: r.deduction != null ? Number(r.deduction) : 0,
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

function onRowChange(row) {
  const basic = Object.values(row.itemsMap || {}).reduce((s, v) => s + Number(v || 0), 0)
  row.totalPayable = Number((basic + Number(row.reward || 0) - Number(row.deduction || 0)).toFixed(2))
}

const totalAll = computed(() => {
  return records.value.reduce((s, r) => s + Number(r.totalPayable || 0), 0)
})

async function saveDraft() {
  try {
    const payload = { records: records.value.map(r => ({ id: r.id, reward: r.reward, deduction: r.deduction })) }
    const res = await request.post(`/api/pay/run/${encodeURIComponent(String(runId))}/save`, payload)
    if (res && res.code === 200) {
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(res?.msg || '保存失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败')
  }
}

async function approve() {
  try {
    await ElMessageBox.confirm('确认批准该发放单并标记为已发放/待支付吗？', '确认批准', { type: 'warning' })
  } catch { return }

  actioning.value = true
  try {
    const payload = {
      action: 'approve',
      comment: '',
      records: records.value.map(r => ({ id: r.id, reward: r.reward, deduction: r.deduction }))
    }
    const res = await request.post(`/api/pay/run/${encodeURIComponent(String(runId))}/review`, payload)
    if (res && res.code === 200) {
      ElMessage.success('复核通过，发放单状态已更新')
      // 通过后跳回经理专属列表路径 /salary/pushreview
      router.push({ path: '/salary/pushreview' })
    } else {
      ElMessage.error(res?.msg || '复核操作失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('复核操作失败')
  } finally {
    actioning.value = false
  }
}

async function reject() {
  try {
    const reason = await ElMessageBox.prompt('请输入拒绝原因（将通知专员）', '拒绝原因', { confirmButtonText: '提交', cancelButtonText: '取消' })
    actioning.value = true
    const payload = {
      action: 'reject',
      comment: reason.value,
      records: records.value.map(r => ({ id: r.id, reward: r.reward, deduction: r.deduction }))
    }
    const res = await request.post(`/api/pay/run/${encodeURIComponent(String(runId))}/review`, payload)
    if (res && res.code === 200) {
      ElMessage.success('已拒绝并通知专员')
      router.push({ path: '/salary/pushreview' })
    } else {
      ElMessage.error(res?.msg || '拒绝操作失败')
    }
  } catch (e) {
    if (e && e !== 'cancel') console.error(e)
  } finally {
    actioning.value = false
  }
}

function goBack() { router.back() }

onMounted(() => { loadRun() })
</script>

<style scoped>
.payrun-review-detail { padding: 16px; }

/* 调整输入框大小（和登记页保持一致） */
.el-input-number .el-input__inner { height: 40px; line-height: 40px; padding: 8px 12px; font-size: 14px; }
.el-input-number__decrease, .el-input-number__increase { width: 36px; height: 36px; line-height: 36px; font-size: 18px; margin: 0 6px; }
</style>