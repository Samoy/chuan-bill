import type { MessageOptions, MessageResult } from 'wot-design-uni/components/wd-message-box/types'
import { defineStore } from 'pinia'

export type GlobalMessageOptions = MessageOptions & {
  success?: (res: MessageResult) => void
  fail?: (res: MessageResult) => void
}

interface GlobalMessage {
  messageOptions: GlobalMessageOptions | null
  currentPage: string
}

export const useGlobalMessage = defineStore('global-message', {
  state: (): GlobalMessage => ({
    messageOptions: null,
    currentPage: '',
  }),
  actions: {
    show(option: GlobalMessageOptions | string) {
      const isOptionStr = CommonUtil.isString(option)
      this.currentPage = getCurrentPath()
      this.messageOptions = {
        ...isOptionStr ? { title: option } : option,
        cancelButtonProps: {
          ...(isOptionStr ? { } : option.cancelButtonProps),
        },
        confirmButtonProps: {
          ...(isOptionStr ? {} : option.confirmButtonProps),
        },
        zIndex: 999999,
      }
    },
    alert(option: GlobalMessageOptions | string) {
      const messageOptions = CommonUtil.deepMerge({ type: 'alert' }, CommonUtil.isString(option) ? { title: option } : option) as MessageOptions
      messageOptions.showCancelButton = false
      this.show(messageOptions)
    },
    confirm(option: GlobalMessageOptions | string) {
      const messageOptions = CommonUtil.deepMerge({ type: 'confirm' }, CommonUtil.isString(option) ? { title: option } : option) as MessageOptions
      messageOptions.showCancelButton = true
      this.show(messageOptions)
    },
    prompt(option: GlobalMessageOptions | string) {
      const messageOptions = CommonUtil.deepMerge({ type: 'prompt' }, CommonUtil.isString(option) ? { title: option } : option) as MessageOptions
      messageOptions.showCancelButton = true
      this.show(messageOptions)
    },
    close() {
      this.messageOptions = null
      this.currentPage = ''
    },
  },
})
