<template>
  <div>
    <div class="card">
      <el-tabs v-model="tab">
        <el-tab-pane label="Chat 模型" name="chat">
          <el-form label-width="100px">
            <el-form-item label="Base URL"><el-input v-model="cfg.chat.baseUrl" /></el-form-item>
            <el-form-item label="API Key"><el-input v-model="cfg.chat.apiKey" type="password" show-password /></el-form-item>
            <el-form-item label="模型"><el-input v-model="cfg.chat.model" /></el-form-item>
            <el-form-item label="Temperature"><el-input-number v-model="cfg.chat.temperature" :min="0" :max="2" :step="0.1" /></el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="Embedding" name="embedding">
          <el-form label-width="100px">
            <el-form-item label="Base URL"><el-input v-model="cfg.embedding.baseUrl" /></el-form-item>
            <el-form-item label="API Key"><el-input v-model="cfg.embedding.apiKey" type="password" show-password /></el-form-item>
            <el-form-item label="模型"><el-input v-model="cfg.embedding.model" /></el-form-item>
            <el-form-item label="维度">
              <el-input-number v-model="cfg.embedding.dimensions" :min="64" :step="64" />
              <span style="margin-left:8px;color:#94a3b8">注意维度调整后需重建索引</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="Vision" name="vision">
          <el-form label-width="100px">
            <el-form-item label="启用"><el-switch v-model="cfg.vision.enabled" /></el-form-item>
            <el-form-item label="Base URL"><el-input v-model="cfg.vision.baseUrl" /></el-form-item>
            <el-form-item label="API Key"><el-input v-model="cfg.vision.apiKey" type="password" show-password /></el-form-item>
            <el-form-item label="模型"><el-input v-model="cfg.vision.model" /></el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <el-divider />
      <el-button type="primary" @click="onSave">保存</el-button>
      <el-button @click="onPing" :loading="pinging">健康检查</el-button>
      <el-button @click="load">重置</el-button>
    </div>

    <div class="card" v-if="pingResult">
      <strong>健康检查结果</strong>
      <pre style="background:#0f172a;color:#e2e8f0;padding:10px;border-radius:6px">{{ JSON.stringify(pingResult, null, 2) }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getLlmSettings, updateLlmSettings, pingLlm } from '@/api'

const tab = ref('chat')
const cfg = ref<any>({ chat: {}, embedding: {}, vision: {} })
const pingResult = ref<any>(null)
const pinging = ref(false)

async function load() { cfg.value = await getLlmSettings() }
async function onSave() { await updateLlmSettings(cfg.value); ElMessage.success('已保存') }
async function onPing() { pinging.value = true; try { pingResult.value = await pingLlm() } finally { pinging.value = false } }

onMounted(load)
</script>
