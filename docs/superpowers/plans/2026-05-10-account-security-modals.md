# 账号安全弹框重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将账号安全页面的修改密码和修改手机号弹框拆分为独立组件，修复验证码发送逻辑，支持多种用户状态。

**Architecture:** 使用 `wd-action-sheet` 组件替代 `wd-popup`，通过 `defineModel<boolean>()` 控制显隐。密码修改和手机号修改各为独立组件，各自管理状态和 API 调用。

**Tech Stack:** Vue 3 Composition API、TypeScript、wot-design-uni (wd-action-sheet)、UnoCSS、Alova.js

---

## 文件结构

| 操作 | 文件路径 | 说明 |
|------|----------|------|
| 修改 | `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/UserServiceImpl.java:248` | 修复 getPhoneCode bug |
| 创建 | `chuan-bill-app/src/pages/mine/components/PasswordChangeModal.vue` | 修改密码弹框组件 |
| 创建 | `chuan-bill-app/src/pages/mine/components/PhoneChangeModal.vue` | 修改手机号弹框组件 |
| 修改 | `chuan-bill-app/src/pages/mine/account.vue` | 移除弹框逻辑，引入新组件 |

---

### Task 1: 修复后端 getPhoneCode bug

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/UserServiceImpl.java:248`

- [ ] **Step 1: 修复条件取反 bug**

将 line 248 的 `if (!CharSequenceUtil.isEmpty(user.getPhone()))` 改为 `if (CharSequenceUtil.isEmpty(user.getPhone()))`。

原代码在用户有手机号时抛出异常，但该接口的目的是向已有手机号发送验证码。

```java
// 修改前
if (!CharSequenceUtil.isEmpty(user.getPhone())) {
    throw new BusinessException(ResultEnum.PHONE_NOT_FOUND);
}

// 修改后
if (CharSequenceUtil.isEmpty(user.getPhone())) {
    throw new BusinessException(ResultEnum.PHONE_NOT_FOUND);
}
```

- [ ] **Step 2: 验证后端编译**

Run: `cd chuan-bill-server && mvn compile -q`
Expected: 编译成功，无错误

- [ ] **Step 3: 提交**

```bash
cd chuan-bill-server
git add src/main/java/com/samoy/chuanbillserver/service/impl/UserServiceImpl.java
git commit -m "fix(user): 修复 getPhoneCode 接口条件取反 bug"
```

---

### Task 2: 创建 PasswordChangeModal 组件

**Files:**
- Create: `chuan-bill-app/src/pages/mine/components/PasswordChangeModal.vue`

- [ ] **Step 1: 创建组件文件**

创建 `PasswordChangeModal.vue`，实现以下功能：
- 使用 `wd-action-sheet` 作为弹框容器
- 使用 `defineModel<boolean>()` 控制显隐
- 使用 `wd-segmented` 组件实现 Tab 切换（非自定义 Tab）
- 打开时查询 `user.hasPassword` 决定显示模式
- 有密码时显示「密码验证/验证码验证」双 Tab
- 无密码时仅显示验证码 Tab
- 微信小程序端需在弹框打开后调用 `segmentedRef.updateActiveStyle()` 更新样式

```vue
<script setup lang="ts">
import type { SegmentedInstance } from 'wot-design-uni/components/wd-segmented/types'

defineOptions({
  name: 'PasswordChangeModal',
})

const modelValue = defineModel<boolean>()
const userStore = useUserStore()
const toast = useGlobalToast()
const router = useRouter()

// 状态
const activeTab = ref('验证码验证')
const hasPassword = ref(false)
const loading = ref(false)
const segmentedRef = ref<SegmentedInstance>()

// Tab 选项
const tabOptions = ['密码验证', '验证码验证']

// 密码验证表单
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

// 验证码验证表单
const codeForm = ref({
  code: '',
  newPassword: '',
  confirmPassword: '',
})

// 倒计时
const countdown = ref(0)
const sending = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

