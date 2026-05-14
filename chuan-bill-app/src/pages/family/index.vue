<script setup lang="ts">
definePage({
  name: 'family',
  layout: 'tabbar',
  style: {
    navigationBarTitleText: '家庭',
  },
})

const user = useUserStore()
const familyStore = useFamilyStore()
const messageStore = useMessageStore()
const toast = useGlobalToast()
const router = useRouter()

// 加入家庭弹框
const showJoinPopup = ref(false)
const joinForm = ref({ inviteCode: '', remark: '' })
const joinLoading = ref(false)
const showKeyboard = ref(false)

// 页面加载时获取数据
onShow(async () => {
  if (user.isLoggedIn) {
    await familyStore.fetchFamilyList()
    await messageStore.fetchUnreadCount()
  }
})

// 加入家庭
async function handleJoin() {
  if (!joinForm.value.inviteCode.trim()) {
    toast.warning('请输入邀请码')
    return
  }
  joinLoading.value = true
  try {
    const result = await familyStore.joinFamily(joinForm.value.inviteCode.trim(), joinForm.value.remark.trim())
    if (result) {
      toast.success('申请已提交，等待户主审批')
      showJoinPopup.value = false
      joinForm.value = { inviteCode: '', remark: '' }
    }
  }
  finally {
    joinLoading.value = false
  }
}
</script>

