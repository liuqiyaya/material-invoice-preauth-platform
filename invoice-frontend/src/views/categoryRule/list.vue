
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listCategoryRule,
  addCategoryRule,
  updateCategoryRule,
  deleteCategoryRule,
  toggleCategoryRule,
  type CategoryRule,
  type CategoryRuleQuery,
} from '@/api/categoryRule'
import { CATEGORY_LEVEL_MAP } from '@/constants/status'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const records = ref<CategoryRule[]>([])
const total = ref(0)

const query = reactive<CategoryRuleQuery>({
  pageNum: 1,
  pageSize: 20,
  categoryName: '',
  level: undefined,
  enabled: undefined,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增品类规则')
const submitting = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  categoryName: '',
  keywords: '',
  level: 1,
  remark: '',
  enabled: 1,
})

async function fetchList() {
  loading.value = true
  try {
    const data = await listCategoryRule(query)
    records.value = data.records
    total.value = data.total
  } catch {
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.pageNum = 1
  query.categoryName = ''
  query.level = undefined
  query.enabled = undefined
  fetchList()
}

function handlePageChange(page: number) {
  query.pageNum = page
  fetchList()
}

function handleSizeChange(size: number) {
  query.pageSize = size
  query.pageNum = 1
  fetchList()
}

function handleAdd() {
  dialogTitle.value = '新增品类规则'
  form.id = undefined
  form.categoryName = ''
  form.keywords = ''
  form.level = 1
  form.remark = ''
  form.enabled = 1
  dialogVisible.value = true
}

function handleEdit(row: CategoryRule) {
  dialogTitle.value = '修改品类规则'
  form.id = row.id
  form.categoryName = row.categoryName
  form.keywords = row.keywords || ''
  form.level = row.level
  form.remark = row.remark || ''
  form.enabled = row.enabled
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.categoryName) {
    ElMessage.warning('品类名不能为空')
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await updateCategoryRule({ ...form })
      ElMessage.success('修改成功')
    } else {
      await addCategoryRule({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch {
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: CategoryRule) {
  try {
    await ElMessageBox.confirm(
      `确认删除品类规则「${row.categoryName}」？`,
      '删除确认',
      { type: 'warning' }
    )
  } catch { return }
  try {
    await deleteCategoryRule(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch {}
}

async function handleToggle(row: CategoryRule) {
  const target = row.enabled === 1 ? 0 : 1
  try {
    await toggleCategoryRule(row.id, target)
    ElMessage.success(target === 1 ? '已启用' : '已禁用')
    fetchList()
  } catch {}
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="category-rule-page">
    <el-card shadow="never">
      <el-form :inline="true" :model="query" @submit.prevent="handleSearch">
        <el-form-item label="品类名">
          <el-input
            v-model="query.categoryName"
            placeholder="模糊匹配"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="结论">
          <el-select v-model="query.level" placeholder="全部" clearable style="width: 140px">
            <el-option :value="1" label="可以" />
            <el-option :value="2" label="存疑" />
            <el-option :value="3" label="不可以" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.enabled" placeholder="全部" clearable style="width: 120px">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增规则</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="records" v-loading="loading" border stripe empty-text="暂无数据">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="categoryName" label="品类名" min-width="150" show-overflow-tooltip />
        <el-table-column prop="keywords" label="关键词" min-width="240" show-overflow-tooltip />
        <el-table-column label="结论" width="100" align="center">
          <template #default="{ row }">
            <el-tag :color="CATEGORY_LEVEL_MAP[row.level]?.color" effect="dark">
              {{ CATEGORY_LEVEL_MAP[row.level]?.label || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled === 1"
              @change="handleToggle(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="品类名" required>
          <el-input v-model="form.categoryName" placeholder="例如：嵌入式/电子硬件" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="form.keywords"
            type="textarea"
            :rows="3"
            placeholder="多个用英文逗号分隔，例如：开发板,电路板,电机"
          />
        </el-form-item>
        <el-form-item label="结论" required>
          <el-radio-group v-model="form.level">
            <el-radio :value="1">可以</el-radio>
            <el-radio :value="2">存疑</el-radio>
            <el-radio :value="3">不可以</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.category-rule-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>