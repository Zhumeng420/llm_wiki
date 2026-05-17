<template>
  <div>
    <!-- 顶部 4 个核心指标 -->
    <el-row :gutter="16">
      <el-col :span="6"><div class="card metric"><span class="label">Wiki 页数</span><span class="value">{{ overview.wikiTotal ?? '-' }}</span></div></el-col>
      <el-col :span="6"><div class="card metric"><span class="label">数据源</span><span class="value">{{ overview.sourceTotal ?? '-' }}</span></div></el-col>
      <el-col :span="6"><div class="card metric"><span class="label">图谱节点 / 边</span><span class="value">{{ overview.graphNodes ?? 0 }} / {{ overview.graphEdges ?? 0 }}</span></div></el-col>
      <el-col :span="6"><div class="card metric"><span class="label">社区数</span><span class="value">{{ overview.communities ?? 0 }}</span></div></el-col>
    </el-row>

    <!-- 实时进度 -->
    <div class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;">
        <strong>实时进度（SSE）</strong>
        <el-tag :type="connected ? 'success' : 'info'">{{ connected ? '已连接' : '未连接' }}</el-tag>
      </div>
      <el-table :data="progressList" size="small" max-height="320" style="margin-top:10px">
        <el-table-column prop="taskId" label="TaskId" width="80" />
        <el-table-column prop="displayName" label="名称" />
        <el-table-column prop="stage" label="阶段" width="110">
          <template #default="{ row }">
            <el-tag :type="stageType(row.stage)">{{ row.stage }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="220">
          <template #default="{ row }">
            <el-progress :percentage="row.percent || 0" :status="statusType(row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" />
        <el-table-column prop="timestamp" label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.timestamp) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 最近任务 + 阶段分布 -->
    <el-row :gutter="16">
      <el-col :span="14">
        <div class="card">
          <strong>最近任务</strong>
          <el-table :data="overview.recentTasks || []" size="small" style="margin-top:10px">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="stage" label="阶段" width="100" />
            <el-table-column prop="percent" label="进度" width="80" />
            <el-table-column prop="errorMessage" label="错误" />
          </el-table>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="card">
          <strong>页面类型分布</strong>
          <div ref="chartRef" style="height:280px;margin-top:10px"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getOverview, wikiStats } from '@/api'

const overview = ref<any>({})
const progressList = ref<any[]>([])
const connected = ref(false)
const chartRef = ref<HTMLDivElement>()
let es: EventSource | null = null
let chart: any = null

async function refresh() {
  overview.value = await getOverview()
  const stats = await wikiStats()
  await nextTick()
  if (chartRef.value) {
    if (!chart) chart = echarts.init(chartRef.value)
    const data = Object.entries(stats.byType || {}).map(([name, value]: any) => ({ name, value }))
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c}' },
        data
      }]
    })
  }
}

function connect() {
  es = new EventSource('/api/progress/stream')
  es.onopen = () => connected.value = true
  es.onerror = () => connected.value = false
  es.addEventListener('progress', (ev: any) => {
    try {
      const e = JSON.parse(ev.data)
      const idx = progressList.value.findIndex((p: any) => p.taskId === e.taskId)
      if (idx >= 0) progressList.value[idx] = e
      else progressList.value.unshift(e)
      if (progressList.value.length > 50) progressList.value.pop()
    } catch {}
  })
}

function stageType(stage: string) {
  return { PARSE: '', ANALYZE: 'warning', GENERATE: 'warning', INDEX: 'success', GRAPH: 'success', DONE: 'success', FAIL: 'danger' }[stage] || ''
}
function statusType(s: string) { return s === 'FAILED' ? 'exception' : s === 'SUCCESS' ? 'success' : '' }
function formatTime(t: any) { return t ? new Date(t).toLocaleTimeString() : '' }

onMounted(() => { refresh(); connect(); })
onUnmounted(() => { es?.close(); chart?.dispose() })
</script>
