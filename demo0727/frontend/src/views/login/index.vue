<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="login-bg">
      <div class="bg-circle circle-1"></div>
      <div class="bg-circle circle-2"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <!-- Logo 区域 -->
      <div class="login-logo">
        <el-icon :size="36" color="var(--primary-color)"><Cpu /></el-icon>
        <h1 class="logo-title">{{ systemName }}</h1>
        <p class="logo-subtitle">{{ systemDescription }}</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            size="large"
            placeholder="请输入用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            size="large"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="loginForm.captchaCode"
              size="large"
              placeholder="请输入验证码"
              :prefix-icon="Key"
              class="captcha-input"
            />
            <img
              :src="captchaImg"
              alt="验证码"
              class="captcha-img"
              title="点击刷新"
              @click="refreshCaptcha"
            />
          </div>
        </el-form-item>

        <div class="login-options">
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
        </div>

        <el-button
          type="primary"
          size="large"
          class="login-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>

      <!-- 提示信息 -->
      <div class="login-tip">
        <el-text type="info" size="small">
          默认账号: admin / admin123
        </el-text>
      </div>

      <!-- 备案信息（系统配置 system.icp，未配置时不展示） -->
      <div v-if="systemIcp" class="login-icp">
        <el-text type="info" size="small">{{ systemIcp }}</el-text>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormItemRule } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'
import { getCaptcha } from '@/api/captcha'
import { getPublicSystemConfig } from '@/api/system'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)

/** 系统名称/描述/备案信息（从公开配置加载） */
const systemName = ref('AI 知识管理平台')
const systemDescription = ref('Enterprise AI Knowledge Platform')
const systemIcp = ref('')

const loginForm = reactive({
  username: 'admin',
  password: 'admin123',
  captchaUuid: '',
  captchaCode: '',
})

/** 验证码图片 */
const captchaImg = ref('')

/** 刷新验证码 */
async function refreshCaptcha() {
  try {
    const res = await getCaptcha()
    loginForm.captchaUuid = res.uuid
    captchaImg.value = `data:image/png;base64,${res.imgBase64}`
    loginForm.captchaCode = ''
  } catch {
    // 验证码获取失败由拦截器统一提示
  }
}

const loginRules: Record<string, FormItemRule[]> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在 6-20 个字符之间', trigger: 'blur' },
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

/** 处理登录 */
async function handleLogin() {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
    loading.value = true
    const res = await login(loginForm)
    userStore.loginSuccess(res.accessToken, res.refreshToken, res.user)
    // 加载动态菜单与权限码
    try {
      await userStore.loadAccess()
    } catch {
      // 菜单加载失败不阻塞登录，后续路由守卫会兜底重试
    }
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    // 校验失败或登录失败，刷新验证码
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

/** 加载系统名称/描述/备案信息 */
async function loadSystemInfo() {
  try {
    const cfg = await getPublicSystemConfig()
    if (cfg['system.name']) systemName.value = cfg['system.name']
    if (cfg['system.description']) systemDescription.value = cfg['system.description']
    if (cfg['system.icp']) systemIcp.value = cfg['system.icp']
  } catch {
    // 加载失败时保留默认值
  }
}

onMounted(() => {
  refreshCaptcha()
  loadSystemInfo()
})
</script>

<style scoped>
.login-container {
  position: relative;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f4ff 0%, #f5f6f8 50%, #ffffff 100%);
  overflow: hidden;
}

/* 背景装饰圆 */
.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.4;
}

.circle-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(74, 122, 254, 0.15), transparent 70%);
  top: -100px;
  right: -100px;
}

.circle-2 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(74, 122, 254, 0.1), transparent 70%);
  bottom: -50px;
  left: -50px;
}

/* 登录卡片 */
.login-card {
  position: relative;
  width: 400px;
  padding: 40px 36px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  z-index: 1;
}

/* Logo */
.login-logo {
  text-align: center;
  margin-bottom: 32px;
}

.logo-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 12px 0 4px;
}

.logo-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  letter-spacing: 0.5px;
}

/* 表单 */
.login-form {
  margin-top: 8px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
}

/* 验证码 */
.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 110px;
  height: 40px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  cursor: pointer;
  flex-shrink: 0;
  background: #f5f7fa;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  border-radius: var(--radius-md);
}

/* 提示 */
.login-tip {
  margin-top: 20px;
  text-align: center;
}

/* 备案信息 */
.login-icp {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
  text-align: center;
}

/* 响应式 */
@media (max-width: 480px) {
  .login-card {
    width: 90%;
    padding: 32px 24px;
  }
}
</style>
