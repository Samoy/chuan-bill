<script setup lang="ts">
definePage({
  name: 'agreement',
  layout: 'default',
  style: {
    navigationBarTitleText: '用户协议',
  },
})

const userStore = useUserStore()

// 通用协议条款
const baseContent = `
<div style="padding: 0 16px; font-size: 14px; line-height: 1.8; color: #333;">
  <p style="text-align: center; font-size: 16px; font-weight: bold; margin-bottom: 16px;">小川记账用户协议</p>
  <p style="color: #999; text-align: center; margin-bottom: 24px;">更新日期：2026年4月17日 | 生效日期：2026年4月17日</p>

  <p>欢迎使用"小川记账"（以下简称"本应用"）。请您在使用本应用前，仔细阅读并充分理解本协议的全部内容。<b>您使用本应用即表示您已阅读并同意本协议的约束。</b></p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">一、服务说明</p>
  <p>1.1 本应用是一款个人及家庭记账工具，为用户提供账单记录、统计分析、数据管理等服务。</p>
  <p>1.2 本应用提供两种使用模式：</p>
  <p style="padding-left: 16px;">（1）<b>未登录（游客）模式</b>：您无需注册或登录即可使用基础记账功能，数据仅保存在您的本地设备中。</p>
  <p style="padding-left: 16px;">（2）<b>已登录（注册用户）模式</b>：通过手机号或微信登录后，您可享受云端同步、多设备访问、图片记账、语音记账等高级功能。</p>
  <p>1.3 我们有权根据业务发展需要，对服务内容进行调整，并将以适当方式通知您。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">二、账号注册与管理</p>
  <p>2.1 <b>如果您选择注册或登录本应用</b>，您需要提供真实、准确的手机号码或通过微信授权登录。您应妥善保管账号信息，因您个人原因导致的账号安全问题由您自行承担。</p>
  <p>2.2 如果您仅以游客模式使用，则无需提供任何个人信息，但本地数据在设备卸载或清除后将无法恢复。</p>
  <p>2.3 <b>您不得将账号转让、出借或以任何方式提供给他人使用。</b></p>
  <p>2.4 如发现账号被盗用或存在安全风险，请立即联系我们处理。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">三、用户行为规范</p>
  <p>3.1 您在使用本应用时应遵守中华人民共和国相关法律法规，不得利用本应用从事任何违法违规活动。</p>
  <p>3.2 <b>您不得利用本应用进行以下行为：</b></p>
  <p style="padding-left: 16px;">（1）上传、发布含有违法、色情、暴力、虚假等内容的信息；</p>
  <p style="padding-left: 16px;">（2）干扰本应用的正常运行，包括但不限于攻击服务器、篡改数据等；</p>
  <p style="padding-left: 16px;">（3）利用本应用侵犯他人合法权益；</p>
  <p style="padding-left: 16px;">（4）对本应用进行反编译、反汇编或其他逆向工程操作。</p>
  <p>3.3 如您违反上述规范，我们有权暂停或终止向您提供服务，并保留追究法律责任的权利。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">四、数据与知识产权</p>
  <p>4.1 您在本应用中创建的账单数据归您所有。我们不会未经您许可将您的数据用于与本应用服务无关的用途。</p>
  <p>4.2 <b>本应用的所有软件、技术、界面设计、图标、文案等知识产权均归本应用开发者所有</b>，未经书面许可，不得复制、修改或以任何方式使用。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">五、免责声明</p>
  <p>5.1 <b>本应用按"现状"提供服务，不对服务的及时性、安全性、准确性做任何明示或暗示的保证。</b></p>
  <p>5.2 因不可抗力（包括但不限于自然灾害、网络故障、政策变更等）导致的服务中断，本应用不承担责任。</p>
  <p>5.3 对于游客模式下因设备损坏、卸载应用等导致的本地数据丢失，本应用不承担任何责任。<b>建议您及时登录并同步数据至云端。</b></p>
  <p>5.4 对于AI识别功能（如图片记账、语音记账），识别结果仅供参考，本应用不保证其完全准确，请您在确认后保存。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">六、服务变更与终止</p>
  <p>6.1 我们有权根据业务需要修改或终止部分或全部服务，并会提前以应用内公告等适当方式通知您。</p>
  <p>6.2 您可随时停止使用本应用。<b>注册用户注销账号后，我们将在合理期限内删除您的个人信息和云端数据，但法律法规要求保留的除外。</b></p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">七、协议修改</p>
  <p>7.1 我们有权根据需要修订本协议，修订后的协议将在本应用内发布。</p>
  <p>7.2 <b>如您在协议修订后继续使用本应用，即视为您同意修订后的协议内容。</b>如您不同意，请停止使用本应用。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">八、法律适用与争议解决</p>
  <p>8.1 本协议的签订、履行、解释及争议解决均适用中华人民共和国法律。</p>
  <p>8.2 因本协议引起的任何争议，双方应友好协商解决；协商不成的，应提交本应用开发者所在地有管辖权的人民法院诉讼解决。</p>

  <p style="font-size: 15px; font-weight: bold; margin-top: 20px;">九、联系方式</p>
  <p>如您对本协议有任何疑问或建议，可通过以下方式联系我们：</p>
  <p style="padding-left: 16px;">邮箱：feedback@chuanbill.com</p>
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
    <!-- 通用协议内容 -->
    <rich-text :nodes="baseContent" />
  </view>
</template>
