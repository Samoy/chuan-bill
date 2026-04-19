<script setup lang="ts">
definePage({
  name: 'family-detail',
  style: {
    navigationBarTitleText: '家庭详情',
  },
})

const familyStore = useFamilyStore()
const toast = useGlobalToast()

const familyId = ref('')
const showRemoveConfirm = ref(false)
const removeTarget = ref<{ id: string, nickname: string } | null>(null)

onLoad((options) => {
  if (options?.familyId) {
    familyId.value = options.familyId
  }
})

onShow(async () => {
  if (familyId.value) {
    await Promise.all([
      familyStore.fetchFamilyDetail(familyId.value),
      familyStore.fetchMembers(familyId.value),
    ])
    // 如果是户主，也获取待处理申请
    if (familyStore.currentFamily?.isOwner) {
      await familyStore.fetchPendingApplies(familyId.value)
    }
  }
})

const currentFamily = computed(() => familyStore.currentFamily)
const members = computed(() => familyStore.memberList)
const pendingApplies = computed(() => familyStore.pendingApplies)
const isOwner = computed(() => currentFamily.value?.isOwner ?? false)

// 复制邀请码
function copyInviteCode() {
  if (currentFamily.value?.inviteCode) {
    uni.setClipboardData({
      data: currentFamily.value.inviteCode,
      success: () => {
        toast.success('邀请码已复制')
      },
    })
  }
}

// 处理加入申请
async function handleApply(applyId: string, approved: boolean) {
  const success = await familyStore.handleJoinApply(applyId, familyId.value, approved)
  if (success) {
    toast.success(approved ? '已同意加入申请' : '已拒绝加入申请')
  }
}

// 移除成员确认
function confirmRemoveMember(memberId: string, nickname: string) {
  removeTarget.value = { id: memberId, nickname }
  showRemoveConfirm.value = true
}

// 移除成员
async function removeMember() {
  if (!removeTarget.value)
    return
  const success = await familyStore.removeMember(familyId.value, removeTarget.value.id)
  if (success) {
    toast.success('已移除成员')
  }
  showRemoveConfirm.value = false
  removeTarget.value = null
}

// 退出家庭
function handleLeave() {
  uni.showModal({
    title: '确认退出',
    content: '退出后将无法查看该家庭账单，确认退出？',
    success: async (res) => {
      if (res.confirm) {
        const success = await familyStore.leaveFamily(familyId.value)
        if (success) {
          toast.success('已退出家庭')
          uni.navigateBack()
        }
      }
    },
  })
}

// 删除家庭
function handleDelete() {
  uni.showModal({
    title: '确认删除',
    content: '删除家庭后所有数据将无法恢复，确认删除？',
    success: async (res) => {
      if (res.confirm) {
        const success = await familyStore.deleteFamily(familyId.value)
        if (success) {
          toast.success('家庭已删除')
          uni.navigateBack()
        }
      }
    },
  })
}

