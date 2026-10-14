<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getInvoiceDetail,
  updateInvoiceStatus,
  type InvoiceDetailVO,
} from '@/api/invoice'
import { downloadFile } from '@/api/file'
import { type EvidenceGroupVO } from '@/api/evidence'
import EvidenceReviewDialog from './components/EvidenceReviewDialog.vue'
import ManualEditDialog from './components/ManualEditDialog.vue'
import {
  QUALIFY_STATUS,
  QUALIFY_STATUS_MAP,
  FINANCE_STATUS_MAP,
  REIMBURSE_STATUS_MAP,
  CATEGORY_LEVEL_MAP,
  PRICE_TYPE_MAP,
  EVIDENCE_STATUS_MAP,
  LOG_ACTION_MAP,
} from '@/constants/status'
import { formatMoney, formatDateTime, formatDate } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const invoiceId = computed(() => Number(route.params.id))

const loading = ref(false)
const detail = ref<InvoiceDetailVO | null>(null)

async function fetchDetail() {
  loading.value = true
  try {
    detail.value = await getInvoiceDetail(invoiceId.value)
  } catch (e) {
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDetail()
})

const canUploadEvidence = computed(() => {
  return detail.value?.qualifyStatus === QUALIFY_STATUS.NEED_EVIDENCE
})
const canSubmitFinance = computed(() => {
  return detail.value?.qualifyStatus === QUALIFY_STATUS.PASS
      && detail.value?.financeStatus === 0
})
const canReimburse = computed(() => {
  return detail.value?.financeStatus === 1 && detail.value?.reimburseStatus === 0
})
const canCancel = computed(() => {
  return detail.value?.financeStatus === 0
      && detail.value?.qualifyStatus !== QUALIFY_STATUS.CANCELLED
      && detail.value?.qualifyStatus !== QUALIFY_STATUS.REJECT
})

function handleDownload() {
  if (!detail.value?.sourceFileKey || !detail.value.sourceFileName) return
  downloadFile(detail.value.sourceFileKey, detail.value.sourceFileName)
}

function handlePreviewFile(filePath: string) {
  downloadFile(filePath, filePath.split('/').pop() || 'file')
}

async function handleSubmitFinance() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm('确认将该发票提交至财务处？', '提交财务', { type: 'warning' })
  } catch { return }
  try {
    await updateInvoiceStatus({
      invoiceId: detail.value.id,
      type: 'FINANCE',
      status: 1,
      remark: '提交财务处',
    })
    ElMessage.success('已提交财务')
    fetchDetail()
  } catch (e) {}
}

async function handleReimburse() {
  if (!detail.value) return
  let value: string
  try {
    const r = await ElMessageBox.prompt('请输入备注', '报销结果', {
      inputPlaceholder: '报销到账',
      confirmButtonText: '报销成功',
      cancelButtonText: '取消',
      showCancelButton: true,
    })
    value = r.value || ''
  } catch { return }
  try {
    await updateInvoiceStatus({
      invoiceId: detail.value.id,
      type: 'REIMBURSE',
      status: 1,
      remark: value,
    })
    ElMessage.success('已标记报销成功')
    fetchDetail()
  } catch (e) {}
}

async function handleCancel() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm('确认取消该发票？取消后不再参与周累计。', '取消发票', { type: 'warning' })
  } catch { return }
  try {
    await updateInvoiceStatus({
      invoiceId: detail.value.id,
      type: 'CANCEL',
      remark: '管理员取消',
    })
    ElMessage.success('已取消')
    fetchDetail()
  } catch (e) {}
}

const editDialogVisible = ref(false)

function handleManualEdit() {
  editDialogVisible.value = true
}

function handleUploadEvidence() {
  router.push(`/evidence/upload/${detail.value?.id}`)
}

function getFilePreviewUrl(filePath: string): string {
  const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  return `${baseURL}/file/download?filePath=${encodeURIComponent(filePath)}&inline=true`
}

const hideStatusSync = ref(false)

const displayLogs = computed(() => {
  if (!detail.value?.logs) return []
  if (!hideStatusSync.value) return detail.value.logs
  return detail.value.logs.filter((l) => l.action !== 'QUALIFY_STATUS_SYNC')
})

const reviewDialogVisible = ref(false)
const reviewGroup = ref<EvidenceGroupVO | null>(null)

