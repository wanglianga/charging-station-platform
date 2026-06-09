<template>
  <div class="min-h-screen flex items-center justify-center" style="background: linear-gradient(135deg, #0F4C45 0%, #0F766E 50%, #14B8A6 100%)">
    <div class="w-full max-w-md">
      <div class="bg-white rounded-2xl shadow-2xl p-8">
        <div class="flex items-center justify-center gap-2 mb-8">
          <Zap class="w-8 h-8 text-teal-700" />
          <h1 class="text-2xl font-bold" style="color: var(--color-primary)">充电站管理平台</h1>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="UserIcon" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="LockIcon" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="w-full" :loading="loading" @click="handleLogin" style="height: 44px; border-radius: 8px">
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="text-center text-sm text-gray-400 mt-4">
          演示账号: admin / 123456
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User as UserIcon, Lock as LockIcon, Zap } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { LoginResponse, Role } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const DEMO_ACCOUNTS: Record<string, { password: string; role: Role; name: string; id: number }> = {
  admin: { password: '123456', role: 'OPERATOR', name: '管理员', id: 1 },
  engineer: { password: '123456', role: 'ENGINEER', name: '张工程师', id: 2 },
  owner: { password: '123456', role: 'CAR_OWNER', name: '李车主', id: 3 },
  siteowner: { password: '123456', role: 'SITE_OWNER', name: '王场地方', id: 4 },
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await userStore.login(form.username, form.password)
    const roleRoute: Record<string, string> = {
      OPERATOR: '/dashboard',
      ENGINEER: '/dashboard',
      CAR_OWNER: '/map',
      SITE_OWNER: '/dashboard',
    }
    router.push(roleRoute[data.user.role] || '/dashboard')
    ElMessage.success('登录成功')
  } catch {
    const demo = DEMO_ACCOUNTS[form.username]
    if (demo && demo.password === form.password) {
      const mockData: LoginResponse = {
        token: 'demo-token-' + Date.now(),
        user: { id: demo.id, username: form.username, role: demo.role, name: demo.name },
      }
      userStore.setAuth(mockData)
      const roleRoute: Record<string, string> = {
        OPERATOR: '/dashboard',
        ENGINEER: '/dashboard',
        CAR_OWNER: '/map',
        SITE_OWNER: '/dashboard',
      }
      router.push(roleRoute[demo.role] || '/dashboard')
      ElMessage.success('演示模式登录成功')
    } else {
      ElMessage.error('用户名或密码错误')
    }
  } finally {
    loading.value = false
  }
}
</script>