// 刷新邀请码
async function handleRefreshInviteCode() {
  uni.showModal({
    title: '刷新邀请码',
    content: '刷新后旧邀请码将失效，确认刷新？',
    success: async (res) => {
      if (res.confirm) {
        const newCode = await familyStore.refreshInviteCode(familyId.value)
        if (newCode) {
          toast.success('邀请码已刷新')
        }
      }
    },
  })
}
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <template v-if="currentFamily && currentFamily.id === familyId">
      <!-- 家庭信息卡片 -->
      <view class="mx-3 rounded-2xl from-primary to-primary/80 bg-gradient-to-br p-5 text-white shadow-lg">
        <view class="flex items-center gap-3">
          <view class="h-14 w-14 flex items-center justify-center rounded-2xl bg-white/20 backdrop-blur-sm">
            <image v-if="currentFamily.avatar" :src="currentFamily.avatar" class="h-14 w-14 rounded-2xl" mode="aspectFill" />
            <view v-else class="i-lucide:home h-7 w-7" />
          </view>
          <view class="flex-1">
            <view class="flex items-center gap-2">
              <text class="text-lg font-bold">
                {{ currentFamily.name }}
              </text>
              <view v-if="isOwner" class="rounded bg-white/20 px-1.5 py-0.5">
                <text class="text-xs">
                  户主
                </text>
              </view>
            </view>
            <text class="mt-0.5 block text-sm text-white/80">
              {{ members.length }} 位成员
            </text>
          </view>
        </view>
        <text v-if="currentFamily.description" class="mt-3 block text-sm text-white/90 leading-relaxed">
          {{ currentFamily.description }}
        </text>
      </view>

      <!-- 邀请码区域（仅户主可见） -->
      <view v-if="isOwner" class="mx-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view class="flex items-center justify-between">
          <view>
            <text class="block text-sm font-500">
              邀请码
            </text>
            <text class="mt-0.5 block text-xs text-gray-500">
              分享给家人加入家庭
            </text>
          </view>
          <view class="flex items-center gap-2">
            <text class="mr-2 text-lg font-bold tracking-widest font-mono">
              {{ currentFamily.inviteCode }}
            </text>
            <wd-button size="small" plain @click="copyInviteCode">
              复制
            </wd-button>
            <wd-button size="small" plain @click="handleRefreshInviteCode">
              刷新
            </wd-button>
          </view>
        </view>
      </view>

      <!-- 待处理申请（仅户主可见） -->
      <template v-if="isOwner && pendingApplies.length > 0">
        <view class="mx-3">
          <text class="mb-2 block px-1 text-sm text-gray-500 font-500">
            待处理申请 ({{ pendingApplies.length }})
          </text>
        </view>
        <view class="mx-3 rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
          <view
            v-for="(apply, index) in pendingApplies"
            :key="apply.id"
            class="flex items-center gap-3 p-4"
            :class="index < pendingApplies.length - 1 && 'border-b border-gray-100 dark:border-gray-700'"
          >
            <view class="h-10 w-10 flex items-center justify-center rounded-full bg-gray-100 dark:bg-gray-700">
              <view class="i-lucide:user h-5 w-5 text-gray-400" />
            </view>
            <view class="flex-1">
              <text class="block text-sm font-500">
                {{ apply.nickname || '用户' }}
              </text>
              <text v-if="apply.remark" class="mt-0.5 block text-xs text-gray-500">
                {{ apply.remark }}
              </text>
            </view>
            <view class="flex gap-2">
              <wd-button size="small" type="success" plain @click="handleApply(apply.id, true)">
                同意
              </wd-button>
              <wd-button size="small" type="error" plain @click="handleApply(apply.id, false)">
                拒绝
              </wd-button>
            </view>
          </view>
        </view>
      </template>

      <!-- 成员列表 -->
      <view class="mx-3">
        <text class="mb-2 block px-1 text-sm text-gray-500 font-500">
          家庭成员 ({{ members.length }})
        </text>
      </view>
      <view v-if="familyStore.memberListLoading" class="mx-3 rounded-2xl bg-white p-6 text-center shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <wd-loading />
      </view>
      <view v-else class="mx-3 rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view
          v-for="(member, index) in members"
          :key="member.id"
          class="flex items-center gap-3 p-4"
          :class="index < members.length - 1 && 'border-b border-gray-100 dark:border-gray-700'"
        >
          <view class="h-10 w-10 flex items-center justify-center rounded-full bg-primary/10">
            <image v-if="member.avatar" :src="member.avatar" class="h-10 w-10 rounded-full" mode="aspectFill" />
            <view v-else class="i-lucide:user h-5 w-5 text-primary" />
          </view>
          <view class="flex-1">
            <view class="flex items-center gap-2">
              <text class="text-sm font-500">
                {{ member.nickname || '成员' }}
              </text>
              <view v-if="member.isOwner" class="rounded bg-primary/10 px-1.5 py-0.5">
                <text class="text-xs text-primary">
                  户主
                </text>
              </view>
            </view>
          </view>
          <!-- 移除按钮（仅户主可移除非户主成员） -->
          <wd-button
            v-if="isOwner && !member.isOwner"
            size="small"
            type="error"
            plain
            @click="confirmRemoveMember(member.id, member.nickname || '成员')"
          >
            移除
          </wd-button>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="mx-3 mt-2">
        <wd-button v-if="isOwner" type="error" plain block @click="handleDelete">
          解散家庭
        </wd-button>
        <wd-button v-else type="error" plain block @click="handleLeave">
          退出家庭
        </wd-button>
      </view>
    </template>

    <!-- 加载中 -->
    <view v-else class="mx-3 rounded-2xl bg-white p-6 text-center shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <wd-loading />
    </view>

    <!-- 移除确认弹框 -->
    <wd-msgbox v-model="showRemoveConfirm" title="确认移除" :content="`确认移除成员「${removeTarget?.nickname}」？`" @confirm="removeMember" />
  </view>
</template>
