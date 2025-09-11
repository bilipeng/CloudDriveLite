<template>
  <div class="register">
    <el-card class="register-panel" shadow="always">
      <div class="panel-inner">
        <div class="panel-left">
          <img class="illustration" src="@/assets/login-db.svg" alt="register" />
        </div>
        <div class="panel-right">
          <div class="brand">
            <img class="logo" src="@/assets/logo.png" alt="CloudDriveLite" />
            <div class="brand-name">CloudDriveLite</div>
          </div>
          
          <el-form :model="form" :rules="rules" ref="formRef" label-width="0" class="register-form">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="用户名" clearable prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="userNumber">
              <el-input v-model="form.userNumber" placeholder="用户账号" clearable prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="phoneNumber">
              <el-input v-model="form.phoneNumber" placeholder="手机号" clearable prefix-icon="Phone" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="form.email" placeholder="邮箱（可选）" clearable prefix-icon="Message" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码" show-password prefix-icon="Lock" />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password prefix-icon="Lock" />
            </el-form-item>
            <el-button type="primary" class="submit-btn" size="large" :loading="submitting" @click="onSubmit">
              注册
            </el-button>
            <div class="form-footer">
              <el-link type="primary" @click="goToLogin">已有账号？立即登录</el-link>
            </div>
          </el-form>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { User, Lock, Phone, Message } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'

const router = useRouter()

const form = reactive({
  username: '',
  userNumber: '',
  phoneNumber: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const formRef = ref<FormInstance>()
const submitting = ref(false)

// 自定义验证函数
const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  userNumber: [{ required: true, message: '请输入用户账号', trigger: 'blur' }],
  phoneNumber: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    const response = await authApi.register({
      username: form.username,
      userNumber: form.userNumber,
      phoneNumber: form.phoneNumber,
      password: form.password,
      email: form.email || undefined
    })
    
    ElMessage.success('注册成功！请登录')
    router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    submitting.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<style scoped>
.register {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.register-panel {
  width: 1200px;
  border-radius: 12px;
}

.panel-inner {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
}

.panel-left {
  padding: 48px 40px;
  border-right: 1px solid var(--el-border-color-lighter);
  display: flex;
  align-items: center;
  justify-content: center;
}
.illustration { max-width: 100%; height: 360px; object-fit: contain; }

.panel-right {
  padding: 48px 56px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand { display: flex; align-items: center; gap: 10px; justify-content: center; }
.logo { width: 56px; height: 56px; object-fit: contain; }
.brand-name { font-size: 22px; font-weight: 700; }

.register-form { max-width: 440px; margin: 0 auto; width: 100%; }
.submit-btn { width: 100%; margin-bottom: 16px; }
.form-footer { text-align: center; }
</style>

