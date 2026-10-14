<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type UploadFile } from 'element-plus'
import { uploadEvidence, listEvidence } from '@/api/evidence'
import { getInvoiceDetail, type InvoiceDetailVO } from '@/api/invoice'
import { EVIDENCE_STATUS_MAP } from '@/constants/status'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const invoiceId = computed(() => Number(route.params.invoiceId))

const invoice = ref<InvoiceDetailVO | null>(null)
const loading = ref(false)

const screenshotFiles = ref<File[]>([])
const paymentFiles = ref<File[]>([])
const remark = ref('')
const uploading = ref(false)

// 加载发票信息
async function loadInvoice() {
  loading.value = true
  try {
    invoice.value = await getInvoiceDetail(invoiceId.value)
  } catch (e) {
    ElMessage.error('发票信息加载失败')
  } finally {
    loading.value = false
  }
}

// 选择文件
function handleScreenshotChange(file: UploadFile, files: UploadFile[]) {
  screenshotFiles.value = files.map((f) => f.raw as File).filter(Boolean)
}
function handleScreenshotRemove(file: UploadFile, files: UploadFile[]) {
  screenshotFiles.value = files.map((f) => f.raw as File).filter(Boolean)
}
function handlePaymentChange(file: UploadFile, files: UploadFile[]) {
  paymentFiles.value = files.map((f) => f.raw as File).filter(Boolean)
}
function handlePaymentRemove(file: UploadFile, files: UploadFile[]) {
  paymentFiles.value = files.map((f) => f.raw as File).filter(Boolean)
}

// 上传
async function handleUpload() {
  if (screenshotFiles.value.length === 0) {
    ElMessage.warning('请至少上传一张订单截图')
    return
  }
  if (paymentFiles.value.length === 0) {
    ElMessage.warning('请至少上传一张支付记录')
    return
  }
  uploading.value = true
  try {
    const groupId = await uploadEvidence({
      invoiceId: invoiceId.value,
      screenshots: screenshotFiles.value,
      payments: paymentFiles.value,
      remark: remark.value,
    })
    ElMessage.success(`上传成功，凭证组 ID：${groupId}`)
    // 跳回详情页
    setTimeout(() => {
      router.push(`/invoice/detail/${invoiceId.value}`)
    }, 800)
  } catch (e) {
    // 拦截器已弹错
  } finally {
    uploading.value = false
  }
}

function handleBack() {
  router.back()
}

onMounted(() => {
  loadInvoice()
})
</script>

<template>
  <div class="evidence-upload-page" v-loading="loading">
    <!-- 发票信息 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>上传支付凭证</span>
          <el-button link @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-descriptions v-if="invoice" :column="3" border>
        <el-descriptions-item label="发票 ID">{{ invoice.id }}</el-descriptions-item>
        <el-descriptions-item label="销售方">{{ invoice.sellerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="价税合计">¥{{ invoice.totalAmount?.toFixed(2) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发票号码">{{ invoice.invoiceNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="资质状态">{{ invoice.qualifyStatusDesc }}</el-descriptions-item>
        <el-descriptions-item label="补凭证原因">
          <el-tag v-if="invoice.evidenceReason === 1" type="warning">低值品</el-tag>
          <el-tag v-else-if="invoice.evidenceReason === 2" type="warning">累计超 1000</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 上传表单 -->
    <el-card shadow="never">
      <el-form label-width="120px">
        <el-form-item label="订单截图" required>
          <el-upload
            multiple
            :auto-upload="false"
            accept="image/*"
            :file-list="[]"
            :on-change="handleScreenshotChange"
            :on-remove="handleScreenshotRemove"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="hint">至少 1 张，可多张（订单页截图）</div>
        </el-form-item>

        <el-form-item label="支付记录" required>
          <el-upload
            multiple
            :auto-upload="false"
            accept="image/*"
            :file-list="[]"
            :on-change="handlePaymentChange"
            :on-remove="handlePaymentRemove"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="hint">至少 1 张，可多张（支付成功截图）</div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="remark"
            type="textarea"
            :rows="2"
            placeholder="可填写补充说明"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="uploading" @click="handleUpload">
            提交凭证
          </el-button>
          <el-button @click="handleBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.evidence-upload-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hint {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
</style>