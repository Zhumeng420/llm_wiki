<template>
  <div>
    <div class="card" style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
      <span>最低权重</span>
      <el-slider v-model="minWeight" :min="0" :max="20" :step="0.5" style="width:240px" @change="refresh" />
      <el-button @click="refresh">刷新</el-button>
      <el-button @click="fitView">自适应</el-button>
      <el-button type="warning" plain @click="rebuild">重建图谱</el-button>
      <el-checkbox v-model="hideSources" @change="render">隐藏 source 节点</el-checkbox>
      <el-tag>节点 {{ visibleNodeCount }} · 边 {{ visibleEdgeCount }} · 社区 {{ communities }}</el-tag>
      <span v-if="loading" style="color:#94a3b8">加载中...</span>
      <span v-if="errorMsg" style="color:#ef4444">{{ errorMsg }}</span>
    </div>
    <div class="card" style="height:calc(100vh - 200px);padding:0;position:relative;overflow:hidden;">
      <div ref="container" style="width:100%;height:100%;min-height:400px;"></div>
      <div v-if="!loading && nodes.length === 0" style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;color:#94a3b8;">
        暂无图谱数据，请先到 Sources 页导入内容。
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, nextTick, computed } from 'vue'
import { Graph } from '@antv/g6'
import { getGraph } from '@/api'
import http from '@/api/http'
import { ElMessage } from 'element-plus'

const container = ref<HTMLDivElement>()
const minWeight = ref(0)
const nodes = ref<any[]>([])
const edges = ref<any[]>([])
const communities = ref(0)
const loading = ref(false)
const errorMsg = ref('')
const hideSources = ref(true)
const visibleNodeCount = ref(0)
const visibleEdgeCount = ref(0)
let graph: any = null
let resizeObs: ResizeObserver | null = null

const palette = ['#6366f1', '#ec4899', '#10b981', '#f59e0b', '#06b6d4', '#a855f7', '#ef4444', '#14b8a6', '#84cc16', '#eab308']

async function refresh() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await getGraph(minWeight.value)
    nodes.value = data.nodes || []
    edges.value = data.edges || []
    communities.value = data.communityCount || 0
  } catch (e: any) {
    errorMsg.value = '加载失败: ' + (e?.message || e)
    nodes.value = []
    edges.value = []
  } finally {
    loading.value = false
  }
  await nextTick()
  await render()
}

async function rebuild() {
  loading.value = true
  try {
    const r = await http.post('/graph/rebuild').then(x => x.data)
    ElMessage.success(`重建完成：${r.nodes} 节点 / ${r.edges} 边，合并 ${r.merged} 个同义实体`)
    await refresh()
  } catch (e: any) {
    ElMessage.error('重建失败: ' + (e?.message || e))
  } finally {
    loading.value = false
  }
}

async function render() {
  if (!container.value) return
  if (graph) { graph.destroy(); graph = null }
  if (nodes.value.length === 0) { visibleNodeCount.value = 0; visibleEdgeCount.value = 0; return }

  const rect = container.value.getBoundingClientRect()
  const width = Math.max(400, Math.floor(rect.width))
  const height = Math.max(400, Math.floor(rect.height))

  // 可选：隐藏 source 节点（它们通常默度高且标题长，会遮挡主题联通）
  const filteredNodes = hideSources.value
    ? nodes.value.filter((n: any) => (n.type || '').toLowerCase() !== 'source')
    : nodes.value
  const idSet = new Set(filteredNodes.map((n: any) => String(n.id)))
  const validEdges = edges.value.filter((e: any) => idSet.has(String(e.source)) && idSet.has(String(e.target)))
  visibleNodeCount.value = filteredNodes.length
  visibleEdgeCount.value = validEdges.length

  // 节点尺寸以 degree 开方控制，避免差距过大；max 22
  const formatted = {
    nodes: filteredNodes.map((n: any) => {
      const deg = n.degree || 0
      const size = Math.min(22, 8 + Math.sqrt(deg) * 3)
      // 根据实体类型控制形状
      const isConcept = (n.type || '').toLowerCase() === 'concept'
      return {
        id: String(n.id),
        data: { ...n },
        style: {
          labelText: truncateLabel(n.label || n.id, 12),
          labelFill: '#374151',
          labelFontSize: 10,
          labelPlacement: 'bottom',
          labelOffsetY: 4,
          labelBackground: true,
          labelBackgroundFill: 'rgba(255,255,255,0.85)',
          labelBackgroundRadius: 3,
          labelPadding: [1, 4],
          size,
          fill: palette[(n.community ?? 0) % palette.length],
          fillOpacity: isConcept ? 0.7 : 0.95,
          stroke: '#ffffff',
          lineWidth: 1.5
        }
      }
    }),
    edges: validEdges.map((e: any, i: number) => ({
      id: 'e' + i,
      source: String(e.source),
      target: String(e.target),
      data: { weight: e.weight },
      style: { lineWidth: Math.min(3, 0.5 + (e.weight || 1) * 0.25), stroke: '#cbd5e1', strokeOpacity: 0.7, endArrow: false }
    }))
  }

  graph = new Graph({
    container: container.value,
    width,
    height,
    data: formatted,
    autoFit: 'view',
    layout: {
      type: 'force',
      linkDistance: (edge: any) => 160 / Math.max(1, edge.data?.weight || 1),
      nodeStrength: -350,
      edgeStrength: 0.4,
      collide: { strength: 1, radius: 28 },
      preventOverlap: true,
      nodeSize: 30,
      alpha: 0.9,
      alphaDecay: 0.02
    },
    behaviors: ['zoom-canvas', 'drag-canvas', 'drag-element']
  })
  try {
    await graph.render()
  } catch (e: any) {
    errorMsg.value = '渲染失败: ' + (e?.message || e)
    console.error('[Graph] render error', e)
  }
}

function truncateLabel(text: string, maxLen: number): string {
  if (!text) return ''
  const t = String(text)
  return t.length > maxLen ? t.substring(0, maxLen) + '…' : t
}

function fitView() { graph?.fitView() }

function handleResize() {
  if (!graph || !container.value) return
  const rect = container.value.getBoundingClientRect()
  const w = Math.max(400, Math.floor(rect.width))
  const h = Math.max(400, Math.floor(rect.height))
  try { graph.setSize(w, h) } catch (e) { /* ignore */ }
}

onMounted(async () => {
  await refresh()
  if (container.value) {
    resizeObs = new ResizeObserver(handleResize)
    resizeObs.observe(container.value)
  }
})
onUnmounted(() => {
  resizeObs?.disconnect()
  graph?.destroy()
})
</script>
