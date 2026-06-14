<script setup lang="ts">
import { EVENTS } from '@/constant/events'
import { eventBus } from '@/utils/eventBus'

definePage({
  name: 'family-detail',
  layout: 'default',
  style: {
    navigationBarTitleText: '家庭详情',
  },
})

const familyStore = useFamilyStore()
const toast = useGlobalToast()
const message = useGlobalMessage()
const userStore = useUserStore()
const router = useRouter()

const familyId = ref('')
const showRemoveConfirm = ref(false)
const removeTarget = ref<{ id: string, nickname: string } | null>(null)

onLoad(async (options) => {
  if (options?.familyId) {
    familyId.value = options.familyId
    if (familyId.value) {
      uni.showShareMenu()
      await Promise.all([
        familyStore.fetchFamilyDetail(familyId.value),
        familyStore.fetchMembers(familyId.value),
      ])
      // 如果是户主，也获取待处理申请
      if (familyStore.currentFamily?.isOwner) {
        await familyStore.fetchPendingApplies(familyId.value)
      }
    }
  }
})

const currentFamily = computed(() => familyStore.currentFamily)
const members = computed(() => familyStore.memberList)
const pendingApplies = computed(() => familyStore.pendingApplies)
const isOwner = computed(() => currentFamily.value?.isOwner || false)

// 分享家庭
function shareFamily() {
  // #ifndef MP-WEIXIN
  const inviteCode = currentFamily.value?.inviteCode
  const familyName = currentFamily.value?.name || '我的家庭'
  if (!inviteCode)
    return
  const shareUrl = `${import.meta.env.VITE_SHARE_BASE_URL}/pages/family/index?inviteCode=${inviteCode}`
  const shareText = `${userStore.nickname || '您的好友'}邀请你加入「${familyName}」，邀请码：${inviteCode}\n链接：${shareUrl}`
  uni.setClipboardData({
    data: shareText,
    success: () => {
      toast.success('分享链接已复制到剪贴板')
    },
  })
  // #endif
}

// #ifdef MP-WEIXIN
onShareAppMessage(() => {
  return {
    title: `${userStore.nickname || '您的好友'}邀请你加入「${currentFamily.value?.name || '我的家庭'}」`,
    path: `/pages/family/index?inviteCode=${currentFamily.value?.inviteCode}`,
    imageUrl: currentFamily.value?.avatar,
  }
})
// #endif

// 处理加入申请
async function handleApply(applyId: string, approved: boolean) {
  const success = await familyStore.handleJoinApply(applyId, familyId.value, approved)
  if (success) {
    toast.success(approved ? '已同意加入申请' : '已拒绝加入申请')
    if (approved) {
      eventBus.emit(EVENTS.FAMILY.MEMBER_CHANGED)
    }
  }
}

// 移除成员
async function removeMember(familyId: string, userId: string, nickname: string) {
  message.confirm({
    title: '提示',
    msg: `确认移除成员「${nickname}」？`,
    beforeConfirm: async ({ resolve }) => {
      const success = await familyStore.removeMember(familyId, userId)
      if (success) {
        resolve(true)
        toast.success('已移除成员')
        eventBus.emit(EVENTS.FAMILY.MEMBER_CHANGED)
      }
      showRemoveConfirm.value = false
      removeTarget.value = null
    },
  })
}

// 退出家庭
function handleLeave() {
  message.confirm({
    title: '确认退出',
    msg: '退出后将无法查看该家庭账单，确认退出？',
    beforeConfirm: async ({ resolve }) => {
      const success = await familyStore.leaveFamily(familyId.value)
      if (success) {
        resolve(true)
        toast.success('已退出家庭')
        eventBus.emit(EVENTS.FAMILY.MEMBER_CHANGED)
        router.back()
      }
    },
  })
}

// 删除家庭
function handleDelete() {
  message.confirm({
    title: '确认删除',
    msg: '删除家庭后所有数据将无法恢复，确认删除？',
    beforeConfirm: async ({ resolve }) => {
      const success = await familyStore.deleteFamily(familyId.value)
      if (success) {
        resolve(true)
        toast.success('家庭已删除')
        router.back()
      }
    },
  })
}

