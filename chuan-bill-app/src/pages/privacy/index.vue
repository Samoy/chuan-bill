<script setup lang="ts">
definePage({
  name: 'privacy',
  layout: 'default',
  style: {
    navigationBarTitleText: '隐私政策',
  },
})

const userStore = useUserStore()

// 通用隐私政策内容
const baseContent = `
<div style="padding: 0 16px; font-size: 14px; line-height: 1.8; color: #333;">
  <p style="text-align: center; font-size: 16px; font-weight: bold; margin-bottom: 16px;">小川记账隐私政策</p>
  <p style="color: #999; text-align: center; margin-bottom: 24px;">更新日期：2026年4月17日 | 生效日期：2026年4月17日</p>

  <p>"小川记账"（以下简称"我们"）深知个人信息对您的重要性，我们将按照法律法规要求，采取相应安全措施，保护您的个人信息安全。请您在使用我们的服务前，仔细阅读并充分理解本隐私政策。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">一、我们如何收集和使用您的信息</p>
  <p>根据您选择的使用方式，我们收集的信息有所不同：</p>

  <p style="font-weight: bold; margin-top: 12px;">1. 游客模式（未登录）</p>
  <p style="padding-left: 16px;"><b>在游客模式下，我们不会收集您的任何个人身份信息。</b>您的账单数据仅存储在您的本地设备中，我们无法访问。</p>
  <p style="padding-left: 16px;">我们可能收集以下非个人身份信息用于改进服务：</p>
  <p style="padding-left: 32px;">- 设备型号、操作系统版本（用于兼容性优化）；</p>
  <p style="padding-left: 32px;">- 应用崩溃日志（用于修复问题）；</p>
  <p style="padding-left: 32px;">- 基本使用统计（如功能使用频次，不关联个人身份）。</p>

  <p style="font-weight: bold; margin-top: 12px;">2. 注册用户模式（已登录）</p>
  <p style="padding-left: 16px;">当您选择注册登录时，我们会收集以下信息：</p>
  <p style="padding-left: 32px;"><b>（1）手机号码</b>：用于账号注册、登录验证和安全通知；</p>
  <p style="padding-left: 32px;"><b>（2）微信授权信息</b>（仅限微信登录）：包括微信昵称、头像等公开信息；</p>
  <p style="padding-left: 32px;"><b>（3）账单数据</b>：您主动录入的收支记录，用于提供云端同步和统计分析服务；</p>
  <p style="padding-left: 32px;"><b>（4）上传的图片</b>：使用图片记账功能时上传的票据/小票照片，用于AI识别；</p>
  <p style="padding-left: 32px;"><b>（5）语音数据</b>：使用语音记账功能时的语音输入，用于语音识别转文字。</p>
  <p style="padding-left: 16px;"><b>上述图片和语音数据仅在识别过程中临时使用，识别完成后即删除原始文件，不做永久存储。</b></p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">二、我们如何存储您的信息</p>
  <p>2.1 <b>游客模式下</b>，所有数据仅存储在您的本地设备中，不会上传到我们的服务器。</p>
  <p>2.2 <b>注册用户模式下</b>，您的数据存储在位于中华人民共和国境内的安全服务器中，我们采用加密传输和存储等安全措施保护您的数据。</p>
  <p>2.3 我们将在实现服务目的所必需的最短时间内保留您的个人信息。<b>当您注销账号后，我们将在15个工作日内删除您的个人信息</b>，法律法规另有规定的除外。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">三、我们如何共享、转让和公开披露您的信息</p>
  <p>3.1 <b>我们不会向任何第三方出售您的个人信息。</b></p>
  <p>3.2 在以下情况下，我们可能会共享您的信息：</p>
  <p style="padding-left: 16px;">（1）获得您的明确同意后；</p>
  <p style="padding-left: 16px;">（2）根据法律法规的要求、强制性的行政或司法要求；</p>
  <p style="padding-left: 16px;">（3）为提供服务所必需的第三方服务商（如云服务器、短信服务），<b>这些服务商仅能在我们授权的范围内处理您的信息</b>。</p>
  <p>3.3 如涉及业务合并、收购或资产转让，我们将要求受让方继续受本隐私政策的约束。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">四、我们如何保护您的信息</p>
  <p>4.1 我们采用符合行业标准的安全措施保护您的个人信息，包括但不限于：</p>
  <p style="padding-left: 16px;">（1）数据传输采用HTTPS加密；</p>
  <p style="padding-left: 16px;">（2）用户密码使用不可逆加密存储；</p>
  <p style="padding-left: 16px;">（3）严格的数据访问权限控制。</p>
  <p>4.2 <b>尽管我们已采取合理措施保护您的信息安全，但请理解互联网环境并非绝对安全</b>，我们建议您妥善保管账号密码，不要将密码告知他人。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">五、您的权利</p>
  <p><b>无论您是否注册登录，您都享有以下权利：</b></p>
  <p style="padding-left: 16px;">（1）<b>访问权</b>：您可以随时查看您的账单数据；</p>
  <p style="padding-left: 16px;">（2）<b>更正权</b>：您可以随时修改您的账单记录；</p>
  <p style="padding-left: 16px;">（3）<b>删除权</b>：您可以随时删除您的账单数据。</p>
  <p>注册用户还享有以下额外权利：</p>
  <p style="padding-left: 16px;">（4）<b>导出权</b>：您可以导出您的全部账单数据；</p>
  <p style="padding-left: 16px;">（5）<b>注销权</b>：您可以申请注销账号，<b>注销后我们将删除您的个人信息和云端数据</b>；</p>
  <p style="padding-left: 16px;">（6）<b>撤回同意权</b>：您可以在设备设置中关闭相关权限授权。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">六、第三方服务</p>
  <p>本应用可能使用以下第三方服务：</p>
  <p style="padding-left: 16px;">（1）阿里云（服务器托管、AI识别服务）；</p>
  <p style="padding-left: 16px;">（2）微信开放平台（微信登录服务）；</p>
  <p style="padding-left: 16px;">（3）短信服务商（发送验证码）。</p>
  <p><b>这些第三方服务有各自的隐私政策，我们建议您阅读了解。</b></p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">七、未成年人保护</p>
  <p><b>本应用主要面向成年人。如果您是未满14周岁的未成年人，请在监护人的指导和同意下使用本应用。</b>我们不会主动收集未成年人的个人信息。如发现我们在未获得监护人同意的情况下收集了未成年人的信息，我们将尽快删除。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">八、隐私政策的更新</p>
  <p>我们可能会根据业务调整或法律法规要求更新本隐私政策。更新后的政策将在本应用内发布。</p>
  <p><b>对于重大变更，我们将通过应用内弹窗或推送通知等方式向您告知。</b>如您在政策更新后继续使用本应用，即视为您同意更新后的政策。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">九、联系方式</p>
  <p>如您对本隐私政策有任何疑问、意见或建议，或希望行使您的个人信息权利，可通过以下方式联系我们：</p>
  <p style="padding-left: 16px;">邮箱：feedback@chuanbill.com</p>
  <p>我们将在收到您的请求后15个工作日内予以回复。</p>
</div>
`
const from = ref('')

onLoad((options) => {
  from.value = options?.from
  userStore.showLoginPopup = false
})

onBeforeUnmount(() => {
  if (from.value === 'login') {
    userStore.showLoginPopup = true
  }
})
</script>

<template>
  <view class="min-h-screen bg-white dark:bg-[var(--wot-dark-background)]">
    <!-- 通用隐私政策内容 -->
    <rich-text :nodes="baseContent" />
  </view>
</template>
