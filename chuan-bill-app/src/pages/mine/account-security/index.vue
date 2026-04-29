<script setup lang="ts">
definePage({
  name: 'account-security',
  layout: 'default',
  style: {
    navigationBarTitleText: '账号与安全',
  },
})

const userStore = useUserStore()
const toast = useGlobalToast()
const message = useGlobalMessage()
const router = useRouter()

// 密码表单
const showPasswordForm = ref(false)
const passwordForm = ref({
  code: '',
  newPassword: '',
  confirmPassword: '',
})

// 手机号表单
const showPhoneForm = ref(false)
const phoneForm = ref({
  newPhone: '',
  code: '',
})

// 倒计时
const countdown = ref(0)
const sending = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

// 密码强度
const passwordStrength = computed(() => {
  const pwd = passwordForm.value.newPassword
  if (!pwd)
    return 0
  let strength = 0
  if (pwd.length >= 6)
    strength++
  if (/[a-z]/i.test(pwd) && /\d/.test(pwd))
    strength++
  if (/[^a-z0-9]/i.test(pwd))
    strength++
  if (pwd.length >= 10)
    strength++
  return Math.min(strength, 3)
})

const strengthText = ['弱', '中', '强', '极强']
const strengthColor = ['text-red-500', 'text-yellow-500', 'text-green-500', 'text-green-600']

onUnload(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})

// 发送验证码
async function sendCode(phone: string) {
  if (countdown.value > 0 || sending.value)
    return

  sending.value = true
  try {
    const res = await Apis.auth.sendCode({ data: { phone } })
    if (res.success) {
      toast.success('验证码已发送')
      countdown.value = 60
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0 && timer) {
          clearInterval(timer)
          timer = null
        }
      }, 1000)
    }
    else {
      toast.error(res.message || '发送失败')
    }
  }
  catch {
    toast.error('发送失败，请重试')
  }
  finally {
    sending.value = false
  }
}

// 修改密码
async function handleUpdatePassword() {
  if (!passwordForm.value.code) {
    toast.warning('请输入验证码')
    return
  }
  if (!passwordForm.value.newPassword) {
    toast.warning('请输入新密码')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    toast.warning('密码长度不能少于6位')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    toast.warning('两次输入的密码不一致')
    return
  }

  try {
    const res = await Apis.user.updatePasswordByCode({
      data: {
        phone: userStore.phone!,
        code: passwordForm.value.code,
        newPassword: passwordForm.value.newPassword,
      },
    })
    if (res.success) {
      toast.success('密码修改成功，请重新登录')
      showPasswordForm.value = false
      setTimeout(() => {
        userStore.logout()
        router.replace('/pages/bill/index')
      }, 1500)
    }
    else {
      toast.error(res.message || '修改失败')
    }
  }
  catch {
    toast.error('修改失败，请重试')
  }
}

// 修改手机号
async function handleUpdatePhone() {
  if (!phoneForm.value.newPhone) {
    toast.warning('请输入新手机号')
    return
  }
  if (!phoneForm.value.code) {
    toast.warning('请输入验证码')
    return
  }

  // TODO: 调用后端修改手机号接口
  toast.info('功能开发中')
}

// 注销账号
function handleDeleteAccount() {
  message.confirm({
    title: '注销账号',
    msg: '注销后，所有数据将被永久删除且无法恢复。确定要注销吗？',
    beforeConfirm: async ({ resolve }) => {
      // TODO: 调用后端注销账号接口
      toast.info('功能开发中')
      resolve(false)
    },
  })
}

// 设备管理
function goToDeviceManagement() {
  // TODO: 跳转到设备管理页面
  toast.info('功能开发中')
}
</script>

