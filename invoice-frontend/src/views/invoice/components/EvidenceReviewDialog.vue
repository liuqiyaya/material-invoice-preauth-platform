<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { reviewEvidence, type EvidenceGroupVO } from '@/api/evidence'
import { EVIDENCE_STATUS_MAP } from '@/constants/status'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  visible: boolean
  group: EvidenceGroupVO | null
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  success: []
}>()

const pass = ref(true)
const remark = ref('')
const submitting = ref(false)

watch(
  () => props.visible,
  (val) => {
    if (val) {
      pass.value = true
      remark.value = ''
    }
  }
)

function handleClose() {
  emit('update:visible', false)
}

function getFilePreviewUrl(filePath: string): string {
  const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  return `${baseURL}/file/download?filePath=${encodeURIComponent(filePath)}&inline=true`
}

async function handleSubmit() {
  if (!props.group) return
  submitting.value = true
  try {
    await reviewEvidence({
      groupId: props.group.id,
      pass: pass.value,
      remark: remark.value,
    })
    ElMessage.success(pass.value ? '已通过' : '已拒绝')
    emit('success')
    handleClose()
  } catch (e) {
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="审核支付凭证"
    width="800px"
    @update:model-value="handleClose"
  >
    <template v-if="group">
      <el-descriptions :column="2" border style="margin-bottom: 16px">
        <el-descriptions-item label="凭证组 ID">#{{ group.id }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          {{ EVIDENCE_STATUS_MAP[group.status]?.label || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="上传时间">
          {{ formatDateTime(group.createTime) }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="section">
        <div class="section-title">订单截图（{{ group.screenshots.length }}）</div>
        <div class="img-list">
          <el-image
            v-for="s in group.screenshots"
            :key="s.id"
            :src="getFilePreviewUrl(s.filePath)"
            :preview-src-list="group.screenshots.map((x) => getFilePreviewUrl(x.filePath))"
            :initial-index="group.screenshots.findIndex((x) => x.id === s.id)"
            :preview-teleported="true"
            fit="cover"
            class="preview-img"
          >
            <template #error>
              <div class="img-error">加载失败</div>
            </template>
          </el-image>
        </div>
      </div>

      <div class="section">
        <div class="section-title">支付记录（{{ group.payments.length }}）</div>
        <div class="img-list">
          <el-image
            v-for="p in group.payments"
            :key="p.id"
            :src="getFilePreviewUrl(p.filePath)"
            :preview-src-list="group.payments.map((x) => getFilePreviewUrl(x.filePath))"
            :initial-index="group.payments.findIndex((x) => x.id === p.id)"
            :preview-teleported="true"
            fit="cover"
            class="preview-img"
          >
            <template #error>
              <div class="img-error">加载失败</div>
            </template>
          </el-image>
        </div>
      </div>

      <el-form label-width="80px" style="margin-top: 16px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="pass">
            <el-radio :value="true">
              <el-tag type="success" effect="dark">通过</el-tag>
            </el-radio>
            <el-radio :value="false">
              <el-tag type="danger" effect="dark">不通过</el-tag>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="remark"
            type="textarea"
            :rows="2"
            placeholder="例如：凭证齐全 / 订单金额与发票不符"
          />
        </el-form-item>
      </el-form>
    </template>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交审核</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.section {
  margin-bottom: 16px;
}

.section-title {
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 14px;
}

.img-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-img {
  width: 100px;
  height: 100px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
}

.img-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
  background: #f5f7fa;
}
</style>