<template>
  <div class="layout-2col">
    <div class="card" style="width:300px;flex:0 0 300px;overflow:auto;">
      <el-input v-model="filter" placeholder="搜索 slug / 标题" clearable />
      <el-select v-model="type" placeholder="类型" clearable style="margin-top:8px;width:100%" @change="refresh">
        <el-option v-for="t in types" :key="t" :label="t" :value="t" />
      </el-select>
      <el-divider />
      <div v-for="p in filteredPages" :key="p.slug" @click="select(p.slug)" class="page-item" :class="{ active: current?.slug === p.slug }">
        <div style="font-weight:600;font-size:13px">{{ p.title }}</div>
        <div style="color:#94a3b8;font-size:11px">{{ p.type }} · {{ p.slug }}</div>
      </div>
    </div>
    <div class="card" style="flex:1;overflow:auto;">
      <div v-if="!current">请选择左侧的页面</div>
      <div v-else>
        <h2 style="margin:0">{{ current.title }}</h2>
        <div style="color:#64748b;font-size:13px">{{ current.type }} · {{ current.slug }}</div>
        <div style="margin-top:6px"><span class="tag-pill" v-for="t in (current.tags || '').split(',').filter(Boolean)" :key="t">{{ t }}</span></div>
        <el-divider />
        <div class="markdown-body" v-html="rendered"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import { listWikiPages, getWikiPage } from '@/api'

const md = new MarkdownIt({ html: false, breaks: true, linkify: true })
const pages = ref<any[]>([])
const current = ref<any>(null)
const filter = ref('')
const type = ref('')
const types = ['entity', 'concept', 'source', 'overview', 'index', 'log', 'purpose']

const filteredPages = computed(() => {
  const f = filter.value.toLowerCase()
  return pages.value.filter((p: any) => !f || p.slug.toLowerCase().includes(f) || (p.title || '').toLowerCase().includes(f))
})
const rendered = computed(() => md.render(current.value?.content || ''))

async function refresh() {
  pages.value = await listWikiPages(type.value || undefined)
}
async function select(slug: string) {
  current.value = await getWikiPage(slug)
}

onMounted(refresh)
</script>

<style scoped>
.layout-2col { display:flex; gap:16px; height:calc(100vh - 110px); }
.page-item { padding:8px 6px; border-radius:6px; cursor:pointer; }
.page-item:hover { background:#f1f5f9 }
.page-item.active { background:#eef2ff; color:#4338ca }
</style>
