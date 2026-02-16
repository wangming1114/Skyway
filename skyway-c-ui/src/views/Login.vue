<template>
  <div class="auth-page">
    <div class="auth-bg" aria-hidden="true">
      <div class="auth-bg-gradient"></div>
      <div class="auth-bg-orb auth-bg-orb--left" aria-hidden="true"></div>
      <div class="auth-bg-orb auth-bg-orb--right" aria-hidden="true"></div>
      <div class="auth-bg-grid" aria-hidden="true"></div>
      <div class="auth-bg-shapes">
        <span class="shape shape-1"></span>
        <span class="shape shape-2"></span>
        <span class="shape shape-3"></span>
      </div>
    </div>
    <header class="brand-logo" aria-label="品牌">
      <span class="brand-icon" aria-hidden="true">
        <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="20" cy="8" r="3.5" fill="currentColor"/>
          <circle cx="8" cy="24" r="3.5" fill="currentColor"/>
          <circle cx="32" cy="24" r="3.5" fill="currentColor"/>
          <circle cx="20" cy="34" r="3.5" fill="currentColor"/>
          <path d="M20 11.5v5M20 29v4M20 16.5l-9 4.5M20 16.5l9 4.5M8 24l11.5-5.5M32 24l-11.5-5.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </span>
      <span class="brand-name">NetCloud</span>
    </header>
    <div class="auth-main">
      <div class="auth-slogan">
        <h1 class="slogan-title">构建无界网络，赋能全球业务</h1>
        <p class="slogan-sub">企业级智能路由与安全接入平台。提供极速、稳定、端到端加密的跨域互联方案。</p>
        <div class="monitor-badges">
          <span class="monitor-badge"><span class="monitor-dot monitor-dot--green"></span> Global Nodes: 124 Online</span>
          <span class="monitor-badge"><span class="monitor-dot monitor-dot--yellow"></span> Avg Latency: &lt; 10ms</span>
          <span class="monitor-badge"><span class="monitor-dot monitor-dot--blue"></span> AES-256 Secured</span>
        </div>
      </div>
      <div class="auth-form-wrap">
      <div class="auth-glass-card">
        <div class="tabs tabs-single">
          <span class="tab active">{{ activeTab === 'login' ? '账号登录' : activeTab === 'forgot' ? '找回密码' : '邮箱注册' }}</span>
        </div>

        <div v-if="activeTab === 'forgot'" class="forgot-panel">
          <form class="form" @submit.prevent="handleResetPassword">
            <div class="field">
              <div class="input-with-icon">
                <span class="input-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
                </span>
                <input v-model="forgotForm.email" type="email" placeholder="请输入注册邮箱" class="input" autocomplete="email" />
              </div>
              <p v-if="forgotErrors.email" class="err">{{ forgotErrors.email }}</p>
            </div>
            <div class="field code-field">
              <div class="code-row">
                <input v-model="forgotForm.code" type="text" placeholder="验证码" class="input code-input" maxlength="6" />
                <button type="button" class="btn secondary" :disabled="resetCodeCountdown > 0" @click="handleSendResetCode">
                  {{ resetCodeCountdown > 0 ? `${resetCodeCountdown}s 后重试` : '获取验证码' }}
                </button>
              </div>
              <p v-if="forgotErrors.code" class="err">{{ forgotErrors.code }}</p>
            </div>
