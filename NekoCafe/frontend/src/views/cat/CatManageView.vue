<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">CAT_CARETAKER 猫咪管家</p>
        <h1>猫咪档案管理</h1>
        <p>维护猫咪基础信息、性格标签和健康状态。组员可直接在此页面补充表单和接口逻辑。</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">新增猫咪</el-button>
    </header>

    <el-table :data="cats" border empty-text="暂无猫咪档案，点击新增猫咪创建第一条记录">
      <el-table-column prop="name" label="名字" min-width="120" />
      <el-table-column prop="breed" label="品种" min-width="120" />
      <el-table-column prop="age" label="年龄" width="90" />
      <el-table-column prop="personality" label="性格" min-width="160" />
      <el-table-column prop="healthStatus" label="健康状态" width="120">
        <template #default="{ row }"><el-tag>{{ row.healthStatus || '待填写' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑猫咪' : '新增猫咪'" width="520px">
      <el-form :model="form" label-position="top">
        <el-form-item label="名字"><el-input v-model="form.name" placeholder="例如：拿铁" /></el-form-item>
        <el-form-item label="品种"><el-input v-model="form.breed" placeholder="例如：英短" /></el-form-item>
        <el-form-item label="年龄"><el-input v-model.number="form.age" placeholder="例如：2" /></el-form-item>
        <el-form-item label="性格"><el-input v-model="form.personality" placeholder="亲人 / 安静 / 活泼" /></el-form-item>
        <el-form-item label="健康状态"><el-input v-model="form.healthStatus" placeholder="健康 / 观察中" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createCat, deleteCat, fetchCats, updateCat, type CatProfile } from '@/api/cat'

const cats = ref<CatProfile[]>([])
const dialogVisible = ref(false)
const editingId = ref<number>()
const form = reactive<CatProfile>({ name: '', breed: '', age: undefined, personality: '', healthStatus: '', description: '', status: 'ACTIVE' })

function resetForm() {
  editingId.value = undefined
  Object.assign(form, { name: '', breed: '', age: undefined, personality: '', healthStatus: '', description: '', status: 'ACTIVE' })
}

async function loadCats() {
  try {
    cats.value = await fetchCats()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '猫咪档案接口待接入')
  }
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: CatProfile) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    if (editingId.value) await updateCat(editingId.value, form)
    else await createCat(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadCats()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存接口待接入')
  }
}

async function handleDelete(id?: number) {
  if (!id) return
  try {
    await deleteCat(id)
    ElMessage.success('删除成功')
    await loadCats()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除接口待接入')
  }
}

onMounted(loadCats)
</script>

<style scoped>
.role-page { display: grid; gap: 18px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
</style>
