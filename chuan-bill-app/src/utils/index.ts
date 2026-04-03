import { e } from '@unocss/core'
import dayjs from 'dayjs'
/**
 * 获取当前页面路径
 * @returns 当前页面路径
 */
export function getCurrentPath() {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  return currentPage.route || ''
}

/**
 * 将十六进制颜色转换为 RGB 字符串，支持透明通道
 * @param hex 十六进制颜色字符串，支持格式：
 *            - 3 位（如 'fff' 或 '#fff'）
 *            - 4 位（如 'ffff' 或 '#ffff'，包含 alpha 通道）
 *            - 6 位（如 'ffffff' 或 '#ffffff'）
 *            - 8 位（如 'ffffffff' 或 '#ffffffff'，包含 alpha 通道）
 * @returns RGB 字符串，格式：
 *          - 无 alpha：'255 255 255'
 *          - 有 alpha：'255 255 255 / 0.5'
 *          若输入无效则返回空字符串
 */
export function hexToRgbString(hex: string): string {
  // 去除前缀 # 并转为小写
  let raw = hex.trim()
  if (raw.startsWith('#')) {
    raw = raw.slice(1)
  }
  raw = raw.toLowerCase()

  let r = 0
  let g = 0
  let b = 0
  let alpha: number | null = null

  // 根据长度处理
  if (raw.length === 3) {
    // 3 位: 'abc' -> 'aa bb cc'
    r = Number.parseInt(raw[0] + raw[0], 16)
    g = Number.parseInt(raw[1] + raw[1], 16)
    b = Number.parseInt(raw[2] + raw[2], 16)
  }
  else if (raw.length === 4) {
    // 4 位: 'abcd' -> 'aa bb cc' + alpha 'dd'
    r = Number.parseInt(raw[0] + raw[0], 16)
    g = Number.parseInt(raw[1] + raw[1], 16)
    b = Number.parseInt(raw[2] + raw[2], 16)
    alpha = Number.parseInt(raw[3] + raw[3], 16)
  }
  else if (raw.length === 6) {
    // 6 位: 'aabbcc'
    r = Number.parseInt(raw.slice(0, 2), 16)
    g = Number.parseInt(raw.slice(2, 4), 16)
    b = Number.parseInt(raw.slice(4, 6), 16)
  }
  else if (raw.length === 8) {
    // 8 位: 'aabbccdd'
    r = Number.parseInt(raw.slice(0, 2), 16)
    g = Number.parseInt(raw.slice(2, 4), 16)
    b = Number.parseInt(raw.slice(4, 6), 16)
    alpha = Number.parseInt(raw.slice(6, 8), 16)
  }
  else {
    // 不支持的格式
    throw new Error('Invalid hex color format')
  }

  // 构建 RGB 部分
  const rgbPart = `${r} ${g} ${b}`

  // 若有 alpha 通道，则添加透明度部分
  if (alpha !== null) {
    const alphaValue = (alpha / 255).toString() // 直接输出小数，如 0.5, 0.5019607843137255
    return `${rgbPart} / ${alphaValue}`
  }

  return rgbPart
}

/**
 * 转换 UnoCSS 语法的 CSS 类名
 * @param cssName CSS 类名
 * @returns 转换后的 CSS 类名
 */
export function transformUnoCSS(cssName: string) {
  // #ifdef MP
  const unsupportedChars = ['.', ':', '%', '!', '#', '(', ')', '[', '/', ']', ',', '$', '{', '}', '@', '+', '^', '&', '<', '>', '\'', '\\', '"', '?', '*']
  const esacpedUnsupportedChars = unsupportedChars.map(char => e(char))
  return cssName.replace(new RegExp(`[${esacpedUnsupportedChars.join('')}]`, 'g'), '_a_')
  // #endif
  return cssName
}

/**
 * 友好化显示时间
 * @param t 时间
 * @returns 友好时间
 */
export function friendlyTime(t: string | number | Date): string {
  const d = dayjs(t)
  const n = dayjs()
  const f = 'HH:mm'
  const s = d.format('YYYY-MM-DD')
  const w = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  if (!d.isValid()) {
    return '未知时间'
  }
  if (s === n.format('YYYY-MM-DD')) {
    return d.format(f)
  }
  if (s === n.subtract(1, 'd').format('YYYY-MM-DD')) {
    return `昨天 ${d.format(f)}`
  }
  if (n.diff(d, 'd') < 7) {
    return `${w[d.day()]}` + ` ${d.format(f)}`
  }
  return d.format(d.year() === n.year() ? `MM-DD ${f}` : `YYYY-MM-DD ${f}`)
}