<div class="field pwd-field">
            <div class="input-with-icon input-wrap">
              <span class="input-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              </span>
              <input v-model="forgotForm.password" :type="forgotPwdVisible ? 'text' : 'password'" placeholder="新密码" class="input" autocomplete="new-password"
                  @focus="forgotPwdBubbleVisible = true" @blur="forgotPwdBubbleVisible = false" />
                <button type="button" class="pwd-toggle" @click="forgotPwdVisible = !forgotPwdVisible">{{ forgotPwdVisible ? '隐藏' : '显示' }}</button>
              </div>
              <Transition name="bubble">
                <div v-if="forgotPwdBubbleVisible" class="pwd-bubble" role="tooltip">
                  <template v-if="forgotPwdRulesStatus.every(r => r.met)">
                    <span class="pwd-bubble-ok">✓ 密码强度符合要求</span>
                  </template>
                  <template v-else>
                    <span class="pwd-bubble-title">还缺：</span>
                    <span class="pwd-bubble-missing">{{ forgotPwdMissingText }}</span>
                  </template>
                </div>
              </Transition>
              <p v-if="forgotErrors.password" class="err">{{ forgotErrors.password }}</p>
            </div>
            <div class="field">
              <div class="input-with-icon input-wrap">
                <span class="input-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                </span>
                <input v-model="forgotForm.confirmPassword" :type="forgotConfirmVisible ? 'text' : 'password'" placeholder="再次输入新密码" class="input" autocomplete="new-password" />
                <button type="button" class="pwd-toggle" @click="forgotConfirmVisible = !forgotConfirmVisible">{{ forgotConfirmVisible ? '隐藏' : '显示' }}</button>
              </div>
              <p v-if="forgotErrors.confirmPassword" class="err">{{ forgotErrors.confirmPassword }}</p>
            </div>
            <button type="submit" class="btn primary" :disabled="resetLoading">
              {{ resetLoading ? '提交中…' : '确认重置密码' }}
            </button>
            <p class="tab-link below">
              <a href="javascript:;" @click.prevent="activeTab = 'login'">返回登录</a>
            </p>
          </form>
        </div>

        <form v-else-if="activeTab === 'login'" class="form" @submit.prevent="handleLogin">
          <div class="field">
            <div class="input-with-icon">
              <span class="input-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </span>
              <input v-model="loginForm.account" type="text" placeholder="邮箱或用户名" class="input" autocomplete="username" />
            </div>
            <p v-if="loginErrors.account" class="err">{{ loginErrors.account }}</p>
          </div>
          <div class="field">
            <div class="input-with-icon input-wrap">
              <span class="input-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              </span>
              <input v-model="loginForm.password" :type="loginPwdVisible ? 'text' : 'password'" placeholder="密码" class="input" autocomplete="current-password" />
              <button type="button" class="pwd-toggle" :aria-label="loginPwdVisible ? '隐藏' : '显示'" @click="loginPwdVisible = !loginPwdVisible">{{ loginPwdVisible ? '隐藏' : '显示' }}</button>
            </div>
            <p v-if="loginErrors.password" class="err">{{ loginErrors.password }}</p>
          </div>
          <div class="field field-verify">
            <SliderVerify ref="loginSliderRef" @success="loginVerified = true" />
          </div>
          <div class="login-options">
            <label class="remember-row">
              <input v-model="rememberMe" type="checkbox" class="remember-checkbox" />
              <span class="remember-text">记住密码</span>
            </label>
            <a href="javascript:;" class="forgot-link" @click.prevent="activeTab = 'forgot'">忘记密码？</a>
          </div>
          <button type="submit" class="btn primary" :disabled="loginLoading">
            {{ loginLoading ? '登录中…' : '立即登录' }}
          </button>
          <p class="tab-link below">
            还没有账户？<a href="javascript:;" @click.prevent="activeTab = 'register'">现在注册</a>
          </p>
        </form>

        <form v-else class="form" @submit.prevent="handleRegister">
          <div class="field">
            <div class="input-with-icon">
              <span class="input-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </span>
              <input v-model="registerForm.username" type="text" placeholder="用户名（英文 5-16 位）" class="input" autocomplete="username" maxlength="16" />
            </div>
            <p v-if="registerErrors.username" class="err">{{ registerErrors.username }}</p>
          </div>
          <div class="field">
            <div class="input-with-icon">
              <span class="input-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
              </span>
              <input v-model="registerForm.email" type="email" placeholder="请输入您的邮箱" class="input" autocomplete="email" />
            </div>
            <p v-if="registerErrors.email" class="err">{{ registerErrors.email }}</p>
          </div>
          <div class="field code-field">
            <div class="code-row">
              <input v-model="registerForm.code" type="text" placeholder="验证码" class="input code-input" maxlength="6" />
              <button type="button" class="btn secondary" :disabled="codeCountdown > 0" @click="handleSendCode">
                {{ codeCountdown > 0 ? `${codeCountdown}s 后重试` : '获取验证码' }}
              </button>
            </div>
            <p v-if="registerErrors.code" class="err">{{ registerErrors.code }}</p>
          </div>
          <div class="field pwd-field">
            <div class="input-with-icon input-wrap">
              <span class="input-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              </span>
              <input v-model="registerForm.password" :type="regPwdVisible ? 'text' : 'password'" placeholder="密码" class="input" autocomplete="new-password"
                @focus="registerPwdBubbleVisible = true" @blur="registerPwdBubbleVisible = false" />
              <button type="button" class="pwd-toggle" @click="regPwdVisible = !regPwdVisible">{{ regPwdVisible ? '隐藏' : '显示' }}</button>
            </div>
            <Transition name="bubble">
              <div v-if="registerPwdBubbleVisible" class="pwd-bubble" role="tooltip">
                <template v-if="registerPwdRulesStatus.every(r => r.met)">
                  <span class="pwd-bubble-ok">✓ 密码强度符合要求</span>
                </template>
                <template v-else>
                  <span class="pwd-bubble-title">还缺：</span>
                  <span class="pwd-bubble-missing">{{ registerPwdMissingText }}</span>
                </template>
              </div>
            </Transition>
            <p v-if="registerErrors.password" class="err">{{ registerErrors.password }}</p>
          </div>
          <div class="field">
            <div class="input-with-icon input-wrap">
              <span class="input-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              </span>
              <input v-model="registerForm.confirmPassword" :type="regConfirmVisible ? 'text' : 'password'" placeholder="再次输入密码" class="input" autocomplete="new-password" />
              <button type="button" class="pwd-toggle" @click="regConfirmVisible = !regConfirmVisible">{{ regConfirmVisible ? '隐藏' : '显示' }}</button>
            </div>
            <p v-if="registerErrors.confirmPassword" class="err">{{ registerErrors.confirmPassword }}</p>
          </div>
          <div class="field field-verify">
            <SliderVerify ref="registerSliderRef" @success="registerVerified = true" />
          </div>
          <label class="agreement-row">
            <input v-model="agreementChecked" type="checkbox" class="remember-checkbox" />
            <span class="agreement-text">我已阅读并同意<a href="#">《服务条款》</a>与<a href="#">《隐私政策》</a></span>
          </label>
          <button type="submit" class="btn primary" :disabled="registerLoading">
            {{ registerLoading ? '注册中…' : '注册' }}
          </button>
          <p class="tab-link below">
            已有账号？<a href="javascript:;" @click.prevent="activeTab = 'login'">直接登录</a>
          </p>
        </form>
      </div>
    </div>
    </div>
    <footer class="page-footer">
      <a href="#">服务条款</a><span class="divider">|</span><a href="#">隐私政策</a>
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import toast from '@/utils/toast'
import { setToken } from '@/utils/auth'
import { sendEmailCode, sendResetCode, register, login, resetPassword } from '@/api/auth'
import SliderVerify from '@/components/SliderVerify.vue'

