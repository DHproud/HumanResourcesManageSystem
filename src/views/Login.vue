<template>
  <div class="login-container">
    <el-card class="box-card">
      <template #header>
        <div class="header-title">
          <span>人力资源管理系统</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" stretch>
        <!-- 登录面板 -->
        <el-tab-pane label="用户登录" name="login">
          <el-form :model="loginForm" :rules="loginRules" ref="loginRef" size="large">
            <el-form-item prop="username">
              <el-input
                  v-model="loginForm.username"
                  placeholder="请输入账号"
                  :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                  type="password"
                  v-model="loginForm.password"
                  placeholder="请输入密码"
                  :prefix-icon="Lock"
                  show-password
                  @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-button type="primary" :loading="loading" style="width:100%" @click="handleLogin">
              {{ loading ? '登 录 中...' : '立 即 登 录' }}
            </el-button>
          </el-form>
        </el-tab-pane>

        <!-- 注册面板 -->
        <el-tab-pane label="新用户注册" name="register">
          <el-form :model="regForm" :rules="regRules" ref="regRef" label-width="80px">
            <el-form-item label="账号" prop="username">
              <el-input v-model="regForm.username" placeholder="设置登录账号"/>
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input type="password" v-model="regForm.password" placeholder="设置登录密码"/>
            </el-form-item>
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="regForm.realName" placeholder="填写真实姓名"/>
            </el-form-item>
            <el-form-item label="角色" prop="role">
              <!-- 【关键】发送给后端的英文代码 -->
              <el-radio-group v-model="regForm.role">
                <el-radio value="SPECIALIST">人事专员</el-radio>
                <el-radio value="MANAGER">人事经理</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-button type="success" :loading="loading" style="width:100%" @click="handleRegister">
              提 交 注 册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const activeTab = ref('login')
const loading = ref(false)
const loginRef = ref(null)
const regRef = ref(null)

const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const regForm = reactive({
  username: '', password: '', realName: '', role: 'SPECIALIST'
})
const regRules = {
  username: [{ required: true, message: '必填', trigger: 'blur' }],
  password: [{ required: true, message: '必填', trigger: 'blur' }],
  realName: [{ required: true, message: '必填', trigger: 'blur' }],
  role: [{ required: true, message: '必选', trigger: 'change' }]
}

const handleLogin = () => {
  loginRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await request.post('/api/sys/login', loginForm)
        if (res.code === 200) {
          // 保存 Token 和 英文角色
          localStorage.setItem('token', res.data.token)
          localStorage.setItem('role', res.data.role)
          localStorage.setItem('username', res.data.username)

          ElMessage.success('登录成功')

          // 根据英文角色跳转 (可选)
          const role = res.data.role
          if (role === 'ADMIN') router.push('/sys/org')
          else if (role === 'MANAGER') router.push('/archive/review')
          else router.push('/archive/register')

        } else {
          ElMessage.error(res.msg || '登录失败')
        }
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}

const handleRegister = () => {
  regRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await request.post('/api/sys/register', regForm)
        if (res.code === 200) {
          ElMessage.success('注册成功')
          activeTab.value = 'login'
          loginForm.username = regForm.username
          regRef.value.resetFields()
        } else {
          ElMessage.error(res.msg || '注册失败')
        }
      } catch (e) {
        ElMessage.error('服务异常')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #2d3a4b;
}
.box-card { width: 420px; }
.header-title { text-align: center; font-size: 20px; font-weight: bold; }
</style>