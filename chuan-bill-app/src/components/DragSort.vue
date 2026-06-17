<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, onMounted, ref, useSlots, watch } from 'vue'
import { sleep } from '@/utils/index'

interface ListItem {
  id: string | number
  label?: string
  draggable?: boolean
  x?: number
  y?: number
  [key: string]: any
}

interface Props {
  initialList?: ListItem[]
  draggable?: boolean
  vibrate?: boolean
  direction?: 'vertical' | 'horizontal' | 'all'
  /** 列数属性，用于all模式 */
  columns?: number
}

const props = withDefaults(defineProps<Props>(), {
  initialList: () => [],
  draggable: true,
  vibrate: true,
  direction: 'vertical',
  columns: 3,
})

const emit = defineEmits<{
  'drag-end': [list: ListItem[]]
}>()

const instance = getCurrentInstance()
const slots = useSlots()

const list = ref<ListItem[]>([])
const dragIndex = ref(-1)
const sortChanged = ref(false)
const itemHeight = ref(0)
const itemWidth = ref(0)
const areaWidth = ref(0) // 可拖动区域宽度
const areaHeight = ref(0) // 可拖动区域高度
const currentPosition = ref({ x: 0, y: 0 })

let timer: ReturnType<typeof setTimeout> | null = null

const movableAreaStyle = computed(() => {
  if (props.direction === 'vertical') {
    return {
      height: itemHeight.value ? `${list.value.length * itemHeight.value}px` : 'auto',
      width: '100%',
    }
  }
  else if (props.direction === 'horizontal') {
    return {
      height: itemHeight.value ? `${itemHeight.value}px` : 'auto',
      width: itemWidth.value ? `${list.value.length * itemWidth.value}px` : 'auto',
    }
  }
  else {
    // all模式，计算网格布局所需的高度
    const rows = Math.ceil(list.value.length / props.columns)
    return {
      height: itemHeight.value ? `${rows * itemHeight.value}px` : 'auto',
      width: '100%',
    }
  }
})

watch(() => props.initialList, () => {
  nextTick(() => {
    initList()
  })
})

watch(() => props.direction, () => {
  nextTick(() => {
    initList()
    calculateItemSize()
    calculateAreaSize()
  })
})

watch(() => props.columns, () => {
  if (props.direction === 'all') {
    nextTick(() => {
      initList()
      updatePositions()
    })
  }
})

onMounted(async () => {
  await nextTick()
  initList()
  calculateItemSize()
  calculateAreaSize()
})

function initList() {
  // 初始化列表项的位置
  list.value = props.initialList.map((item, index) => {
    let x: number | undefined
    let y: number | undefined

    if (props.direction === 'horizontal' && itemWidth.value) {
      x = index * itemWidth.value
      y = 0
    }
    else if (props.direction === 'vertical' && itemHeight.value) {
      x = 0
      y = index * itemHeight.value
    }
    else if (itemWidth.value && itemHeight.value) {
      // all模式，网格布局
      const col = index % props.columns
      const row = Math.floor(index / props.columns)
      x = col * itemWidth.value
      y = row * itemHeight.value
    }

    return {
      ...item,
      x,
      y,
    }
  })
}

async function calculateItemSize() {
  // 计算项目尺寸
  await sleep(30)
  return new Promise((resolve) => {
    uni.createSelectorQuery()
      .in(instance)
      .select('.u-dragsort-item-content')
      .boundingClientRect((res: any) => {
        if (res) {
          itemHeight.value = res.height || 40
          itemWidth.value = res.width || 80

          // 更新所有项目的位置
          updatePositions()
        }
        resolve(res)
      })
      .exec()
  })
}

async function calculateAreaSize() {
  // 计算可拖动区域尺寸
  await sleep(30)
  return new Promise((resolve) => {
    uni.createSelectorQuery()
      .in(instance)
      .select('.u-dragsort-area')
      .boundingClientRect((res: any) => {
        if (res) {
          areaWidth.value = res.width || 300
          areaHeight.value = res.height || 300
        }
        resolve(res)
      })
      .exec()
  })
}

function updatePositions(isDragging?: boolean) {
  // 更新所有项目的位置
  list.value = list.value.map((item, index) => {
    // 当前正在拖动的项目保持拖动位置不动，避免抖动
    if (isDragging && dragIndex.value === index) {
      return item
    }

    if (props.direction === 'vertical') {
      return {
        ...item,
        x: 0,
        y: index * itemHeight.value,
      }
    }

    if (props.direction === 'horizontal') {
      return {
        ...item,
        x: index * itemWidth.value,
        y: 0,
      }
    }

    // all模式，网格布局
    const col = index % props.columns
    const row = Math.floor(index / props.columns)

    return {
      ...item,
      x: col * itemWidth.value,
      y: row * itemHeight.value,
    }
  })
}

function onTouchStart(index: number, e: any) {
  if (slots.handler && e.currentTarget.dataset.action !== 'handler') {
    return
  }
  if (list.value[index]?.draggable === false)
    return
  if (timer)
    clearTimeout(timer)
  sortChanged.value = false
  dragIndex.value = index
}

function onTouchMove(e: any) {
  if (dragIndex.value !== -1) {
    // 目前只对H5生效, 如果该组件放置在开启了下拉刷新的scroll-view中, 向下拉动item还是会触发下拉刷新
    e.stopPropagation()
    e.preventDefault()
  }
}

