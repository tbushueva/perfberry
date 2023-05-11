'use strict';

const moment = require('moment');
const tz = require('moment-timezone');

class ProjectGraphs {
	constructor(locator) {
		this._apiClient = locator.resolve('apiClient');

		/**
		 * Current config.
		 * @type {Object}
		 * @private
		 */
		this._config = locator.resolve('config');

		this.$context.setDependency('Pages');
		this.$context.setDependency('Project');
	}

	getHistoryLines(projectId, isMultiMode) {
		return new Promise((fulfill, reject) => {
			let graphs;
			const graphsState = this.$context.state.graphs;
			if (Array.isArray(graphsState)) {
				graphs = graphsState;
			} else if (graphsState !== undefined && graphsState !== '') {
				graphs = [graphsState];
			} else {
				graphs = [];
			}

			if (graphs.length > 0) {
				fulfill(graphs.map(graph => graph.replace(/\+/g, ' ').split(';')))
			} else {
				if (isMultiMode) {
					this._apiClient.projectGraphs(projectId)
						.then(data => {
							const gr = data.map(g => {
								return g.map(line => {
									const search = line.search_name || '';
									const group = line.group || '';
									return search + ',' +
										line.env + ',' +
										group + ',' +
										line.metric + ',' +
										line.selector
								});
							});
							fulfill(gr);
						});

				} else {
					this._apiClient.projectOverview(projectId)
						.then(data => {
							const gr = data.map(line => {
								const search = line.search_name || '';
								const group = line.group || '';
								return search + ',' +
									line.env + ',' +
									group + ',' +
									line.metric + ',' +
									line.selector
							});
							fulfill([gr]);
						});
				}
			}
		});
	}

	_now() {
		return moment().utc().tz('Asia/Novosibirsk');
	}