const REMEMBER_ACCOUNT_KEY = 'c_remember_account'

const router = useRouter()
const route = useRoute()
const activeTab = ref('login')
const loginSliderRef = ref(null)
const registerSliderRef = ref(null)
const loginVerified = ref(false)
const registerVerified = ref(false)
const loginLoading = ref(false)
const registerLoading = ref(false)
const resetLoading = ref(false)
const codeCountdown = ref(0)
const resetCodeCountdown = ref(0)
const loginPwdVisible = ref(false)
const regPwdVisible = ref(false)
const regConfirmVisible = ref(false)
const forgotPwdVisible = ref(false)
const forgotConfirmVisible = ref(false)
const rememberMe = ref(false)
const agreementChecked = ref(false)
let codeTimer = null
let resetCodeTimer = null

const loginForm = reactive({ account: '', password: '' })
const loginErrors = reactive({ account: '', password: '' })

const registerForm = reactive({
  username: '',
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})
const registerErrors = reactive({
  username: '',
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})
const forgotForm = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})
const forgotErrors = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})

/** 密码强度规则：8-20位，含大小写、数字、特殊字符 */
const PWD_RULES = [
  { label: '8-20 位', test: p => p.length >= 8 && p.length <= 20 },
  { label: '包含大写字母', test: p => /[A-Z]/.test(p) },
  { label: '包含小写字母', test: p => /[a-z]/.test(p) },
  { label: '包含数字', test: p => /[0-9]/.test(p) },
  { label: '包含特殊字符', test: p => /[^A-Za-z0-9]/.test(p) }
]
function getPwdRulesStatus(password) {
  return PWD_RULES.map(r => ({ label: r.label, met: r.test(password || '') }))
}
const registerPwdRulesStatus = computed(() => getPwdRulesStatus(registerForm.password))
const forgotPwdRulesStatus = computed(() => getPwdRulesStatus(forgotForm.password))
const registerPwdMissingText = computed(() =>
  registerPwdRulesStatus.value.filter(r => !r.met).map(r => r.label).join('、') || '无'
)
const forgotPwdMissingText = computed(() =>
  forgotPwdRulesStatus.value.filter(r => !r.met).map(r => r.label).join('、') || '无'
)
const registerPwdBubbleVisible = ref(false)
const forgotPwdBubbleVisible = ref(false)

