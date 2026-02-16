<template>
  <div class="profile-page">
    <div class="profile-layout">
      <aside class="profile-sidebar">
        <div class="menu-item parent active"><span>用户中心</span></div>
        <div class="menu-item sub active">个人中心</div>
        <div class="menu-item">消息中心</div>
        <div class="menu-item">订购产品</div>
        <div class="menu-item">活动中心</div>
        <div class="menu-item">信息市场</div>
        <div class="menu-item has-arrow">管理实例</div>
        <div class="menu-item has-arrow">退款申请</div>
        <div class="menu-item has-arrow">账户管理</div>
        <div class="menu-item has-arrow">财务管理</div>
        <div class="menu-item">工单求助</div>
        <div class="menu-item has-arrow">拼团活动</div>
      </aside>
      <div class="profile-main">
        <div class="user-info-card">
          <div class="avatar-wrap">
            <div class="big-avatar" :style="userInfo.avatar ? { backgroundImage: `url(${userInfo.avatar})` } : {}">
              <span v-if="!userInfo.avatar" class="avatar-placeholder">{{ avatarText }}</span>
            </div>
            <span class="account-tag">主账号</span>
          </div>
          <div class="user-name">{{ userInfo.username || '—' }}</div>
          <div class="user-id">ID {{ userInfo.id || '—' }}</div>
          <div class="security-row">
            <span class="label">安全手机</span>
            <span class="value">{{ userInfo.phone || '未设置' }}</span>
            <span :class="['bind-tag', userInfo.phone ? 'ok' : 'warn']">{{ userInfo.phone ? '已绑定' : '未绑定' }}</span>
          </div>
          <div class="security-row">
            <span class="label">安全邮箱</span>
            <span class="value">{{ userInfo.email || '未设置' }}</span>
            <span :class="['bind-tag', userInfo.email ? 'ok' : 'warn']">{{ userInfo.email ? '已绑定' : '未绑定' }}</span>
          </div>
          <div class="register-time">注册于 {{ userInfo.registerTime || '—' }}</div>
          <div class="real-name-card">
            <div class="real-name-row">
              <span class="real-name-label">真实姓名</span>
              <span class="real-name-value">{{ realNameMasked }}</span>
              <span class="cert-tag">个人认证</span>
            </div>
            <div class="id-number-row">
              <span class="label">证件号码</span>
              <span class="value">{{ idNumberMasked }}</span>
            </div>
          </div>
        </div>
        <div class="profile-content">
          <div class="tabs">
            <button type="button" :class="['tab', { active: activeTab === 'contact' }]" @click="activeTab = 'contact'">联系信息</button>
            <button type="button" :class="['tab', { active: activeTab === 'detail' }]" @click="activeTab = 'detail'">详细资料</button>
            <button type="button" :class="['tab', { active: activeTab === 'other' }]" @click="activeTab = 'other'">其他信息</button>
          </div>
          <div v-if="activeTab === 'contact'" class="tab-inner">
            <form class="form" @submit.prevent="handleSubmit">
              <div class="field">
                <label class="label">邮箱地址</label>
                <input v-model="form.email" type="email" class="input" placeholder="请输入邮箱" disabled />
                <p class="warm-tip">绑定邮箱或更改邮箱请前往（安全中心）进行操作</p>
              </div>
              <div class="field">
                <label class="label">手机号</label>
                <input v-model="form.phone" type="text" class="input" placeholder="请输入手机号" />
                <p class="warm-tip">绑定手机或更绑手机号请前往（安全中心）进行操作</p>
              </div>
              <div class="field">
                <label class="label">QQ号码</label>
                <input v-model="form.qq" type="text" class="input" placeholder="请输入QQ号" />
              </div>
              <button type="submit" class="btn primary">提交</button>
            </form>
          </div>
          <div v-else-if="activeTab === 'detail'" class="tab-inner">
            <p class="placeholder-tip">暂无详细资料，敬请期待。</p>
          </div>
          <div v-else class="tab-inner">
            <p class="placeholder-tip">暂无其他信息，敬请期待。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import toast from '@/utils/toast'
import { getUserInfo, updateProfile } from '@/api/user'

const activeTab = ref('contact')
const userInfo = ref({})
const form = reactive({
  email: '',
  phone: '',
  qq: ''
})

const realNameMasked = ref('王*')
const idNumberMasked = ref('32*************14')

