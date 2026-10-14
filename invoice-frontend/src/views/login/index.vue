<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

onMounted(() => {
  if (userStore.token) {
    void router.replace('/invoice/list')
  }
})

async function handleSubmit(): Promise<void> {
  if (!formRef.value || submitting.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const token = await login({
      username: form.username.trim(),
      password: form.password,
    })
    userStore.setSession(token, form.username.trim())
    await router.replace('/invoice/list')
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <h1 class="login-title">材料费发票智能预审与管理平台</h1>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @submit.prevent="handleSubmit"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            autocomplete="username"
            maxlength="64"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            autocomplete="current-password"
            maxlength="64"
            show-password
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            class="login-btn"
            :loading="submitting"
            :disabled="submitting"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-extra">
        <router-link to="/register">去注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  box-sizing: border-box;
  background: #f5f7fa;
}

.login-card {
  width: 100%;
  max-width: 420px;
}

.login-title {
  margin: 0 0 28px;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.4;
  text-align: center;
  color: #303133;
}

.login-btn {
  width: 100%;
}

.login-extra {
  margin-top: -8px;
  text-align: center;
}

.login-extra a {
  color: var(--el-color-primary);
  text-decoration: none;
  font-size: 14px;
}

.login-extra a:hover {
  text-decoration: underline;
}

@media (max-width: 480px) {
  .login-title {
    font-size: 18px;
  }
}
</style>
