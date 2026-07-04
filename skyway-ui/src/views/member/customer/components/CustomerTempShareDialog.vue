<template>
  <el-dialog title="临时访问" v-model="visible" width="760px" append-to-body destroy-on-close @open="loadList">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" class="temp-share-form">
      <el-form-item label="有效期" prop="expireTime">
        <el-date-picker
          v-model="form.expireTime"
          type="datetime"
          placeholder="选择过期时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="访问密码" prop="accessPassword">
        <el-input v-model="form.accessPassword" type="password" maxlength="32" show-password placeholder="请输入访问密码">
          <template #append>
            <el-button @click="generatePassword">随机密码</el-button>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="creating" @click="submitCreate">生成链接</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="shareList" border size="small">
      <el-table-column label="访问链接" min-width="280" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ buildShareUrl(row.token) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="有效期" width="170" align="center">
        <template #default="{ row }">
          <span :class="{ 'is-expired': isExpired(row.expireTime) }">{{ row.expireTime || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' && !isExpired(row.expireTime) ? 'success' : 'info'">
            {{ row.status === '0' && !isExpired(row.expireTime) ? '有效' : (row.status === '1' ? '已作废' : '已过期') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170" align="center" prop="createTime" />
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="copyShareInfo(row)">复制</el-button>
          <el-button v-if="row.status === '0'" link type="danger" @click="handleRevoke(row)">作废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="visible = false">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { addCustomerTempShare, listCustomerTempShare, revokeCustomerTempShare } from '@/api/member/customerTempShare'

const { proxy } = getCurrentInstance()

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  customerId: { type: Number, required: true }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const formRef = ref(null)
const loading = ref(false)
const creating = ref(false)
const shareList = ref([])
const sharePasswordMap = ref(loadSharePasswordMap())
const form = reactive({
  expireTime: '',
  accessPassword: ''
})
const rules = {
  expireTime: [{ required: true, message: '请选择有效期', trigger: 'change' }],
  accessPassword: [
    { required: true, message: '请输入访问密码', trigger: 'blur' },
    { min: 4, max: 32, message: '访问密码长度为 4-32 个字符', trigger: 'blur' }
  ]
}
const passwordGroups = [
  'ABCDEFGHJKLMNPQRSTUVWXYZ',
  'abcdefghijkmnopqrstuvwxyz',
  '23456789',
  '!@#$%^&*_-+=?'
]
const passwordPool = passwordGroups.join('')
const randomPasswordLength = 16

function loadList() {
  if (!props.customerId) return
  loading.value = true
  listCustomerTempShare(props.customerId).then(res => {
    shareList.value = res.data || []
  }).catch(() => {
    shareList.value = []
  }).finally(() => {
    loading.value = false
  })
}

function submitCreate() {
  formRef.value.validate(valid => {
    if (!valid) return
    const accessPassword = form.accessPassword
    creating.value = true
    addCustomerTempShare(props.customerId, {
      expireTime: form.expireTime,
      accessPassword
    }).then(res => {
      form.expireTime = ''
      form.accessPassword = ''
      formRef.value?.clearValidate()
      proxy.$modal.msgSuccess('临时访问链接已生成')
      loadList()
      if (res.data?.token) {
        rememberSharePassword(res.data.token, accessPassword)
        copyText(buildShareCopyText(res.data.token, accessPassword))
      }
    }).catch(() => {}).finally(() => {
      creating.value = false
    })
  })
}

function generatePassword() {
  const requiredChars = passwordGroups.map(group => pickRandomChar(group))
  const remainingChars = Array.from({ length: randomPasswordLength - requiredChars.length }, () => pickRandomChar(passwordPool))
  form.accessPassword = shuffleChars([...requiredChars, ...remainingChars]).join('')
  formRef.value?.validateField('accessPassword')
}

function pickRandomChar(chars) {
  const values = new Uint32Array(1)
  window.crypto.getRandomValues(values)
  return chars[values[0] % chars.length]
}

function shuffleChars(chars) {
  const result = [...chars]
  for (let i = result.length - 1; i > 0; i--) {
    const values = new Uint32Array(1)
    window.crypto.getRandomValues(values)
    const j = values[0] % (i + 1)
    ;[result[i], result[j]] = [result[j], result[i]]
  }
  return result
}

function handleRevoke(row) {
  proxy.$modal.confirm('确认作废该临时访问链接吗？').then(() => {
    return revokeCustomerTempShare(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('已作废')
    loadList()
  }).catch(() => {})
}

function buildShareUrl(token) {
  return `${window.location.origin}/share/customer/${token || ''}`
}

function copyShareInfo(row) {
  const password = sharePasswordMap.value[row.token]
  if (!password) {
    proxy.$modal.msgWarning('该链接的明文密码无法取回，请使用创建时保存的密码或重新生成链接')
    copyText(buildShareCopyText(row.token, ''))
    return
  }
  copyText(buildShareCopyText(row.token, password))
}

function buildShareCopyText(token, password) {
  return [
    '订阅信息',
    `地址：${buildShareUrl(token)}`,
    `密码：${password || ''}`
  ].join('\n')
}

function rememberSharePassword(token, password) {
  sharePasswordMap.value = {
    ...sharePasswordMap.value,
    [token]: password
  }
  localStorage.setItem('skyway_customer_temp_share_passwords', JSON.stringify(sharePasswordMap.value))
}

function loadSharePasswordMap() {
  try {
    return JSON.parse(localStorage.getItem('skyway_customer_temp_share_passwords') || '{}')
  } catch {
    return {}
  }
}

function copyText(text) {
  navigator.clipboard.writeText(text).then(() => {
    proxy.$modal.msgSuccess('已复制到剪贴板')
  }).catch(() => {
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    proxy.$modal.msgSuccess('已复制到剪贴板')
  })
}

function isExpired(expireTime) {
  return expireTime ? new Date(expireTime) < new Date() : false
}
</script>

<style scoped>
.temp-share-form {
  margin-bottom: 12px;
}
.is-expired {
  color: var(--el-color-danger);
}
</style>
