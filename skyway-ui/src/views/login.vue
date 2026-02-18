<template>
  <div class="login-split">
    <!-- 左侧视觉区 -->
    <div class="login-visual">
      <img
        src="https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&q=80&w=1600"
        alt=""
        class="login-visual__img"
      />
      <div class="login-visual__overlay login-visual__overlay--multiply"></div>
      <div class="login-visual__overlay login-visual__overlay--gradient"></div>

      <div class="login-visual__content">
        <div class="login-visual__logo">
          <span class="login-visual__logo-icon" aria-hidden="true">
            <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="20" cy="8" r="3.5" fill="currentColor"/>
              <circle cx="8" cy="24" r="3.5" fill="currentColor"/>
              <circle cx="32" cy="24" r="3.5" fill="currentColor"/>
              <circle cx="20" cy="34" r="3.5" fill="currentColor"/>
              <path d="M20 11.5v5M20 29v4M20 16.5l-9 4.5M20 16.5l9 4.5M8 24l11.5-5.5M32 24l-11.5-5.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </span>
          <span class="login-visual__logo-text">Skyway <span class="login-visual__logo-sub">Admin</span></span>
        </div>

        <div class="login-visual__main">
          <h1 class="login-visual__title">全球网络运营中心</h1>
          <p class="login-visual__subtitle">Global Network Operation Center</p>

          <div class="login-visual__features">
            <div class="feature">
              <div class="feature__icon feature__icon--blue"><Connection class="icon" /></div>
              <div>
                <h3 class="feature__title">全网节点集中管控</h3>
                <p class="feature__desc">实时监控全球 124+ 边缘节点状态</p>
              </div>
            </div>
            <div class="feature">
              <div class="feature__icon feature__icon--purple"><Monitor class="icon" /></div>
              <div>
                <h3 class="feature__title">智能路由策略配置</h3>
                <p class="feature__desc">毫秒级自动切换最优传输链路</p>
              </div>
            </div>
            <div class="feature">
              <div class="feature__icon feature__icon--green"><Document class="icon" /></div>
              <div>
                <h3 class="feature__title">企业级安全审计</h3>
                <p class="feature__desc">全链路加密日志与访问控制溯源</p>
              </div>
            </div>
          </div>
        </div>

        <div class="login-visual__footer">
          <div class="login-visual__footer-secure">
            <CircleCheck class="icon" />
            <span>© SYSTEM SECURE V3.9.1</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧操作区 -->
    <div class="login-form-wrap">
      <div class="login-form-box">
        <div class="login-form-header">
          <h2 class="login-form-title">欢迎回来</h2>
          <p class="login-form-subtitle">请输入管理员账号以访问控制台</p>
        </div>

        <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form" @submit.prevent="handleLogin">
          <div class="form-fields">
            <div class="field field--underline">
              <label class="field__label">账号</label>
              <div class="field__input-wrap">
                <User class="field__icon" />
                <el-form-item prop="username" class="field__item">
                  <el-input
                    v-model="loginForm.username"
                    type="text"
                    autocomplete="off"
                    placeholder="admin"
                    class="field__input"
                    :border="false"
                    @keyup.enter="handleLogin"
                  />
                </el-form-item>
              </div>
            </div>

            <div class="field field--underline">
              <label class="field__label">密码</label>
              <div class="field__input-wrap">
                <Lock class="field__icon" />
                <el-form-item prop="password" class="field__item">
                  <el-input
                    v-model="loginForm.password"
                    type="password"
                    autocomplete="off"
                    placeholder="••••••••"
                    class="field__input"
                    :border="false"
                    show-password
                    @keyup.enter="handleLogin"
                  />
                </el-form-item>
              </div>
            </div>

            <!-- 图形验证码：与账号/密码同级结构，保证与上方横线间距一致 -->
            <div v-if="captchaEnabled" class="field field--underline field--captcha">
              <label class="field__label">验证码</label>
              <div class="field__input-row">
                <div class="field__input-wrap">
                  <CircleCheck class="field__icon" />
                  <el-form-item prop="code" class="field__item">
                    <el-input
                      v-model="loginForm.code"
                      placeholder="输入结果"
                      class="field__input"
                      :border="false"
                      @keyup.enter="handleLogin"
                    />
                  </el-form-item>
                </div>
                <div class="captcha-box" title="点击刷新" @click="getCode">
                  <div class="captcha-box__noise" aria-hidden="true"></div>
                  <img v-if="codeUrl" :src="codeUrl" class="captcha-box__img" alt="验证码" />
                  <div class="captcha-box__refresh">
                    <RefreshRight class="captcha-box__refresh-icon" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="form-options">
            <el-checkbox v-model="loginForm.rememberMe">记住登录状态</el-checkbox>
          </div>

          <el-form-item class="form-submit">
            <el-button
              type="primary"
              :loading="loading"
              class="submit-btn"
              @click.prevent="handleLogin"
            >
              {{ loading ? '登录中...' : '立即登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <p class="login-form-footer">© 2026 Skyway Inc.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { User, Lock, CircleCheck, Connection, Monitor, Document, RefreshRight } from '@element-plus/icons-vue'
import { getCodeImg } from '@/api/login'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import useUserStore from '@/store/modules/user'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: '',
  password: '',
  rememberMe: false,
  code: '',
  uuid: ''
})

