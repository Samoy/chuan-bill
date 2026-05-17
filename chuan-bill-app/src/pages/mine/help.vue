<script setup lang="ts">
definePage({
  name: 'help-feedback',
  layout: 'default',
  style: {
    navigationBarTitleText: '帮助与反馈',
  },
})

const userStore = useUserStore()
const toast = useGlobalToast()

// FAQ 分类
const faqCategories = [
  {
    label: '记账',
    icon: 'i-lucide:pen-line',
    list: [
      {
        question: '如何手动记账？',
        answer: '进入底部导航栏「账单」tab → 点击右下角的 "+" 按钮 → 选择「手动添加」→ 填写账单信息（类型、金额、名称、类目、支付方式等）→ 点击「确认入账」。',
      },
      {
        question: '如何用 OCR 拍照记账？',
        answer: '点击 "+" 按钮 → 选择「图片识别」→ 拍摄或从相册选择票据/小票图片 → AI 自动识别 → 确认或修改识别结果 → 「确认入账」。请确保图片清晰、光线充足。',
      },
      {
        question: '如何用语音记账？',
        answer: '点击 "+" 按钮 → 选择「语音识别」→ 按住麦克风按钮说话（如"午餐花了25元"）→ 松开后 AI 自动解析 → 确认结果 → 「确认入账」。需要联网和麦克风权限。',
      },
      {
        question: 'OCR 识别不准确怎么办？',
        answer: '1. 确保图片清晰、完整拍摄票据；2. 光线充足、避免反光；3. 识别后可在确认页面手动修改；4. 如果识别完全不对，可切换手动记账。',
      },
      {
        question: '语音识别不工作？',
        answer: '1. 检查麦克风权限是否已授权；2. 确保网络连接正常；3. 在安静环境下使用；4. 说话清晰；5. 如持续不工作，尝试重启 App。',
      },
      {
        question: '未登录时记的账单会丢失吗？',
        answer: '不会。未登录时账单保存在本地（最多1000条），登录后可通过「我的」→「账单同步」批量同步到云端。',
      },
      {
        question: '账单可以编辑或删除吗？',
        answer: '可以。点击账单列表中的某条账单 → 查看详情 → 点击编辑或删除按钮。只能编辑/删除自己创建的账单。',
      },
      {
        question: '账单怎么筛选？',
        answer: '在账单列表页顶部有搜索框，支持按名称/备注搜索。点击筛选按钮可按：类型（收入/支出）、类目、支付方式、金额范围、日期范围筛选。',
      },
      {
        question: '如何导出账单？',
        answer: '进入「我的」→「账单导出」，支持按日期范围、类型等条件筛选后导出 Excel 或 PDF 格式的账单数据。',
      },
    ],
  },
  {
    label: '统计',
    icon: 'i-lucide:bar-chart-3',
    list: [
      {
        question: '统计页面怎么看？',
        answer: '进入底部导航栏「统计」tab，自动显示当月数据。顶部可切换月份（最近12个月）。',
      },
      {
        question: 'AI 消费建议是什么？',
        answer: 'AI 基于您当月的账单数据，分析消费习惯并给出理财建议。支持个人维度和家庭维度分析。',
      },
      {
        question: 'AI 分析次数用完了？',
        answer: '非VIP用户每天可使用5次AI分析，升级VIP后无限制。每天0点重置次数。',
      },
      {
        question: '统计数据和实际不符？',
        answer: '统计基于当月已记录的账单计算。请检查：1. 是否有遗漏未记录的账单；2. 是否在正确的月份；3. 家庭统计需选择对应家庭。',
      },
    ],
  },
  {
    label: '家庭',
    icon: 'i-lucide:users',
    list: [
      {
        question: '如何创建/加入家庭？',
        answer: '进入「家庭」tab → 「创建家庭」→ 填写名称、头像、描述即可创建。加入家庭需向户主索要6位邀请码 → 「加入家庭」→ 输入邀请码 → 等待审批。',
      },
      {
        question: '邀请码在哪里？可以更换吗？',
        answer: '户主在家庭详情页可查看邀请码，也可分享给想加入的家人。点击「刷新邀请码」即可生成新的，旧邀请码立即失效。',
      },
      {
        question: '户主可以转让吗？',
        answer: '可以。户主进入家庭详情 → 成员列表 → 选择目标成员 → 「转让户主」。转让后原户主变为普通成员。',
      },
      {
        question: '户主能退出家庭吗？',
        answer: '不能。户主需要先转让户主身份，然后才能退出。',
      },
      {
        question: '家庭账单和个人账单的区别？',
        answer: '个人账单默认只有自己可见。在记账时选择关联家庭，该账单就会在家庭成员间共享。只有您主动关联到家庭的账单，家庭成员才能看到。',
      },
    ],
  },
  {
    label: '账号',
    icon: 'i-lucide:user',
    list: [
      {
        question: '怎么登录？',
        answer: '支持三种方式：1. 验证码登录：输入手机号 → 获取验证码 → 输入验证码；2. 密码登录：输入手机号+密码；3. 微信登录：点击微信登录按钮。未注册的手机号会自动注册。',
      },
      {
        question: '忘记密码怎么办？',
        answer: '使用验证码方式登录，登录后进入「我的」→「账号与安全」→「修改密码」，支持旧密码修改或验证码修改两种方式。',
      },
      {
        question: '手机号可以更换吗？',
        answer: '可以，登录后进入「我的」→「账号与安全」→「手机号」，支持密码修改或验证码修改两种方式。',
      },
      {
        question: '登录不了怎么办？',
        answer: '1. 检查手机号是否正确；2. 验证码是否在5分钟有效期内；3. 尝试切换登录方式；4. 检查网络；5. 如仍不行请联系客服。',
      },
    ],
  },
  {
    label: '设置',
    icon: 'i-lucide:settings',
    list: [
      {
        question: '怎么切换主题？',
        answer: '「我的」→「主题切换」→ 选择跟随系统或手动切换深色/浅色主题。还可自定义主题色。',
      },
      {
        question: '怎么修改头像和昵称？',
        answer: '「我的」→ 点击头像区域 →「个人信息」→ 修改昵称、头像、性别 → 保存。',
      },
      {
        question: '怎么设置消息通知？',
        answer: '「我的」→「通知设置」→ 配置通知偏好。',
      },
    ],
  },
]

