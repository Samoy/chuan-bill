import type { FamilyJoinApplyVO, FamilyMemberVO, FamilyVO } from '@/api/globals'

export const useFamilyStore = defineStore('family', () => {
  const user = useUserStore()

  // 我所在的家庭列表
  const familyList = ref<FamilyVO[]>([])
  // 当前选中的家庭
  const currentFamily = ref<FamilyVO | null>(null)
  // 当前家庭的成员列表
  const memberList = ref<FamilyMemberVO[]>([])
  // 待处理的加入申请
  const pendingApplies = ref<FamilyJoinApplyVO[]>([])
  // 加载状态
  const familyListLoading = ref(false)
  const memberListLoading = ref(false)
  const applyListLoading = ref(false)

  // 当前选中的家庭ID
  const currentFamilyId = computed(() => currentFamily.value?.id || '')

  // 是否有家庭
  const hasFamily = computed(() => familyList.value.length > 0)

  /**
   * 获取我的家庭列表
   */
  async function fetchFamilyList() {
    if (!user.isLoggedIn)
      return
    familyListLoading.value = true
    try {
      const res = await Apis.family.getMyFamilies()
      if (res.success && res.data) {
        familyList.value = res.data
        // 如果没有选中家庭，自动选中第一个
        if (!currentFamily.value && familyList.value.length > 0) {
          currentFamily.value = familyList.value[0]
        }
      }
    }
    finally {
      familyListLoading.value = false
    }
  }

  /**
   * 获取家庭详情
   */
  async function fetchFamilyDetail(familyId: string) {
    if (!user.isLoggedIn)
      return
    const res = await Apis.family.getFamilyDetail({ params: { familyId } })
    if (res.success && res.data) {
      currentFamily.value = res.data
      // 更新列表中对应的家庭信息
      const index = familyList.value.findIndex(f => f.id === familyId)
      if (index !== -1) {
        familyList.value[index] = res.data
      }
    }
  }

  /**
   * 创建家庭
   */
  async function createFamily(data: { name: string, avatar?: string, description?: string }) {
    const res = await Apis.family.createFamily({ data })
    if (res.success && res.data) {
      familyList.value.unshift(res.data)
      currentFamily.value = res.data
      return res.data
    }
    return null
  }

  /**
   * 更新家庭信息
   */
  async function updateFamily(data: { id: string, name?: string, avatar?: string, description?: string }) {
    const res = await Apis.family.updateFamily({ data })
    if (res.success && res.data) {
      const index = familyList.value.findIndex(f => f.id === data.id)
      if (index !== -1) {
        familyList.value[index] = res.data
      }
      if (currentFamily.value?.id === data.id) {
        currentFamily.value = res.data
      }
      return res.data
    }
    return null
  }

  /**
   * 删除家庭
   */
  async function deleteFamily(familyId: string) {
    const res = await Apis.family.deleteFamily({ params: { familyId } })
    if (res.success) {
      familyList.value = familyList.value.filter(f => f.id !== familyId)
      if (currentFamily.value?.id === familyId) {
        currentFamily.value = familyList.value[0] || null
      }
      memberList.value = memberList.value.filter(m => m.familyId !== familyId)
      return true
    }
    return false
  }

  /**
   * 申请加入家庭
   */
  async function joinFamily(inviteCode: string, remark?: string) {
    const res = await Apis.family.joinFamily({ data: { inviteCode, remark } })
    return res.success ? res.data : null
  }

  /**
   * 退出家庭
   */
  async function leaveFamily(familyId: string) {
    const res = await Apis.family.leaveFamily({ params: { familyId } })
    if (res.success) {
      familyList.value = familyList.value.filter(f => f.id !== familyId)
      if (currentFamily.value?.id === familyId) {
        currentFamily.value = familyList.value[0] || null
      }
      memberList.value = memberList.value.filter(m => m.familyId !== familyId)
      return true
    }
    return false
  }

  /**
   * 移除家庭成员
   */
  async function removeMember(familyId: string, memberId: string) {
    const res = await Apis.family.removeMember({ data: { familyId, memberId } })
    if (res.success) {
      memberList.value = memberList.value.filter(m => m.id !== memberId)
      return true
    }
    return false
  }

  /**
   * 转让户主
   */
  async function transferOwner(familyId: string, targetUserId: string) {
    const res = await Apis.family.transferOwner({ data: { familyId, targetUserId } })
    if (res.success) {
      // 刷新家庭成员列表
      await fetchMembers(familyId)
      // 刷新家庭详情
      await fetchFamilyDetail(familyId)
      return true
    }
    return false
  }

  /**
   * 获取家庭成员列表
   */
  async function fetchMembers(familyId: string) {
    memberListLoading.value = true
    try {
      const res = await Apis.family.getMembers({ params: { familyId } })
      if (res.success && res.data) {
        memberList.value = res.data
      }
    }
    finally {
      memberListLoading.value = false
    }
  }

  /**
   * 获取待处理的加入申请
   */
  async function fetchPendingApplies(familyId: string) {
    applyListLoading.value = true
    try {
      const res = await Apis.family.getPendingApplies({ params: { familyId } })
      if (res.success && res.data) {
        pendingApplies.value = res.data
      }
    }
    finally {
      applyListLoading.value = false
    }
  }

  /**
   * 处理加入申请
   */
  async function handleJoinApply(applyId: string, familyId: string, approved: boolean) {
    const res = await Apis.family.handleJoinApply({ data: { applyId, familyId, approved } })
    if (res.success) {
      // 从待处理列表中移除
      pendingApplies.value = pendingApplies.value.filter(a => a.id !== applyId)
      // 如果同意，刷新成员列表
      if (approved) {
        await fetchMembers(familyId)
      }
      return true
    }
    return false
  }

  /**
   * 刷新邀请码
   */
  async function refreshInviteCode(familyId: string) {
    const res = await Apis.family.refreshInviteCode({ params: { familyId } })
    if (res.success && res.data) {
      if (currentFamily.value?.id === familyId) {
        currentFamily.value = { ...currentFamily.value, inviteCode: res.data }
      }
      const index = familyList.value.findIndex(f => f.id === familyId)
      if (index !== -1) {
        familyList.value[index] = { ...familyList.value[index], inviteCode: res.data }
      }
      return res.data
    }
    return null
  }

  /**
   * 重置状态
   */
  function reset() {
    familyList.value = []
    currentFamily.value = null
    memberList.value = []
    pendingApplies.value = []
    familyListLoading.value = false
    memberListLoading.value = false
    applyListLoading.value = false
  }

  return {
    familyList,
    currentFamily,
    memberList,
    pendingApplies,
    familyListLoading,
    memberListLoading,
    applyListLoading,
    currentFamilyId,
    hasFamily,
    fetchFamilyList,
    fetchFamilyDetail,
    createFamily,
    updateFamily,
    deleteFamily,
    joinFamily,
    leaveFamily,
    removeMember,
    transferOwner,
    fetchMembers,
    fetchPendingApplies,
    handleJoinApply,
    refreshInviteCode,
    reset,
  }
})
