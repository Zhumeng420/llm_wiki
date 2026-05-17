<template>
  <div>
    <div class="card">
      <el-form inline @submit.prevent>
        <el-form-item label="启用">
          <el-switch v-model="config.enabled" />
        </el-form-item>
        <el-form-item label="cron">
          <el-input v-model="config.cron" style="width:240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSave">保存</el-button>
          <el-button type="success" @click="onRunNow">立即执行</el-button>
        </el-form-item>
      </el-form>
      <el-alert :closable="false" type="info" title="cron 示例：0 0 3 * * ?（每天 3 点执行）" />
    </div>

    <div class="card">
      <strong>已启用定时刷新的来源</strong>
      <el-table :data="watched" size="small" style="margin-top:10px">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="kind" label="类型" width="100" />
        <el-table-column prop="displayName" label="名称" />
        <el-table-column prop="ref" label="ref" />
        <el-table-column prop="lastFetchedAt" label="上次刷新" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="onToggle(row.id, false)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getScheduleConfig, updateScheduleConfig, listWatched, toggleWatch, runScheduleNow } from '@/api'

const config = ref<any>({ enabled: true, cron: '0 0 3 * * ?' })
const watched = ref<any[]>([])
async function refresh() { config.value = await getScheduleConfig(); watched.value = await listWatched() }
async function onSave() { await updateScheduleConfig(config.value); ElMessage.success('已保存（cron 修改需要重启生效）') }
async function onRunNow() { await runScheduleNow(); ElMessage.success('已触发一次') }
async function onToggle(id: number, enabled: boolean) { await toggleWatch(id, enabled); await refresh() }
onMounted(refresh)
</script>
