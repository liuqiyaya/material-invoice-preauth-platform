export const QUALIFY_STATUS = {
  PENDING: 0,
  NEED_EVIDENCE: 1,
  NEED_MANUAL: 2,
  PASS: 3,
  REJECT: 4,
  CANCELLED: 5,
} as const

export const QUALIFY_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待审核', color: '#909399' },
  1: { label: '待补凭证', color: '#e6a23c' },
  2: { label: '待人工处理', color: '#e67e22' },
  3: { label: '通过', color: '#67c23a' },
  4: { label: '不通过', color: '#f56c6c' },
  5: { label: '已取消', color: '#909399' },
}

export const FINANCE_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '未提交', color: '#909399' },
  1: { label: '已提交', color: '#67c23a' },
}

export const REIMBURSE_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '未完成', color: '#909399' },
  1: { label: '成功', color: '#67c23a' },
  2: { label: '失败', color: '#f56c6c' },
}

export const CATEGORY_LEVEL_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '可以', color: '#67c23a' },
  2: { label: '存疑', color: '#e6a23c' },
  3: { label: '不可以', color: '#f56c6c' },
}

export const PRICE_TYPE_MAP: Record<number, { label: string }> = {
  1: { label: '材料' },
  2: { label: '低值品' },
  3: { label: '资产' },
}

export const STATUS_UPDATE_TYPE = {
  FINANCE: 'FINANCE',
  REIMBURSE: 'REIMBURSE',
  CANCEL: 'CANCEL',
} as const

export const EVIDENCE_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待上传', color: '#909399' },
  1: { label: '待人工审核', color: '#e6a23c' },
  2: { label: '已通过', color: '#67c23a' },
  3: { label: '不通过', color: '#f56c6c' },
}

export const LOG_ACTION_MAP: Record<string, { label: string; color: string }> = {
  PRE_AUDIT: { label: '系统预审', color: '#409eff' },
  PRE_AUDIT_FAIL: { label: '预审失败', color: '#f56c6c' },
  DUPLICATE_HIT: { label: '重复检测', color: '#e6a23c' },
  EVIDENCE_UPLOAD: { label: '上传凭证', color: '#409eff' },
  EVIDENCE_REVIEW: { label: '审核凭证', color: '#67c23a' },
  QUALIFY_STATUS_SYNC: { label: '状态同步', color: '#909399' },
  MANUAL_EDIT: { label: '人工修改', color: '#e67e22' },
  MANUAL_EDIT_ITEMS: { label: '修改商品明细', color: '#e67e22' },
  FINANCE_STATUS_CHANGE: { label: '财务提交', color: '#409eff' },
  REIMBURSE_STATUS_CHANGE: { label: '报销结果', color: '#67c23a' },
  INVOICE_CANCEL: { label: '取消发票', color: '#f56c6c' },
  CATEGORY_RULE_ADD: { label: '新增规则', color: '#67c23a' },
  CATEGORY_RULE_UPDATE: { label: '修改规则', color: '#e6a23c' },
  CATEGORY_RULE_DELETE: { label: '删除规则', color: '#f56c6c' },
}