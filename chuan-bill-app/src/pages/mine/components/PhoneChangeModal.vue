<script setup lang="ts">
defineOptions({
  name: 'PhoneChangeModal',
})

const modelValue = defineModel<boolean>({ default: false })
const userStore = useUserStore()
const toast = useGlobalToast()

// 模式：bind=绑定手机, code=验证码换绑, password=密码换绑
type Mode = 'bind' | 'code' | 'password'
const mode = ref<Mode>('code')
const hasPassword = ref(false)
const loading = ref(false)

// 绑定手机表单
const bindForm = ref({
  phone: '',
  code: '',
})

// 验证码换绑表单
const codeForm = ref({
  newPhone: '',
  oldPhoneCode: '',
  newPhoneCode: '',
})

// 密码换绑表单
const passwordForm = ref({
  newPhone: '',
  password: '',
  newPhoneCode: '',
})

// 倒计时（当前手机）
const oldPhoneCountdown = ref(0)
const oldPhoneSending = ref(false)
let oldPhoneTimer: ReturnType<typeof setInterval> | null = null

// 倒计时（新手机）
const newPhoneCountdown = ref(0)
const newPhoneSending = ref(false)
let newPhoneTimer: ReturnType<typeof setInterval> | null = null

// 手机号格式校验
function isValidPhone(phone: string) {
  return /^1\d{10}$/.test(phone)
}

// 监听弹框打开
watch(modelValue, async (val) => {
  if (val) {
    // 判断初始模式
    if (!userStore.phone) {
      mode.value = 'bind'
    }
    else {
      mode.value = 'code'
      // 查询是否有密码
      try {
        const res = await Apis.user.hasPassword()
        hasPassword.value = res.data ?? false
      }
      catch {
        hasPassword.value = false
      }
    }
  }
  else {
    resetForms()
  }
})

// 重置表单
function resetForms() {
  bindForm.value = { phone: '', code: '' }
  codeForm.value = { newPhone: '', oldPhoneCode: '', newPhoneCode: '' }
  passwordForm.value = { newPhone: '', password: '', newPhoneCode: '' }
  if (oldPhoneTimer) {
    clearInterval(oldPhoneTimer)
    oldPhoneTimer = null
  }
  if (newPhoneTimer) {
    clearInterval(newPhoneTimer)
    newPhoneTimer = null
  }
  oldPhoneCountdown.value = 0
  newPhoneCountdown.value = 0
}

onUnload(() => {
  if (oldPhoneTimer) {
    clearInterval(oldPhoneTimer)
    oldPhoneTimer = null
  }
  if (newPhoneTimer) {
    clearInterval(newPhoneTimer)
    newPhoneTimer = null
  }
})

