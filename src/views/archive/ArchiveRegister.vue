<template>
  <div class="archive-register">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>档案登记</span>
          <el-button style="float: right; padding: 3px 0" type="primary" link @click="resetForm">重置表单</el-button>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px" label-position="top">

        <!-- 第一部分：基础身份信息 -->
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="档案编号">
              <el-input v-model="form.archiveCode" placeholder="提交后系统自动生成" disabled class="bg-gray" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入员工姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="选择性别" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号码" prop="idCard">
              <el-input v-model="form.idCard" placeholder="请输入身份证号码" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">机构与职位信息</el-divider>

        <!-- 第二部分：机构级联选择 -->
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="一级机构" prop="firstLevelOrgId">
              <el-select
                  v-model="form.firstLevelOrgId"
                  placeholder="请选择"
                  @change="handleFirstChange"
                  style="width: 100%">
                <el-option v-for="item in level1List" :key="item.id" :label="item.orgName" :value="item.id"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="二级机构" prop="secondLevelOrgId">
              <el-select
                  v-model="form.secondLevelOrgId"
                  placeholder="请选择"
                  :disabled="!form.firstLevelOrgId"
                  @change="handleSecondChange"
                  style="width: 100%">
                <el-option v-for="item in level2List" :key="item.id" :label="item.orgName" :value="item.id"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="三级机构" prop="thirdLevelOrgId">
              <el-select
                  v-model="form.thirdLevelOrgId"
                  placeholder="请选择"
                  :disabled="!form.secondLevelOrgId"
                  @change="handleThirdChange"
                  style="width: 100%">
                <el-option v-for="item in level3List" :key="item.id" :label="item.orgName" :value="item.id"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 机构回显提示 -->
        <el-row v-if="fullOrgName" style="margin-bottom: 20px;">
          <el-col :span="24">
            <el-alert :title="'当前归属：' + fullOrgName" type="success" :closable="false" show-icon />
          </el-col>
        </el-row>

        <!-- 职位与薪酬 -->
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="职位名称" prop="positionName">
              <!-- 【关键修改】职位从手动输入改为下拉选择 -->
              <el-select
                  v-model="form.positionName"
                  placeholder="请先选择三级机构"
                  :disabled="!form.thirdLevelOrgId || positionList.length === 0"
                  style="width: 100%">
                <el-option
                    v-for="p in positionList"
                    :key="p.id"
                    :label="p.name"
                    :value="p.name"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="职称" prop="jobTitleName">
              <el-select v-model="form.jobTitleName" placeholder="请选择" style="width: 100%">
                <el-option label="正高级" value="正高级" />
                <el-option label="副高级" value="副高级" />
                <el-option label="中级" value="中级" />
                <el-option label="初级" value="初级" />
                <el-option label="员级" value="员级" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="薪酬标准" prop="salaryStandardName">
              <el-input v-model="form.salaryStandardName" placeholder="输入薪酬标准" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">个人详细信息</el-divider>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="Email" prop="email">
              <el-input v-model="form.email" placeholder="example@company.com" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系电话" prop="mobile">
              <el-input v-model="form.mobile" placeholder="输入手机号码" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学历" prop="education">
              <el-select v-model="form.education" placeholder="请选择" style="width: 100%">
                <el-option label="博士" value="博士" />
                <el-option label="硕士" value="硕士" />
                <el-option label="本科" value="本科" />
                <el-option label="大专" value="大专" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="住址" prop="address">
              <el-input v-model="form.address" placeholder="输入详细住址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生地" prop="birthplace">
              <el-input v-model="form.birthplace" placeholder="输入出生地" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="国籍" prop="nationality">
              <el-input v-model="form.nationality" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="政治面貌" prop="politicalStatus">
              <el-input v-model="form.politicalStatus" placeholder="如：党员/团员" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="专业" prop="major">
              <el-input v-model="form.major" placeholder="输入主修专业" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item style="margin-top: 30px;">
          <el-button type="primary" size="large" @click="submitForm" style="width: 150px;">提交登记</el-button>
          <el-button size="large" @click="resetForm" style="width: 150px;">重置</el-button>
        </el-form-item>

      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const formRef = ref(null)

