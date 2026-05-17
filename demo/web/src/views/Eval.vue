<template>
  <div>
    <div class="card">
      <el-form inline @submit.prevent>
        <el-form-item label="报告名"><el-input v-model="name" /></el-form-item>
        <el-form-item label="LLM 评分"><el-switch v-model="useJudge" /></el-form-item>
        <el-form-item label="CSV 文件">
          <el-upload :auto-upload="false" :on-change="onFileChange" :show-file-list="false" accept=".csv">
            <el-button>选择 CSV</el-button>
          </el-upload>
          <span v-if="file" style="margin-left:8px;color:#10b981">{{ file.name }}</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="running" :disabled="!file" @click="onRun">开始评测</el-button>
        </el-form-item>
      </el-form>
      <el-alert :closable="false" type="info"
                title="CSV 表头：question,expected_slugs；expected_slugs 用分号分隔多个候选 slug" />
    </div>

    <div class="card">
      <strong>历史报告</strong>
      <el-table :data="reports" size="small" style="margin-top:10px" @row-click="onSelect">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="total" label="总数" width="80" />
        <el-table-column label="answerRate" width="120">
          <template #default="{ row }">{{ pct(row.answerRate) }}</template>
        </el-table-column>
        <el-table-column label="hitRate@5" width="120">
          <template #default="{ row }">{{ pct(row.hitRateAt5) }}</template>
        </el-table-column>
        <el-table-column prop="avgRelevance" label="avgRelevance" width="120" />
        <el-table-column prop="avgLatencyMs" label="avgLatencyMs" width="120" />
        <el-table-column prop="createdAt" label="时间" width="200" />
      </el-table>
    </div>

    <div class="card" v-if="selected">
      <strong>报告 #{{ selected.id }} 指标趋势</strong>
      <div ref="chartRef" style="height:280px"></div>
      <strong>明细</strong>
      <el-table :data="details" size="small">
        <el-table-column prop="question" label="question" />
        <el-table-column prop="answered" label="answered" width="100" />
        <el-table-column prop="hit" label="hit" width="80" />
        <el-table-column prop="relevance" label="relevance" width="100" />
        <el-table-column prop="latencyMs" label="latencyMs" width="100" />
        <el-table-column prop="error" label="error" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { runEval, listEvalReports, getEvalReport } from '@/api'

const name = ref('report-' + new Date().toISOString().slice(0, 10))
const useJudge = ref(false)
const file = ref<File | null>(null)
const running = ref(false)
const reports = ref<any[]>([])
const selected = ref<any>(null)
const details = ref<any[]>([])
const chartRef = ref<HTMLDivElement>()
let chart: any

function onFileChange(f: any) { file.value = f.raw }
function pct(v: number) { return v == null ? '-' : (v * 100).toFixed(1) + '%' }

async function refresh() { reports.value = await listEvalReports() }
async function onRun() {
  if (!file.value) return
  running.value = true
  try {
    const r = await runEval(file.value, name.value, useJudge.value)
    ElMessage.success('评测完成 #' + r.id)
    await refresh()
    onSelect(r)
  } finally { running.value = false }
}
async function onSelect(row: any) {
  selected.value = await getEvalReport(row.id)
  details.value = JSON.parse(selected.value.details || '[]')
  await nextTick(); renderChart()
}
function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['answered', 'hit', 'relevance'] },
    xAxis: { type: 'category', data: details.value.map((_: any, i: number) => 'Q' + (i + 1)) },
    yAxis: { type: 'value' },
    series: [
      { name: 'answered', type: 'bar', stack: 'a', data: details.value.map((d: any) => d.answered ? 1 : 0) },
      { name: 'hit', type: 'bar', stack: 'b', data: details.value.map((d: any) => d.hit ? 1 : 0) },
      { name: 'relevance', type: 'line', data: details.value.map((d: any) => d.relevance) }
    ]
  })
}
watch(details, () => nextTick(renderChart))
onMounted(refresh)
</script>
