<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type UploadFile, type UploadRawFile } from 'element-plus'
import { uploadInvoices, type UploadResultVO } from '@/api/invoice'

const router = useRouter()

const fileList = ref<File[]>([])
const uploading = ref(false)
const result = ref<UploadResultVO | null>(null)

// 选择文件时收集
function handleChange(file: UploadFile, files: UploadFile[]) {
  fileList.value = files.map((f) => f.raw as File).filter(Boolean)
}

// 移除文件
function handleRemove(file: UploadFile, files: UploadFile[]) {
  fileList.value = files.map((f) => f.raw as File).filter(Boolean)
}

// 上传前校验扩展名
function beforeUpload(file: UploadRawFile) {
  const ext = file.name.toLowerCase().split('.').pop() || ''
  if (!['pdf', 'zip'].includes(ext)) {
    ElMessage.error(`不支持的文件类型：${file.name}`)
    return false
  }
  return true
}

// 手动上传（阻止 el-upload 的自动上传）
async function handleUpload() {
  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }
  uploading.value = true
  result.value = null
  try {
    const data = await uploadInvoices(fileList.value)
    result.value = data
    ElMessage.success(`上传完成：成功 ${data.successCount}，失败 ${data.failCount}`)
  } catch (e) {
    // 拦截器已弹错误
  } finally {
    uploading.value = false
  }
}

// 重置
function handleReset() {
  fileList.value = []
  result.value = null
}

// 查看详情
function gotoDetail(invoiceId?: number) {
  if (invoiceId) {
    router.push(`/invoice/detail/${invoiceId}`)
  }
}
</script>

<template>
  <div class="invoice-upload-page">
    <el-card shadow="never">
      <template #header>
        <span>上传发票</span>
      </template>

      <el-alert
        title="支持单个 PDF、多个 PDF、ZIP 压缩包（ZIP 内的 PDF 会逐个建档）"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />

      <el-upload
        drag
        multiple
        :auto-upload="false"
        :file-list="[]"
        accept=".pdf,.zip"
        :before-upload="beforeUpload"
        :on-change="handleChange"
        :on-remove="handleRemove"
        :show-file-list="true"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处，或 <em>点击选择</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">支持 .pdf / .zip，单个文件最大 50MB</div>
        </template>
      </el-upload>

      <div class="upload-actions">
        <el-button
          type="primary"
          :loading="uploading"
          :disabled="fileList.length === 0"
          @click="handleUpload"
        >
          上传（{{ fileList.length }} 个文件）
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <!-- 上传结果 -->
    <el-card v-if="result" shadow="never" class="result-card">
      <template #header>
        <span>上传结果</span>
        <span class="batch-id">批次号：{{ result.batchId }}</span>
      </template>

      <el-descriptions :column="3" border style="margin-bottom: 16px">
        <el-descriptions-item label="总数">{{ result.total }}</el-descriptions-item>
        <el-descriptions-item label="成功">
          <span style="color: #67c23a">{{ result.successCount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="失败">
          <span style="color: #f56c6c">{{ result.failCount }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-table :data="result.items" border stripe>
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceId" label="发票ID" width="100" align="center">
          <template #default="{ row }">
            {{ row.invoiceId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="失败原因" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.errorMsg" style="color: #f56c6c">{{ row.errorMsg }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'SUCCESS' && row.invoiceId"
              type="primary"
              link
              @click="gotoDetail(row.invoiceId)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.invoice-upload-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}

.result-card :deep(.el-card__header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.batch-id {
  font-size: 13px;
  color: #909399;
  font-weight: 400;
}
</style>