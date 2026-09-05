<template>
  <div class="domain-whitelist-editor">
    <div class="domain-whitelist-switch">
      <el-radio-group v-model="mode" @change="onModeChange">
        <el-radio-button value="">不限制</el-radio-button>
        <el-radio-button value="whitelist">白名单</el-radio-button>
        <el-radio-button value="blacklist">黑名单</el-radio-button>
      </el-radio-group>
      <span v-if="mode === 'whitelist'" class="domain-whitelist-warning">仅允许命中域名；直接 IP、无法识别域名和其他连接都会中断。</span>
      <span v-else-if="mode === 'blacklist'" class="domain-whitelist-warning">命中域名及其子域会被阻断；其他域名和直接 IP 正常放行。</span>
    </div>
    <template v-if="mode">
      <div class="domain-whitelist-toolbar">
        <span>预置分组（可多选）</span>
        <el-button link type="primary" size="small" :disabled="loading || !!loadError" @click="selectAllPresets">全部常用</el-button>
        <el-button link size="small" @click="clearPresets">清空分组</el-button>
      </div>
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon class="domain-preset-error" />
      <el-checkbox-group v-model="presetKeys" class="domain-preset-grid" :disabled="loading || !!loadError" @change="emitValue">
        <el-checkbox v-for="preset in presets" :key="preset.key" :value="preset.key" border>
          <span class="domain-preset-name">{{ preset.name }}</span>
          <span class="domain-preset-count">{{ preset.domains?.length || 0 }} 个</span>
        </el-checkbox>
      </el-checkbox-group>
      <el-input
        v-model="customText"
        type="textarea"
        :rows="4"
        :placeholder="`追加自定义域名，每行一个；example.com 会同时${mode === 'whitelist' ? '允许' : '阻断'}其所有子域`"
        @input="emitValue"
      />
      <div :class="['domain-whitelist-summary', { invalid: !valid }]">
        {{ valid ? `最终${mode === 'whitelist' ? '允许' : '阻断'} ${resolvedDomains.length} 个域名` : '请至少选择一个分组或填写一个自定义域名' }}
      </div>
      <el-collapse v-if="resolvedDomains.length" class="domain-whitelist-preview">
        <el-collapse-item title="查看最终域名快照">
          <el-tag v-for="domain in resolvedDomains" :key="domain" size="small" effect="plain">{{ domain }}</el-tag>
        </el-collapse-item>
      </el-collapse>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { getProxyDomainPolicyPresets } from '@/api/resource/vps'

const props = defineProps({ modelValue: { type: Object, default: null } })
const emit = defineEmits(['update:modelValue'])
const presets = ref([])
const loading = ref(false)
const loadError = ref('')
const mode = ref('')
const presetKeys = ref([])
const customText = ref('')
let syncing = false

function normalizePreviewDomain(input) {
  let value = input.trim().toLowerCase().replace(/\.+$/, '')
  if (value.startsWith('*.')) value = value.slice(2)
  if (/[\/:@*?#]/.test(value)) return value
  try { return new URL(`http://${value}`).hostname || value } catch { return value }
}
const customDomains = computed(() => Array.from(new Set(customText.value.split(/[\n,;]+/)
  .map(value => value.trim()).filter(Boolean))))
const previewCustomDomains = computed(() => Array.from(new Set(customDomains.value.map(normalizePreviewDomain))))
const resolvedDomains = computed(() => {
  const values = new Set()
  presetKeys.value.forEach(key => {
    const preset = presets.value.find(item => item.key === key)
    ;(preset?.domains || []).forEach(domain => values.add(domain))
  })
  previewCustomDomains.value.forEach(domain => values.add(domain))
  return Array.from(values)
})
const valid = computed(() => !mode.value || resolvedDomains.value.length > 0)

function syncFromModel(value) {
  syncing = true
  mode.value = value ? (value.mode || 'whitelist') : ''
  presetKeys.value = Array.isArray(value?.presetKeys) ? [...value.presetKeys] : []
  customText.value = Array.isArray(value?.customDomains) ? value.customDomains.join('\n') : ''
  syncing = false
}

function emitValue() {
  if (syncing) return
  emit('update:modelValue', mode.value ? { mode: mode.value, presetKeys: [...presetKeys.value], customDomains: customDomains.value } : null)
}

function onModeChange() {
  presetKeys.value = []
  customText.value = ''
  emitValue()
}

function selectAllPresets() {
  presetKeys.value = presets.value.map(item => item.key)
  emitValue()
}
function clearPresets() {
  presetKeys.value = []
  emitValue()
}
function validate() { return valid.value }
function getValue() { return mode.value ? { mode: mode.value, presetKeys: [...presetKeys.value], customDomains: customDomains.value } : null }

watch(() => props.modelValue, value => syncFromModel(value), { immediate: true, deep: true })
onMounted(() => {
  loading.value = true
  getProxyDomainPolicyPresets().then(res => {
    presets.value = res.data || []
    loadError.value = ''
  }).catch(() => {
    presets.value = []
    loadError.value = '预置分组加载失败，仍可填写自定义域名'
  }).finally(() => { loading.value = false })
})
defineExpose({ validate, getValue })
</script>

<style scoped>
.domain-whitelist-editor { width: 100%; }
.domain-whitelist-switch { display: flex; flex-direction: column; gap: 6px; }
.domain-whitelist-warning { color: var(--el-color-warning-dark-2); font-size: 12px; line-height: 1.45; }
.domain-whitelist-toolbar { display: flex; align-items: center; gap: 8px; margin: 14px 0 8px; font-size: 13px; }
.domain-preset-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-bottom: 12px; }
.domain-preset-error { margin-bottom: 10px; }
.domain-preset-grid :deep(.el-checkbox) { margin: 0; width: 100%; }
.domain-preset-name { margin-right: 8px; }
.domain-preset-count { color: var(--el-text-color-secondary); font-size: 12px; }
.domain-whitelist-summary { margin-top: 8px; color: var(--el-color-success); font-size: 12px; }
.domain-whitelist-summary.invalid { color: var(--el-color-danger); }
.domain-whitelist-preview { margin-top: 6px; }
.domain-whitelist-preview :deep(.el-tag) { margin: 0 6px 6px 0; }
@media (max-width: 600px) { .domain-preset-grid { grid-template-columns: 1fr; } }
</style>
