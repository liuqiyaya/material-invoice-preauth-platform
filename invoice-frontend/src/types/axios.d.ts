import 'axios'

declare module 'axios' {
  interface AxiosRequestConfig {
    skipLoading?: boolean
    skipAuth?: boolean
    raw?: boolean
  }
}
