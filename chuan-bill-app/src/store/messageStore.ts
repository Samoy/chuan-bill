import type { MessageListDTO, MessageVO, UnreadCountVO } from '@/api/globals'

export const useMessageStore = defineStore('message', () => {
  const user = useUserStore()

  // 未读消息数量
  const unreadCount = ref<UnreadCountVO>({ total: 0, familyCount: 0 })
  // 消息列表
  const messageList = ref<MessageVO[]>([])
  // 是否有未读消息
  const hasUnread = computed(() => (unreadCount.value?.total || 0) > 0)
  // 加载状态
  const messageListLoading = ref(false)

  /**
   * 获取未读消息数量
   */
  async function fetchUnreadCount() {
    if (!user.isLoggedIn)
      return
    try {
      const res = await Apis.message.getUnreadCount({ meta: { slient: true } })
      if (res.success && res.data) {
        unreadCount.value = res.data
      }
    }
    catch {
      // 静默失败
    }
  }

  /**
   * 获取消息列表
   */
  async function fetchMessageList(params: MessageListDTO) {
    if (!user.isLoggedIn)
      return
    messageListLoading.value = true
    try {
      const res = await Apis.message.getMessageList({ params: { ...params } })
      if (res.success && res.data) {
        if (params.page === 1) {
          messageList.value = res.data.records || []
        }
        else {
          messageList.value.push(...(res.data.records || []))
        }
        return res.data
      }
    }
    finally {
      messageListLoading.value = false
    }
  }

  /**
   * 标记消息已读
   */
  async function markAsRead(id: string) {
    const res = await Apis.message.markAsRead({ params: { id } })
    if (res.success) {
      // 更新本地消息状态
      const msg = messageList.value.find(m => m.id === id)
      if (msg) {
        msg.status = 1
      }
      // 减少未读数
      if ((unreadCount.value?.total || 0) > 0) {
        unreadCount.value.total = (unreadCount.value?.total || 0) - 1
      }
      if (msg?.type === 'family' && (unreadCount.value?.familyCount || 0) > 0) {
        unreadCount.value.familyCount = (unreadCount.value?.familyCount || 0) - 1
      }
      return true
    }
    return false
  }

  /**
   * 全部标记已读
   */
  async function markAllAsRead() {
    const res = await Apis.message.markAllAsRead()
    if (res.success) {
      messageList.value.forEach(m => m.status = 1)
      unreadCount.value = { total: 0, familyCount: 0 }
      return true
    }
    return false
  }

  /**
   * 重置状态
   */
  function reset() {
    unreadCount.value = { total: 0, familyCount: 0 }
    messageList.value = []
    messageListLoading.value = false
  }

  return {
    unreadCount,
    messageList,
    hasUnread,
    messageListLoading,
    fetchUnreadCount,
    fetchMessageList,
    markAsRead,
    markAllAsRead,
    reset,
  }
})
