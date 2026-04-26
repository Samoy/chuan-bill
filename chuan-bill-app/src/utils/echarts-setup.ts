import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { DataZoomComponent, GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { provideEcharts } from 'uni-echarts/shared'

echarts.use([PieChart, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer, DataZoomComponent])

export function setupEcharts() {
  provideEcharts(echarts)
}