// 发送验证码到当前手机
async function sendCodeToOldPhone() {
  if (oldPhoneCountdown.value > 0 || oldPhoneSending.value)
    return
  if (!userStore.phone) {
    toast.warning('未绑定手机号')
    return
  }

  oldPhoneSending.value = true
  try {
    const res = await Apis.user.getPhoneCode()
    if (res.success) {
      toast.success('验证码已发送')
      oldPhoneCountdown.value = 60
      oldPhoneTimer = setInterval(() => {
        oldPhoneCountdown.value--
        if (oldPhoneCountdown.value <= 0 && oldPhoneTimer) {
          clearInterval(oldPhoneTimer)
          oldPhoneTimer = null
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
    oldPhoneSending.value = false
  }
}

// 发送验证码到新手机
async function sendCodeToNewPhone(phone: string) {
  if (newPhoneCountdown.value > 0 || newPhoneSending.value)
    return
  if (!isValidPhone(phone)) {
    toast.warning('请输入正确的手机号')
    return
  }

  newPhoneSending.value = true
  try {
    const res = await Apis.auth.sendCode({ data: { phone } })
    if (res.success) {
      toast.success('验证码已发送')
      newPhoneCountdown.value = 60
      newPhoneTimer = setInterval(() => {
        newPhoneCountdown.value--
        if (newPhoneCountdown.value <= 0 && newPhoneTimer) {
          clearInterval(newPhoneTimer)
          newPhoneTimer = null
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
    newPhoneSending.value = false
  }
}

// 绑定手机号
async function handleBindPhone() {
  if (!bindForm.value.phone) {
    toast.warning('请输入手机号')
    return
  }
  if (!isValidPhone(bindForm.value.phone)) {
    toast.warning('请输入正确的手机号')
    return
  }
  if (!bindForm.value.code) {
    toast.warning('请输入验证码')
    return
  }

  loading.value = true
  try {
    const res = await Apis.user.bindPhone({
      data: {
        phone: bindForm.value.phone,
        code: bindForm.value.code,
      },
    })
    if (res.success) {
      toast.success('绑定成功')
      modelValue.value = false
      // 刷新用户信息
      userStore.getProfile()
    }
    else {
      toast.error(res.message || '绑定失败')
    }
  }
  catch {
    toast.error('绑定失败，请重试')
  }
  finally {
    loading.value = false
  }
}

// 通过验证码换绑手机
async function handleUpdateByCode() {
  if (!codeForm.value.newPhone) {
    toast.warning('请输入新手机号')
    return
  }
  if (!isValidPhone(codeForm.value.newPhone)) {
    toast.warning('请输入正确的新手机号')
    return
  }
  if (!codeForm.value.oldPhoneCode) {
    toast.warning('请输入当前手机验证码')
    return
  }
  if (!codeForm.value.newPhoneCode) {
    toast.warning('请输入新手机验证码')
    return
  }

  loading.value = true
  try {
    const res = await Apis.user.updatePhoneByCode({
      data: {
        oldPhoneCode: codeForm.value.oldPhoneCode,
        newPhone: codeForm.value.newPhone,
        newPhoneCode: codeForm.value.newPhoneCode,
      },
    })
    if (res.success) {
      toast.success('手机号更换成功')
      modelValue.value = false
      userStore.getProfile()
    }
    else {
      toast.error(res.message || '更换失败')
    }
  }
  catch {
    toast.error('更换失败，请重试')
  }
  finally {
    loading.value = false
  }
}

// 通过密码换绑手机
async function handleUpdateByPassword() {
  if (!passwordForm.value.newPhone) {
    toast.warning('请输入新手机号')
    return
  }
  if (!isValidPhone(passwordForm.value.newPhone)) {
    toast.warning('请输入正确的新手机号')
    return
  }
  if (!passwordForm.value.password) {
    toast.warning('请输入密码')
    return
  }
  if (!passwordForm.value.newPhoneCode) {
    toast.warning('请输入新手机验证码')
    return
  }

  loading.value = true
  try {
    const res = await Apis.user.updatePhoneByPassword({
      data: {
        password: passwordForm.value.password,
        newPhone: passwordForm.value.newPhone,
        newPhoneCode: passwordForm.value.newPhoneCode,
      },
    })
    if (res.success) {
      toast.success('手机号更换成功')
      modelValue.value = false
      userStore.getProfile()
    }
    else {
      toast.error(res.message || '更换失败')
    }
  }
  catch {
    toast.error('更换失败，请重试')
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <wd-action-sheet
    v-model="modelValue"
    :title="mode === 'bind' ? '绑定手机号' : '更换手机号'"
    :z-index="999"
    safe-area-inset-bottom
    :close-on-click-modal="false"
  >
    <view class="p-4">
      <!-- 绑定手机号模式 -->
      <view v-if="mode === 'bind'">
        <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            您还没有绑定手机号，请先绑定手机号
          </text>
        </view>
        <view class="mb-4">
          <wd-input
            v-model="bindForm.phone"
            placeholder="请输入手机号"
            type="number"
            :maxlength="11"
            no-border
          />
        </view>
        <view class="mb-4 flex items-center gap-3">
          <wd-input
            v-model="bindForm.code"
            placeholder="请输入验证码"
            :maxlength="6"
            type="number"
            no-border
            custom-class="flex-1"
          />
          <wd-button
            :disabled="newPhoneCountdown > 0 || !isValidPhone(bindForm.phone)"
            size="small"
            @click="sendCodeToNewPhone(bindForm.phone)"
          >
            {{ newPhoneCountdown > 0 ? `${newPhoneCountdown}s` : '发送验证码' }}
          </wd-button>
        </view>
        <wd-button type="primary" block :loading="loading" @click="handleBindPhone">
          确认绑定
        </wd-button>
      </view>

      <!-- 验证码换绑模式 -->
      <view v-if="mode === 'code'">
        <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            当前手机号：{{ userStore.phone ? userStore.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
          </text>
        </view>
        <view class="mb-4">
          <wd-input
            v-model="codeForm.newPhone"
            placeholder="请输入新手机号"
            type="number"
            :maxlength="11"
            no-border
          />
        </view>
        <!-- 当前手机验证码 -->
        <view class="mb-4 flex items-center gap-3">
          <wd-input
            v-model="codeForm.oldPhoneCode"
            placeholder="当前手机验证码"
            :maxlength="6"
            type="number"
            no-border
            custom-class="flex-1"
          />
          <wd-button
            :disabled="oldPhoneCountdown > 0"
            size="small"
            @click="sendCodeToOldPhone"
          >
            {{ oldPhoneCountdown > 0 ? `${oldPhoneCountdown}s` : '发送验证码' }}
          </wd-button>
        </view>
        <!-- 新手机验证码 -->
        <view class="mb-4 flex items-center gap-3">
          <wd-input
            v-model="codeForm.newPhoneCode"
            placeholder="新手机验证码"
            :maxlength="6"
            type="number"
            no-border
            custom-class="flex-1"
          />
          <wd-button
            :disabled="newPhoneCountdown > 0 || !isValidPhone(codeForm.newPhone)"
            size="small"
            @click="sendCodeToNewPhone(codeForm.newPhone)"
          >
            {{ newPhoneCountdown > 0 ? `${newPhoneCountdown}s` : '发送验证码' }}
          </wd-button>
        </view>
        <wd-button type="primary" block :loading="loading" @click="handleUpdateByCode">
          确认更换
        </wd-button>
        <!-- 切换到密码模式 -->
        <view v-if="hasPassword" class="mt-4 text-center">
          <text class="text-sm text-primary" @click="mode = 'password'">
            当前手机不可用？
          </text>
        </view>
      </view>

      <!-- 密码换绑模式 -->
      <view v-if="mode === 'password'">
        <view class="mb-4">
          <wd-input
            v-model="passwordForm.newPhone"
            placeholder="请输入新手机号"
            type="number"
            :maxlength="11"
            no-border
          />
        </view>
        <view class="mb-4">
          <wd-input
            v-model="passwordForm.password"
            placeholder="请输入登录密码"
            show-password
            :maxlength="20"
            no-border
          />
        </view>
        <view class="mb-4 flex items-center gap-3">
          <wd-input
            v-model="passwordForm.newPhoneCode"
            placeholder="新手机验证码"
            :maxlength="6"
            type="number"
            no-border
            custom-class="flex-1"
          />
          <wd-button
            :disabled="newPhoneCountdown > 0 || !isValidPhone(passwordForm.newPhone)"
            size="small"
            @click="sendCodeToNewPhone(passwordForm.newPhone)"
          >
            {{ newPhoneCountdown > 0 ? `${newPhoneCountdown}s` : '发送验证码' }}
          </wd-button>
        </view>
        <wd-button type="primary" block :loading="loading" @click="handleUpdateByPassword">
          确认更换
        </wd-button>
        <!-- 切换回验证码模式 -->
        <view class="mt-4 text-center">
          <text class="text-sm text-primary" @click="mode = 'code'">
            使用验证码修改手机号
          </text>
        </view>
      </view>
    </view>
  </wd-action-sheet>
</template>