function clearLoginErrors() {
  loginErrors.account = ''
  loginErrors.password = ''
}
function clearRegisterErrors() {
  registerErrors.username = ''
  registerErrors.email = ''
  registerErrors.code = ''
  registerErrors.password = ''
  registerErrors.confirmPassword = ''
}
function clearForgotErrors() {
  forgotErrors.email = ''
  forgotErrors.code = ''
  forgotErrors.password = ''
  forgotErrors.confirmPassword = ''
}

function validateLogin() {
  clearLoginErrors()
  let ok = true
  if (!loginForm.account.trim()) {
    loginErrors.account = '请输入邮箱或用户名'
    ok = false
  }
  if (!loginForm.password) {
    loginErrors.password = '请输入密码'
    ok = false
  } else if (loginForm.password.length < 5 || loginForm.password.length > 20) {
    loginErrors.password = '密码长度 5-20 位'
    ok = false
  }
  return ok
}

function validateRegister() {
  clearRegisterErrors()
  let ok = true
  if (!registerForm.username.trim()) {
    registerErrors.username = '请输入用户名'
    ok = false
  } else if (registerForm.username.length < 5 || registerForm.username.length > 16) {
    registerErrors.username = '用户名长度为 5-16 位'
    ok = false
  } else if (!/^[a-zA-Z]+$/.test(registerForm.username)) {
    registerErrors.username = '用户名只能为英文字母'
    ok = false
  }
  if (!registerForm.email.trim()) {
    registerErrors.email = '请输入邮箱'
    ok = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)) {
    registerErrors.email = '邮箱格式不正确'
    ok = false
  }
  if (!registerForm.code.trim()) {
    registerErrors.code = '请输入验证码'
    ok = false
  } else if (registerForm.code.length !== 6) {
    registerErrors.code = '验证码为6位'
    ok = false
  }
  if (!registerForm.password) {
    registerErrors.password = '请输入密码'
    ok = false
  } else if (!registerPwdRulesStatus.value.every(r => r.met)) {
    registerErrors.password = '请满足全部密码强度要求'
    ok = false
  }
  if (!registerForm.confirmPassword) {
    registerErrors.confirmPassword = '请再次输入密码'
    ok = false
  } else if (registerForm.password !== registerForm.confirmPassword) {
    registerErrors.confirmPassword = '两次输入的密码不一致'
    ok = false
  }
  if (!agreementChecked.value) {
    toast.warning('请先阅读并同意服务条款与隐私政策')
    ok = false
  }
  return ok
}

onMounted(() => {
  try {
    const saved = localStorage.getItem(REMEMBER_ACCOUNT_KEY)
    if (saved) {
      loginForm.account = saved
      rememberMe.value = true
    }
  } catch (_) {}
})

watch(activeTab, () => {
  loginVerified.value = false
  registerVerified.value = false
  loginSliderRef.value?.reset()
  registerSliderRef.value?.reset()
  clearLoginErrors()
  clearRegisterErrors()
  clearForgotErrors()
})

function goHome() {
  router.replace(route.query.redirect || '/')
}

async function handleLogin() {
  if (!loginVerified.value) {
    toast.warning('请先完成滑动验证')
    return
  }
  if (!validateLogin()) return
  loginLoading.value = true
  try {
    const res = await login({ account: loginForm.account, password: loginForm.password })
    const token = res?.token
    if (token) {
      setToken(token)
      try {
        if (rememberMe.value && loginForm.account) {
          localStorage.setItem(REMEMBER_ACCOUNT_KEY, loginForm.account)
        } else {
          localStorage.removeItem(REMEMBER_ACCOUNT_KEY)
        }
      } catch (_) {}
      toast.success('登录成功')
      goHome()
    } else {
      toast.error(res?.msg || '登录失败')
      loginSliderRef.value?.reset()
      loginVerified.value = false
    }
  } catch (_) {}
  finally {
    loginLoading.value = false
  }
}