<template>
  <view class="box-border flex flex-col gap-4 p-4">
    <!-- 账号信息 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        账号信息
      </view>
      <!-- 手机号 -->
      <view
        class="flex items-center justify-between px-4 pb-4"
        @click="showPhoneForm = true"
      >
        <text class="text-sm">
          手机号
        </text>
        <view class="flex items-center gap-1">
          <text class="text-sm text-gray-400">
            {{ userStore.phone ? userStore.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
          </text>
          <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
        </view>
      </view>
      <!-- 修改密码 -->
      <view
        class="flex items-center justify-between border-t border-gray-100 px-4 py-4 dark:border-gray-700"
        @click="showPasswordForm = true"
      >
        <text class="text-sm">
          修改密码
        </text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
    </view>

    <!-- 安全设置 -->
    <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        安全设置
      </view>
      <!-- 登录设备 -->
      <view
        class="flex items-center justify-between px-4 pb-4"
        @click="goToDeviceManagement"
      >
        <text class="text-sm">
          登录设备管理
        </text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
      <!-- 注销账号 -->
      <view
        class="flex items-center justify-between border-t border-gray-100 px-4 py-4 dark:border-gray-700"
        @click="handleDeleteAccount"
      >
        <text class="text-sm text-red-500">
          注销账号
        </text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
    </view>

    <!-- 修改密码弹窗 -->
    <wd-popup
      v-model="showPasswordForm"
      position="bottom"
      closable
      safe-area-inset-bottom
      custom-class="rounded-tl-2xl rounded-tr-2xl"
    >
      <view class="p-4">
        <view class="mb-4 text-center text-lg font-500">
          修改密码
        </view>
        <!-- 手机号 -->
        <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            手机号：{{ userStore.phone ? userStore.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
          </text>
        </view>
        <!-- 验证码 -->
        <view class="mb-4 flex items-center gap-3">
          <wd-input
            v-model="passwordForm.code"
            placeholder="请输入验证码"
            :maxlength="6"
            type="number"
            no-border
            custom-class="flex-1"
          />
          <wd-button
            :disabled="countdown > 0 || !userStore.phone"
            size="small"
            @click="sendCode(userStore.phone!)"
          >
            {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
          </wd-button>
        </view>
        <!-- 新密码 -->
        <view class="mb-4">
          <wd-input
            v-model="passwordForm.newPassword"
            placeholder="请输入新密码"
            show-password
            :maxlength="20"
            no-border
          />
          <view v-if="passwordForm.newPassword" class="mt-2 flex items-center gap-2">
            <text class="text-xs text-gray-500">
              密码强度：
            </text>
            <text class="text-xs" :class="strengthColor[passwordStrength - 1]">
              {{ strengthText[passwordStrength - 1] || '弱' }}
            </text>
          </view>
        </view>
        <!-- 确认密码 -->
        <view class="mb-4">
          <wd-input
            v-model="passwordForm.confirmPassword"
            placeholder="请确认新密码"
            show-password
            :maxlength="20"
            no-border
          />
        </view>
        <text class="mb-4 block text-xs text-gray-400">
          密码需6-20位，建议包含字母和数字
        </text>
        <wd-button type="primary" block @click="handleUpdatePassword">
          确认修改
        </wd-button>
      </view>
    </wd-popup>

    <!-- 修改手机号弹窗 -->
    <wd-popup
      v-model="showPhoneForm"
      position="bottom"
      closable
      safe-area-inset-bottom
      custom-class="rounded-tl-2xl rounded-tr-2xl"
    >
      <view class="p-4">
        <view class="mb-4 text-center text-lg font-500">
          更换手机号
        </view>
        <!-- 当前手机号 -->
        <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            当前手机号：{{ userStore.phone ? userStore.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
          </text>
        </view>
        <!-- 新手机号 -->
        <view class="mb-4">
          <wd-input
            v-model="phoneForm.newPhone"
            placeholder="请输入新手机号"
            type="number"
            :maxlength="11"
            no-border
          />
        </view>
        <!-- 验证码 -->
        <view class="mb-4 flex items-center gap-3">
          <wd-input
            v-model="phoneForm.code"
            placeholder="请输入验证码"
            :maxlength="6"
            type="number"
            no-border
            custom-class="flex-1"
          />
          <wd-button
            :disabled="countdown > 0 || !phoneForm.newPhone"
            size="small"
            @click="sendCode(phoneForm.newPhone)"
          >
            {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
          </wd-button>
        </view>
        <wd-button type="primary" block @click="handleUpdatePhone">
          确认更换
        </wd-button>
      </view>
    </wd-popup>
  </view>
</template>