function openReviewDialog(group: EvidenceGroupVO) {
  reviewGroup.value = group
  reviewDialogVisible.value = true
}

function parseAfterValue(value: string): Array<{ label: string; value: string }> {
  if (!value) return []
  try {
    const obj = JSON.parse(value)
    if (typeof obj === 'object' && obj !== null) {
      return Object.entries(obj)
        .filter(([_, v]) => v !== null && v !== undefined && v !== '')
        .map(([k, v]) => ({
          label: translateField(k),
          value: translateValue(k, v),
        }))
    }
  } catch {}
  return value.split(',').map((segment) => {
    const trimmed = segment.trim()
    const eq = trimmed.indexOf('=')
    if (eq > 0) {
      const k = trimmed.slice(0, eq).trim()
      const v = trimmed.slice(eq + 1).trim()
      return { label: translateField(k), value: translateValue(k, v) }
    }
    return { label: '', value: trimmed }
  })
}

function translateField(key: string): string {
  const map: Record<string, string> = {
    sellerName: '销售方',
    sellerTaxNo: '销售方税号',
    buyerName: '购买方',
    buyerTaxNo: '购买方税号',
    invoiceNo: '发票号',
    invoiceDate: '开票日期',
    totalAmount: '金额',
    qualifyStatus: '审核状态',
    qualify_status: '审核状态',
    manualRemark: '人工备注',
    needManual: '需人工',
    financeStatus: '财务状态',
    reimburseStatus: '报销状态',
    status: '状态',
    reason: '原因',
    categoryResult: '品类结论',
    weekAccumulatedBefore: '周累计前',
    weekAccumulatedAfter: '周累计后',
    groupId: '凭证组',
    pass: '审核结果',
    remark: '备注',
    rejectReason: '拒绝原因',
    screenshot: '截图',
    payment: '支付记录',
    fileName: '文件名',
  }
  return map[key] || key
}

function translateValue(key: string, value: any): string {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'boolean' || value === 'true' || value === 'false') {
    const b = value === true || value === 'true'
    if (key === 'pass') return b ? '通过' : '不通过'
    return b ? '是' : '否'
  }
  const str = String(value)
  if (key === 'qualify_status' || key === 'qualifyStatus') {
    const map: Record<string, string> = {
      PENDING: '待审核',
      PENDING_EVIDENCE: '待补凭证',
      PENDING_MANUAL: '待人工处理',
      APPROVED: '审核通过',
      REJECTED: '不通过',
      CANCELLED: '已取消',
    }
    return map[str] || str
  }
  if (key === 'categoryResult' || key === 'categoryLevel') {
    const map: Record<string, string> = { PASS: '可以', DOUBT: '存疑', REJECT: '不可以' }
    return map[str] || str
  }
  if (key === 'financeStatus') {
    const map: Record<string, string> = { NOT_SUBMITTED: '未提交', SUBMITTED: '已提交' }
    return map[str] || str
  }
  if (key === 'reimburseStatus') {
    const map: Record<string, string> = { NOT_FINISHED: '未完成', SUCCESS: '报销成功', FAILED: '报销失败' }
    return map[str] || str
  }
  return str
}
</script>

