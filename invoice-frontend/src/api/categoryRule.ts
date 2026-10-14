// d:\JavaCode\invoice-frontend\src\api\categoryRule.ts
import { get, post, put, del } from './request'

export interface CategoryRule {
  id: number
  categoryName: string
  keywords: string
  level: number
  remark?: string
  enabled: number
  createTime: string
  updateTime: string
}

export interface CategoryRuleQuery {
  pageNum?: number
  pageSize?: number
  categoryName?: string
  level?: number
  enabled?: number
  keyword?: string
}

export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  pages: number
  records: T[]
}

export function listCategoryRule(query: CategoryRuleQuery) {
  return get<PageResult<CategoryRule>>('/category-rule/list', query)
}

export function getCategoryRule(id: number) {
  return get<CategoryRule>('/category-rule/detail', { id })
}

export function addCategoryRule(data: Partial<CategoryRule>) {
  return post<number>('/category-rule/add', data)
}

export function updateCategoryRule(data: Partial<CategoryRule>) {
  return put<void>('/category-rule/update', data)
}

export function deleteCategoryRule(id: number) {
  return del<void>('/category-rule/delete', { id })
}

export function toggleCategoryRule(id: number, enabled: number) {
  return post<void>(`/category-rule/toggle-enabled?id=${id}&enabled=${enabled}`)
}