// 表单数据 (与后端 Archive.java 实体类完全对应)
const form = reactive({
  archiveCode: '',

  // 机构信息
  firstLevelOrgId: '',
  firstLevelOrgName: '',
  secondLevelOrgId: '',
  secondLevelOrgName: '',
  thirdLevelOrgId: '',
  thirdLevelOrgName: '',

  // 基础信息
  name: '',
  gender: '',
  idCard: '',
  email: '',
  mobile: '',
  address: '',

  // 职位薪酬
  positionName: '', // 这里存储选中的职位名称
  jobTitleName: '',
  salaryStandardName: '',

  // 详细信息
  nationality: '中国',
  birthplace: '',
  education: '',
  major: '',
  politicalStatus: ''
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  idCard: [{ required: true, message: '请输入身份证号码', trigger: 'blur' }],
  firstLevelOrgId: [{ required: true, message: '请选择一级机构', trigger: 'change' }],
  secondLevelOrgId: [{ required: true, message: '请选择二级机构', trigger: 'change' }],
  thirdLevelOrgId: [{ required: true, message: '请选择三级机构', trigger: 'change' }],
  positionName: [{ required: true, message: '请选择职位', trigger: 'change' }]
}

// 数据源
const level1List = ref([])
const level2List = ref([])
const level3List = ref([])
const positionList = ref([]) // 职位列表数据源

// 计算属性：机构全名
const fullOrgName = computed(() => {
  const parts = []
  if (form.firstLevelOrgName) parts.push(form.firstLevelOrgName)
  if (form.secondLevelOrgName) parts.push(form.secondLevelOrgName)
  if (form.thirdLevelOrgName) parts.push(form.thirdLevelOrgName)
  return parts.join(' / ')
})

// 初始化
onMounted(async () => {
  level1List.value = await getOrgList(0)
})

// 通用获取机构
const getOrgList = async (parentId) => {
  const res = await request.get(`/api/org/list/${parentId}`)
  return res.code === 200 ? res.data : []
}

// 一级机构变化
const handleFirstChange = async (val) => {
  const item = level1List.value.find(i => i.id === val)
  form.firstLevelOrgName = item ? item.orgName : ''

  // 清空下级
  form.secondLevelOrgId = ''; form.secondLevelOrgName = ''
  form.thirdLevelOrgId = ''; form.thirdLevelOrgName = ''
  form.positionName = ''
  level2List.value = []; level3List.value = []; positionList.value = []

  if (val) level2List.value = await getOrgList(val)
}

// 二级机构变化
const handleSecondChange = async (val) => {
  const item = level2List.value.find(i => i.id === val)
  form.secondLevelOrgName = item ? item.orgName : ''

  form.thirdLevelOrgId = ''; form.thirdLevelOrgName = ''
  form.positionName = ''
  level3List.value = []; positionList.value = []

  if (val) level3List.value = await getOrgList(val)
}

// 三级机构变化 (触发职位查询)
const handleThirdChange = async (val) => {
  const item = level3List.value.find(i => i.id === val)
  form.thirdLevelOrgName = item ? item.orgName : ''
  form.positionName = ''
  positionList.value = []

  if (val) {
    // 【关键】根据三级机构ID获取职位列表
    const res = await request.get(`/api/position/list?orgId=${val}`)
    if (res.code === 200) {
      positionList.value = res.data
    }
  }
}

// 提交表单
const submitForm = async () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const res = await request.post('/api/archive/add', form)
        if (res.code === 200) {
          ElMessage.success('档案登记成功！等待经理复核。')
          resetForm()
        } else {
          ElMessage.error(res.msg || '登记失败')
        }
      } catch (error) {
        ElMessage.error('系统繁忙，请稍后再试')
      }
    }
  })
}

// 重置
const resetForm = () => {
  formRef.value.resetFields()
  form.firstLevelOrgName = ''
  form.secondLevelOrgName = ''
  form.thirdLevelOrgName = ''
  level2List.value = []
  level3List.value = []
  positionList.value = []
}
</script>

<style scoped>
.archive-register {
  padding: 20px;
}
.bg-gray :deep(.el-input__wrapper) {
  background-color: #f5f7fa;
}
</style>