// 密码强度
const passwordStrength = computed(() => {
  const pwd = activeTab.value === '密码验证' ? passwordForm.value.newPassword : codeForm.value.newPassword
  if (!pwd) return 0
  let strength = 0
  if (pwd.length >= 6) strength++
  if (/[a-z]/i.test(pwd) && /\d/.test(pwd)) strength++
  if (/[^a-z0-9]/i.test(pwd)) strength++
  if (pwd.length >= 10) strength++
  return Math.min(strength, 3)
})

const strengthText = ['弱', '中', '强', '极强']
const strengthColor = ['text-red-500', 'text-yellow-500', 'text-green-500', 'text-green-600']

// 监听弹框打开
watch(modelValue, async (val) => {
  if (val) {
    // 查询是否有密码
    try {
      const res = await Apis.user.hasPassword()
      hasPassword.value = res.data ?? false
      // 无密码时默认选中验证码 Tab
      if (!hasPassword.value) {
        activeTab.value = '验证码验证'
      }
    } catch {
      hasPassword.value = false
    }
    // 微信小程序端需要在弹框打开后更新分段器样式
    nextTick(() => {
      segmentedRef.value?.updateActiveStyle()
    })
  } else {
    // 关闭时重置表单
    resetForms()
  }
})

// 重置表单
function resetForms() {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  codeForm.value = { code: '', newPassword: '', confirmPassword: '' }
  activeTab.value = '验证码验证'
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  countdown.value = 0
}

onUnload(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})

// 发送验证码到当前手机
async function sendCodeToCurrentPhone() {
  if (countdown.value > 0 || sending.value) return
  if (!userStore.phone) {
    toast.warning('未绑定手机号')
    return
  }

  sending.value = true
  try {
    const res = await Apis.user.getPhoneCode()
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
    } else {
      toast.error(res.message || '发送失败')
    }
  } catch {
    toast.error('发送失败，请重试')
  } finally {
    sending.value = false
  }
}