<template>
  <div v-loading="loading" class="invoice-detail-page">
    <template v-if="detail">
      <el-card shadow="never" class="header-card">
        <div class="header-top">
          <div class="title">
            发票详情 <span class="invoice-id">#{{ detail.id }}</span>
          </div>
          <div class="actions">
            <el-button type="primary" @click="handleManualEdit">人工修改</el-button>
            <el-button v-if="canUploadEvidence" type="warning" @click="handleUploadEvidence">上传凭证</el-button>
            <el-button v-if="canSubmitFinance" type="success" @click="handleSubmitFinance">提交财务</el-button>
            <el-button v-if="canReimburse" type="success" @click="handleReimburse">报销成功</el-button>
            <el-button v-if="canCancel" type="danger" @click="handleCancel">取消</el-button>
            <el-button @click="router.back()">返回</el-button>
          </div>
        </div>

        <el-descriptions :column="3" border class="info-desc">
          <el-descriptions-item label="销售方名称">{{ detail.sellerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="销售方税号">{{ detail.sellerTaxNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发票号码">{{ detail.invoiceNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开票日期">{{ detail.invoiceDate ? formatDate(detail.invoiceDate) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="价税合计">{{ formatMoney(detail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(detail.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="资质审核">
            <el-tag :color="QUALIFY_STATUS_MAP[detail.qualifyStatus]?.color" effect="dark">
              {{ QUALIFY_STATUS_MAP[detail.qualifyStatus]?.label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="财务提交">
            <el-tag :color="FINANCE_STATUS_MAP[detail.financeStatus]?.color" effect="dark">
              {{ FINANCE_STATUS_MAP[detail.financeStatus]?.label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="报销状态">
            <el-tag :color="REIMBURSE_STATUS_MAP[detail.reimburseStatus]?.color" effect="dark">
              {{ REIMBURSE_STATUS_MAP[detail.reimburseStatus]?.label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.rejectReason" label="拒绝原因" :span="3">
            <span style="color: #f56c6c">{{ detail.rejectReason }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.manualRemark" label="人工备注" :span="3">
            {{ detail.manualRemark }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.sourceFileKey" class="file-row">
          <span>原始文件：</span>
          <el-link type="primary" @click="handleDownload">
            {{ detail.sourceFileName || detail.sourceFileKey }}
          </el-link>
        </div>
      </el-card>

      <el-alert
        v-if="detail.duplicates && detail.duplicates.length > 0"
        type="warning"
        :closable="false"
        class="duplicate-alert"
      >
        <template #title>
          ⚠️ 检测到 {{ detail.duplicates.length }} 张疑似重复发票：
          <el-link
            v-for="d in detail.duplicates"
            :key="d.id"
            type="primary"
            @click="router.push(`/invoice/detail/${d.id}`)"
            style="margin-left: 8px"
          >
            #{{ d.id }}
          </el-link>
        </template>
      </el-alert>

      <el-card shadow="never" class="section-card">
        <template #header><span>商品明细</span></template>
        <el-table :data="detail.items" border stripe empty-text="无商品明细">
          <el-table-column prop="itemName" label="商品名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="quantity" label="数量" width="90" align="center" />
          <el-table-column label="单价" width="100" align="right">
            <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
          </el-table-column>
          <el-table-column label="金额" width="100" align="right">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="品类结论" width="110" align="center">
            <template #default="{ row }">
              <el-tag :color="CATEGORY_LEVEL_MAP[row.categoryLevel]?.color" effect="dark">
                {{ CATEGORY_LEVEL_MAP[row.categoryLevel]?.label || '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="归入品类" width="140" />
          <el-table-column label="单价类别" width="100" align="center">
            <template #default="{ row }">
              {{ PRICE_TYPE_MAP[row.priceType]?.label || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="aiReason" label="判定理由" min-width="180" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card shadow="never" class="section-card">
        <template #header>
          <span>支付凭证（{{ detail.evidenceGroups?.length || 0 }} 组）</span>
        </template>
        <el-empty v-if="!detail.evidenceGroups || detail.evidenceGroups.length === 0" description="暂无凭证" />
        <div v-else class="evidence-groups">
          <el-card
            v-for="g in detail.evidenceGroups"
            :key="g.id"
            class="evidence-group-card"
            shadow="hover"
          >
            <div class="evidence-group-header">
              <span>凭证组 #{{ g.id }}</span>
              <div style="display: flex; gap: 8px; align-items: center">
                <el-tag
                  :color="EVIDENCE_STATUS_MAP[g.status]?.color"
                  effect="dark"
                >
                  {{ EVIDENCE_STATUS_MAP[g.status]?.label || '-' }}
                </el-tag>
                <el-button
                  v-if="g.status === 1"
                  type="primary"
                  size="small"
                  @click="openReviewDialog(g)"
                >
                  审核
                </el-button>
              </div>
            </div>
            <div class="evidence-group-meta">
              <span v-if="g.reviewTime">审核时间：{{ formatDateTime(g.reviewTime) }}</span>
              <span v-if="g.reviewRemark">备注：{{ g.reviewRemark }}</span>
            </div>
            <div class="evidence-section">
              <div class="evidence-label">订单截图（{{ g.screenshots.length }}）</div>
              <div class="evidence-thumbs">
                <div v-for="s in g.screenshots" :key="s.id" class="thumb-image">
                  <el-image
                    :src="getFilePreviewUrl(s.filePath)"
                    :preview-src-list="g.screenshots.map(x => getFilePreviewUrl(x.filePath))"
                    :initial-index="g.screenshots.findIndex(x => x.id === s.id)"
                    :preview-teleported="true"
                    fit="cover"
                    class="thumb-img"
                  >
                    <template #error>
                      <div class="img-error">
                        <el-icon><Picture /></el-icon>
                        <span class="img-error-text">{{ s.originalName }}</span>
                      </div>
                    </template>
                  </el-image>
                </div>
              </div>
            </div>
            <div class="evidence-section">
              <div class="evidence-label">支付记录（{{ g.payments.length }}）</div>
              <div class="evidence-thumbs">
                <div v-for="p in g.payments" :key="p.id" class="thumb-image">
                  <el-image
                    :src="getFilePreviewUrl(p.filePath)"
                    :preview-src-list="g.payments.map(x => getFilePreviewUrl(x.filePath))"
                    :initial-index="g.payments.findIndex(x => x.id === p.id)"
                    :preview-teleported="true"
                    fit="cover"
                    class="thumb-img"
                  >
                    <template #error>
                      <div class="img-error">
                        <el-icon><Picture /></el-icon>
                        <span class="img-error-text">{{ p.originalName }}</span>
                      </div>
                    </template>
                  </el-image>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </el-card>

      <el-card shadow="never" class="section-card">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span>操作日志</span>
            <el-checkbox v-model="hideStatusSync" size="small">
              隐藏状态同步
            </el-checkbox>
          </div>
        </template>

        <el-timeline v-if="displayLogs.length > 0">
          <el-timeline-item
            v-for="log in displayLogs"
            :key="log.id"
            :timestamp="formatDateTime(log.createTime)"
            placement="top"
            :color="LOG_ACTION_MAP[log.action]?.color || '#909399'"
          >
            <div class="log-header">
              <el-tag
                :color="LOG_ACTION_MAP[log.action]?.color"
                effect="dark"
                size="small"
                class="log-action-tag"
              >
                {{ LOG_ACTION_MAP[log.action]?.label || log.action }}
              </el-tag>
              <span v-if="log.remark" class="log-remark">{{ log.remark }}</span>
            </div>

            <div v-if="log.afterValue" class="log-change">
              <div
                v-for="(item, idx) in parseAfterValue(log.afterValue)"
                :key="idx"
                class="change-row"
              >
                <span class="change-label">{{ item.label }}</span>
                <span class="change-after">{{ item.value }}</span>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>

        <el-empty v-else description="暂无日志" />
      </el-card>
    </template>

    <el-empty v-else-if="!loading" description="发票不存在" />

    <EvidenceReviewDialog
      v-model:visible="reviewDialogVisible"
      :group="reviewGroup"
      @success="fetchDetail"
    />

    <ManualEditDialog
      v-model:visible="editDialogVisible"
      :detail="detail"
      @success="fetchDetail"
    />
  </div>
</template>

<style scoped>
.invoice-detail-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.invoice-id {
  color: #909399;
  font-weight: 400;
  font-size: 14px;
  margin-left: 8px;
}

.actions {
  display: flex;
  gap: 8px;
}

.info-desc {
  margin-bottom: 12px;
}

.file-row {
  padding: 8px 0;
  border-top: 1px dashed #ebeef5;
  font-size: 14px;
}

.duplicate-alert {
  margin: 0;
}

.section-card :deep(.el-card__header) {
  font-weight: 600;
}

.evidence-groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.evidence-group-card {
  border: 1px solid #ebeef5;
}

.evidence-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 600;
}

.evidence-group-meta {
  color: #909399;
  font-size: 13px;
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.evidence-section {
  margin-bottom: 8px;
}

.evidence-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}

.evidence-thumbs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.thumb-image {
  width: 100px;
  height: 100px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
}

.thumb-img {
  width: 100%;
  height: 100%;
}

.img-error {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
  gap: 4px;
  padding: 4px;
  text-align: center;
}

.img-error-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.log-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.log-action-tag {
  width: auto !important;
}

.log-remark {
  color: #606266;
  font-size: 13px;
}

.log-change {
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}

.change-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  line-height: 1.6;
}

.change-label {
  flex-shrink: 0;
  min-width: 80px;
  color: #909399;
  font-weight: 600;
}

.change-after {
  color: #303133;
  flex: 1;
  word-break: break-all;
}
</style>