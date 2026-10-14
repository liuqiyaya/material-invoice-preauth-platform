import { get, post, upload } from './request'

// ========== 上传凭证 ==========

export interface EvidenceUploadPayload {
  invoiceId: number
  screenshots: File[]
  payments: File[]
  remark?: string
}

export function uploadEvidence(payload: EvidenceUploadPayload) {
  const formData = new FormData()
  formData.append('invoiceId', String(payload.invoiceId))
  payload.screenshots.forEach((f) => formData.append('screenshots', f))
  payload.payments.forEach((f) => formData.append('payments', f))
  if (payload.remark) formData.append('remark', payload.remark)
  return upload<number>('/evidence/upload', formData)
}

// ========== 查询凭证组 ==========

export interface EvidenceFileVO {
  id: number
  fileId: number
  filePath: string
  originalName: string
}

export interface EvidenceGroupVO {
  id: number
  invoiceId: number
  status: number
  statusDesc: string
  reviewerId?: number
  reviewTime?: string
  reviewRemark?: string
  createTime: string
  screenshots: EvidenceFileVO[]
  payments: EvidenceFileVO[]
}

export function listEvidence(invoiceId: number) {
  return get<EvidenceGroupVO[]>('/evidence/list', { invoiceId })
}

export function getEvidenceDetail(groupId: number) {
  return get<EvidenceGroupVO>('/evidence/detail', { groupId })
}

// ========== 审核凭证 ==========

export interface EvidenceReviewDTO {
  groupId: number
  pass: boolean
  remark?: string
}

export function reviewEvidence(payload: EvidenceReviewDTO) {
  return post<void>('/evidence/review', payload)
}