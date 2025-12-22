<template>
  <div class="upload-photo">
    <el-card>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h2>上传 / 替换员工照片</h2>
        <div>
          <el-button @click="goBack">返回</el-button>
        </div>
      </div>

      <div style="margin-top:12px;">
        <el-form label-width="100px">
          <el-form-item label="档案ID">
            <el-input v-model="archiveId" disabled />
          </el-form-item>

          <el-form-item label="姓名">
            <el-input v-model="archiveName" disabled />
          </el-form-item>

          <el-form-item label="当前照片">
            <div v-if="photoUrl" style="display:flex; flex-direction:column; gap:8px;">
              <img :src="photoUrl" alt="当前照片" style="max-width:240px;max-height:240px;border:1px solid #eee;border-radius:4px;" />
              <div>
                <el-button type="danger" size="small" @click="removePhoto" :loading="removing">删除照片</el-button>
              </div>
            </div>
            <div v-else>
              <el-empty description="未找到照片"></el-empty>
            </div>
          </el-form-item>

          <el-form-item label="选择照片">
            <input type="file" accept="image/jpeg,image/jpg" @change="onFileChange" ref="fileInput" />
            <div v-if="preview" style="margin-top:8px;">
              <img :src="preview" alt="预览" style="max-width:240px;max-height:240px;border:1px solid #eee;border-radius:4px;" />
            </div>
            <div style="margin-top:8px; color:#999; font-size:12px;">
              仅支持 JPG/JPEG 格式，建议尺寸不超过 2MB。
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="upload" :loading="uploading" :disabled="!file">上传并保存</el-button>
            <el-button @click="clear" style="margin-left:8px;">清除</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const archiveId = ref(route.params.id || route.query.id || '')
const archiveName = ref('')
const photoUrl = ref('')
const file = ref(null)
const preview = ref('')
const uploading = ref(false)
const removing = ref(false)
const fileInput = ref(null)

// validation limits
const MAX_SIZE_BYTES = 2 * 1024 * 1024 // 2 MB

async function loadArchive() {
  if (!archiveId.value) {
    ElMessage.error('缺少档案 ID')
    return
  }
  try {
    const res = await request.get(`/api/archive/${encodeURIComponent(String(archiveId.value))}`)
    if (res && res.code === 200) {
      const data = res.data || {}
      archiveName.value = data.name || data.employeeName || ''
      photoUrl.value = data.photoUrl || data.photo_url || ''
    } else {
      const obj = res || {}
      archiveName.value = obj.name || ''
      photoUrl.value = obj.photoUrl || obj.photo_url || ''
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取档案信息失败')
  }
}

function onFileChange(e) {
  const f = e.target.files && e.target.files[0]
  if (!f) {
    file.value = null
    preview.value = ''
    return
  }

  const mime = (f.type || '').toLowerCase()
  const nameLower = (f.name || '').toLowerCase()
  const isJpegMime = mime === 'image/jpeg' || mime === 'image/jpg'
  const isJpgExt = nameLower.endsWith('.jpg') || nameLower.endsWith('.jpeg')

  if (!(isJpegMime || isJpgExt)) {
    ElMessage.error('只支持 JPG/JPEG 格式的图片')
    if (fileInput.value) fileInput.value.value = ''
    file.value = null
    preview.value = ''
    return
  }

  if (f.size > MAX_SIZE_BYTES) {
    ElMessage.error('图片过大，请选择小于 2MB 的图片')
    if (fileInput.value) fileInput.value.value = ''
    file.value = null
    preview.value = ''
    return
  }

  file.value = f
  preview.value = URL.createObjectURL(f)
}

function clear() {
  file.value = null
  preview.value = ''
  if (fileInput.value) fileInput.value.value = ''
}

async function upload() {
  if (!file.value) {
    ElMessage.warning('请先选择图片')
    return
  }
  if (!archiveId.value) {
    ElMessage.error('缺少档案 ID，无法上传')
    return
  }

  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('photo', file.value)

    const url = `/api/archive/${encodeURIComponent(String(archiveId.value))}/photo`

    // 从 localStorage 读取 token 并附加到请求头，保证登录态
    const token = localStorage.getItem('token') || ''
    const headers = {}
    if (token) {
      // 后端以前在请求头里直接取 Authorization 的值（看你的项目），保持一致
      headers['Authorization'] = token
      // 如果你的后端需要 "Bearer <token>"，请改为: headers['Authorization'] = `Bearer ${token}`
    }

    // 重要：不要手动设置 Content-Type，浏览器/axios 会自动带上 boundary
    const resAxios = await axios.post(url, fd, { headers })

    const res = resAxios && resAxios.data ? resAxios.data : null
    if (res && res.code === 200) {
      ElMessage.success('上传成功')
      await loadArchive()
      clear()
      return
    }

    if (resAxios && resAxios.status >= 200 && resAxios.status < 300) {
      ElMessage.success('上传成功')
      await loadArchive()
      clear()
      return
    }

    const errMsg = (res && res.msg) || `上传失败，HTTP ${resAxios.status}`
    ElMessage.error(errMsg)
  } catch (e) {
    console.error('upload error', e)
    // 若后端返回 {code:401,...} 或 axios 抛异常，优先展示服务器返回的 msg
    const serverData = e?.response?.data
    if (serverData && typeof serverData === 'object') {
      const sMsg = serverData.msg || serverData.message || JSON.stringify(serverData)
      ElMessage.error('上传失败: ' + sMsg)
    } else {
      ElMessage.error('上传失败: ' + (e.message || '未知错误'))
    }
  } finally {
    uploading.value = false
  }
}

async function removePhoto() {
  try {
    await ElMessageBox.confirm('确认删除当前照片？', '确认', { type: 'warning' })
  } catch { return }

  if (!archiveId.value) {
    ElMessage.error('缺少档案 ID')
    return
  }
  removing.value = true
  try {
    const token = localStorage.getItem('token') || ''
    const headers = {}
    if (token) headers['Authorization'] = token

    // 使用 request（假设 request 会自动带 token），但也可以用 axios 同样附带 headers
    const res = await request.post(`/api/archive/${encodeURIComponent(String(archiveId.value))}/photo/delete`, {})
    if (res && res.code === 200) {
      ElMessage.success('删除成功')
      await loadArchive()
    } else {
      ElMessage.error(res?.msg || '删除失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('删除失败')
  } finally {
    removing.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadArchive()
})
</script>

<style scoped>
.upload-photo { padding: 16px; }
</style>