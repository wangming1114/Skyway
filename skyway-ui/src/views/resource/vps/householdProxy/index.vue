<template>
  <div class="household-proxy-page">
    <section class="credential-bar">
      <div class="credential-title">
        <div class="credential-icon">
          <el-icon><Connection /></el-icon>
        </div>
        <div>
          <h2>家宽代理</h2>
          <p>520proxy 家宽 IP 代理平台</p>
        </div>
      </div>

      <div class="credential-list">
        <div class="credential-item">
          <span class="credential-label">账号</span>
          <strong>{{ account.username }}</strong>
          <el-button :icon="CopyDocument" text type="primary" @click="copyText(account.username)">复制</el-button>
        </div>
        <div class="credential-item">
          <span class="credential-label">密码</span>
          <strong>{{ account.password }}</strong>
          <el-button :icon="CopyDocument" text type="primary" @click="copyText(account.password)">复制</el-button>
        </div>
      </div>

      <div class="credential-actions">
        <el-button :icon="TopRight" type="primary" @click="openExternal">打开家宽代理</el-button>
      </div>
    </section>

    <section class="proxy-launch-panel">
      <div class="launch-content">
        <el-icon><TopRight /></el-icon>
        <h3>打开 520proxy 登录页</h3>
        <p>该站点登录态需要在独立窗口中保存，直接打开后登录不会反复跳回登录页。</p>
        <div class="launch-url">
          <span>{{ proxyUrl }}</span>
          <el-button :icon="CopyDocument" text type="primary" @click="copyText(proxyUrl)">复制地址</el-button>
        </div>
        <el-button :icon="TopRight" type="primary" size="large" @click="openExternal">打开家宽代理</el-button>
      </div>
    </section>
  </div>
</template>

<script setup name="HouseholdProxy">
import { Connection, CopyDocument, TopRight } from '@element-plus/icons-vue'

const proxyUrl = 'https://520proxy.com/login'
const account = {
  username: 'w22222',
  password: 'wangming1114'
}

function copyText(text) {
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).then(() => {
      ElMessage.success('已复制')
    }).catch(() => {
      fallbackCopyText(text)
    })
    return
  }
  fallbackCopyText(text)
}

function fallbackCopyText(text) {
  const input = document.createElement('textarea')
  input.value = text
  input.setAttribute('readonly', 'readonly')
  input.style.position = 'fixed'
  input.style.left = '-9999px'
  document.body.appendChild(input)
  input.select()
  const copied = document.execCommand('copy')
  document.body.removeChild(input)
  if (copied) {
    ElMessage.success('已复制')
  } else {
    ElMessage.error('复制失败，请手动复制')
  }
}

function openExternal() {
  window.open(proxyUrl, '_blank', 'noopener,noreferrer')
}
</script>

<style lang="scss" scoped>
.household-proxy-page {
  min-height: calc(100vh - 84px);
  padding: 12px;
  background: #f5f7fb;
}

.credential-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 72px;
  padding: 14px 16px;
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.credential-title {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 220px;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #1f2937;
  }

  p {
    margin: 4px 0 0;
    font-size: 13px;
    color: #6b7280;
  }
}

.credential-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  color: #0f766e;
  background: #dff7f3;
  border-radius: 6px;
  font-size: 22px;
}

.credential-list {
  display: flex;
  flex: 1;
  gap: 12px;
  min-width: 0;
}

.credential-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 220px;
  padding: 8px 10px;
  background: #f9fafb;
  border: 1px solid #edf0f5;
  border-radius: 6px;

  strong {
    flex: 1;
    min-width: 0;
    font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
    font-size: 14px;
    color: #111827;
    overflow-wrap: anywhere;
  }
}

.credential-label {
  flex: 0 0 auto;
  font-size: 13px;
  color: #6b7280;
}

.credential-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.proxy-launch-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 180px);
  min-height: 520px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.launch-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: min(520px, 100%);
  padding: 32px 24px;
  text-align: center;

  > .el-icon {
    width: 56px;
    height: 56px;
    margin-bottom: 18px;
    color: #0f766e;
    background: #dff7f3;
    border-radius: 6px;
    font-size: 28px;
  }

  h3 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: #1f2937;
  }

  p {
    margin: 10px 0 18px;
    line-height: 1.7;
    color: #6b7280;
  }
}

.launch-url {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  margin-bottom: 22px;
  background: #f9fafb;
  border: 1px solid #edf0f5;
  border-radius: 6px;

  span {
    flex: 1;
    min-width: 0;
    font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
    font-size: 14px;
    color: #111827;
    overflow-wrap: anywhere;
    text-align: left;
  }
}

@media (max-width: 1200px) {
  .credential-bar,
  .credential-list {
    flex-wrap: wrap;
  }

  .credential-actions {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .household-proxy-page {
    padding: 8px;
  }

  .credential-bar {
    align-items: stretch;
  }

  .credential-title,
  .credential-item,
  .credential-actions {
    width: 100%;
  }

  .credential-list {
    width: 100%;
  }

  .credential-item {
    min-width: 100%;
  }

  .credential-actions {
    .el-button {
      flex: 1;
    }
  }

  .proxy-launch-panel {
    height: calc(100vh - 286px);
    min-height: 420px;
  }

  .launch-content {
    padding: 24px 16px;
  }

  .launch-url {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
