import type { AxiosRequestConfig } from 'axios'
import { http } from './http'

export function get<T>(url: string, params?: object, config?: AxiosRequestConfig) {
  return http.get<T, T>(url, { params, ...config })
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return http.post<T, T>(url, data, config)
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return http.put<T, T>(url, data, config)
}

export function del<T>(url: string, params?: object, config?: AxiosRequestConfig) {
  return http.delete<T, T>(url, { params, ...config })
}

export function upload<T>(url: string, formData: FormData, config?: AxiosRequestConfig) {
  return http.post<T, T>(url, formData, config)
}
