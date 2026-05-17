import http from './http'

/** Dashboard 总览 */
export const getOverview = () => http.get('/dashboard').then(r => r.data)

/** Settings */
export const getLlmSettings = () => http.get('/settings/llm').then(r => r.data)
export const updateLlmSettings = (cfg: any) => http.put('/settings/llm', cfg).then(r => r.data)
export const pingLlm = () => http.post('/settings/llm/ping').then(r => r.data)

/** Sources & Tasks */
export const listSources = () => http.get('/sources').then(r => r.data)
export const uploadSourceFile = (file: File) => {
  const fd = new FormData()
  fd.append('file', file)
  return http.post('/sources/file', fd, { headers: { 'Content-Type': 'multipart/form-data' } }).then(r => r.data)
}
export const submitUrl = (url: string, watch: boolean) =>
  http.post('/sources/url', { url, watch }).then(r => r.data)
export const submitRemote = (kind: string, ref: string, displayName: string, watch: boolean) =>
  http.post('/sources/remote', { kind, ref, displayName, watch }).then(r => r.data)
export const listTasks = () => http.get('/sources/tasks').then(r => r.data)
export const cancelTask = (id: number) => http.post(`/sources/tasks/${id}/cancel`).then(r => r.data)
export const retryTask = (id: number) => http.post(`/sources/tasks/${id}/retry`).then(r => r.data)
export const deleteSource = (id: number) => http.delete(`/sources/${id}`).then(r => r.data)

/** Wiki */
export const listWikiPages = (type?: string) =>
  http.get('/wiki/pages', { params: type ? { type } : {} }).then(r => r.data)
export const getWikiPage = (slug: string) => http.get(`/wiki/pages/${slug}`).then(r => r.data)
export const wikiStats = () => http.get('/wiki/stats').then(r => r.data)

/** Graph */
export const getGraph = (minWeight = 0) =>
  http.get('/graph', { params: { minWeight } }).then(r => r.data)
export const graphInsights = () => http.get('/graph/insights').then(r => r.data)

/** Search */
export const search = (q: string, topK = 10) =>
  http.get('/search', { params: { q, topK } }).then(r => r.data)

/** Insight */
export const getGap = (useLlm = true) =>
  http.get('/insights/gap', { params: { useLlm } }).then(r => r.data)
export const getProactiveGap = (count = 15, topK = 5) =>
  http.get('/insights/proactive-gap', { params: { count, topK }, timeout: 30 * 60 * 1000 }).then(r => r.data)

/** Schedule */
export const getScheduleConfig = () => http.get('/schedule/config').then(r => r.data)
export const updateScheduleConfig = (cfg: any) =>
  http.post('/schedule/config', cfg).then(r => r.data)
export const listWatched = () => http.get('/schedule/watched').then(r => r.data)
export const toggleWatch = (id: number, enabled: boolean) =>
  http.post(`/schedule/sources/${id}/toggle`, { enabled }).then(r => r.data)
export const runScheduleNow = () => http.post('/schedule/run-now').then(r => r.data)

/** Eval */
export const runEval = (file: File, name: string, useJudge: boolean) => {
  const fd = new FormData()
  fd.append('file', file)
  return http.post('/eval/run', fd, {
    params: { name, useJudge },
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 600000
  }).then(r => r.data)
}
export const listEvalReports = () => http.get('/eval/reports').then(r => r.data)
export const getEvalReport = (id: number) => http.get(`/eval/reports/${id}`).then(r => r.data)

/** Progress 最近事件 */
export const recentProgress = () => http.get('/progress/recent').then(r => r.data)
