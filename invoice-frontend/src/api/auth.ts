import { post } from './request'

export interface LoginPayload {
  username: string
  password: string
}

export function login(payload: LoginPayload) {
  return post<string>('/admin/login', payload)
}

export interface RegisterPayload {
  username: string
  password: string
  confirmPassword: string
  nickname?: string
}

export function register(payload: RegisterPayload) {
  return post<number>('/admin/register', payload)
}