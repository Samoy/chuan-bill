# 账号注销功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现账号注销功能，移除设备管理入口，有手机号的用户可通过短信验证码注销账号。

**Architecture:** 后端新增注销接口（校验验证码 → 软删除 → 清除标识 → 登出），前端新建注销弹窗组件，复用现有 `wd-action-sheet` + 短信验证码模式。

**Tech Stack:** Spring Boot 3 / MyBatis-Plus / Sa-Token / Vue 3 / uni-app / wot-design-uni / Alova.js

---

## File Structure

### Backend (chuan-bill-server)
- **Create:** `src/main/java/com/samoy/chuanbillserver/dto/DeleteAccountDTO.java`
- **Modify:** `src/main/java/com/samoy/chuanbillserver/service/IUserService.java`
- **Modify:** `src/main/java/com/samoy/chuanbillserver/service/impl/UserServiceImpl.java`
- **Modify:** `src/main/java/com/samoy/chuanbillserver/controller/UserController.java`

### Frontend (chuan-bill-app)
- **Modify:** `src/pages/mine/account.vue`
- **Create:** `src/pages/mine/components/AccountDeletePopup.vue`

---

### Task 1: Create DeleteAccountDTO

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/DeleteAccountDTO.java`

- [ ] **Step 1: Create DTO class**

```java
package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "注销账号请求参数")
public class DeleteAccountDTO {

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "手机验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd chuan-bill-server && mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd chuan-bill-server
git add src/main/java/com/samoy/chuanbillserver/dto/DeleteAccountDTO.java
git commit -m "feat: 添加注销账号请求参数 DTO"
```

---

### Task 2: Add deleteAccount to Service Layer

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IUserService.java`
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/UserServiceImpl.java`

- [ ] **Step 1: Add method to IUserService interface**

在 `IUserService.java` 的 `bindPhone` 方法后添加：

```java
/**
 * 注销账号
 *
 * @param userId 用户ID
 * @param dto    注销请求参数
 */
void deleteAccount(String userId, DeleteAccountDTO dto);
```

- [ ] **Step 2: Implement in UserServiceImpl**

在 `UserServiceImpl.java` 中添加实现方法。需要注入 `StpUtil`（通过静态调用即可，无需注入）。在类末尾添加：

```java
@Override
public void deleteAccount(String userId, DeleteAccountDTO dto) {
    User user = this.getById(userId);
    if (user == null) {
        throw new BusinessException(ResultEnum.USER_NOT_FOUND);
    }

    // 校验验证码
    if (!verificationCodeService.verifyCode(user.getPhone(), dto.getCode())) {
        throw new BusinessException(ResultEnum.CAPTCHA_INVALID);
    }

    // 软删除 + 清除标识字段（避免重新登录产生重复记录）
    user.setDeleted(true);
    user.setPhone(user.getPhone() + "_deleted_" + userId);
    user.setOpenid(user.getOpenid() != null ? user.getOpenid() + "_deleted_" + userId : null);
    this.updateById(user);

    // 登出
    StpUtil.logout(userId);
}
```

确保 `UserServiceImpl` 中有以下 import（如果缺失则添加）：

```java
import cn.dev33.satoken.stp.StpUtil;
import com.samoy.chuanbillserver.dto.DeleteAccountDTO;
```

- [ ] **Step 3: Verify compilation**

Run: `cd chuan-bill-server && mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
cd chuan-bill-server
git add src/main/java/com/samoy/chuanbillserver/service/IUserService.java src/main/java/com/samoy/chuanbillserver/service/impl/UserServiceImpl.java
git commit -m "feat: 实现账号注销服务层逻辑"
```

---

### Task 3: Add deleteAccount Endpoint to Controller

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/UserController.java`

- [ ] **Step 1: Add endpoint method**

在 `UserController.java` 末尾（类的最后一个方法之后、类的关闭大括号之前）添加：

```java
@Operation(summary = "注销账号", description = "通过手机验证码验证身份后注销账号")
@PostMapping("/account/delete")
public Result<Void> deleteAccount(@Validated @RequestBody DeleteAccountDTO dto) {
    String userId = StpUtil.getLoginIdAsString();
    userService.deleteAccount(userId, dto);
    return Result.success();
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd chuan-bill-server && mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd chuan-bill-server
git add src/main/java/com/samoy/chuanbillserver/controller/UserController.java
git commit -m "feat: 添加注销账号接口 POST /user/account/delete"
```

---

### Task 4: Modify account.vue

**Files:**
- Modify: `chuan-bill-app/src/pages/mine/account.vue`

- [ ] **Step 1: Remove device management and add conditional rendering**

将整个 `<script setup>` 和 `<template>` 替换为以下内容：

```vue
<script setup lang="ts">
import AccountDeletePopup from './components/AccountDeletePopup.vue'
import PasswordChangePopup from './components/PasswordChangePopup.vue'
import PhoneChangePopup from './components/PhoneChangePopup.vue'

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
const showDeleteModal = ref(false)

// 注销账号 - 第一步：确认弹框
function handleDeleteAccount() {
  message.confirm({
    title: '注销账号',
    msg: '注销后，所有数据将被永久删除且无法恢复。确定要注销吗？',
    beforeConfirm: async ({ resolve }) => {
      resolve(true)
    },
    success: (res) => {
      if (res.action === 'confirm') {
        showDeleteModal.value = true
      }
    },
  })
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

    <!-- 安全设置（仅有手机号时显示） -->
    <view v-if="userStore.phone" class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <view class="p-4 text-xs text-gray-400 font-medium">
        安全设置
      </view>
      <!-- 注销账号 -->
      <view
        class="flex items-center justify-between px-4 py-4"
        @click="handleDeleteAccount"
      >
        <text class="text-sm text-red-500">
          注销账号
        </text>
        <view class="i-lucide:chevron-right h-4 w-4 text-gray-400" />
      </view>
    </view>

    <!-- 修改密码弹框 -->
    <PasswordChangePopup v-model="showPasswordModal" />

    <!-- 修改手机号弹框 -->
    <PhoneChangePopup v-model="showPhoneModal" />

    <!-- 注销账号弹框 -->
    <AccountDeletePopup v-model="showDeleteModal" />
  </view>
</template>
```

- [ ] **Step 2: Verify no TypeScript errors**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: No errors (AccountDeletePopup.vue may not exist yet, that's OK - will be created in next task)

- [ ] **Step 3: Commit**

```bash
cd chuan-bill-app
git add src/pages/mine/account.vue
git commit -m "feat: 移除设备管理入口，添加注销账号条件显示"
```

---

### Task 5: Create AccountDeletePopup.vue

**Files:**
- Create: `chuan-bill-app/src/pages/mine/components/AccountDeletePopup.vue`

- [ ] **Step 1: Create the popup component**

```vue
<script setup lang="ts">
defineOptions({ name: 'AccountDeleteModal' })

const modelValue = defineModel<boolean>({ default: false })
const userStore = useUserStore()
const toast = useGlobalToast()
const router = useRouter()

// 验证码
const code = ref('')
const countdown = ref(0)
const sending = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

// 提交状态
const loading = ref(false)

// 监听弹窗打开/关闭
watch(modelValue, (val) => {
  if (val) {
    // 打开时重置
    code.value = ''
    countdown.value = 0
    loading.value = false
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }
})

// 发送验证码
async function sendCode() {
  if (countdown.value > 0 || sending.value)
    return
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

// 提交注销
async function handleSubmit() {
  if (!code.value) {
    toast.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    const res = await Apis.user.accountDelete({ data: { code: code.value } })
    if (res.success) {
      toast.success('账号已注销')
      modelValue.value = false
      userStore.logout()
      router.replaceAll('/')
    }
    else {
      toast.error(res.message || '注销失败')
    }
  }
  catch {
    toast.error('注销失败，请重试')
  }
  finally {
    loading.value = false
  }
}

// 清除定时器
onUnload(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<template>
  <wd-action-sheet
    v-model="modelValue"
    title="注销账号"
    :z-index="999"
    safe-area-inset-bottom
    :close-on-click-modal="false"
  >
    <view class="p-4">
      <!-- 警告文案 -->
      <view class="mb-4 rounded-xl bg-red-50 p-3 dark:bg-red-900/20">
        <view class="flex items-start gap-2">
          <view class="i-lucide:alert-triangle mt-0.5 h-4 w-4 shrink-0 text-red-500" />
          <text class="text-sm text-red-600 dark:text-red-400">
            注销后，您的所有数据将被永久删除且无法恢复，请谨慎操作。
          </text>
        </view>
      </view>

      <!-- 手机号展示 -->
      <view class="mb-4 flex items-center gap-2 text-sm text-gray-500">
        <view class="i-lucide:smartphone h-4 w-4" />
        <text>验证手机号：</text>
        <text>{{ userStore.phone?.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') }}</text>
      </view>

      <!-- 验证码输入 -->
      <wd-input
        v-model="code"
        type="number"
        placeholder="请输入验证码"
        :maxlength="6"
        custom-class="login-input"
      >
        <template #prefix>
          <view class="i-lucide:message-square text-gray-400" />
        </template>
        <template #suffix>
          <view
            class="cursor-pointer text-sm"
            :class="countdown > 0 ? 'text-gray-400' : 'text-primary'"
            @click="sendCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </view>
        </template>
      </wd-input>

      <!-- 确认按钮 -->
      <wd-button
        type="error"
        round
        block
        :loading="loading"
        custom-class="mt-2"
        @click="handleSubmit"
      >
        确认注销
      </wd-button>
    </view>
  </wd-action-sheet>
</template>

<style lang="scss" scoped>
:deep(.login-input) {
  @apply rounded-2xl bg-gray-100 px-3 py-1 dark:bg-gray-700;

  &::after {
    display: none !important;
  }

  .wd-icon {
    background: none;
  }
}
</style>
```

- [ ] **Step 2: Verify no TypeScript errors**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: May show error for `Apis.user.accountDelete` not existing yet - this is expected, will be resolved after API generation in Task 6.

- [ ] **Step 3: Commit**

```bash
cd chuan-bill-app
git add src/pages/mine/components/AccountDeletePopup.vue
git commit -m "feat: 添加账号注销弹窗组件"
```

---

### Task 6: Generate API Definitions

**Files:**
- Regenerate: `chuan-bill-app/src/api/apiDefinitions.ts`
- Fix: `chuan-bill-app/src/api/globals.d.ts` (if needed)

- [ ] **Step 1: Start backend server (if not running)**

Run: `cd chuan-bill-server && mvn spring-boot:run -q`
Wait for startup, then verify: `curl -s http://localhost:8080/swagger-ui/index.html | head -5`

- [ ] **Step 2: Run alova-gen**

Run: `cd chuan-bill-app && pnpm alova-gen`
Expected: `apiDefinitions.ts` updated with new `user.accountDelete` method

- [ ] **Step 3: Run alova-api-fix**

Run: `cd chuan-bill-app && pnpm alova-api-fix`
Expected: Type definitions fixed (amount fields as strings, DTO field deduplication)

- [ ] **Step 4: Verify type check passes**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: No errors (the `Apis.user.accountDelete` call in AccountDeletePopup.vue should now resolve)

- [ ] **Step 5: Commit**

```bash
cd chuan-bill-app
git add src/api/apiDefinitions.ts src/api/globals.d.ts
git commit -m "chore: 重新生成 API 定义（含注销账号接口）"
```

---

### Task 7: Verify and Test

- [ ] **Step 1: Run frontend lint**

Run: `cd chuan-bill-app && pnpm lint:fix`
Expected: No errors

- [ ] **Step 2: Run frontend type check**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: No errors

- [ ] **Step 3: Manual test - H5 dev server**

Run: `cd chuan-bill-app && pnpm dev`
Open browser, navigate to "我的" → "账号与安全":
1. Verify "登录设备管理"入口已移除
2. Verify"安全设置"组仅在有手机号时显示
3. 点击"注销账号" → 确认弹框 → 验证码弹窗 → 获取验证码 → 输入 → 确认注销

- [ ] **Step 4: Commit all remaining changes**

```bash
cd chuan-bill-app
git add -A
git commit -m "feat: 完成账号注销功能前后端联调"
```
