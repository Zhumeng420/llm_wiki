<template>
  <div>
    <div class="card" style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
      <strong>知识空白反推</strong>
      <el-switch v-model="useLlm" active-text="LLM 语义审计" />
      <el-button type="primary" :loading="loading" @click="run">基础分析</el-button>
      <el-divider direction="vertical" />
      <span>主动反推：</span>
      <span>问题数</span>
      <el-input-number v-model="pCount" :min="5" :max="30" size="small" />
      <span>topK</span>
      <el-input-number v-model="pTopK" :min="3" :max="10" size="small" />
      <el-button type="warning" :loading="pLoading" @click="runProactive">主动反推</el-button>
    </div>

    <el-row :gutter="16" v-if="report">
      <el-col :span="12">
        <div class="card">
          <strong>未答问题（LLM）</strong>
          <el-empty v-if="!report.unanswered?.length" description="暂无 / 未启用 LLM" />
          <div v-for="(u, i) in report.unanswered" :key="i" style="padding:8px 0;border-bottom:1px dashed #e5e7eb">
            <div style="font-weight:600">Q{{ i + 1 }}: {{ u.question }}</div>
            <div style="color:#64748b">原因: {{ u.reason }}</div>
            <div style="margin-top:4px">
              建议补充：
              <el-tag v-for="s in u.suggestedSources" :key="s" size="small" style="margin-right:4px">{{ s }}</el-tag>
            </div>
          </div>
        </div>
        <div class="card">
          <strong>缺失主题</strong>
          <div style="margin-top:8px">
            <el-tag v-for="t in (report.missingTopics || [])" :key="t" type="warning" style="margin:2px">{{ t }}</el-tag>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="card">
          <strong>结构信号</strong>
          <p>孤立节点（{{ report.isolatedNodes?.length || 0 }}）</p>
          <el-tag v-for="n in (report.isolatedNodes || [])" :key="n" size="small" style="margin:2px">{{ n }}</el-tag>
          <p>桥节点（{{ report.bridgeNodes?.length || 0 }}）</p>
          <el-tag v-for="n in (report.bridgeNodes || [])" :key="n" size="small" type="success" style="margin:2px">{{ n }}</el-tag>
          <p>稀疏社区（{{ report.sparseCommunities?.length || 0 }}）</p>
          <div v-for="c in (report.sparseCommunities || [])" :key="c.communityId" style="margin-top:4px">
            <el-tag type="warning">community {{ c.communityId }}</el-tag>
            <el-tag v-for="m in c.members" :key="m" size="small" style="margin-left:4px">{{ m }}</el-tag>
          </div>
        </div>
        <div class="card">
          <strong>建议</strong>
          <ul><li v-for="s in (report.suggestions || [])" :key="s">{{ s }}</li></ul>
        </div>
      </el-col>
    </el-row>

    <!-- 主动空白反推结果 -->
    <div v-if="pReport" class="card" style="margin-top:16px">
      <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
        <strong>🤖 主动空白反推结果</strong>
        <el-tag>总计 {{ pReport.summary?.total || 0 }}</el-tag>
        <el-tag type="success">可回答 {{ pReport.summary?.answerable || 0 }}</el-tag>
        <el-tag type="warning">部分覆盖 {{ pReport.summary?.partial || 0 }}</el-tag>
        <el-tag type="danger">无法回答 {{ pReport.summary?.no || 0 }}</el-tag>
      </div>
      <el-alert v-if="pReport.llmError" type="error" :title="pReport.llmError" show-icon style="margin-top:8px" />

      <div v-if="pReport.suggestions?.length" style="margin-top:12px">
        <strong>📌 综合补充建议</strong>
        <ul><li v-for="s in pReport.suggestions" :key="s">{{ s }}</li></ul>
      </div>

      <el-table :data="pReport.results || []" style="margin-top:12px" stripe>
        <el-table-column prop="question" label="系统猜测的问题" min-width="280" show-overflow-tooltip />
        <el-table-column prop="angle" label="角度" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ row.angle }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="verdict" label="判定" width="110">
          <template #default="{ row }">
            <el-tag :type="verdictType(row.verdict)" size="small">{{ verdictLabel(row.verdict) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="缺失要点" min-width="220">
          <template #default="{ row }">
            <div v-if="row.missingPoints?.length">
              <el-tag v-for="m in row.missingPoints" :key="m" type="danger" size="small" style="margin:2px">{{ m }}</el-tag>
            </div>
            <span v-else style="color:#94a3b8">—</span>
          </template>
        </el-table-column>
        <el-table-column label="建议补充" min-width="220">
          <template #default="{ row }">
            <div v-if="row.suggestedSources?.length">
              <el-tag v-for="s in row.suggestedSources" :key="s" type="warning" size="small" style="margin:2px">{{ s }}</el-tag>
            </div>
            <span v-else style="color:#94a3b8">—</span>
          </template>
        </el-table-column>
        <el-table-column label="检索证据" min-width="200">
          <template #default="{ row }">
            <div v-if="row.evidence?.length">
              <div v-for="e in row.evidence.slice(0,3)" :key="e.slug" style="font-size:12px;color:#64748b">
                · {{ e.title }} <span style="color:#cbd5e1">({{ (e.score||0).toFixed(4) }})</span>
              </div>
            </div>
            <span v-else style="color:#94a3b8">无</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getGap, getProactiveGap } from '@/api'
const useLlm = ref(true)
const report = ref<any>(null)
const loading = ref(false)
async function run() { loading.value = true; try { report.value = await getGap(useLlm.value) } finally { loading.value = false } }

const pCount = ref(15)
const pTopK = ref(5)
const pReport = ref<any>(null)
const pLoading = ref(false)
async function runProactive() {
  pLoading.value = true
  try {
    pReport.value = await getProactiveGap(pCount.value, pTopK.value)
    ElMessage.success(`主动反推完成：共 ${pReport.value?.summary?.total || 0} 个问题`)
  } catch (e: any) {
    ElMessage.error('主动反推失败：' + (e?.message || e))
  } finally {
    pLoading.value = false
  }
}
function verdictType(v: string) {
  if (v === 'answerable') return 'success'
  if (v === 'partial') return 'warning'
  return 'danger'
}
function verdictLabel(v: string) {
  if (v === 'answerable') return '可回答'
  if (v === 'partial') return '部分覆盖'
  return '无法回答'
}
</script>
