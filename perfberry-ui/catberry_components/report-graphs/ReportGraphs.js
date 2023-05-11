'use strict';

class ReportGraphs {

	/**
	 * Gets data for the component's template.
	 * @returns {Promise<Object>} Promise of data.
	 */
	render() {
		return this.$context.getStoreData();
	}

	bind() {
		$('#filters .ui.checkbox').checkbox({
			onChange: () => {
				$('#filters').submit();
			}
		});

		$('.ui.dropdown').dropdown({
			fullTextSearch: true,
			onChange: () => {
				$('#filters').submit();
			}
		});

		const Highcharts = require('highcharts');
		Highcharts.setOptions({
			global: {
				useUTC: false
			}
		});

		Highcharts.Point.prototype.highlight = function (event) {
			this.onMouseOver(); // Show the hover marker
			// this.series.chart.tooltip.refresh(this); // Show the tooltip
			// this.series.chart.xAxis[0].drawCrosshair(event, this); // Show the crosshair
		};
		Highcharts.Pointer.prototype.reset = function () {
			return undefined;
		};

		// TODO hover only over single build
		$('div[data-name="graph"]').bind('mousemove touchmove touchstart', function (e) {
			let chart,
				point,
				i,
				event;

			for (i = 0; i < Highcharts.charts.length; i = i + 1) {
				chart = Highcharts.charts[i];
				event = chart.pointer.normalize(e.originalEvent); // Find coordinates within the chart
				point = chart.series[0].searchPoint(event, true); // Get the hovered point

                if (point) {
					point.highlight(e);
				}
			}
		});

		return {
			click: {
				'#filters a': this.handleClear,
			}
		}
	}

	handleClear(event) {
		event.preventDefault();
		event.stopPropagation();

		$('.ui.dropdown').dropdown('set selected', 'Global');

		$(this.$context.element.querySelectorAll('#filters .ui.checkbox'))
			.checkbox('uncheck');
	}
}

module.exports = ReportGraphs;
