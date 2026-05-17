<template>
  <div>
    <div class="card">
      <el-input v-model="q" placeholder="输入问题，回车检索" clearable size="large" @keyup.enter="run">
        <template #append><el-button type="primary" @click="run">检索</el-button></template>
      </el-input>
    </div>
    <div class="card">
      <el-empty v-if="!hits.length && !loading" description="暂无结果" />
      <el-skeleton v-if="loading" :rows="5" animated />
      <div v-for="(h, i) in hits" :key="h.slug" class="hit">
        <div style="display:flex;align-items:center;gap:8px;">
          <span style="color:#6366f1;font-weight:600">#{{ i + 1 }}</span>
          <span style="font-size:16px;font-weight:600">{{ h.title }}</span>
          <el-tag size="small">{{ h.type }}</el-tag>
          <el-tag size="small" type="info">{{ h.source }}</el-tag>
          <el-tag size="small" type="success">score {{ (h.score || 0).toFixed(3) }}</el-tag>
        </div>
        <div style="color:#475569;margin-top:6px">{{ h.summary }}</div>
        <div style="color:#94a3b8;font-size:12px;margin-top:4px">slug: {{ h.slug }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { search } from '@/api'

const q = ref('')
const hits = ref<any[]>([])
const loading = ref(false)

async function run() {
  if (!q.value.trim()) return
  loading.value = true
  try { hits.value = await search(q.value, 10) }
  finally { loading.value = false }
}
</script>
<style scoped>.hit { padding: 12px 0; border-bottom: 1px dashed #e5e7eb; } .hit:last-child { border:none }</style>
