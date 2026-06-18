import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { DataZoomComponent, GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { provideEcharts } from 'uni-echarts/shared'

echarts.use([PieChart, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer, DataZoomComponent])

// 颜色列表
export const CHART_COLORS = [
  '#2563EB',
  '#FF7D00',
  '#07C160',
  '#FF69B4',
  '#8A2BE2',
  '#FF4757',
]

echarts.registerTheme('morandi', {
  color: CHART_COLORS,
  backgroundColor: 'transparent',
})

export function setupEcharts() {
  provideEcharts(echarts)
}