// 当前选中的分类
const activeCategory = ref(0)

// 当前分类的 FAQ 列表
const faqList = computed(() => faqCategories[activeCategory.value].list)

// 展开状态
const expandedIndex = ref<number | null>(null)

function toggleExpand(index: number) {
  expandedIndex.value = expandedIndex.value === index ? null : index
}

// AI客服入口
function goToAiChat() {
  toast.info('AI客服功能即将上线，敬请期待')
  // TODO: 接入百炼平台AI客服
  // router.push('/pages/mine/ai-chat/index')
}
</script>

<template>
  <view class="box-border flex flex-col gap-4 p-4">
    <!-- AI 客服入口（仅登录显示） -->
    <view
      v-if="userStore.isLoggedIn"
      class="rounded-2xl from-primary to-primary/80 bg-gradient-to-br p-5 text-white shadow-lg"
      @click="goToAiChat"
    >
      <view class="flex items-center gap-3">
        <view class="h-12 w-12 flex items-center justify-center rounded-full bg-white/20">
          <view class="i-lucide:bot h-6 w-6" />
        </view>
        <view class="flex-1">
          <text class="block text-base font-bold">
            AI 智能客服
          </text>
          <text class="mt-0.5 block text-xs text-white/80">
            有任何问题都可以问我
          </text>
        </view>
        <view class="i-lucide:chevron-right h-5 w-5" />
      </view>
    </view>

    <!-- 未登录提示 -->
    <view
      v-else
      class="rounded-2xl bg-blue-50 p-5 dark:bg-blue-900/20"
      @click="userStore.showLoginPopup = true"
    >
      <view class="flex items-center gap-3">
        <view class="h-12 w-12 flex items-center justify-center rounded-full bg-blue-100 dark:bg-blue-800">
          <view class="i-lucide:bot h-6 w-6 text-blue-600 dark:text-blue-400" />
        </view>
        <view class="flex-1">
          <text class="block text-sm text-blue-800 font-medium dark:text-blue-200">
            登录后使用 AI 客服
          </text>
          <text class="mt-0.5 block text-xs text-blue-600 dark:text-blue-300">
            智能解答您的所有问题
          </text>
        </view>
        <wd-button type="primary" size="small">
          去登录
        </wd-button>
      </view>
    </view>

    <!-- 常见问题 -->
    <view>
      <text class="mb-3 block text-sm text-gray-600 font-medium dark:text-gray-400">
        常见问题
      </text>
      <!-- 分类标签 -->
      <scroll-view
        scroll-x
        class="mb-3 min-w-full whitespace-nowrap"
        :show-scrollbar="false"
      >
        <view
          v-for="(cat, ci) in faqCategories"
          :key="ci"
          class="mr-2 inline-flex items-center gap-1 rounded-full px-3 py-1.5 text-xs transition-colors"
          :class="activeCategory === ci
            ? 'bg-primary text-white'
            : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'"
          @click="activeCategory = ci"
        >
          <view :class="cat.icon" class="h-3.5 w-3.5" />
          <text>{{ cat.label }}</text>
        </view>
      </scroll-view>
      <view class="rounded-2xl bg-white shadow-sm dark:bg-[var(--wot-dark-background2)]">
        <view
          v-for="(item, index) in faqList"
          :key="index"
          class="faq-item"
          :class="[
            index < faqList.length - 1 && 'border-b border-gray-100 dark:border-gray-700',
            expandedIndex === index && 'is-expanded',
          ]"
        >
          <view
            class="flex items-center justify-between p-4"
            @click="toggleExpand(index)"
          >
            <text class="flex-1 text-sm">
              {{ item.question }}
            </text>
            <view
              class="i-lucide:chevron-down h-4 w-4 text-gray-400 transition-transform duration-200"
              :class="expandedIndex === index && 'rotate-180'"
            />
          </view>
          <view
            v-show="expandedIndex === index"
            class="px-4 pb-4"
          >
            <text class="block text-xs text-gray-500 leading-relaxed dark:text-gray-400">
              {{ item.answer }}
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- 联系客服 -->
    <view class="mt-4 rounded-2xl bg-white p-4 text-center shadow-sm dark:bg-[var(--wot-dark-background2)]">
      <text class="block text-xs text-gray-400">
        还有其他问题？
      </text>
      <text class="mt-1 block text-sm text-primary">
        反馈邮箱：chuanbill@samoy.site
      </text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.faq-item {
  .is-expanded {
    background-color: rgba(0, 0, 0, 0.02);
  }
}
</style>