async function handleSendCode() {
  if (!registerForm.email.trim()) {
    toast.warning('请先输入邮箱')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)) {
    toast.warning('邮箱格式不正确')
    return
  }
  try {
    await sendEmailCode(registerForm.email)
    toast.success('验证码已发送，请查收邮件')
    codeCountdown.value = 60
    if (codeTimer) clearInterval(codeTimer)
    codeTimer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) clearInterval(codeTimer)
    }, 1000)
  } catch (_) {}
}

async function handleSendResetCode() {
  clearForgotErrors()
  if (!forgotForm.email.trim()) {
    forgotErrors.email = '请输入邮箱'
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(forgotForm.email)) {
    forgotErrors.email = '邮箱格式不正确'
    return
  }
  try {
    await sendResetCode(forgotForm.email)
    toast.success('验证码已发送，请查收邮件')
    resetCodeCountdown.value = 60
    if (resetCodeTimer) clearInterval(resetCodeTimer)
    resetCodeTimer = setInterval(() => {
      resetCodeCountdown.value--
      if (resetCodeCountdown.value <= 0) clearInterval(resetCodeTimer)
    }, 1000)
  } catch (e) {
    toast.error(e?.response?.data?.msg || e?.message || '发送失败')
  }
}

function validateResetPassword() {
  clearForgotErrors()
  let ok = true
  if (!forgotForm.email.trim()) {
    forgotErrors.email = '请输入邮箱'
    ok = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(forgotForm.email)) {
    forgotErrors.email = '邮箱格式不正确'
    ok = false
  }
  if (!forgotForm.code.trim()) {
    forgotErrors.code = '请输入验证码'
    ok = false
  } else if (forgotForm.code.length !== 6) {
    forgotErrors.code = '验证码为6位'
    ok = false
  }
  if (!forgotForm.password) {
    forgotErrors.password = '请输入新密码'
    ok = false
  } else if (!forgotPwdRulesStatus.value.every(r => r.met)) {
    forgotErrors.password = '请满足全部密码强度要求'
    ok = false
  }
  if (!forgotForm.confirmPassword) {
    forgotErrors.confirmPassword = '请再次输入新密码'
    ok = false
  } else if (forgotForm.password !== forgotForm.confirmPassword) {
    forgotErrors.confirmPassword = '两次输入的密码不一致'
    ok = false
  }
  return ok
}

async function handleResetPassword() {
  if (!validateResetPassword()) return
  resetLoading.value = true
  try {
    await resetPassword({
      email: forgotForm.email,
      code: forgotForm.code,
      password: forgotForm.password
    })
    toast.success('密码已重置，请使用新密码登录')
    activeTab.value = 'login'
    forgotForm.email = ''
    forgotForm.code = ''
    forgotForm.password = ''
    forgotForm.confirmPassword = ''
  } catch (e) {
    toast.error(e?.response?.data?.msg || e?.message || '重置失败')
  } finally {
    resetLoading.value = false
  }
}

