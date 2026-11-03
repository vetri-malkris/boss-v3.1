import { Component, computed, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import type { EChartsCoreOption } from 'echarts/core';
import { Chart } from '../../../../shared/components/chart/chart';
import { CashFlowPoint } from '../../../../shared/models/dashboard.model';

/**
 * Net cash flow (bars) and the cash balance it moves (line) - both built only from
 * the balance actually recorded at upload time, never derived from revenue/expenses.
 * A month with no recorded balance simply leaves a gap rather than a fabricated value.
 */
@Component({
  selector: 'app-cash-flow-chart',
  imports: [MatCardModule, Chart],
  templateUrl: './cash-flow-chart.html',
  styleUrl: './cash-flow-chart.css',
})
export class CashFlowChart {
  readonly series = input.required<CashFlowPoint[]>();

  readonly hasAnyRecordedBalance = computed(() => this.series().some((point) => point.cashBalance != null));

  readonly option = computed<EChartsCoreOption>(() => {
    const series = this.series();
    const months = series.map((point) => point.month);
    return {
      backgroundColor: 'transparent',
      textStyle: { color: '#3d4759' },
      grid: { left: 56, right: 56, top: 56, bottom: 40, containLabel: false },
      legend: { top: 4, left: 'center', data: ['Net cash flow', 'Cash balance'], textStyle: { color: '#3d4759' } },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: months, axisLine: { lineStyle: { color: '#c3cbd6' } } },
      yAxis: [
        { type: 'value', name: 'Net change', axisLine: { lineStyle: { color: '#c3cbd6' } }, splitLine: { lineStyle: { color: '#dde2ea' } } },
        { type: 'value', name: 'Balance', axisLine: { lineStyle: { color: '#c3cbd6' } }, splitLine: { show: false } },
      ],
      series: [
        {
          name: 'Net cash flow', type: 'bar', yAxisIndex: 0, data: series.map((point) => point.netChange),
          itemStyle: { color: '#6FA8D8', borderRadius: [6, 6, 0, 0] },
        },
        {
          name: 'Cash balance', type: 'line', yAxisIndex: 1, data: series.map((point) => point.cashBalance),
          itemStyle: { color: '#2C5F8A' }, lineStyle: { color: '#2C5F8A', width: 2.5 }, smooth: true, connectNulls: true,
        },
      ],
    };
  });
}
