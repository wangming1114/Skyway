<template>
  <div class="auth-page">
    <!-- 左侧视觉区：浅色，非黑暗系 -->
    <div class="auth-visual">
      <div class="auth-visual__gradient" aria-hidden="true"></div>
      <div class="auth-visual__content">
        <div class="auth-visual__logo" aria-label="品牌">
          <span class="auth-visual__logo-icon" aria-hidden="true">
            <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="20" cy="8" r="3.5" fill="currentColor"/>
              <circle cx="8" cy="24" r="3.5" fill="currentColor"/>
              <circle cx="32" cy="24" r="3.5" fill="currentColor"/>
              <circle cx="20" cy="34" r="3.5" fill="currentColor"/>
              <path d="M20 11.5v5M20 29v4M20 16.5l-9 4.5M20 16.5l9 4.5M8 24l11.5-5.5M32 24l-11.5-5.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </span>
          <span class="auth-visual__logo-text">Skyway</span>
        </div>
        <div class="auth-visual__main">
          <h1 class="auth-visual__title">构建无界网络，赋能全球业务</h1>
          <p class="auth-visual__subtitle">企业级智能路由与安全接入平台。提供极速、稳定、端到端加密的跨域互联方案。</p>
          <div class="monitor-badges">
            <span class="monitor-badge"><span class="monitor-dot monitor-dot--green"></span> Global Nodes: 124 Online</span>
            <span class="monitor-badge"><span class="monitor-dot monitor-dot--yellow"></span> Avg Latency: &lt; 10ms</span>
            <span class="monitor-badge"><span class="monitor-dot monitor-dot--blue"></span> AES-256 Secured</span>
          </div>
        </div>
        <div class="auth-visual__footer">© 2026 Skyway Inc.</div>
      </div>
    </div>

    <!-- 右侧表单区：白底、下划线输入、与 Admin 一致风格 -->
    <div class="auth-form-wrap">
      <div class="auth-form-box">
        <div class="auth-form-header">
          <h2 class="auth-form-title">{{ activeTab === 'login' ? '欢迎回来' : activeTab === 'forgot' ? '找回密码' : '注册账号' }}</h2>
          <p class="auth-form-subtitle">{{ activeTab === 'login' ? '请输入邮箱或用户名登录' : activeTab === 'forgot' ? '通过注册邮箱重置密码' : '填写信息完成注册' }}</p>
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
                <span class="code-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2l8 4v6c0 5-3.5 9.5-8 10-4.5-.5-8-5-8-10V6l8-4z"/>
                    <path d="M9 12l2 2 4-4"/>
                  </svg>
                </span>
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
                <button type="button" class="pwd-toggle" :aria-label="forgotPwdVisible ? '隐藏密码' : '显示密码'" @click="forgotPwdVisible = !forgotPwdVisible">
                  <svg v-if="forgotPwdVisible" class="pwd-toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                  <svg v-else class="pwd-toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                </button>
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
                <button type="button" class="pwd-toggle" :aria-label="forgotConfirmVisible ? '隐藏密码' : '显示密码'" @click="forgotConfirmVisible = !forgotConfirmVisible">
                <svg v-if="forgotConfirmVisible" class="pwd-toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                <svg v-else class="pwd-toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              </button>
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
              <button type="button" class="pwd-toggle" :aria-label="loginPwdVisible ? '隐藏密码' : '显示密码'" @click="loginPwdVisible = !loginPwdVisible">
              <svg v-if="loginPwdVisible" class="pwd-toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
              <svg v-else class="pwd-toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
            </button>
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
              <span class="code-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2l8 4v6c0 5-3.5 9.5-8 10-4.5-.5-8-5-8-10V6l8-4z"/>
                  <path d="M9 12l2 2 4-4"/>
                </svg>
              </span>
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
              <button type="button" class="pwd-toggle" :aria-label="regPwdVisible ? '隐藏密码' : '显示密码'" @click="regPwdVisible = !regPwdVisible">
              <svg v-if="regPwdVisible" class="pwd-toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
              <svg v-else class="pwd-toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
            </button>
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
              <button type="button" class="pwd-toggle" :aria-label="regConfirmVisible ? '隐藏密码' : '显示密码'" @click="regConfirmVisible = !regConfirmVisible">
              <svg v-if="regConfirmVisible" class="pwd-toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
              <svg v-else class="pwd-toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
            </button>
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
      <p class="auth-form-footer"><a href="#">服务条款</a><span class="divider">|</span><a href="#">隐私政策</a></p>
    </div>
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
/* 与 Admin 一致：系统字体、浅色整体 */
.auth-page {
  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'SF Pro Text', sans-serif;
  font-family: var(--font-sans);
  min-height: 100vh;
  display: flex;
  width: 100%;
  box-sizing: border-box;
  color: #334155;
  background: #fff;
  -webkit-font-smoothing: antialiased;
}

