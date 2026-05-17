<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">LLM <span class="accent">Wiki</span></div>
      <div
        v-for="r in menus"
        :key="r.path"
        class="menu-item"
        :class="{ active: $route.path === r.path }"
        @click="$router.push(r.path)"
      >
        <el-icon v-if="r.meta?.icon"><component :is="r.meta.icon" /></el-icon>
        <span>{{ r.meta?.title }}</span>
      </div>
    </aside>
    <main class="main">
      <div class="topbar">{{ currentTitle }}</div>
      <div class="content">
        <router-view v-slot="{ Component }">
          <transition name="el-fade-in-linear">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()
const menus = computed(() => router.options.routes.filter((r: any) => r.name))
const currentTitle = computed(() => (route.meta?.title as string) || 'LLM Wiki')
</script>
