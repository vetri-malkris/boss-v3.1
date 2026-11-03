import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  effect,
  input,
} from '@angular/core';
import type { EChartsCoreOption } from 'echarts/core';
import { echarts } from './echarts-setup';

/**
 * Thin ECharts wrapper: pass an option object in, get a resizing chart out.
 * Kept generic (not tied to financial data) so any future dashboard chart reuses it.
 */
@Component({
  selector: 'app-chart',
  template: '<div #host class="chart-host"></div>',
  styles: [
    `
      :host {
        display: block;
      }
      .chart-host {
        width: 100%;
        height: 100%;
      }
    `,
  ],
})
export class Chart implements AfterViewInit, OnDestroy {
  readonly option = input.required<EChartsCoreOption>();

  @ViewChild('host', { static: true }) private hostRef!: ElementRef<HTMLDivElement>;

  private instance: echarts.ECharts | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private readonly resizeHandler = () => this.instance?.resize();

  constructor() {
    effect(() => {
      const option = this.option();
      this.instance?.setOption(option, true);
    });
  }

  ngAfterViewInit(): void {
    this.instance = echarts.init(this.hostRef.nativeElement, null, { renderer: 'canvas' });
    this.instance.setOption(this.option(), true);
    window.addEventListener('resize', this.resizeHandler);

    // The host element isn't always at its final layout size on first paint
    // (e.g. inside a card that reflows after fonts/box-shadow settle, or a
    // sidebar that later collapses/expands) - a ResizeObserver catches that
    // and re-measures, unlike the window resize listener alone.
    this.resizeObserver = new ResizeObserver(() => this.instance?.resize());
    this.resizeObserver.observe(this.hostRef.nativeElement);
  }

  ngOnDestroy(): void {
    window.removeEventListener('resize', this.resizeHandler);
    this.resizeObserver?.disconnect();
    this.instance?.dispose();
  }
}