/* ========== 左侧视觉区：浅色系、抽象 3D 科技感背景（参考图风格） ========== */
.auth-visual {
  display: none;
  width: 50%;
  position: relative;
  overflow: hidden;
  background-color: #f1f5f9;
  background-image:
    linear-gradient(135deg, rgba(255,255,255,0.2) 0%, rgba(241,245,249,0.12) 52%, rgba(226,232,240,0.08) 100%),
    url('/login-bg-tech3.png');
  background-size: cover;
  background-position: 5% center;
}
@media (min-width: 1024px) {
  .auth-visual { display: flex; flex-direction: column; }
}
.auth-visual__gradient {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(to bottom, rgba(255,255,255,0.25) 0%, transparent 60%);
}
.auth-visual__content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 2.5rem 3rem;
  justify-content: space-between;
}
.auth-visual__logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.auth-visual__logo-icon {
  width: 2.5rem;
  height: 2.5rem;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 6px;
  color: #2563eb;
}
.auth-visual__logo-icon svg { width: 1.25rem; height: 1.25rem; }
.auth-visual__logo-text {
  font-weight: 700;
  font-size: 1.25rem;
  letter-spacing: 0.02em;
  color: #0f172a;
}
.auth-visual__main { margin-top: auto; margin-bottom: 2rem; }
.auth-visual__title {
  font-size: 1.875rem;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.025em;
  line-height: 1.2;
  margin: 0 0 0.5rem 0;
}
.auth-visual__subtitle {
  font-size: 1rem;
  color: #475569;
  line-height: 1.6;
  margin: 0 0 0;
}
.monitor-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-top: 24px;
}
.monitor-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-family: ui-monospace, 'SF Mono', monospace;
  font-size: 11px;
  font-weight: 500;
  color: #15803d;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}
