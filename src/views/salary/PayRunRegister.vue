<template>
  <div class="payrun-register">
    <el-card>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h2>薪酬发放登记 - 单号：{{ run?.runCode || run?.run_code || '' }}</h2>
        <div>
          <el-button @click="goBack">返回</el-button>
        </div>
      </div>

      <div v-if="runSummary" style="margin-top:12px;">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="机构">{{ runSummary.orgName }}</el-descriptions-item>
          <el-descriptions-item label="总人数">{{ runSummary.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="基本薪酬总额">{{ formatMoney(runSummary.totalBasic) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <el-divider style="margin-top:12px;">发放明细（按员工）</el-divider>

      <div v-if="loading" style="padding:32px 0;">
        <el-skeleton rows="6" animated />
      </div>

      <div v-else>
        <div v-if="records.length && columns.length">
          <el-table :data="records" v-loading="loading" border style="width:100%" :row-key="r => r.id">
            <el-table-column prop="employeeName" label="姓名" width="160" />
            <el-table-column prop="position" label="职位" width="180" />

            <!-- 动态薪酬项目列 -->
            <el-table-column
                v-for="col in columns"
                :key="col.key"
                :label="col.title"
                :width="160"
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
                <!-- step 设置为 100，size 设置为 large，controls-position=right，宽度加大 -->
                <el-input-number
                    v-model="row.reward"
                    :min="0"
                    :step="100"
                    :precision="2"
                    size="large"
                    controls
                    controls-position="right"
                    @change="recalcRow(row)"
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
                    @change="recalcRow(row)"
                    :style="{ width: '160px' }"
                />
              </template>
            </el-table-column>

            <el-table-column label="合计应发" width="180">
              <template #default="{ row }">
                {{ formatMoney(row.totalPayable) }}
              </template>
            </el-table-column>
          </el-table>

          <div style="margin-top:12px;display:flex;justify-content:space-between;align-items:center;">
            <div>
              <el-button type="primary" @click="submitForReview" :loading="submitting">提交并发送复核</el-button>
              <el-button @click="saveDraft" style="margin-left:8px;">保存草稿</el-button>
            </div>
            <div style="text-align:right;">
              <div>合计应发： <strong>{{ formatMoney(totalAll) }}</strong></div>
            </div>
          </div>
        </div>

        <div v-else>
          <el-empty description="未找到薪酬项目或明细"></el-empty>
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

// 支持从 params 或 query 读取 id
const rawId = route.params.id || route.query.id
const runId = rawId != null ? String(rawId) : null

const loading = ref(false)
const submitting = ref(false)
const run = ref(null)
const runSummary = ref(null)
const records = ref([])
const columns = ref([])

function formatMoney(v) { return v == null ? '¥0.00' : '¥' + Number(v).toFixed(2) }

async function loadRun() {
  if (!runId) {
    ElMessage.error('未提供发放单ID，无法加载发放单')
    return
  }
  loading.value = true
  try {
    const res = await request.get(`/api/pay/run/${encodeURIComponent(String(runId))}`)
    console.debug('loadRun response:', res)

    let data = null
    if (res && res.code === 200 && res.data !== undefined) {
      data = res.data
    } else if (res && (res.run || res.records || res.projects)) {
      data = res
    } else if (res && res.data === undefined && typeof res === 'object') {
      data = { run: res }
    } else {
      data = res && res.data ? res.data : res
    }

    const runObj = data.run || data || null
    const recordsArr = data.records || data.payRecords || data.items || []
    const projectsArr = data.projects || data.projectsList || []

    run.value = runObj || null

    const firstName = run.value?.firstLevelOrgName || run.value?.first_level_org_name || run.value?.firstLevelOrg || ''
    const secondName = run.value?.secondLevelOrgName || run.value?.second_level_org_name || run.value?.secondLevelOrg || ''
    const thirdName = run.value?.thirdLevelOrgName || run.value?.third_level_org_name || run.value?.thirdLevelOrg || ''
    runSummary.value = {
      orgName: [firstName, secondName, thirdName].filter(Boolean).join('/'),
      totalCount: run.value?.totalCount || run.value?.total_count || 0,
      totalBasic: run.value?.totalBasic || run.value?.total_basic || 0
    }

    const recs = (Array.isArray(recordsArr) ? recordsArr : []).map(r => {
      const items = r.items || r.recordItems || r.itemsList || []
      const itemsMap = {}
      items.forEach(it => {
        const key = it.projectCode || it.project_code || String(it.projectId || it.project_id || '')
        itemsMap[key] = Number(it.amount || it.amountStr || it.value || 0)
      })
      const basicSum = Object.values(itemsMap).reduce((s, v) => s + Number(v || 0), 0)
      return {
        id: r.id || r.recordId || r.record_id || null,
        employeeId: r.employeeId || r.employee_id || null,
        employeeName: r.employeeName || r.employee_name || r.name || '',
        position: r.position || r.positionName || r.position_name || '',
        items: items,
        itemsMap,
        reward: r.reward != null ? Number(r.reward) : (r.rewardAmount != null ? Number(r.rewardAmount) : 0),
        deduction: r.deduction != null ? Number(r.deduction) : (r.deductionAmount != null ? Number(r.deductionAmount) : 0),
        totalPayable: Number(((basicSum || 0) + (r.reward || 0) - (r.deduction || 0)).toFixed(2))
      }
    })
    records.value = recs

    if (Array.isArray(projectsArr) && projectsArr.length > 0) {
      columns.value = projectsArr.map(p => {
        const key = p.projectCode || p.project_code || String(p.projectId || p.project_id || '')
        const title = (p.projectCode ? (p.projectCode + ' - ') : '') + (p.projectName || p.project_name || '')
        return { key, title }
      })
    } else {
      const keys = new Set()
      records.value.forEach(r => Object.keys(r.itemsMap || {}).forEach(k => keys.add(k)))
      columns.value = Array.from(keys).map(k => ({ key: k, title: k }))
    }

  } catch (e) {
    console.error('loadRun error:', e)
    ElMessage.error('获取发放单失败：' + (e?.message || '网络或后端错误'))
  } finally {
    loading.value = false
  }
}

function recalcRow(row) {
  const basic = Object.values(row.itemsMap || {}).reduce((s, v) => s + Number(v || 0), 0)
  row.totalPayable = Number((basic + Number(row.reward || 0) - Number(row.deduction || 0)).toFixed(2))
}

const totalAll = computed(() => {
  return records.value.reduce((s, r) => s + Number(r.totalPayable || 0), 0)
})

async function saveDraft() {
  try {
    const payload = {
      records: records.value.map(r => ({ id: r.id, reward: r.reward, deduction: r.deduction }))
    }
    const res = await request.post(`/api/pay/run/${encodeURIComponent(String(runId))}/save`, payload)
    if (res && res.code === 200) {
      ElMessage.success('保存成功')
    } else {
      console.warn('saveDraft response:', res)
      ElMessage.error(res?.msg || '保存失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败')
  }
}

async function submitForReview() {
  submitting.value = true
  try {
    const payload = {
      records: records.value.map(r => ({ id: r.id, reward: r.reward, deduction: r.deduction }))
    }
    const res = await request.post(`/api/pay/run/${encodeURIComponent(String(runId))}/submit`, payload)
    if (res && res.code === 200) {
      ElMessage.success('提交成功，已发送复核')
      router.push({ path: '/salary/payrun' })
    } else {
      console.warn('submitForReview response:', res)
      ElMessage.error(res?.msg || '提交失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadRun()
})
</script>

<style scoped>
.payrun-register { padding: 16px; }

/* 放大 input-number 控制按钮和输入框，使 + / - 可见且交互更友好 */
.el-input-number {
  display: inline-flex;
  align-items: center;
}
.el-input-number .el-input__inner {
  height: 40px;
  line-height: 40px;
  padding: 8px 12px;
  font-size: 14px;
}
.el-input-number__decrease,
.el-input-number__increase {
  width: 36px;
  height: 36px;
  line-height: 36px;
  font-size: 18px;
  border-radius: 4px;
  margin: 0 6px;
}
.el-input-number__decrease i,
.el-input-number__increase i {
  font-size: 18px;
}

/* 让 controls 放在右侧时按钮与输入框对齐更美观 */
.el-input-number--large .el-input-number__decrease,
.el-input-number--large .el-input-number__increase {
  width: 38px;
  height: 38px;
  line-height: 38px;
  font-size: 18px;
}

/* 增大表格内 Reward/Deduction 列的单元格内间距 */
.el-table td .el-input-number {
  margin: 4px 0;
}
</style>