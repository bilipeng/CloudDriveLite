<template>
  <div class="login">
    <el-card class="login-panel" shadow="always">
      <div class="panel-inner">
        <div class="panel-left">
          <img class="illustration" src="@/assets/login-db.svg" alt="login" />
        </div>
        <div class="panel-right">
          <div class="brand">
            <img class="logo" src="@/assets/logo.png" alt="CloudDriveLite" />
            <div class="brand-name">CloudDriveLite</div>
          </div>
          
          <el-form :model="form" :rules="rules" ref="formRef" label-width="0" class="login-form">
            <el-form-item prop="userNumber">
              <el-input v-model="form.userNumber" placeholder="用户账号" clearable prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码" show-password prefix-icon="Lock" />
            </el-form-item>
            <div class="form-extras">
              <el-checkbox v-model="remember">记住密码</el-checkbox>
              <el-link type="info" :underline="false">忘记密码</el-link>
            </div>
            <div class="form-footer">
              <el-link type="primary" @click="goToRegister">没有账号？立即注册</el-link>
            </div>
            <el-button type="primary" class="submit-btn" size="large" :loading="submitting" @click="onSubmit">
              登 录
            </el-button>
          </el-form>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'

const router = useRouter()

const form = reactive({ userNumber: '', password: '' })
const formRef = ref<FormInstance>()
const submitting = ref(false)
const remember = ref(false)

const rules: FormRules<typeof form> = {
  userNumber: [{ required: true, message: '请输入用户账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    // 调用真实的后端登录API
    const response = await authApi.login({
      userNumber: form.userNumber,
      password: form.password
    })
    
    // 登录成功，保存用户信息
    localStorage.setItem('token', 'authenticated') // 简化处理，实际项目中应该保存JWT token
    localStorage.setItem('userId', response.userId.toString())
    localStorage.setItem('userNumber', form.userNumber)
    
    ElMessage.success(response.message || '登录成功')
    router.replace('/files')
  } catch (error) {
    // 错误处理已经在axios拦截器中完成
    console.error('登录失败:', error)
  } finally {
    submitting.value = false
  }
}

function goToRegister() {
  router.push('/register')
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-panel {
  width: 1100px;
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

.panel-title { margin: 18px 0 18px; text-align: center; font-size: 16px; color: var(--el-text-color-secondary); }

.login-form { max-width: 440px; margin: 0 auto; width: 100%; }
.form-extras { display: flex; justify-content: space-between; align-items: center; margin: 6px 0 16px; }
.form-footer { text-align: center; margin-bottom: 16px; }
.submit-btn { width: 100%; }
</style>


