<template>
  <el-card>
    <div style="display:flex;justify-content:space-between;align-items:center;">
      <h2>档案编辑</h2>
      <div>
        <el-button @click="goBack">返回</el-button>
      </div>
    </div>

    <div v-if="archive" style="margin-top:12px;">
      <el-form :model="archive" label-width="120px" ref="formRef">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="档案编号">
              <div>{{ archive.archiveCode || '-' }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="archive.name"></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="性别">
              <el-input v-model="archive.gender"></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="身份证号">
              <el-input v-model="archive.idCard"></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="职位">
              <el-input v-model="archive.positionName"></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="手机">
              <el-input v-model="archive.mobile"></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="archive.email"></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="archive.address"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <div style="text-align:right;margin-top:12px;">
          <!-- 专员提交为“提交复核”，经理直接保存 -->
          <el-button type="primary" @click="onSave">保存（提交复核）</el-button>
        </div>
      </el-form>
    </div>

    <div v-else style="text-align:center;padding:18px;">正在加载...</div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const id = route.params.id
const archive = ref(null)
const role = localStorage.getItem('role') || ''
const username = localStorage.getItem('username') || ''

onMounted(async () => {
  try {
    let res = await request.get(`/api/archive/${id}`)
    if (!res || res.code === undefined) res = await request.get(`/api/archive/detail/${id}`)
    const data = res && res.code !== undefined ? res.data : res
    if (data) archive.value = data
    else ElMessage.error('未获取到档案数据')
  } catch (e) {
    console.error(e)
    ElMessage.error('获取档案失败')
  }
})

async function onSave() {
  if (!archive.value || !archive.value.id) {
    ElMessage.warning('档案数据不完整')
    return
  }

  // 如果当前是人事专员，则提交修改申请（不直接写库）
  if (role === 'SPECIALIST') {
    try {
      // 前端再做一次保护：确保是专员
      const payload = {
        archiveId: archive.value.id,
        newData: JSON.stringify(archive.value),
        requester: username || ''
      }
      // 注意：这里必须带上 Role header，后端会从 header 校验权限
      const headers = { Role: role }
      const res = await request.post('/api/archive/editRequest/submit', payload, { headers })
      if (res && res.code === 200) {
        ElMessage.success('修改申请已提交，等待人事经理复核')
        router.push({ name: 'ArchiveQuery' })
      } else {
        ElMessage.error(res?.msg || '提交失败')
      }
    } catch (e) {
      console.error(e)
      ElMessage.error('提交失败：网络或服务器错误')
    }
    return
  }

  // 若非专员（例如经理），允许直接更新（保持兼容）
  try {
    const headers = { Role: role }
    const res = await request.put('/api/archive/update', archive.value, { headers })
    if (res && res.code === 200) {
      ElMessage.success('保存成功')
      router.push({ name: 'ArchiveQuery' })
    } else {
      ElMessage.error(res?.msg || '保存失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败：网络或服务器错误')
  }
}

function goBack() {
  router.push({ name: 'ArchiveQuery' })
}
</script>

<style scoped>
</style>