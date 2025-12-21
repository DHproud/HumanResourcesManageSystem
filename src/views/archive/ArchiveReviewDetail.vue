<template>
  <div>
    <el-form :model="form" label-width="110px" label-position="top">
      <el-alert title="请仔细核对员工信息，确认无误后点击底部【复核通过】按钮。" type="warning" :closable="false" style="margin-bottom: 20px;"/>

      <!-- 只读区域：核心归属信息不可改 -->
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="档案编号"><el-input v-model="form.archiveCode" disabled /></el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="所属一级机构"><el-input v-model="form.firstLevelOrgName" disabled /></el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="所属二级机构"><el-input v-model="form.secondLevelOrgName" disabled /></el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="所属三级机构"><el-input v-model="form.thirdLevelOrgName" disabled /></el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="职位名称"><el-input v-model="form.positionName" disabled /></el-form-item>
        </el-col>
      </el-row>

      <el-divider>以下信息可修正</el-divider>

      <!-- 可编辑区域 -->
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="性别">
            <el-select v-model="form.gender" style="width:100%"><el-option label="男" value="男"/><el-option label="女" value="女"/></el-select>
          </el-form-item>
        </el-col>
        <!-- 更多字段可按需添加，如同登记页... -->
        <el-col :span="8"><el-form-item label="Email"><el-input v-model="form.email" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="电话"><el-input v-model="form.mobile" /></el-form-item></el-col>
      </el-row>

      <div style="text-align: center; margin-top: 30px;">
        <el-button @click="emit('close', false)">取消</el-button>
        <el-button type="success" size="large" @click="submitReview">复核通过</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const props = defineProps(['archiveData'])
const emit = defineEmits(['close'])

const form = reactive({})

// 监听传入的数据变化，赋值给 form
watch(() => props.archiveData, (val) => {
  Object.assign(form, val)
}, { immediate: true })

const submitReview = async () => {
  try {
    const res = await request.post('/api/archive/review', form)
    if(res.code === 200) {
      ElMessage.success('复核成功！')
      emit('close', true)
    } else {
      ElMessage.error(res.msg)
    }
  } catch(e) {
    ElMessage.error('系统错误')
  }
}
</script>