.monitor-badge:nth-child(2) { color: #0369a1; border-color: rgba(3, 105, 161, 0.2); }
.monitor-badge:nth-child(3) { color: #6d28d9; border-color: rgba(109, 40, 217, 0.2); }
.monitor-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.monitor-dot--green { background: #22c55e; }
.monitor-dot--yellow { background: #eab308; }
.monitor-dot--blue { background: #0ea5e9; }
.auth-visual__footer {
  padding-top: 2rem;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  font-size: 0.75rem;
  color: #64748b;
}

/* ========== 右侧表单区：白底、下划线输入 ========== */
.auth-form-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem 1.5rem;
}
@media (min-width: 1024px) {
  .auth-form-wrap { width: 50%; padding: 3rem 4rem; }
}
.auth-form-box {
  width: 100%;
  max-width: 400px;
}
.auth-form-header { margin-bottom: 1.5rem; }
.auth-form-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.025em;
  margin: 0 0 0.5rem 0;
}
.auth-form-subtitle {
  font-size: 0.875rem;
  color: #475569;
  margin: 0;
}
.forgot-panel { margin-top: 0; }

/* 下划线输入框（与 Admin 一致） */
.form { display: flex; flex-direction: column; gap: 1.25rem; }
.field { position: relative; }
.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
  height: 44px;
  border-bottom: 1px solid #e2e8f0;
  transition: border-color 0.3s ease;
}
.input-with-icon:focus-within { border-color: #2563eb; }
.input-with-icon .input-icon {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  color: #94a3b8;
  pointer-events: none;
  flex-shrink: 0;
  transition: color 0.3s ease;
}
.input-with-icon:focus-within .input-icon { color: #2563eb; }
.input-with-icon .input-icon svg { width: 100%; height: 100%; display: block; }
.input {
  width: 100%;
  height: 44px;
  padding: 0 0 0 28px;
  font-size: 14px;
  line-height: 42px;
  color: #0f172a;
  background: transparent;
  border: none;
  border-radius: 0;
  outline: none;
  box-sizing: border-box;
}
.input::placeholder { color: #94a3b8; }
/* 覆盖浏览器自动填充默认高亮，保持输入框样式一致 */
.input:-webkit-autofill,
.input:-webkit-autofill:hover,
.input:-webkit-autofill:focus,
.input:-webkit-autofill:active {
  -webkit-text-fill-color: #0f172a !important;
  caret-color: #0f172a;
  -webkit-box-shadow: 0 0 0 1000px #fff inset !important;
  box-shadow: 0 0 0 1000px #fff inset !important;
  transition: background-color 9999s ease-out 0s, color 9999s ease-out 0s;
}

.input:-moz-autofill {
  box-shadow: 0 0 0 1000px #fff inset !important;
  -moz-text-fill-color: #0f172a;
  caret-color: #0f172a;
}
.code-field { display: flex; flex-direction: column; gap: 0; }
.code-row {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 44px;
  border-bottom: 1px solid #e2e8f0;
  transition: border-color 0.3s ease;
}
.code-row:focus-within { border-color: #2563eb; }
.code-row .input-with-icon { flex: 1; min-width: 0; }
.code-icon {
  width: 18px;
  height: 18px;
  color: #94a3b8;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.code-icon svg {
  width: 100%;
  height: 100%;
  display: block;
}
.code-row:focus-within .code-icon {
  color: #2563eb;
}
.code-row .input.code-input {
  flex: 1;
  min-width: 0;
  padding-left: 0;
}
.code-input { flex: 1; min-width: 0; }
.code-row .btn.secondary {
  height: 34px;
  padding: 0 12px;
  font-size: 12px;
  line-height: 34px;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;
}
.input-wrap { position: relative; height: 44px; }
.pwd-toggle {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  color: #64748b;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.pwd-toggle-icon {
  width: 18px;
  height: 18px;
  display: block;
}
.pwd-toggle:hover { color: #2563eb; }
.input-wrap .input { padding-right: 52px; }

/* 密码强度气泡：浅色主题 */
.pwd-bubble {
  position: absolute;
  left: 0;
  top: 100%;
  margin-top: 8px;
  padding: 10px 14px;
  font-size: 13px;
  line-height: 1.45;
  color: #334155;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 10px 40px -10px rgba(0, 0, 0, 0.12);
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
  background: #fff;
  border-left: 1px solid #e2e8f0;
  border-top: 1px solid #e2e8f0;
  transform: rotate(45deg);
}
.pwd-bubble-ok { color: #16a34a; font-weight: 500; }
.pwd-bubble-title { color: #64748b; margin-right: 4px; }
.pwd-bubble-missing { color: #b45309; }
.pwd-field { position: relative; }
.bubble-enter-active, .bubble-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.bubble-enter-from, .bubble-leave-to { opacity: 0; transform: translateY(-4px); }

.login-options { display: flex; align-items: center; justify-content: space-between; }
.remember-row {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #64748b;
}
.remember-checkbox { width: 14px; height: 14px; border-radius: 2px; accent-color: #2563eb; }
.remember-text { user-select: none; }
.forgot-link { font-size: 13px; color: #2563eb; text-decoration: none; }
.forgot-link:hover { text-decoration: underline; color: #1d4ed8; }
.agreement-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #64748b;
}
.agreement-row .remember-checkbox { margin-top: 2px; flex-shrink: 0; }
.agreement-text a { color: #2563eb; text-decoration: none; }
.agreement-text a:hover { text-decoration: underline; color: #1d4ed8; }
.err { margin: 6px 0 0; font-size: 12px; color: #dc2626; }

.btn {
  height: 44px;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.15s ease, box-shadow 0.2s ease;
}
.btn:disabled { opacity: 0.7; cursor: not-allowed; }
.btn.primary {
  width: 100%;
  height: 46px;
  background: #3b82f6;
  color: #fff;
}
.btn.primary:hover:not(:disabled) {
  background: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.35);
}
.btn.primary:active:not(:disabled) { transform: translateY(0) scale(0.98); }
.btn.secondary {
  height: 44px;
  flex-shrink: 0;
  padding: 0 20px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 6px;
}
.btn.secondary:hover:not(:disabled) { background: rgba(59, 130, 246, 0.18); color: #1d4ed8; }
.btn.secondary:disabled { border-color: #e2e8f0; color: #94a3b8; }

.tab-link.below { margin-top: 1.5rem; text-align: center; font-size: 14px; color: #64748b; }
.tab-link a { color: #2563eb; text-decoration: none; }
.tab-link a:hover { text-decoration: underline; color: #1d4ed8; }
.auth-form-footer {
  margin-top: 2rem;
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
}
.auth-form-footer a { color: #64748b; text-decoration: none; }
.auth-form-footer a:hover { text-decoration: underline; color: #475569; }
.divider { margin: 0 8px; }

/* 滑块验证：浅色主题覆盖 */
.auth-form-wrap :deep(.slider-track) {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}
.auth-form-wrap :deep(.slider-track:hover:not(.verified)) {
  border-color: #cbd5e1;
  background: #e2e8f0;
}
.auth-form-wrap :deep(.slider-track.verified) {
  background: rgba(34, 197, 94, 0.12);
  border-color: rgba(34, 197, 94, 0.4);
}
.auth-form-wrap :deep(.slider-label) {
  color: #64748b;
}
.auth-form-wrap :deep(.slider-track.verified .slider-label) {
  color: #16a34a;
}
.auth-form-wrap :deep(.slider-thumb) {
  border-radius: 6px;
  background: #3b82f6;
}
.auth-form-wrap :deep(.slider-thumb.verified) {
  background: #22c55e;
}
.auth-form-wrap :deep(.slider-refresh) {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #64748b;
  border-radius: 6px;
}
.auth-form-wrap :deep(.slider-refresh:hover) {
  background: #e2e8f0;
  color: #334155;
}

@media (max-width: 1023px) {
  .auth-page { padding: 0; }
  .auth-form-wrap { padding: 2rem 1rem; }
}
@media (max-width: 480px) {
  .auth-form-box { max-width: none; }
  .auth-form-title { font-size: 1.25rem; }
}
</style>
