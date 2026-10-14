<!-- d:\JavaCode\invoice-frontend\src\views\register\index.vue -->
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
})

const rules: FormRules<typeof form> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3~20', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度 6~30', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value || submitting.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await register({
      username: form.username.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      nickname: form.nickname.trim() || form.username.trim(),
    })
    ElMessage.success('注册成功，请登录')
    setTimeout(() => {
      router.replace('/login')
    }, 600)
  } catch (e) {
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <el-card class="register-card" shadow="hover">
      <h1 class="register-title">材料费发票智能预审与管理平台</h1>
      <p class="register-subtitle">管理员注册</p>

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
            placeholder="用户名（3~20 位）"
            autocomplete="username"
            maxlength="20"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码（6~30 位）"
            autocomplete="new-password"
            maxlength="30"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认密码"
            autocomplete="new-password"
            maxlength="30"
            show-password
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="昵称（可选）"
            maxlength="20"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            class="register-btn"
            :loading="submitting"
            :disabled="submitting"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-extra">
        <router-link to="/login">已有账号？去登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  box-sizing: border-box;
  background: #f5f7fa;
}

.register-card {
  width: 100%;
  max-width: 420px;
}

.register-title {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 600;
  text-align: center;
  color: #303133;
}

.register-subtitle {
  margin: 0 0 24px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.register-btn {
  width: 100%;
}

.register-extra {
  text-align: center;
}

.register-extra a {
  color: var(--el-color-primary);
  text-decoration: none;
  font-size: 14px;
}

.register-extra a:hover {
  text-decoration: underline;
}

@media (max-width: 480px) {
  .register-title {
    font-size: 17px;
  }
}
</style>