async function handleRegister() {
  if (!registerVerified.value) {
    toast.warning('请先完成滑动验证')
    return
  }
  if (!validateRegister()) return
  registerLoading.value = true
  try {
    await register({
      username: registerForm.username.trim(),
      email: registerForm.email,
      code: registerForm.code,
      password: registerForm.password
    })
    toast.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.account = registerForm.email
    registerForm.username = ''
    registerForm.email = ''
    registerForm.code = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    registerVerified.value = false
    agreementChecked.value = false
    registerSliderRef.value?.reset()
  } catch (_) {
    registerSliderRef.value?.reset()
    registerVerified.value = false
  }
  finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px 32px;
  box-sizing: border-box;
}
.auth-main {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 1152px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 100px;
  padding: 0 24px;
  box-sizing: border-box;
}
.auth-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}
.auth-bg-gradient {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse 120% 80% at 50% 20%, #0f172a 0%, #020617 50%, #030712 100%);
}
.auth-bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  animation: breathe 9s ease-in-out infinite;
}
.auth-bg-orb--left {
  width:  min(80vw, 600px);
  height: min(80vw, 600px);
  top: -15%;
  left: -10%;
  background: radial-gradient(circle, rgba(30, 58, 138, 0.5) 0%, rgba(30, 58, 138, 0.15) 40%, transparent 70%);
}
.auth-bg-orb--right {
  width:  min(70vw, 520px);
  height: min(70vw, 520px);
  bottom: -20%;
  right: -10%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.35) 0%, rgba(139, 92, 246, 0.2) 45%, transparent 70%);
  animation-delay: -4.5s;
}
@keyframes breathe {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.12); opacity: 0.75; }
}
.auth-bg-grid {
  position: absolute;
  inset: -50% 0;
  width: 200%;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.06) 1px, transparent 1px);
  background-size: 48px 48px;
  animation: grid-drift 45s linear infinite;
  pointer-events: none;
}
@keyframes grid-drift {
  0% { transform: translate(0, 0); }
  100% { transform: translate(48px, 24px); }
}
.auth-bg-shapes {
  position: absolute;
  inset: 0;
  opacity: 0.12;
}
.shape {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.25) 0%, transparent 70%);
}
.shape-1 { width: 500px; height: 500px; top: -15%; right: -5%; }
.shape-2 { width: 320px; height: 320px; bottom: -5%; left: -5%; opacity: 0.15; }
.shape-3 { width: 240px; height: 240px; top: 50%; left: 15%; opacity: 0.08; }
.brand-logo {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 24px 32px;
}
.brand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: #60a5fa;
}
.brand-icon svg { width: 100%; height: 100%; }
.brand-name {
  font-family: system-ui, -apple-system, sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: #93c5fd;
  letter-spacing: 0.02em;
}
.auth-slogan {
  flex: 1;
  min-width: 280px;
  max-width: 520px;
}
.slogan-title {
  margin: 0 0 20px;
  font-size: clamp(1.75rem, 4vw, 3rem);
  font-weight: 700;
  line-height: 1.3;
  color: #fff;
  letter-spacing: 0.02em;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2), 0 0 40px rgba(0, 0, 0, 0.15);
}
.slogan-sub {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: #9ca3af;
}
.monitor-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-top: 32px;
}
.monitor-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-family: ui-monospace, 'SF Mono', 'Cascadia Code', monospace;
  font-size: 11px;
  font-weight: 500;
  color: #4ade80;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(74, 222, 128, 0.35);
  border-radius: 2px;
  box-shadow: 0 0 12px rgba(74, 222, 128, 0.08);
}
.monitor-badge:nth-child(2) { color: #38bdf8; border-color: rgba(56, 189, 248, 0.4); box-shadow: 0 0 12px rgba(56, 189, 248, 0.08); }
.monitor-badge:nth-child(3) { color: #a78bfa; border-color: rgba(167, 139, 250, 0.35); box-shadow: 0 0 12px rgba(167, 139, 250, 0.08); }
.monitor-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.monitor-dot--green { background: #4ade80; box-shadow: 0 0 6px #4ade80; }
.monitor-dot--yellow { background: #facc15; box-shadow: 0 0 6px #facc15; }
.monitor-dot--blue { background: #38bdf8; box-shadow: 0 0 6px #38bdf8; }
.auth-form-wrap {
  flex-shrink: 0;
  width: 100%;
  max-width: 420px;
}
.auth-glass-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  box-shadow: 0 32px 64px -12px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.06) inset;
  padding: 36px 32px;
  display: flex;
  flex-direction: column;
}
.forgot-panel {
  margin-top: 0;
}
.pwd-bubble {
  position: absolute;
  left: 0;
  top: 100%;
  margin-top: 8px;
  padding: 10px 14px;
  font-size: 13px;
  line-height: 1.45;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(15, 23, 42, 0.95);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  box-shadow: 0 10px 40px -10px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(0, 0, 0, 0.1);
  z-index: 10;
  max-width: 280px;
}
.pwd-bubble::before {
  content: '';
  position: absolute;
  left: 20px;
  top: -6px;
  width: 12px;
  height: 12px;
  background: rgba(15, 23, 42, 0.95);
  border-left: 1px solid rgba(255, 255, 255, 0.12);
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  transform: rotate(45deg);
}
.pwd-bubble-ok {
  color: rgba(74, 222, 128, 0.95);
  font-weight: 500;
}
.pwd-bubble-title {
  color: rgba(255, 255, 255, 0.6);
  margin-right: 4px;
}
.pwd-bubble-missing {
  color: rgba(251, 191, 36, 0.95);
}
.pwd-field {
  position: relative;
}
.bubble-enter-active,
.bubble-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.bubble-enter-from,
.bubble-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
.tabs {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
}
.tabs-single {
  justify-content: center;
}
.tabs-single .tab {
  margin-right: 0;
  cursor: default;
}
.tab {
  padding: 12px 6px 14px;
  margin-right: 32px;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  border-radius: 0;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
}
.tab:hover { color: rgba(255, 255, 255, 0.9); }
.tabs-single .tab:hover { color: #fff; }
.tab.active {
  color: #fff;
  font-weight: 600;
  border-bottom-color: #2563eb;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.field { position: relative; }
.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
  height: 44px;
}
.input-with-icon .input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  color: #9ca3af;
  pointer-events: none;
  flex-shrink: 0;
}
.input-with-icon .input-icon svg {
  width: 100%;
  height: 100%;
  display: block;
}
.input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  font-size: 14px;
  line-height: 42px;
  color: #d1d5db;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  box-sizing: border-box;
}
.input-with-icon .input {
  padding-left: 42px;
  display: block;
}
.input::placeholder { color: #6b7280; }
.input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.25);
}
.code-field { display: flex; flex-direction: column; gap: 0; }
.code-row {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 44px;
}
.code-input {
  flex: 1;
  height: 44px;
  min-width: 0;
}
.code-row .btn.secondary {
  height: 44px;
  flex-shrink: 0;
}
.input-wrap { position: relative; height: 44px; }
.pwd-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
}
.pwd-toggle:hover { color: rgba(255, 255, 255, 0.9); }
.input-wrap .input { padding-right: 52px; }
.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.remember-row {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}
.remember-checkbox {
  width: 14px;
  height: 14px;
  border-radius: 2px;
  accent-color: #2563eb;
}
.remember-text { user-select: none; }
.forgot-link {
  font-size: 13px;
  color: #60a5fa;
  text-decoration: none;
}
.forgot-link:hover { text-decoration: underline; color: #93c5fd; }
.agreement-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 4px;
  cursor: pointer;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}