const loginRules = computed(() => {
  const rules = {
    username: [{ required: true, trigger: 'blur', message: '请输入您的账号' }],
    password: [{ required: true, trigger: 'blur', message: '请输入您的密码' }]
  }
  if (captchaEnabled.value) {
    rules.code = [{ required: true, trigger: 'change', message: '请输入验证码' }]
  }
  return rules
})

const codeUrl = ref('')
const loading = ref(false)
const captchaEnabled = ref(true)
const redirect = ref(undefined)

watch(route, (newRoute) => {
  redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate((valid) => {
    if (!valid) return
    loading.value = true
    if (loginForm.value.rememberMe) {
      Cookies.set('username', loginForm.value.username, { expires: 30 })
      Cookies.set('password', encrypt(loginForm.value.password), { expires: 30 })
      Cookies.set('rememberMe', loginForm.value.rememberMe, { expires: 30 })
    } else {
      Cookies.remove('username')
      Cookies.remove('password')
      Cookies.remove('rememberMe')
    }
    userStore.login(loginForm.value).then(() => {
      const query = route.query
      const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
        if (cur !== 'redirect') acc[cur] = query[cur]
        return acc
      }, {})
      router.push({ path: redirect.value || '/', query: otherQueryParams })
    }).catch(() => {
      loading.value = false
      if (captchaEnabled.value) getCode()
    })
  })
}

function getCode() {
  getCodeImg().then((res) => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = 'data:image/gif;base64,' + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get('username')
  const password = Cookies.get('password')
  const rememberMe = Cookies.get('rememberMe')
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? '' : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

onMounted(() => {
  getCode()
  getCookie()
})
</script>

<style lang="scss" scoped>
.login-split {
  display: flex;
  min-height: 100vh;
  width: 100%;
  font-family: inherit;
  color: #1e293b;
  background: #fff;
}

/* ========== 左侧视觉区 ========== */
.login-visual {
  display: none;
  width: 50%;
  position: relative;
  overflow: hidden;
  background: #0f172a;

  @media (min-width: 1024px) {
    display: block;
  }
}

.login-visual__img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.9;
}

.login-visual__overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;

  /* 深蓝遮罩，更通透、不太黑 */
  &--multiply {
    background: rgba(30, 58, 138, 0.3);
    mix-blend-mode: multiply;
  }

  &--gradient {
    background: linear-gradient(to top, rgba(15, 23, 42, 0.8) 0%, transparent 50%, transparent 100%);
  }
}

.login-visual__content {
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 2.5rem 3rem;
  justify-content: space-between;
}

.login-visual__logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.login-visual__logo-icon {
  width: 2.5rem;
  height: 2.5rem;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  backdrop-filter: blur(8px);
  color: #fff;

  svg {
    width: 1.25rem;
    height: 1.25rem;
  }
}

.login-visual__logo-text {
  font-weight: 700;
  font-size: 1.25rem;
  letter-spacing: 0.02em;
  color: #fff;
}

.login-visual__logo-sub {
  font-weight: 400;
  opacity: 0.85;
}

.login-visual__main {
  margin-top: auto;
  margin-bottom: 2rem;
}

.login-visual__title {
  font-size: 1.875rem;
  font-weight: 700;
  color: #fff;
  letter-spacing: -0.02em;
  line-height: 1.2;
  margin: 0 0 0.5rem 0;
}

.login-visual__subtitle {
  font-size: 1rem;
  font-weight: 300;
  color: rgba(191, 219, 254, 0.9);
  margin: 0 0 1.5rem 0;
}

.login-visual__features {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.feature {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(4px);
  transition: background 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }
}