const avatarText = computed(() => {
  const u = userInfo.value.username
  if (!u) return '?'
  return u.charAt(0).toUpperCase()
})

function loadInfo() {
  getUserInfo()
    .then((res) => {
      const data = (res && res.data) ? res.data : {}
      userInfo.value = data
      form.email = data.email ?? ''
      form.phone = data.phone ?? ''
      form.qq = data.qq ?? ''
    })
    .catch(() => {})
}

function handleSubmit() {
  updateProfile({
    username: userInfo.value.username,
    avatar: userInfo.value.avatar,
    phone: form.phone,
    wechat: userInfo.value.wechat,
    qq: form.qq
  })
    .then(() => toast.success('提交成功'))
    .catch(() => {})
}

onMounted(loadInfo)
</script>

<style scoped>
.profile-page { padding: 20px; min-height: calc(100vh - 56px); background: #f8fafc; }
.profile-layout { display: flex; gap: 20px; max-width: 1100px; margin: 0 auto; }
.profile-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  padding: 10px 0;
}
.menu-item {
  padding: 10px 16px;
  font-size: 14px;
  color: #475569;
  cursor: pointer;
}
.menu-item:hover { background: #f8fafc; }
.menu-item.parent { font-weight: 600; color: #0f172a; }
.menu-item.sub { padding-left: 32px; }
.menu-item.active { background: #eff6ff; color: #2563eb; }
.menu-item.has-arrow::after { content: ''; float: right; border: 4px solid transparent; border-left-color: #94a3b8; margin-top: 5px; }
.profile-main { flex: 1; display: flex; gap: 20px; min-width: 0; }
.user-info-card {
  width: 260px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  padding: 24px;
  height: fit-content;
}
.avatar-wrap { position: relative; display: inline-block; margin-bottom: 16px; }
.big-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #2563eb;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-placeholder { font-size: 28px; font-weight: 600; color: #fff; }
.account-tag {
  position: absolute;
  bottom: 0;
  right: 0;
  padding: 2px 8px;
  font-size: 11px;
  background: #2563eb;
  color: #fff;
  border-radius: 4px;
}
.user-name { font-size: 17px; font-weight: 600; color: #0f172a; margin-bottom: 4px; }
.user-id { font-size: 13px; color: #64748b; margin-bottom: 16px; }
.security-row { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; font-size: 13px; }
.security-row .label { color: #64748b; width: 70px; }
.security-row .value { flex: 1; color: #334155; }
.bind-tag { padding: 2px 8px; font-size: 11px; border-radius: 4px; }
.bind-tag.ok { background: #dcfce7; color: #15803d; }
.bind-tag.warn { background: #fef3c7; color: #b45309; }
.register-time { font-size: 12px; color: #94a3b8; margin-bottom: 16px; }
.real-name-card {
  padding: 12px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 13px;
}
.real-name-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.real-name-label { color: #64748b; }
.real-name-value { flex: 1; color: #334155; }
.cert-tag { padding: 2px 8px; font-size: 11px; background: #dcfce7; color: #15803d; border-radius: 4px; }
.id-number-row { display: flex; gap: 8px; }
.id-number-row .label { color: #64748b; width: 60px; }
.id-number-row .value { color: #334155; }
.profile-content {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  padding: 20px;
}
.tabs { display: flex; gap: 4px; margin-bottom: 20px; }
.tab {
  padding: 8px 16px;
  font-size: 14px;
  color: #64748b;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.tab:hover { color: #0f172a; }
.tab.active { color: #2563eb; font-weight: 500; background: #eff6ff; }
.tab-inner { padding: 0; }
.form { max-width: 420px; display: flex; flex-direction: column; gap: 16px; }
.field .label { display: block; font-size: 13px; color: #475569; margin-bottom: 6px; }
.field .input {
  width: 100%;
  padding: 8px 12px;
  font-size: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  outline: none;
}
.field .input:focus { border-color: #2563eb; }
.field .input:disabled { background: #f8fafc; color: #64748b; }
.warm-tip { margin: 6px 0 0; font-size: 12px; color: #b45309; line-height: 1.5; }
.btn.primary { padding: 8px 16px; font-size: 14px; font-weight: 500; background: #2563eb; color: #fff; border: none; border-radius: 6px; cursor: pointer; align-self: flex-start; }
.btn.primary:hover { background: #1d4ed8; }
.placeholder-tip { color: #94a3b8; font-size: 14px; margin: 0; }
</style>