.agreement-row .remember-checkbox { margin-top: 2px; flex-shrink: 0; }
.agreement-text a { color: #60a5fa; text-decoration: none; }
.agreement-text a:hover { text-decoration: underline; color: #93c5fd; }
.err { margin: 6px 0 0; font-size: 12px; color: #f87171; }
.btn {
  height: 44px;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 2px;
  border: none;
  cursor: pointer;
  transition: background 0.15s;
}
.btn:disabled { opacity: 0.7; cursor: not-allowed; }
.btn.primary {
  width: 100%;
  height: 46px;
  background: #2563eb;
  color: #fff;
}
.btn.primary:hover:not(:disabled) { background: #1d4ed8; }
.btn.secondary {
  height: 44px;
  flex-shrink: 0;
  padding: 0 20px;
  background: rgba(37, 99, 235, 0.2);
  color: #60a5fa;
  border: 1px solid rgba(96, 165, 250, 0.5);
  border-radius: 2px;
}
.btn.secondary:hover:not(:disabled) { background: rgba(37, 99, 235, 0.35); color: #93c5fd; }
.btn.secondary:disabled { border-color: rgba(255,255,255,0.15); color: rgba(255,255,255,0.35); }
.tab-link.below {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}
.tab-link a { color: #60a5fa; text-decoration: none; }
.tab-link a:hover { text-decoration: underline; color: #93c5fd; }
.page-footer {
  position: absolute;
  bottom: 24px;
  left: 0;
  right: 0;
  z-index: 1;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  text-align: center;
}
.page-footer a { color: rgba(255, 255, 255, 0.9); text-decoration: none; }
.page-footer a:hover { text-decoration: underline; }
.divider { margin: 0 8px; }
@media (max-width: 900px) {
  .auth-page { padding: 32px 24px 80px; }
  .brand-logo { padding: 20px 24px; }
  .brand-name { font-size: 18px; }
  .auth-main {
    flex-direction: column;
    align-items: stretch;
    gap: 48px;
    max-width: 100%;
  }
  .auth-slogan {
    max-width: none;
    margin-bottom: 0;
    text-align: center;
  }
  .slogan-title { font-size: 26px; }
  .slogan-sub { font-size: 14px; }
  .monitor-badges { justify-content: center; margin-top: 24px; }
  .auth-form-wrap { max-width: none; }
}
@media (max-width: 480px) {
  .brand-logo { padding: 16px 20px; }
  .brand-icon { width: 32px; height: 32px; }
  .brand-name { font-size: 16px; }
  .auth-glass-card { padding: 28px 20px; }
  .slogan-title { font-size: 22px; }
}
</style>