function onChange(index: number, event: any) {
  if (!event.detail.source || event.detail.source !== 'touch')
    return

  currentPosition.value.x = event.detail.x
  currentPosition.value.y = event.detail.y

  // all模式下使用更智能的位置计算
  if (props.direction === 'all') {
    handleAllModeChange(index)
  }
  else {
    // 原有的垂直和水平模式逻辑
    let itemSize = 0
    let targetIndex = -1

    if (props.direction === 'vertical') {
      itemSize = itemHeight.value
      targetIndex = Math.max(0, Math.min(
        Math.round(currentPosition.value.y / itemSize),
        list.value.length - 1,
      ))
    }
    else if (props.direction === 'horizontal') {
      itemSize = itemWidth.value
      targetIndex = Math.max(0, Math.min(
        Math.round(currentPosition.value.x / itemSize),
        list.value.length - 1,
      ))
    }

    // 如果位置发生变化，则重新排序
    if (targetIndex !== index) {
      reorderItems(index, targetIndex)
    }
  }
}

function handleAllModeChange(index: number) {
  // 在all模式下，根据当前位置计算最近的网格位置
  const col = Math.max(0, Math.min(Math.round(currentPosition.value.x / itemWidth.value), props.columns - 1))
  const row = Math.max(0, Math.round(currentPosition.value.y / itemHeight.value))

  // 计算目标索引
  let targetIndex = row * props.columns + col
  targetIndex = Math.max(0, Math.min(targetIndex, list.value.length - 1))

  // 如果位置发生变化，则重新排序
  if (targetIndex !== index) {
    reorderItems(index, targetIndex)
  }
}

function reorderItems(fromIndex: number, toIndex: number) {
  const movedItem = list.value.splice(fromIndex, 1)[0]
  list.value.splice(toIndex, 0, movedItem)

  // 更新当前拖拽项目的新索引
  dragIndex.value = toIndex
  sortChanged.value = true

  // 更新所有项目的位置
  updatePositions(true)

  // 震动反馈
  if (props.vibrate && uni.vibrateShort) {
    uni.vibrateShort({ type: 'light' })
  }
}

function onTouchEnd() {
  // 未发生位移
  if (dragIndex.value === -1)
    return

  // 0.001是为了解决拖动过快等某些极限场景下位置还原不生效问题
  if (props.direction === 'horizontal') {
    list.value[dragIndex.value].x = currentPosition.value.x + 0.001
  }
  else if (props.direction === 'vertical' || props.direction === 'all') {
    list.value[dragIndex.value].y = currentPosition.value.y + 0.001
    list.value[dragIndex.value].x = currentPosition.value.x + 0.001
  }

  // 重置到位置，需要延迟触发动，否则无效。
  sleep(50).then(() => {
    updatePositions()
    if (sortChanged.value) {
      emit('drag-end', [...list.value])
      sortChanged.value = false
    }
    timer = setTimeout(() => {
      dragIndex.value = -1
    }, 600)
  })
}
</script>

<template>
  <view
    class="u-dragsort"
    :class="[direction === 'horizontal' ? 'u-dragsort--horizontal' : '', direction === 'vertical' ? 'u-dragsort--vertical' : '', direction === 'all' ? 'u-dragsort--all' : '']"
    :style="movableAreaStyle"
  >
    <movable-area class="u-dragsort-area">
      <movable-view
        v-for="(item, index) in list" :id="`u-dragsort-item-${index}`" :key="item.id"
        class="u-dragsort-item" :class="{ dragging: dragIndex === index, disabled: !draggable || item.draggable === false }"
        :direction="direction === 'all' ? 'all' : direction" :x="item.x" :y="item.y" :inertia="false"
        :disabled="!draggable || dragIndex === -1 || item.draggable === false" @change="onChange(index, $event)"
        @touchstart="onTouchStart(index, $event)" @touchend="onTouchEnd" @touchcancel="onTouchEnd" @touchmove="onTouchMove"
      >
        <view class="u-dragsort-item-content">
          <view
            v-if="$slots.handler"
            class="ui-dragSort-item-handler"
            data-action="handler"
            @touchstart="onTouchStart(index, $event)"
          >
            <slot name="handler" :item="item" :index="index" />
          </view>
          <slot :item="item" :index="index">
            {{ item.label }}
          </slot>
        </view>
      </movable-view>
    </movable-area>
  </view>
</template>

<style scoped lang="scss">
.u-dragsort {
    width: 100%;
    height: auto;

    .u-dragsort-area {
        width: 100%;
        height: 100%;
        position: relative;
    }

    .u-dragsort-item {
        position: absolute;
        width: 100%;
        transition: box-shadow 0.45s ease-out;
        cursor: pointer;
        padding-right: 20px;
        box-sizing: border-box;

        &.dragging {
            z-index: 1000;
            box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
        }

        .u-dragsort-item-content {
            position: relative;
            padding: 0;
            box-sizing: border-box;
            border: 1px solid var(--up-border-color, rgba(125, 126, 128, 0.35));
            border-radius: 8px;
            background-color: var(--up-card-bg-color, #ffffff);
        }
    }

    &.u-dragsort--vertical {
        .u-dragsort-item {
            height: auto;
        }
    }

    &.u-dragsort--horizontal {
        .u-dragsort-area {
            display: flex;
            white-space: nowrap;
        }

        .u-dragsort-item {
            width: auto;
            height: auto;
        }
    }

    &.u-dragsort--all {
      .u-dragsort-item {
            width: auto;
            height: auto;
        }
    }
}
</style>
