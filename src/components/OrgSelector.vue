<template>
  <el-row :gutter="10">
    <el-col :span="8">
      <el-select v-model="selected.level1" placeholder="一级机构" @change="handleLevel1Change">
        <el-option v-for="item in level1List" :key="item.id" :label="item.orgName" :value="item.id"/>
      </el-select>
    </el-col>
    <el-col :span="8">
      <el-select v-model="selected.level2" placeholder="二级机构" @change="handleLevel2Change">
        <el-option v-for="item in level2List" :key="item.id" :label="item.orgName" :value="item.id"/>
      </el-select>
    </el-col>
    <el-col :span="8">
      <el-select v-model="selected.level3" placeholder="三级机构" @change="handleLevel3Change">
        <el-option v-for="item in level3List" :key="item.id" :label="item.orgName" :value="item.id"/>
      </el-select>
    </el-col>
  </el-row>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { getOrgList } from '@/api/org'

export default {
  name: 'OrgSelector',
  // 1. 在这里显式声明 emits
  emits: ['change'],

  // 2. 在 setup 函数中，通过解构上下文获取 emit 方法
  setup(props, { emit }) {

    // 定义响应式数据
    const level1List = ref([])
    const level2List = ref([])
    const level3List = ref([])

    const selected = reactive({
      level1: '',
      level2: '',
      level3: ''
    })

    // 加载数据的方法
    const fetchData = async (parentId, targetListRef) => {
      if (!parentId && parentId !== 0) return
      try {
        const res = await getOrgList(parentId)
        targetListRef.value = res.data
      } catch (error) {
        console.error('加载机构失败', error)
      }
    }

    // 事件处理
    const handleLevel1Change = (val) => {
      selected.level2 = ''
      selected.level3 = ''
      level2List.value = []
      level3List.value = []
      fetchData(val, level2List)
      emitChange()
    }

    const handleLevel2Change = (val) => {
      selected.level3 = ''
      level3List.value = []
      fetchData(val, level3List)
      emitChange()
    }

    const handleLevel3Change = () => {
      emitChange()
    }

    // 3. 使用传入的 emit 函数触发事件
    const emitChange = () => {
      // 传递 selected 的副本
      emit('change', { ...selected })
    }

    // 生命周期钩子
    onMounted(() => {
      fetchData(0, level1List)
    })

    // 4. 必须把模板中需要用到的变量和函数 return 出去
    return {
      level1List,
      level2List,
      level3List,
      selected,
      handleLevel1Change,
      handleLevel2Change,
      handleLevel3Change
    }
  }
}
</script>