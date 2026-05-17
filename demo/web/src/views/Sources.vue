<template>
  <div>
    <div class="card">
      <el-tabs v-model="tab">
        <el-tab-pane label="文件上传" name="file">
          <el-upload drag :auto-upload="true" :http-request="customUpload" :show-file-list="false">
            <el-icon style="font-size:40px;color:#6366f1"><UploadFilled /></el-icon>
            <div>将 PDF / Word / Excel / PPT / 图片 拖到此处或<em>点击上传</em></div>
          </el-upload>
        </el-tab-pane>
        <el-tab-pane label="网页 URL" name="url">
          <el-form inline @submit.prevent>
            <el-form-item label="URL"><el-input v-model="url" style="width:480px" placeholder="https://..." /></el-form-item>
            <el-form-item label="定时刷新"><el-switch v-model="watchUrl" /></el-form-item>
            <el-form-item><el-button type="primary" :disabled="!url" @click="onSubmitUrl">提交</el-button></el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="飞书 / 钉钉" name="remote">
          <el-form inline @submit.prevent>
            <el-form-item label="类型">
              <el-select v-model="remoteKind" style="width:120px"><el-option label="FEISHU" value="FEISHU" /><el-option label="DINGTALK" value="DINGTALK" /></el-select>
            </el-form-item>
            <el-form-item label="文档 ref"><el-input v-model="remoteRef" style="width:300px" placeholder="docToken / 链接" /></el-form-item>
            <el-form-item label="名称"><el-input v-model="remoteName" style="width:220px" /></el-form-item>
            <el-form-item label="定时刷新"><el-switch v-model="watchRemote" /></el-form-item>
            <el-form-item><el-button type="primary" :disabled="!remoteRef" @click="onSubmitRemote">提交</el-button></el-form-item>
          </el-form>
          <el-alert type="info" :closable="false" title="飞书：需要在 Settings 里填好 appId / appSecret；钉钉：需要 appKey / appSecret 与 unionId" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <strong>所有数据源</strong>
        <el-button @click="refresh">刷新</el-button>
      </div>
      <el-table :data="sources" size="small" style="margin-top:10px">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="kind" label="类型" width="100" />
        <el-table-column prop="displayName" label="名称" />
        <el-table-column prop="watchEnabled" label="定时" width="80">
          <template #default="{ row }"><el-tag :type="row.watchEnabled ? 'success' : 'info'">{{ row.watchEnabled ? 'ON' : 'OFF' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="lastFetchedAt" label="上次刷新" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }"><el-button size="small" type="danger" @click="onDelete(row.id)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card">
      <strong>最近任务</strong>
      <el-table :data="tasks" size="small" style="margin-top:10px">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="stage" label="阶段" width="100" />
        <el-table-column prop="percent" label="进度" width="80" />
        <el-table-column prop="retryCount" label="重试" width="60" />
        <el-table-column prop="errorMessage" label="错误" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="onRetry(row.id)">重试</el-button>
            <el-button size="small" type="warning" @click="onCancel(row.id)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listSources, listTasks, uploadSourceFile, submitUrl, submitRemote, cancelTask, retryTask, deleteSource } from '@/api'

const tab = ref('file')
const url = ref('')
const watchUrl = ref(false)
const remoteKind = ref('FEISHU')
const remoteRef = ref('')
const remoteName = ref('')
const watchRemote = ref(true)
const sources = ref<any[]>([])
const tasks = ref<any[]>([])

async function refresh() {
  sources.value = await listSources()
  tasks.value = await listTasks()
}
async function customUpload(opt: any) {
  try { const t = await uploadSourceFile(opt.file); ElMessage.success('已入队 task=' + t.id); await refresh() }
  catch (e: any) { ElMessage.error(e?.message) }
}
async function onSubmitUrl() {
  const t = await submitUrl(url.value, watchUrl.value); ElMessage.success('已入队 task=' + t.id); url.value = ''; await refresh()
}
async function onSubmitRemote() {
  const t = await submitRemote(remoteKind.value, remoteRef.value, remoteName.value || remoteRef.value, watchRemote.value)
  ElMessage.success('已入队 task=' + t.id); remoteRef.value = ''; await refresh()
}
async function onCancel(id: number) { await cancelTask(id); await refresh() }
async function onRetry(id: number) { await retryTask(id); await refresh() }
async function onDelete(id: number) { await deleteSource(id); await refresh() }

onMounted(refresh)
</script>
