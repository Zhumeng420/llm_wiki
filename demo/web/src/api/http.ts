import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  // 默认 5 分钟，兼容大多数接口；超长接口（如主动反推）在调用点单独覆盖
  timeout: 5 * 60 * 1000
})

http.interceptors.response.use(
  (resp) => resp,
  (err) => {
    const url = err?.config?.url
    const status = err?.response?.status
    const code = err?.code
    console.error('[api] error', { url, status, code, message: err?.message })
    return Promise.reject(err)
  }
)

export default http