// 刷新邀请码
async function handleRefreshInviteCode() {
  message.confirm({
    title: '刷新邀请码',
    msg: '刷新后旧邀请码将失效，确认刷新？',
    beforeConfirm: async ({ resolve }) => {
      const newCode = await familyStore.refreshInviteCode(familyId.value)
      if (newCode) {
        resolve(true)
        toast.success('邀请码已刷新')
      }
    },
  })
}
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <template v-if="currentFamily && currentFamily.id === familyId">
      <!-- 家庭信息卡片 -->
      <view class="mx-3 rounded-2xl from-primary to-primary/50 bg-gradient-to-br p-5 text-white shadow-lg">
        <view class="flex items-center gap-3">
          <view class="flex items-center justify-center rounded-2xl bg-white/20 p-2 backdrop-blur-sm">
            <wd-img v-if="currentFamily.avatar" :src="currentFamily.avatar" class="h-12 w-12 rounded-2xl" mode="aspectFill" />
            <view v-else class="i-lucide:home h-7 w-7" />
          </view>
          <view class="flex-1">
            <view class="flex items-center gap-2">
              <text class="text-lg font-bold">
                {{ currentFamily.name }}
              </text>
              <view v-if="isOwner" class="flex items-center rounded bg-white/20 px-1.5 py-0.5">
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
        <text v-if="currentFamily.description" class="mt-3 block text-xs text-white/90 leading-relaxed">
          {{ currentFamily.description }}
        </text>
      </view>

      <!-- 功能入口 -->
      <view class="mx-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view class="grid grid-cols-2 gap-4">
          <!-- 家庭账单 -->
          <view
            class="flex items-center gap-3"
            @click="router.push(`/pages/family/bill?familyId=${familyId}&familyName=${encodeURIComponent(currentFamily.name || '')}`)"
          >
            <view class="h-10 w-10 flex items-center justify-center rounded-xl bg-primary/10">
              <view class="i-lucide:receipt h-5 w-5 text-primary" />
            </view>
            <view>
              <text class="block text-sm font-500">
                家庭账单
              </text>
              <text class="mt-0.5 block text-xs text-gray-500">
                查看共享账单
              </text>
            </view>
          </view>
          <!-- 家庭统计 -->
          <view
            class="flex items-center gap-3"
            @click="router.push(`/pages/family/statistics?familyId=${familyId}&familyName=${encodeURIComponent(currentFamily.name || '')}`)"
          >
            <view class="h-10 w-10 flex items-center justify-center rounded-xl bg-primary/10">
              <view class="i-lucide:bar-chart-3 h-5 w-5 text-primary" />
            </view>
            <view>
              <text class="block text-sm font-500">
                家庭统计
              </text>
              <text class="mt-0.5 block text-xs text-gray-500">
                收支统计分析
              </text>
            </view>
          </view>
        </view>
      </view>

      <!-- 邀请码区域 -->
      <view class="mx-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view class="flex items-center justify-between">
          <view>
            <text class="block text-sm font-500">
              邀请码
            </text>
          </view>
          <view class="flex items-center gap-2">
            <text class="mr-2 text-lg font-bold tracking-widest font-mono">
              {{ currentFamily.inviteCode }}
            </text>
            <wd-button size="small" plain open-type="share" @click="shareFamily">
              分享
            </wd-button>
            <wd-button v-if="isOwner" size="small" plain @click="handleRefreshInviteCode">
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
                {{ apply.userNickname || '用户' }}
              </text>
              <text v-if="apply.remark" class="mt-0.5 block text-xs text-gray-500">
                {{ apply.remark }}
              </text>
            </view>
            <view class="flex gap-2">
              <wd-button size="small" type="success" plain @click="handleApply(apply.id!, true)">
                同意
              </wd-button>
              <wd-button size="small" type="error" plain @click="handleApply(apply.id!, false)">
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
            <image v-if="member.userAvatar" :src="member.userAvatar" class="h-10 w-10 rounded-full" mode="aspectFill" />
            <view v-else class="i-lucide:user h-5 w-5 text-primary" />
          </view>
          <view class="flex-1">
            <view class="flex items-center gap-2">
              <text class="text-sm font-500">
                {{ member.nickname }}
              </text>
              <view v-if="member.isOwner" class="flex items-center rounded bg-primary/10 px-1.5 py-0.5">
                <text class="text-xs text-primary">
                  户主
                </text>
              </view>
              <view v-if="member.userId === userStore.userId" class="flex items-center rounded bg-orange-400/30 px-1.5 py-0.5">
                <text class="text-xs text-orange">
                  本人
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
            @click="removeMember(familyId, member.userId!, member.nickname || '成员')"
          >
            移除
          </wd-button>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="mx-3 mt-2">
        <wd-button v-if="isOwner" type="error" block @click="handleDelete">
          解散家庭
        </wd-button>
        <wd-button v-else type="error" block @click="handleLeave">
          退出家庭
        </wd-button>
      </view>
    </template>

    <!-- 加载中 -->
    <view v-else class="mx-3 rounded-2xl bg-white p-6 text-center shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <wd-loading />
    </view>
  </view>
</template>
