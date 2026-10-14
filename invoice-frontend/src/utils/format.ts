import dayjs from 'dayjs'

export function formatDateTime(value?: string | number | Date | null): string {
  if (value === null || value === undefined || value === '') return '-'
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : '-'
}

export function formatDate(value?: string | number | Date | null): string {
  if (value === null || value === undefined || value === '') return '-'
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD') : '-'
}

export function formatMoney(value?: number | string | null): string {
  if (value === null || value === undefined || value === '') return '-'
  const amount = Number(value)
  if (Number.isNaN(amount)) return '-'
  return `¥${amount.toFixed(2)}`
}