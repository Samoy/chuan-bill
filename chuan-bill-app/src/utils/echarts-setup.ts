import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { DataZoomComponent, GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { provideEcharts } from 'uni-echarts/shared'

echarts.use([PieChart, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer, DataZoomComponent])

// 莫兰迪色系 - 低饱和度、柔和护眼
export const MORANDI_COLORS = [
  '#8D9BA3', // 灰蓝
  '#B5C4B1', // 灰绿
  '#C9B1A0', // 灰粉
  '#A3B5C4', // 雾蓝
  '#C4B5A3', // 米灰
  '#B1B5C4', // 薰衣草
  '#C4A3B5', // 玫瑰灰
  '#B5C4C4', // 青灰
  '#C4C4A3', // 暖灰
]

echarts.registerTheme('morandi', {
  color: MORANDI_COLORS,
  backgroundColor: 'transparent',
})

export function setupEcharts() {
  provideEcharts(echarts)
}