	/**
	 * Loads data from somewhere.
	 * @returns {Object} Data object.
	 */
	load() {
		const to = this.$context.state.to || 'now';
		let toDate;
		if (to === 'now') {
			toDate = this._now().toDate();
		} else if (to === 'today') {
			toDate = this._now()
				.add(1, 'day')
				.hours(0).minutes(0).seconds(0).milliseconds(0).toDate();
		} else if (to === 'yesterday') {
			toDate = this._now()
				.hours(0).minutes(0).seconds(0).milliseconds(0).toDate();
		} else if (to === 'week') {
			toDate = this._now()
				.add(8 - this._now().day(), 'days')
				.hours(0).minutes(0).seconds(0).milliseconds(0).toDate();
		} else {
			toDate = moment(to).toDate();
		}

		const from = this.$context.state.from || 'now-P1W';
		let fromDate;
		if (from.startsWith('now-')) {
			const duration = moment.duration(from.split('-')[1]);
			fromDate = this._now().subtract(duration).toDate();
		} else if (from === 'today') {
			fromDate = this._now()
				.hours(0).minutes(0).seconds(0).milliseconds(0).toDate();
		} else if (from === 'yesterday') {
			fromDate = this._now()
				.subtract(1, 'day')
				.hours(0).minutes(0).seconds(0).milliseconds(0).toDate();
		} else if (from === 'week') {
			fromDate = this._now()
				.subtract(this._now().day() - 1, 'days')
				.hours(0).minutes(0).seconds(0).milliseconds(0).toDate();
		} else {
			fromDate = moment(from).toDate();
		}

		let ranges = {
			current: {
				from: {
					value: from,
					date: fromDate,
					dateUnix: moment(fromDate).valueOf(),
					dateFmt: moment(fromDate).format('YYYY-MM-DDTHH:mm:ss'),
					dateUtcFmt: moment.utc(fromDate).format('YYYY-MM-DDTHH:mm:ss'),
					label: moment(fromDate).format('YYYY-MM-DD')
				},
				to: {
					value: to,
					date: toDate,
					dateUnix: moment(toDate).valueOf(),
					dateFmt: moment(toDate).format('YYYY-MM-DDTHH:mm:ss'),
					dateUtcFmt: moment.utc(toDate).format('YYYY-MM-DDTHH:mm:ss'),
					label: moment(toDate).format('YYYY-MM-DD')
				}
			}
		};

		ranges.quick = [[
			{
				from: 'now-P1D',
				to: 'now',
				label: 'Last day'
			}, {
				from: 'now-P1W',
				to: 'now',
				label: 'Last week'
			}, {
				from: 'now-P2W',
				to: 'now',
				label: 'Last 2 week'
			}, {
				from: 'now-P1M',
				to: 'now',
				label: 'Last month'
			}, {
				from: 'now-P3M',
				to: 'now',
				label: 'Last 3 months'
			}
		], [
			{
				from: 'today',
				to: 'today',
				label: 'Today'
			}, {
				from: 'yesterday',
				to: 'yesterday',
				label: 'Yesterday'
			}, {
				from: 'week',
				to: 'week',
				label: 'This week'
			}
		]].map(column => column.map(item => {
			item.isActive = item.from === from && item.to === to;
			if (item.isActive) {
				ranges.current.range = item.label;
			}
			return item;
		}));

		const refreshDuration = this.$context.state.refresh || 'PT5M';
		ranges.durations = [{
			value: 'PT1M',
			label: '1 minute'
		}, {
			value: 'PT5M',
			label: '5 minutes'
		}, {
			value: 'PT15M',
			label: '15 minutes'
		}, {
			value: 'PT30M',
			label: '30 minutes'
		}, {
			value: 'PT1H',
			label: '1 hour'
		}].map(item => {
			item.isActive = item.value === refreshDuration;
			if (item.isActive) {
				ranges.current.duration = {
					value: item.value,
					label: item.label
				};
			}
			return item;
		});

		let showSaveGraphsButton = false;
		if (this.$context.state.graphs !== undefined) {
			showSaveGraphsButton = true;
		}

		const COLORS = ['blue', 'teal', 'violet', 'pink', 'purple'];

		const highlightReport = this.$context.state.highlightReport;

		let project;
		let isMultiMode;
		let fullscreenEnabled;
		let historyGraphs;
		return Promise.all([
			this.$context.getStoreData('Pages'),
			this.$context.getStoreData('Project')
		])
			.then(data => {
				fullscreenEnabled = data[0].fullscreenEnabled;

				project = data[1].project;
				isMultiMode = data[1].tabs.graphs.active;

				return this.getHistoryLines(project.id, isMultiMode);
			})
			.then(data => {
				historyGraphs = data;

				let requests = [
					this._apiClient.info()
				];

				historyGraphs.forEach(lines => {
					let options = {
						from: ranges.current.from.dateUtcFmt,
						to: ranges.current.to.dateUtcFmt,
						limit: 5000
					};
					if (lines[0] !== '') {
						options.lines = lines;
					}
					requests.push(this._apiClient.buildsStats(project.id, options));
				});

				return Promise.all(requests);
			}).then(data => {
				let metricsInfo = {};
				data[0].metrics.forEach(metric => metricsInfo[metric.metric] = metric);
				let selectorsInfo = {};
				data[0].selectors.forEach(selector => selectorsInfo[selector.selector] = selector);

				let graphsResponses = [];
				for (let i = 0; i < historyGraphs.length; i++) {
					graphsResponses.push(data[i + 1]);
				}

				graphsResponses.forEach(histories => {
					histories.forEach((history, i) => {
						history._color = COLORS[i % COLORS.length];
						const apdexRequired = history.history.settings.metric === 'apx';
						history.history._metricLabel = apdexRequired ?
							'Apdex' :
							metricsInfo[history.history.settings.metric].label + ' '
							+ selectorsInfo[history.history.settings.selector].label;

						history.history._unit = apdexRequired ?
							'' :
							metricsInfo[history.history.settings.metric].hasOwnProperty('unit') ?
								(metricsInfo[history.history.settings.metric].spaced ? ' ' : '') +
								metricsInfo[history.history.settings.metric].unit + ' ' :
								'';

						const scope = history.history.settings.hasOwnProperty('group') ? history.history.settings.group : 'Global';
						history.history._title = scope + ' ' + history.history._metricLabel;
						history.filters.metrics.forEach(filter => {
							filter._metrics = filter.stats.map(stat => {
								const label = metricsInfo[stat.metric].label + ' ' + selectorsInfo[stat.selector].label;

								return {
									label: label,
									metric: stat.metric,
									selector: stat.selector
								}
							});

							if (filter.hasApdex === true) {
								filter._metrics.unshift({
									label: 'Apdex',
									metric: 'apx',
									selector: ''
								});
							}
						});

						const alRepSearch = {
							name: 'All reports'
						};
						if (!history.filters.hasOwnProperty('searches')) {
							history.filters.searches = [];
						}
						history.filters.searches.push(alRepSearch);
						if (!history.history.settings.hasOwnProperty('search_name')) {
							history.history.settings.search_name = alRepSearch.name;
						}
						history.history.settings._search_name =
							history.history.settings.search_name === 'All reports' ?
								'' : history.history.settings.search_name;
						history.filters.searches.forEach(search => {
							search._text = search.name === 'All reports' ? '' : search.name;
						});

						history.history.values.forEach(metric => {
							metric.key = moment.utc(metric.created_at).tz('Asia/Novosibirsk').valueOf();
							metric._label = 'on #' + metric.report_id + ' ' + (metric.label ? metric.label : '');
							metric.selected = highlightReport !== undefined &&
								metric.report_id === parseInt(highlightReport);

							metric._reportLink = this.$context.getRouteURI('report', {
								project: project.alias,
								report: metric.report_id
							});
						});
					});
				});

				const showButtons = fullscreenEnabled ? false : isMultiMode || showSaveGraphsButton;

				return {
					project: project,
					graphs: graphsResponses,
					colors: COLORS,
					fullscreenEnabled: fullscreenEnabled,
					isMultiMode: isMultiMode,
					showSaveGraphsButton: showSaveGraphsButton,
					showButtons: showButtons,
					ranges: ranges
				};
			});
	}

	handleUpdateGraphs(data) {
		if (data.isMultiMode) {
			return this._apiClient.updateProjectGraphs(data.projectId, data.graphs);
		} else {
			return this._apiClient.updateProjectOverview(data.projectId, data.graphs.shift());
		}
	}

	handleRefresh() {
		return this.$context.changed();
	}

	handleZoom(range) {
		const route = range.isMultiMode ? 'project-graphs' : 'project';
		const newPage = this.$context.getRouteURI(route, {
			project: this.$context.state.project,
			from: moment(range.from).format('YYYY-MM-DDTHH:mm:ss'),
			to: moment(range.to).format('YYYY-MM-DDTHH:mm:ss'),
			refresh: this.$context.state.refresh,
			graphs: this.$context.state.graphs,
			highlightReport: this.$context.state.highlightReport,
			fullscreen: range.fullscreenEnabled
		});
		this.$context.redirect(newPage);
	}
}

module.exports = ProjectGraphs;
