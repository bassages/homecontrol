import "rxjs/Rx";
import {ChartConfiguration} from "c3";

const defaultChartPadding = { top: 10, bottom: 25, left: 55, right: 20 };

const minimumChartHeight: number = 220;
const maximumChartHeight: number = 500;

export class ChartService {

  public getEmptyChartConfig(): ChartConfiguration {
    return {
      data: { json: {} },
      legend: { show: false },
      axis: {
        x: { tick: { values: [] } },
        y: { tick: { values: [] } }
      },
      padding: defaultChartPadding
    };
  };

  public adjustChartHeightToAvailableWindowHeight(chart: any) {
    const rect = chart.element.getBoundingClientRect();

    let height = window.innerHeight - rect.top - 10;

    if (height < minimumChartHeight) {
      height = minimumChartHeight;
    } else if (height > maximumChartHeight) {
      height = maximumChartHeight;
    }
    chart.resize({height: height});
  }

  public getDefaultChartPadding() {
    return defaultChartPadding;
  }
}
