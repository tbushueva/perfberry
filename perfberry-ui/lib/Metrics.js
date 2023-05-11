'use strict';

function getDiffProperties(value, diff, inverted) {
	let diffClass;

	if (diff > 0) {
		if (inverted) {
			if (diff / value >= 0.25) {
				diffClass = 'red';
			} else {
				diffClass = 'yellow';
			}
		} else {
			diffClass = 'green';
		}
	} else if (diff < 0) {
		if (inverted) {
			diffClass = 'green';
		} else {
			if (diff / value <= -0.25) {
				diffClass = 'red';
			} else {
				diffClass = 'yellow';
			}
		}
	}

	let opacity = Math.abs(diff / value) < 0.25 ? Math.abs(diff / value) * 10 : 1;
	if (diff === 0) {
		opacity = 0.1;
	}

	return {
		className: diffClass,
		opacity: opacity
	};
}

function sortItems(items, metricsInfo, selectorsInfo) {
	items.sort((a, b) => {
		const metricPriority =
			metricsInfo[a.metric].priority - metricsInfo[b.metric].priority;
		return metricPriority === 0 ?
			selectorsInfo[a.selector].priority -
			selectorsInfo[b.selector].priority :
			metricPriority
	});
}

exports.collect = function (metrics, metricsInfo, selectorsInfo) {
	if (metrics.hasOwnProperty('global')) {
		if (metrics.global.hasOwnProperty('apdex') && metrics.global.apdex.hasOwnProperty('diff')) {
			metrics.global.apdex.diff.properties =
				getDiffProperties(metrics.global.apdex.value, metrics.global.apdex.diff.value, false);
			metrics.global.apdex.diff.valueFmt =
				metrics.global.apdex.diff.value > 0 ?
					'+' + metrics.global.apdex.diff.value :
					metrics.global.apdex.diff.value;
		}

		sortItems(metrics.global.items, metricsInfo, selectorsInfo);

		metrics.global.items.forEach(metric => {
			const valueParts = parseFloat(metric.value).toFixed(2).split('.');
			metric._integer = valueParts[0];
			metric._fraction = valueParts[1];

			if (metricsInfo[metric.metric].hasOwnProperty('unit')) {
				metric._unit = metricsInfo[metric.metric].unit;
			}

			metric._label = metricsInfo[metric.metric].label;
			metric._label += ' ' + selectorsInfo[metric.selector].label;

			let title = selectorsInfo[metric.selector].name + ' of ' + metricsInfo[metric.metric].name;
			if (metricsInfo[metric.metric].hasOwnProperty('unit')) {
				title += ' in ' + metricsInfo[metric.metric].unit;
			}
			metric._title = title;

			if (metric.hasOwnProperty('diff')) {
				metric._diffProperties =
					getDiffProperties(metric.value, metric.diff, metricsInfo[metric.metric].inverted);
				const diffParts = parseFloat(metric.diff).toFixed(2).split('.');
				metric._diffInteger = metric.diff > 0 ? '+' + diffParts[0] : diffParts[0];
				metric._diffFraction = diffParts[1];
			}
		});
	}

	if (metrics.hasOwnProperty('groups')) {
		metrics.groups.forEach(group => {
			if (group.hasOwnProperty('apdex') && group.apdex.hasOwnProperty('diff')) {
				group.apdex.diff.properties =
					getDiffProperties(group.apdex.value, group.apdex.diff.value, false);
				group.apdex.diff.valueFmt =
					group.apdex.diff.value > 0 ?
						'+' + group.apdex.diff.value :
						group.apdex.diff.value;
			}

			sortItems(group.items, metricsInfo, selectorsInfo);

			group.items.forEach(metric => {
				const valueParts = parseFloat(metric.value).toFixed(2).split('.');
				metric._integer = valueParts[0];
				metric._fraction = valueParts[1];

				if (metric.hasOwnProperty('diff')) {
					metric._diffProperties =
						getDiffProperties(metric.value, metric.diff, metricsInfo[metric.metric].inverted);
					const diffParts = parseFloat(metric.diff).toFixed(2).split('.');
					metric._diffInteger = metric.diff > 0 ? '+' + diffParts[0] : diffParts[0];
					metric._diffFraction = diffParts[1];
				}
			});
		});
	}

	if (metrics.hasOwnProperty('groups') && metrics.groups.length > 0) {
		let groupMetricsHeaders = [];
		if (metrics.groups[0].hasOwnProperty('items')) {
			metrics.groups[0].items.forEach(metric => {
				let header = {
					label: metricsInfo[metric.metric].label
				};

				header.label += ' ' + selectorsInfo[metric.selector].label;

				header.title = selectorsInfo[metric.selector].name + ' of ' + metricsInfo[metric.metric].name;
				if (metricsInfo[metric.metric].hasOwnProperty('unit')) {
					header.title += ' in ' + metricsInfo[metric.metric].unit;

					header.unit = metricsInfo[metric.metric].unit;
				}

				groupMetricsHeaders.push(header);
			});
		}

		metrics._groupsMetricsHeaders = groupMetricsHeaders;
	}
};

exports.collectGlobalHeaders = function (stats, info) {
	if (stats.hasOwnProperty('global')) {
		return stats.global.metrics.map(metric => {
			let title = info[metric.metric].name;
			if (info[metric.metric].hasOwnProperty('unit')) {
				title += ' in ' + info[metric.metric].unit;
			}
			return {
				label: info[metric.metric].label,
				title: title
			};
		});
	} else {
		return [];
	}
};
