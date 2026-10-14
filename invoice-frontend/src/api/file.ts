export function buildFileDownloadUrl(filePath: string, filename: string): string {
  const base = import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '')
  const query = new URLSearchParams({ filePath, filename })
  return `${base}/file/download?${query.toString()}`
}

export function downloadFile(filePath: string, filename: string): void {
  const link = document.createElement('a')
  link.href = buildFileDownloadUrl(filePath, filename)
  link.download = filename
  link.target = '_blank'
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