<template>
  <view class="box-border flex flex-col gap-3 py-3">
    <!-- 未登录状态 -->
    <template v-if="!user.isLoggedIn">
      <view class="mx-3 rounded-2xl from-primary to-primary/80 bg-gradient-to-br p-6 text-white shadow-lg">
        <view class="mb-4 flex items-center gap-3">
          <view class="h-14 w-14 flex items-center justify-center rounded-2xl bg-white/20 backdrop-blur-sm">
            <view class="i-lucide:home h-7 w-7" />
          </view>
          <view>
            <text class="block text-lg font-bold">
              家庭记账
            </text>
            <text class="mt-0.5 block text-sm text-white/80">
              与家人一起管理财务
            </text>
          </view>
        </view>
        <text class="block text-sm text-white/90 leading-relaxed">
          创建或加入家庭，与家人共同记录和管理家庭收支，让家庭财务更透明、更可控。
        </text>
      </view>

      <view class="mx-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <text class="mb-4 block text-sm font-500">
          功能特性
        </text>
        <view class="grid grid-cols-2 gap-4">
          <view class="flex items-start gap-3">
            <view class="h-10 w-10 flex shrink-0 items-center justify-center rounded-xl bg-primary/10">
              <view class="i-lucide:users h-5 w-5 text-primary" />
            </view>
            <view>
              <text class="block text-sm font-500">
                家庭共享
              </text>
              <text class="mt-0.5 block text-xs text-gray-500">
                与家人共同管理家庭财务
              </text>
            </view>
          </view>
          <view class="flex items-start gap-3">
            <view class="h-10 w-10 flex shrink-0 items-center justify-center rounded-xl bg-primary/10">
              <view class="i-lucide:wallet h-5 w-5 text-primary" />
            </view>
            <view>
              <text class="block text-sm font-500">
                共同记账
              </text>
              <text class="mt-0.5 block text-xs text-gray-500">
                家庭成员可共同记录收支
              </text>
            </view>
          </view>
          <view class="flex items-start gap-3">
            <view class="h-10 w-10 flex shrink-0 items-center justify-center rounded-xl bg-primary/10">
              <view class="i-lucide:pie-chart h-5 w-5 text-primary" />
            </view>
            <view>
              <text class="block text-sm font-500">
                家庭统计
              </text>
              <text class="mt-0.5 block text-xs text-gray-500">
                查看家庭整体财务状况
              </text>
            </view>
          </view>
          <view class="flex items-start gap-3">
            <view class="h-10 w-10 flex shrink-0 items-center justify-center rounded-xl bg-primary/10">
              <view class="i-lucide:shield-check h-5 w-5 text-primary" />
            </view>
            <view>
              <text class="block text-sm font-500">
                权限管理
              </text>
              <text class="mt-0.5 block text-xs text-gray-500">
                灵活设置成员权限
              </text>
            </view>
          </view>
        </view>
      </view>

      <view class="mx-3 rounded-2xl bg-white p-6 text-center shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view class="mb-3 flex justify-center">
          <view class="h-16 w-16 flex items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
            <view class="i-lucide:lock h-8 w-8 text-gray-400" />
          </view>
        </view>
        <text class="mb-1 block text-base font-500">
          登录后体验家庭共享记账
        </text>
        <text class="mb-6 block text-sm text-gray-500">
          创建家庭、邀请成员、共同管理
        </text>
        <wd-button type="primary" block @click="user.showLoginPopup = true">
          立即登录
        </wd-button>
      </view>
    </template>

    <!-- 已登录状态 -->
    <template v-else>
      <!-- 顶部操作栏 -->
      <view class="mx-3 flex gap-3">
        <view class="flex-1 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]" @click="router.push('/pages/family/edit')">
          <view class="flex items-center gap-3">
            <view class="h-10 w-10 flex items-center justify-center rounded-xl bg-primary/10">
              <view class="i-lucide:plus h-5 w-5 text-primary" />
            </view>
            <view>
              <text class="block text-sm font-500">
                创建家庭
              </text>
              <text class="mt-0.5 block text-xs text-gray-500">
                创建新的家庭组
              </text>
            </view>
          </view>
        </view>
        <view class="flex-1 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]" @click="showJoinPopup = true">
          <view class="flex items-center gap-3">
            <view class="h-10 w-10 flex items-center justify-center rounded-xl bg-primary/10">
              <view class="i-lucide:log-in h-5 w-5 text-primary" />
            </view>
            <view>
              <text class="block text-sm font-500">
                加入家庭
              </text>
              <text class="mt-0.5 block text-xs text-gray-500">
                通过邀请码加入
              </text>
            </view>
          </view>
        </view>
      </view>

      <!-- 我的家庭列表 -->
      <view v-if="familyStore.familyListLoading" class="mx-3 rounded-2xl bg-white p-6 text-center shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <wd-loading />
      </view>

      <template v-else-if="familyStore.hasFamily">
        <view class="mx-3">
          <text class="mt-2 block px-1 text-sm text-gray-500 font-500">
            我的家庭
          </text>
        </view>
        <view
          v-for="family in familyStore.familyList"
          :key="family.id"
          class="mx-3 rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]"
          @click="router.push(`/pages/family/detail?familyId=${family.id}`)"
        >
          <view class="flex items-center gap-3">
            <!-- 家庭头像 -->
            <wd-avatar v-if="family.avatar" :src="family.avatar" class="h-12 w-12 rounded-xl" mode="aspectFill" />
            <view v-else class="h-12 w-12 flex items-center justify-center rounded-xl bg-primary/10 p-1">
              <view class="i-mingcute:group-line h-8 w-8 text-primary" />
            </view>
            <view class="flex-1">
              <view class="flex items-center gap-2">
                <text class="text-base font-500">
                  {{ family.name }}
                </text>
                <view v-if="family.isOwner" class="flex items-center rounded bg-primary/10 px-1.5 py-0.5">
                  <text class="text-xs text-primary">
                    户主
                  </text>
                </view>
              </view>
              <text class="mt-0.5 block text-xs text-gray-500">
                {{ family.memberCount }} 位成员
              </text>
            </view>
            <!-- 编辑按钮（仅户主可见） -->
            <view
              v-if="family.isOwner"
              class="h-8 w-8 flex items-center justify-center rounded-full bg-primary/10"
              @click.stop="router.push(`/pages/family/edit?familyId=${family.id}`)"
            >
              <view class="i-lucide:edit h-3 w-3 text-primary" />
            </view>
          </view>
          <!-- 快捷操作 -->
          <view class="mt-3 flex gap-2">
            <view
              class="flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-primary/5 py-2 text-primary transition-all active:scale-95"
              @click.stop="router.push(`/pages/family/bill?familyId=${family.id}&familyName=${encodeURIComponent(family.name || '')}`)"
            >
              <view class="i-lucide:clipboard-list h-4 w-4" />
              <view class="text-xs font-500">
                账单列表
              </view>
            </view>
            <view
              class="flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-primary/5 py-2 text-primary transition-all active:scale-95"
              @click.stop="router.push(`/pages/family/statistics?familyId=${family.id}&familyName=${encodeURIComponent(family.name || '')}`)"
            >
              <view class="i-lucide:bar-chart-3 h-4 w-4" />
              <view class="text-xs font-500">
                家庭统计
              </view>
            </view>
          </view>
        </view>
      </template>

      <!-- 无家庭状态 -->
      <view v-else class="mx-3 rounded-2xl bg-white p-6 text-center shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view class="mb-3 flex justify-center">
          <view class="h-16 w-16 flex items-center justify-center rounded-full bg-primary/10">
            <view class="i-lucide:home h-8 w-8 text-primary" />
          </view>
        </view>
        <text class="mb-1 block text-base font-500">
          您还没有加入任何家庭
        </text>
        <text class="block text-xs text-gray-500">
          通过上方按钮创建或加入已有家庭
        </text>
      </view>
    </template>

    <!-- 加入家庭弹框 -->
    <wd-popup v-model="showJoinPopup" position="bottom" :z-index="100" custom-class="rounded-2xl rounded-b-0 p-6" closable>
      <view class="mb-4">
        <text class="block text-lg font-500">
          加入家庭
        </text>
        <text class="mt-1 block text-sm text-gray-500">
          输入邀请码申请加入家庭
        </text>
      </view>
      <view class="mb-3">
        <text class="mb-2 block text-sm text-gray-600">
          邀请码
        </text>
        <wd-password-input v-model="joinForm.inviteCode" :mask="false" :length="6" :focused="showKeyboard" @focus="showKeyboard = true" />
        <wd-keyboard
          v-model="joinForm.inviteCode" v-model:visible="showKeyboard" type="number" :maxlength="6" close-text="完成" mode="custom"
          @blur="showKeyboard = false"
        />
      </view>
      <view class="mb-4">
        <text class="mb-2 block text-sm text-gray-600">
          备注（可选）
        </text>
        <wd-textarea v-model="joinForm.remark" placeholder="向户主介绍一下自己" :maxlength="200" />
      </view>
      <wd-button type="primary" block :loading="joinLoading" custom-class="mb-4" @click="handleJoin">
        提交申请
      </wd-button>
    </wd-popup>
  </view>
</template>