.feature__icon {
  width: 2.5rem;
  height: 2.5rem;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;

  .icon {
    width: 1.25rem;
    height: 1.25rem;
  }

  &--blue .icon { color: #93c5fd; }
  &--purple .icon { color: #c4b5fd; }
  &--green .icon { color: #86efac; }
}

.feature__title {
  font-size: 0.875rem;
  font-weight: 500;
  color: #fff;
  margin: 0 0 0.125rem 0;
}

.feature__desc {
  font-size: 0.75rem;
  color: rgba(191, 219, 254, 0.6);
  margin: 0;
}

.login-visual__footer {
  padding-top: 2rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.6);
  font-family: ui-monospace, monospace;
}

.login-visual__footer-secure {
  display: flex;
  align-items: center;
  gap: 0.5rem;

  .icon {
    width: 0.75rem;
    height: 0.75rem;
  }
}

/* ========== 右侧操作区 ========== */
.login-form-wrap {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1.5rem;

  @media (min-width: 1024px) {
    width: 50%;
    padding: 3rem 6rem;
  }
}

.login-form-box {
  width: 100%;
  max-width: 380px;
}

.login-form-header {
  margin-bottom: 2rem;
}

.login-form-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
  margin: 0 0 0.5rem 0;
}

.login-form-subtitle {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0;
}

.login-form {
  .form-fields {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
  }

  .form-options {
    margin-bottom: 1.5rem;
  }

  .form-submit {
    margin-bottom: 0;
  }
}

/* 极简下划线输入框 */
.field {
  &--underline {
    .field__label {
      display: block;
      font-size: 0.75rem;
      font-weight: 700;
      color: #94a3b8;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      margin-bottom: 0.5rem;
    }

    .field__input-wrap {
      display: flex;
      align-items: center;
      border-bottom: 1px solid #e2e8f0;
      transition: border-color 0.2s;

      &:focus-within {
        border-color: #2563eb;
      }

      &:focus-within .field__icon {
        color: #2563eb;
      }
    }

    .field__icon {
      width: 1.25rem;
      height: 1.25rem;
      color: #94a3b8;
      flex-shrink: 0;
      margin-right: 0.5rem;
      transition: color 0.2s;
    }

    .field__item {
      flex: 1;
      margin-bottom: 0;
    }

    .field__item :deep(.el-form-item__content) {
      margin-left: 0 !important;
    }

    .field__input :deep(.el-input__wrapper) {
      box-shadow: none !important;
      padding-left: 0;
      padding-right: 0;
      background: transparent !important;
    }

    .field__input :deep(.el-input__inner) {
      --el-input-placeholder-color: #cbd5e1;
    }
  }
}

/* 验证码：与账号同形式，一行内左侧输入（下划线）+ 右侧验证码图 */
/* 验证码：与账号一致，label 在上方，图标在「验证码」三字下方的输入行左侧 */
.field--captcha .field__label {
  display: block;
  margin-top: 0;
  margin-bottom: 0.5rem;
}

/* 与账号/密码一致：label 与输入行之间仅 0.5rem */
.field--captcha .field__label + .field__input-row {
  margin-top: 0;
}

.field__input-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}

/* 验证码图与输入框底边（横线）平齐 */
.field--captcha .field__input-row {
  align-items: flex-end;
}

.field--captcha .field__input-row .field__input-wrap {
  flex: 1;
  min-width: 0;
}

/* 验证码图片容器：灰底、圆角、干扰线，后端图只负责内容，容器做精致样式 */
.captcha-box {
  position: relative;
  width: 7rem;
  height: 2.5rem;
  flex-shrink: 0;
  background: linear-gradient(145deg, #f8fafc 0%, #f1f5f9 100%);
  border: none;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: box-shadow 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);

  &:hover {
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
  }

  &:hover .captcha-box__refresh {
    opacity: 1;
  }
}

.captcha-box__noise {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.08;
  background-image:
    radial-gradient(circle at 20% 30%, #94a3b8 1px, transparent 1px),
    radial-gradient(circle at 80% 70%, #94a3b8 1px, transparent 1px);
  background-size: 6px 6px;
}

.captcha-box__noise::before {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    -45deg,
    transparent,
    transparent 2px,
    rgba(148, 163, 184, 0.06) 2px,
    rgba(148, 163, 184, 0.06) 3px
  );
}

.captcha-box__img {
  position: relative;
  z-index: 1;
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
  display: block;
  border-radius: 6px;
}

.captcha-box__refresh {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  opacity: 0;
  transition: opacity 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.captcha-box__refresh-icon {
  width: 0.75rem;
  height: 0.75rem;
  color: #94a3b8;
}

.form-options :deep(.el-checkbox__label) {
  color: #64748b;
  font-size: 0.875rem;
}

/* 登录按钮：品牌亮蓝 + 轻微蓝色阴影 */
.submit-btn {
  width: 100%;
  height: 2.75rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  border-radius: 8px;
  background: #2563eb !important;
  border-color: #2563eb !important;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);

  &:hover:not(:disabled) {
    background: #1d4ed8 !important;
    border-color: #1d4ed8 !important;
    box-shadow: 0 6px 20px rgba(37, 99, 235, 0.35);
  }

  &:disabled {
    box-shadow: none;
  }
}

.login-form-footer {
  margin-top: 2rem;
  text-align: center;
  font-size: 0.75rem;
  color: #cbd5e1;
  font-weight: 300;
}
</style>
