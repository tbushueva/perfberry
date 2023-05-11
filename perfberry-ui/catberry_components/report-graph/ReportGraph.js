'use strict';

class ReportGraph {

	_findBuild(builds) {
		const buildId = parseInt(this.$context.attributes.build);
		const index = parseInt(this.$context.attributes.index);

		return builds.find(build => build.id === buildId).graphs[index];
	}

	/**
	 * Gets data for the component's template.
	 * @returns {Promise<Object>} Promise of data.
	 */
	render() {
		return this.$context.getStoreData()
			.then(data => this._findBuild(data.builds));
	}

	bind() {
		this.$context.getStoreData()
			.then(data => {
				const graph = this._findBuild(data.builds);

				const Highcharts = require('highcharts');
				require('highcharts/modules/exporting')(Highcharts);

				function syncExtremes(e) {
					const thisChart = this.chart;

					if (e.trigger !== 'syncExtremes') { // Prevent feedback loop
						Highcharts.each(Highcharts.charts, chart => {
							if (chart !== thisChart) {
								if (chart.xAxis[0].setExtremes) { // It is null while updating
									chart.xAxis[0].setExtremes(e.min, e.max, undefined, false, {trigger: 'syncExtremes'});
								}
							}
						});
					}
				}

				Highcharts.chart(graph.container, {
					chart: {
						height: 200 + graph.data.series.length * 10,
						spacingBottom: 0,
						spacingTop: 5,
						style: {
							fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, "Helvetica Neue", Arial, sans-serif'
						},
						type: 'area',
						zoomType: 'x'
					},
					colors: ['#2185D0', '#00B5AD', '#6435C9', '#E03997', '#A333C8'],
					credits: {
						enabled: true
					},
					exporting: {
						enabled: false
					},
					legend: {
						itemStyle: {
							fontWeight: 'normal'
						}
					},
					plotOptions: {
						series: {
							marker: {
								enabled: false,
								states: {
									hover: {
										radiusPlus: 0
									}
								},
								symbol: 'circle'
							},
							fillOpacity: 0.1,
							lineWidth: 1,
							states: {
								hover: {
									lineWidthPlus: 0
								}
							}
						}
					},
					xAxis: {
						crosshair: true,
						events: {
							setExtremes: syncExtremes
						},
						maxPadding: 0,
						minPadding: 0,
						minorTickInterval: 'auto',
						type: 'datetime'
					},
					yAxis: {
						lineWidth: 1,
						title: {
							text: null
						}
					},
					series: graph.data.series.map(series => {
						const description = series.description ? ` (${series.description})` : '';
						return {
							data: series.x.map((x, i) => {
								return {
									x: parseInt(x + '000'),
									y: series.values[i]
								}
							}),
							name: series.name + description
						};
					}),
					title: {
						text: null
					},
					tooltip: {
						borderWidth: 0,
						shadow: false,
						shared: true,
						valueSuffix: ' ' + graph.unit
					}
				});
			});
	}
}

module.exports = ReportGraph;
