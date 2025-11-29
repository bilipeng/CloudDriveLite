<template>
  <div class="forgot-password">
    <el-card class="forgot-panel" shadow="always">
      <div class="panel-inner">
        <div class="panel-left">
          <img class="illustration" src="@/assets/login-db.svg" alt="forgot password" />
        </div>
        <div class="panel-right">
          <div class="brand">
            <img class="logo" src="@/assets/logo.png" alt="CloudDriveLite" />
            <div class="brand-name">找回密码</div>
          </div>
          
          <el-form :model="form" :rules="rules" ref="formRef" label-width="0" class="forgot-form">
            <el-form-item prop="userNumber">
              <el-input 
                v-model="form.userNumber" 
                placeholder="请输入用户账号" 
                clearable 
                prefix-icon="User" 
              />
            </el-form-item>
            
            <el-form-item prop="contactType">
              <el-radio-group v-model="form.contactType" class="contact-type-group">
                <el-radio label="phone">手机号</el-radio>
                <el-radio label="email">邮箱</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item 
              v-if="form.contactType === 'phone'" 
              prop="phoneNumber"
            >
              <el-input 
                v-model="form.phoneNumber" 
                placeholder="请输入手机号" 
                clearable 
                prefix-icon="Phone" 
                maxlength="11"
              />
            </el-form-item>
            
            <el-form-item 
              v-if="form.contactType === 'email'" 
              prop="email"
            >
              <el-input 
                v-model="form.email" 
                placeholder="请输入邮箱" 
                clearable 
                prefix-icon="Message" 
                type="email"
              />
            </el-form-item>
            
            <el-form-item prop="newPassword">
              <el-input 
                v-model="form.newPassword" 
                type="password" 
                placeholder="请输入新密码（至少6位）" 
                show-password 
                prefix-icon="Lock" 
              />
            </el-form-item>
            
            <el-form-item prop="confirmPassword">
              <el-input 
                v-model="form.confirmPassword" 
                type="password" 
                placeholder="请确认新密码" 
                show-password 
                prefix-icon="Lock" 
              />
            </el-form-item>
            
            <div class="form-footer">
              <el-link type="primary" @click="goToLogin">返回登录</el-link>
            </div>
            
            <el-button 
              type="primary" 
              class="submit-btn" 
              size="large" 
              :loading="submitting" 
              @click="onSubmit"
            >
              重置密码
            </el-button>
          </el-form>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'

const router = useRouter()
const form = reactive({
  userNumber: '',
  contactType: 'phone' as 'phone' | 'email',
  phoneNumber: '',
  email: '',
  newPassword: '',
  confirmPassword: ''
})
const formRef = ref<FormInstance>()
const submitting = ref(false)

// 自定义验证规则：确认密码
const validateConfirmPassword = (rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请确认新密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 手机号验证规则
const validatePhone = (rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

// 邮箱验证规则
const validateEmail = (rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请输入邮箱'))
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    callback(new Error('邮箱格式不正确'))
  } else {
    callback()
  }
}

const rules: FormRules<typeof form> = {
  userNumber: [
    { required: true, message: '请输入用户账号', trigger: 'blur' }
  ],
  contactType: [
    { required: true, message: '请选择验证方式', trigger: 'change' }
  ],
  phoneNumber: [
    { validator: validatePhone, trigger: 'blur' }
  ],
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  // 根据选择的验证方式，验证对应的字段
  if (form.contactType === 'phone' && !form.phoneNumber) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (form.contactType === 'email' && !form.email) {
    ElMessage.warning('请输入邮箱')
    return
  }
  
  submitting.value = true
  try {
    await authApi.forgotPassword({
      userNumber: form.userNumber,
      phoneNumber: form.contactType === 'phone' ? form.phoneNumber : undefined,
      email: form.contactType === 'email' ? form.email : undefined,
      newPassword: form.newPassword
    })
    
    ElMessage.success('密码重置成功，请使用新密码登录')
    // 延迟跳转到登录页
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (error: any) {
    ElMessage.error(error.message || '密码重置失败')
  } finally {
    submitting.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<style scoped>
.forgot-password {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.forgot-panel {
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

.illustration {
  max-width: 100%;
  height: 360px;
  object-fit: contain;
}

.panel-right {
  padding: 48px 56px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
  margin-bottom: 24px;
}

.logo {
  width: 56px;
  height: 56px;
  object-fit: contain;
}

.brand-name {
  font-size: 22px;
  font-weight: 700;
}

.forgot-form {
  max-width: 440px;
  margin: 0 auto;
  width: 100%;
}

.contact-type-group {
  width: 100%;
  display: flex;
  justify-content: space-around;
}

.form-footer {
  text-align: center;
  margin-bottom: 16px;
}

.submit-btn {
  width: 100%;
}
</style>


