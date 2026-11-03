import * as echarts from 'echarts/core';
import { BarChart, LineChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

/**
 * Registers only the ECharts pieces this app actually uses (bar + line, one
 * dashboard chart) instead of pulling in the full library, per the bundle
 * budget in angular.json.
 */
echarts.use([BarChart, LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

export { echarts };