// 通过旧密码修改
async function handleUpdateByPassword() {
  if (!passwordForm.value.oldPassword) {
    toast.warning('请输入旧密码')
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

  loading.value = true
  try {
    const res = await Apis.user.updatePasswordByOld({
      data: {
        oldPassword: passwordForm.value.oldPassword,
        newPassword: passwordForm.value.newPassword,
      },
    })
    if (res.success) {
      toast.success('密码修改成功，请重新登录')
      modelValue.value = false
      setTimeout(() => {
        userStore.logout()
        router.replace('/pages/bill/index')
      }, 1500)
    } else {
      toast.error(res.message || '修改失败')
    }
  } catch {
    toast.error('修改失败，请重试')
  } finally {
    loading.value = false
  }
}

// 通过验证码修改
async function handleUpdateByCode() {
  if (!codeForm.value.code) {
    toast.warning('请输入验证码')
    return
  }
  if (!codeForm.value.newPassword) {
    toast.warning('请输入新密码')
    return
  }
  if (codeForm.value.newPassword.length < 6) {
    toast.warning('密码长度不能少于6位')
    return
  }
  if (codeForm.value.newPassword !== codeForm.value.confirmPassword) {
    toast.warning('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    const res = await Apis.user.updatePasswordByCode({
      data: {
        code: codeForm.value.code,
        newPassword: codeForm.value.newPassword,
      },
    })
    if (res.success) {
      toast.success('密码修改成功，请重新登录')
      modelValue.value = false
      setTimeout(() => {
        userStore.logout()
        router.replace('/pages/bill/index')
      }, 1500)
    } else {
      toast.error(res.message || '修改失败')
    }
  } catch {
    toast.error('修改失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <wd-action-sheet
    v-model="modelValue"
    title="修改密码"
    :z-index="999"
    safe-area-inset-bottom
    :close-on-click-modal="false"
  >
    <view class="p-4">
      <!-- Tab 切换（有密码时显示） -->
      <view v-if="hasPassword" class="mb-4">
        <wd-segmented
          ref="segmentedRef"
          :options="tabOptions"
          v-model:value="activeTab"
          size="small"
        />
      </view>

      <!-- 密码验证 Tab -->
      <view v-if="activeTab === '密码验证'">
        <view class="mb-4">
          <wd-input
            v-model="passwordForm.oldPassword"
            placeholder="请输入旧密码"
            show-password
            :maxlength="20"
            no-border
          />
        </view>
        <view class="mb-4">
          <wd-input
            v-model="passwordForm.newPassword"
            placeholder="请输入新密码"
            show-password
            :maxlength="20"
            no-border
          />
          <view v-if="passwordForm.newPassword" class="mt-2 flex items-center gap-2">
            <text class="text-xs text-gray-500">密码强度：</text>
            <text class="text-xs" :class="strengthColor[passwordStrength - 1]">
              {{ strengthText[passwordStrength - 1] || '弱' }}
            </text>
          </view>
        </view>
        <view class="mb-4">
          <wd-input
            v-model="passwordForm.confirmPassword"
            placeholder="请确认新密码"
            show-password
            :maxlength="20"
            no-border
          />
        </view>
        <text class="mb-4 block text-xs text-gray-400">密码需6-20位，建议包含字母和数字</text>
        <wd-button type="primary" block :loading="loading" @click="handleUpdateByPassword">
          确认修改
        </wd-button>
      </view>

      <!-- 验证码验证 Tab -->
      <view v-if="activeTab === '验证码验证'">
        <!-- 当前手机号 -->
        <view class="mb-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-800">
          <text class="text-sm text-gray-600 dark:text-gray-400">
            手机号：{{ userStore.phone ? userStore.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
          </text>
        </view>
        <!-- 验证码 -->
        <view class="mb-4 flex items-center gap-3">
          <wd-input
            v-model="codeForm.code"
            placeholder="请输入验证码"
            :maxlength="6"
            type="number"
            no-border
            custom-class="flex-1"
          />
          <wd-button
            :disabled="countdown > 0 || !userStore.phone"
            size="small"
            @click="sendCodeToCurrentPhone"
          >
            {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
          </wd-button>
        </view>
        <!-- 新密码 -->
        <view class="mb-4">
          <wd-input
            v-model="codeForm.newPassword"
            placeholder="请输入新密码"
            show-password
            :maxlength="20"
            no-border
          />
          <view v-if="codeForm.newPassword" class="mt-2 flex items-center gap-2">
            <text class="text-xs text-gray-500">密码强度：</text>
            <text class="text-xs" :class="strengthColor[passwordStrength - 1]">
              {{ strengthText[passwordStrength - 1] || '弱' }}
            </text>
          </view>
        </view>
        <!-- 确认密码 -->
        <view class="mb-4">
          <wd-input
            v-model="codeForm.confirmPassword"
            placeholder="请确认新密码"
            show-password
            :maxlength="20"
            no-border
          />
        </view>
        <text class="mb-4 block text-xs text-gray-400">密码需6-20位，建议包含字母和数字</text>
        <wd-button type="primary" block :loading="loading" @click="handleUpdateByCode">
          确认修改
        </wd-button>
      </view>
    </view>
  </wd-action-sheet>
</template>
```

- [ ] **Step 2: 验证组件创建成功**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: 类型检查通过（可能有未使用警告，但无错误）

- [ ] **Step 3: 提交**

```bash
cd chuan-bill-app
git add src/pages/mine/components/PasswordChangeModal.vue
git commit -m "feat(mine): 添加修改密码弹框组件"
```

---

### Task 3: 创建 PhoneChangeModal 组件

**Files:**
- Create: `chuan-bill-app/src/pages/mine/components/PhoneChangeModal.vue`

- [ ] **Step 1: 创建组件文件**

创建 `PhoneChangeModal.vue`，实现以下功能：
- 使用 `wd-action-sheet` 作为弹框容器
- 支持三种模式：bind（绑定手机）、code（验证码换绑）、password（密码换绑）
- 打开时根据用户状态自动选择模式
- code 模式下需要两个验证码输入框（当前手机和新手机）

```vue
<script setup lang="ts">
defineOptions({
  name: 'PhoneChangeModal',
})

const modelValue = defineModel<boolean>()
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
    } else {
      mode.value = 'code'
      // 查询是否有密码
      try {
        const res = await Apis.user.hasPassword()
        hasPassword.value = res.data ?? false
      } catch {
        hasPassword.value = false
      }
    }
  } else {
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
  if (oldPhoneCountdown.value > 0 || oldPhoneSending.value) return
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
    } else {
      toast.error(res.message || '发送失败')
    }
  } catch {
    toast.error('发送失败，请重试')
  } finally {
    oldPhoneSending.value = false
  }
}

// 发送验证码到新手机
async function sendCodeToNewPhone(phone: string) {
  if (newPhoneCountdown.value > 0 || newPhoneSending.value) return
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
    } else {
      toast.error(res.message || '发送失败')
    }
  } catch {
    toast.error('发送失败，请重试')
  } finally {
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
    } else {
      toast.error(res.message || '绑定失败')
    }
  } catch {
    toast.error('绑定失败，请重试')
  } finally {
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
    } else {
      toast.error(res.message || '更换失败')
    }
  } catch {
    toast.error('更换失败，请重试')
  } finally {
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
    } else {
      toast.error(res.message || '更换失败')
    }
  } catch {
    toast.error('更换失败，请重试')
  } finally {
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
```

- [ ] **Step 2: 验证组件创建成功**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: 类型检查通过

- [ ] **Step 3: 提交**

```bash
cd chuan-bill-app
git add src/pages/mine/components/PhoneChangeModal.vue
git commit -m "feat(mine): 添加修改手机号弹框组件"
```

---

### Task 4: 更新 account.vue 使用新组件

**Files:**
- Modify: `chuan-bill-app/src/pages/mine/account.vue`

- [ ] **Step 1: 重构 account.vue**

移除所有弹框相关逻辑，引入新组件。保留页面展示和跳转逻辑。

```vue
<script setup lang="ts">
definePage({
  name: 'account',
  layout: 'default',
  style: {
    navigationBarTitleText: '账号与安全',
  },
})

const userStore = useUserStore()
const toast = useGlobalToast()
const message = useGlobalMessage()

onLoad(() => {
  if (!userStore.isLoggedIn) {
    userStore.requireAuth(() => {})
  }
})

// 弹框状态
const showPasswordModal = ref(false)
const showPhoneModal = ref(false)

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
        @click="showPhoneModal = true"
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
        @click="showPasswordModal = true"
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

    <!-- 修改密码弹框 -->
    <PasswordChangeModal v-model="showPasswordModal" />

    <!-- 修改手机号弹框 -->
    <PhoneChangeModal v-model="showPhoneModal" />
  </view>
</template>
```

- [ ] **Step 2: 验证重构正确性**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: 类型检查通过

- [ ] **Step 3: 提交**

```bash
cd chuan-bill-app
git add src/pages/mine/account.vue
git commit -m "refactor(mine): 重构账号安全页面使用独立弹框组件"
```

---

### Task 5: 更新项目记忆

**Files:**
- Modify: `memory/MEMORY.md`
- Create: `memory/feedback_alova_api_fix.md`

- [ ] **Step 1: 创建记忆文件**

创建 `feedback_alova_api_fix.md` 记录 alova-gen 后必须执行 alova-api-fix 的工作流。

```markdown
---
name: alova-api-fix-workflow
description: 每次使用 npm run alova-gen 生成接口定义后，必须使用 alova-api-fix 修复接口定义规范
type: feedback
---

每次使用 `npm run alova-gen` 生成 API 接口定义后，必须使用 `alova-api-fix` skill 修复已生成的接口定义规范。

**Why:** alova-gen 自动生成的接口定义可能不符合项目规范，需要通过 alova-api-fix 进行规范化处理。

**How to apply:** 在执行 `npm run alova-gen` 后，立即调用 `alova-api-fix` skill 修复接口定义。
```

- [ ] **Step 2: 更新 MEMORY.md**

在 MEMORY.md 中添加对新记忆文件的引用。

- [ ] **Step 3: 提交**

```bash
git add memory/
git commit -m "chore: 更新项目记忆 - alova-api-fix 工作流"
```

---

## 自查清单

- [ ] 所有 spec 需求都有对应任务
- [ ] 无 TBD/TODO 占位符（除已标注的后端待实现功能）
- [ ] 类型、方法签名、属性名称在各任务间保持一致
- [ ] 所有步骤包含完整代码或具体命令
- [ ] 后端 bug 修复在最前面执行
