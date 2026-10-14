import { get, post, put, upload } from './request'

export interface InvoiceListQuery {
  pageNum?: number
  pageSize?: number
  sellerName?: string
  sellerTaxNo?: string
  invoiceNo?: string
  qualifyStatus?: number
  financeStatus?: number
  reimburseStatus?: number
  weekStart?: string
  uploadBatch?: string
  submitTimeFrom?: string
  submitTimeTo?: string
}

export interface InvoiceListVO {
  id: number
  sellerName: string
  sellerTaxNo: string
  invoiceNo: string
  invoiceDate: string
  totalAmount: number
  submitTime: string
  weekStart: string
  qualifyStatus: number
  qualifyStatusDesc: string
  financeStatus: number
  financeStatusDesc: string
  reimburseStatus: number
  reimburseStatusDesc: string
  categoryResult: number
  categoryResultDesc: string
  evidenceRequired: number
  needManual: number
  rejectReason?: string
  createTime: string
}

export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  pages: number
  records: T[]
}



export function listInvoice(query: InvoiceListQuery) {
  return get<PageResult<InvoiceListVO>>('/invoice/list', query)
}

export function getInvoiceDetail(invoiceId: number) {
  return get<InvoiceDetailVO>('/invoice/detail', { invoiceId })
}

// ========== 详情相关类型 ==========

export interface InvoiceDetailVO {
  id: number
  submitterId: number
  sellerName: string
  sellerTaxNo: string
  buyerName: string
  buyerTaxNo: string
  invoiceNo: string
  invoiceDate: string
  totalAmount: number
  submitTime: string
  weekStart: string
  qualifyStatus: number
  qualifyStatusDesc: string
  financeStatus: number
  financeStatusDesc: string
  reimburseStatus: number
  reimburseStatusDesc: string
  categoryResult: number
  categoryResultDesc: string
  rejectReason?: string
  evidenceRequired: number
  evidenceReason?: number
  needManual: number
  manualRemark?: string
  sourceFileId?: number
  sourceFileKey?: string
  sourceFileName?: string
  createTime: string
  updateTime: string
  items: InvoiceItemVO[]
  evidenceGroups: EvidenceGroupVO[]
  logs: LogVO[]
  duplicates: InvoiceListVO[]
}

export interface InvoiceItemVO {
  id: number
  itemName: string
  quantity: number
  unitPrice: number
  amount: number
  categoryLevel: number
  categoryLevelDesc: string
  categoryName?: string
  priceType: number
  priceTypeDesc: string
  aiReason?: string
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

export interface EvidenceFileVO {
  id: number
  fileId: number
  filePath: string
  originalName: string
}

export interface LogVO {
  id: number
  operatorId?: number
  operatorName?: string
  action: string
  beforeValue?: string
  afterValue?: string
  remark?: string
  createTime: string
}

// ========== 状态变更 ==========

export function updateInvoiceStatus(payload: {
  invoiceId: number
  type: 'FINANCE' | 'REIMBURSE' | 'CANCEL'
  status?: number
  remark?: string
}) {
  return post<void>('/invoice/status-update', payload)
}

// ========== 人工修改 ==========

export interface ManualEditItemDTO {
  itemName: string
  quantity?: number
  unitPrice?: number
  amount?: number
  categoryLevel?: number
  categoryName?: string
  priceType?: number
  aiReason?: string
}

export function manualEditInvoice(payload: {
  invoiceId: number
  qualifyStatus?: number
  manualRemark?: string
  needManual?: number
  sellerName?: string
  sellerTaxNo?: string
  buyerName?: string
  buyerTaxNo?: string
  invoiceNo?: string
  invoiceDate?: string
  totalAmount?: number
  items?: ManualEditItemDTO[]
  remark?: string
}) {
  return put<void>('/invoice/manual-edit', payload)
}

// ========== 上传 ==========

export interface UploadResultItem {
  fileName: string
  invoiceId?: number
  fileId?: number
  status: 'SUCCESS' | 'FAILED'
  errorMsg?: string
}

export interface UploadResultVO {
  batchId: string
  total: number
  successCount: number
  failCount: number
  items: UploadResultItem[]
}

export function uploadInvoices(files: File[]) {
  const formData = new FormData()
  files.forEach((f) => formData.append('files', f))
  return upload<UploadResultVO>('/invoice/upload', formData)
}