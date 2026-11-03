import { Component, computed, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import type { EChartsCoreOption } from 'echarts/core';
import { Chart } from '../../../../shared/components/chart/chart';
import { MonthlyBreakdown } from '../../../../shared/models/ingestion.model';

@Component({
  selector: 'app-financial-chart',
  imports: [MatCardModule, Chart],
  templateUrl: './financial-chart.html',
  styleUrl: './financial-chart.css',
})
export class FinancialChart {
  readonly series = input.required<MonthlyBreakdown[]>();

  readonly option = computed<EChartsCoreOption>(() => {
    const series = this.series();
    const months = series.map((m) => m.month);
    return {
      backgroundColor: 'transparent',
      textStyle: { color: '#3d4759' },
      grid: { left: 56, right: 16, top: 56, bottom: 40, containLabel: false },
      legend: { top: 4, left: 'center', data: ['Revenue', 'Expenses', 'Profit'], textStyle: { color: '#3d4759' } },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: months, axisLine: { lineStyle: { color: '#c3cbd6' } } },
      yAxis: { type: 'value', axisLine: { lineStyle: { color: '#c3cbd6' } }, splitLine: { lineStyle: { color: '#dde2ea' } } },
      series: [
        {
          name: 'Revenue', type: 'bar', data: series.map((m) => m.revenue),
          itemStyle: { color: '#6FA8D8', borderRadius: [6, 6, 0, 0] },
        },
        {
          name: 'Expenses', type: 'bar', data: series.map((m) => m.expenses),
          itemStyle: { color: '#B8CFE8', borderRadius: [6, 6, 0, 0] },
        },
        {
          name: 'Profit', type: 'line', data: series.map((m) => m.profit),
          itemStyle: { color: '#2C5F8A' }, lineStyle: { color: '#2C5F8A', width: 2.5 }, smooth: true,
        },
      ],
    };
  });
}
