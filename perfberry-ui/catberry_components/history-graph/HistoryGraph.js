'use strict';

const moment = require('moment');
const tz = require('moment-timezone');

class HistoryGraph {

	/**
	 * Gets data for the component's template.
	 * @returns {Promise<Object>} Promise of data.
	 */
	render() {
		const index = parseInt(this.$context.attributes.index);

		return this.$context.getStoreData()
			.then(data => {
				return {
					histories: data.graphs[index],
					index: index,
					isMultiMode: data.isMultiMode,
					fullscreenEnabled: data.fullscreenEnabled
				}
			});
	}

	zoom(from, to, isMultiMode, fullscreenEnabled) {
		return this.$context.sendAction('zoom', {
			from: from,
			to: to,
			isMultiMode: isMultiMode,
			fullscreenEnabled: fullscreenEnabled
		});
	}

	handleEditGraph(event) {
		event.preventDefault();
		event.stopPropagation();

		$(this.$context.element.querySelector('.ui.accordion'))
			.accordion('toggle', 0);
	}

	bind() {
		$(this.$context.element.querySelector('.ui.accordion')).accordion();

		const index = parseInt(this.$context.attributes.index);

		return this.$context.getStoreData()
			.then(data => {
				this.$context.element.querySelector('.ui.segment')
					.classList.remove('loading');

				const ranges = data.ranges;
				const histories = data.graphs[index];

				let height;
				if (data.fullscreenEnabled) {
					const pageHeight = window.innerHeight
						|| document.documentElement.clientHeight
						|| document.body.clientHeight;
					height = pageHeight - 150;
				} else {
					height = (data.isMultiMode ? 200 : 230) + histories.length * 10;
				}

				const Highcharts = require('highcharts');
				require('highcharts/modules/exporting')(Highcharts);

				Highcharts.setOptions({
					global: {
						useUTC: false
					},
					colors: ['#2185D0', '#00B5AD', '#6435C9', '#E03997', '#A333C8']
				});

				Highcharts.chart('container' + index, {
					chart: {
						events: {
							selection: (event) => {
								const from = moment(event.xAxis[0].min).toDate();
								const to = moment(event.xAxis[0].max).toDate();
								//TODO move to ProjectGraphs multimode and fullscreen state
								this.zoom(from, to, data.isMultiMode, data.fullscreenEnabled);

								return false;
							}
						},
						height: height,
						spacingBottom: 0,
						spacingTop: 5,
						style: {
							fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, "Helvetica Neue", Arial, sans-serif'
						},
						type: 'area',
						zoomType: 'x'
					},
					credits: {
						enabled: true
					},
					exporting: {
						enabled: false
					},
					legend: {
						layout: 'vertical',
						itemMarginBottom: 2,
						itemStyle: {
							fontWeight: 'normal'
						}
					},
					plotOptions: {
						series: {
							cursor: 'pointer',
							marker: {
								states: {
									hover: {
										radiusPlus: 0
									}
								},
								symbol: 'circle'
							},
							lineWidth: 1,
							fillOpacity: 0,
							point: {
								events: {
									click: (event) => {
										return this.$context
											.redirect(event.point.options.link);
									}
								}
							},
							softThreshold: true
						}
					},
					xAxis: {
						crosshair: true,
						max: ranges.current.to.dateUnix,
						min: ranges.current.from.dateUnix,
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
					series: histories.flatMap((series, i) => {
						return [{
							data: series.history.values.map(v => {
								return {
									x: v.key,
									y: v.value,
									selected: v.selected,
									link: v._reportLink,
									label: v._label,
									unit: series.history._unit,
								}
							}),
							color: Highcharts.getOptions().colors[i],
							marker: {
								enabled: true,
								radius: 4,
								symbol: 'circle'
							},
							name: series.history._title + ' on ' +
								series.history.settings.env + ' over ' +
								series.history.settings.search_name
						}, {
							data: series.history.values.map(v => {
								return {
									x: v.key,
									y: v.expected,
									selected: v.selected,
									link: v._reportLink,
									label: 'assertion threshold ' + v._label,
									unit: series.history._unit,
								}
							}),
							linkedTo: ':previous',
							color: Highcharts.getOptions().colors[i],
							dashStyle: 'Dash',
							marker: {
								enabled: false
							},
							name: series.history._title + ' on ' +
								series.history.settings.env + ' over ' +
								series.history.settings.search_name
						}];
					}),
					title: {
						text: null
					},
					tooltip: {
						borderWidth: 0,
						shadow: false,
						valueSuffix: '{point.unit}<br>{point.label}'
					}
				});

				return {
					click: {
						'a[name=edit-graph]': this.handleEditGraph
					}
				}
			});
	}

	unbind() {
		this.$context.element.querySelector('.ui.segment')
			.classList.add('loading');
	}
}

module.exports = HistoryGraph;
