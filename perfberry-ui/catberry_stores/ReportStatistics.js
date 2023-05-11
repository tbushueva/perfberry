'use strict';

const Metrics = require('../lib/Metrics');

class ReportStatistics {
	constructor(locator) {
		this.$context.setDependency('Report');

		this._apiClient = locator.resolve('apiClient');
	}

	static transformStatisticsToMap(statistics) {
		const globalItems = [];
		for (let metric in statistics.global.items) {
			for (let selector in statistics.global.items[metric]) globalItems.push({
				metric: metric,
				selector: selector,
				value: statistics.global.items[metric][selector]
			});
		}
		statistics.global.items = globalItems;

		statistics.groups.forEach(group => {
			const groupItems = [];
			for (let metric in group.items) {
				for (let selector in group.items[metric]) groupItems.push({
					metric: metric,
					selector: selector,
					value: group.items[metric][selector]
				});
			}
			group.items = groupItems;
		});
	}

	/**
	 * Loads data from somewhere.
	 * @returns {Object} Data object.
	 */
	load() {
		let report;
		let comparedReport;
		const comparedReportId = this.$context.state.comparedReportId;
		let projectAlias;
		let info;

		return this.$context.getStoreData('Pages')
			.then(data => {
				info = data.info;

				return this.$context.getStoreData('Report')
			})
			.then(data => {
				report = data.report;
				comparedReport = data.comparedReport;
				projectAlias = data.projectAlias;

				let requests = [];

				report.builds.forEach(build => {
					requests.push(this._apiClient.statistics(report.projectId, report.id, build.id));
				});

				if (comparedReportId) {
					report.builds
						.filter(b => b.comparedBuildIndex >= 0)
						.forEach(build => {
							requests.push(this._apiClient.buildStatisticsDiff(build.id, comparedReport.builds[build.comparedBuildIndex].id));
						});
				}

				return Promise.all(requests);
			})
			.then(data => {
				report.builds.forEach((build, i) => {
					build.statistics = data[i];
					ReportStatistics.transformStatisticsToMap(build.statistics);
				});

				// diffs
				const offset = report.builds.length;
				let i = 0;
				report.builds.forEach(build => {
					if (build.comparedBuildIndex >= 0) {
						if (build.statistics.global.hasOwnProperty('apdex')) {
							build.statistics.global.apdex.diffAvailable =
								data[offset + i].global.hasOwnProperty('apdex');

							if (build.statistics.global.apdex.diffAvailable) {
								build.statistics.global.apdex.diff = data[offset + i].global.apdex;
							}
						}
						build.statistics.global.items.forEach(stat => {
							const item = data[offset + i].global.items[stat.metric][stat.selector];
							stat.diffAvailable = item !== undefined;
							if (stat.diffAvailable === true) {
								stat.diff = item;
							}
						});

						build.statistics.groups.forEach(group => {
							if (group.hasOwnProperty('apdex')) {
								const comparedGroup = data[offset + i].groups.find(g => g.name === group.name);
								group.apdex.diffAvailable =
									comparedGroup !== undefined &&
									comparedGroup.hasOwnProperty('apdex');

								if (group.apdex.diffAvailable) {
									group.apdex.diff = comparedGroup.apdex;
								}
							}

							group.items.forEach(stat => {
								const comparedGroup = data[offset + i].groups.find(g => g.name === group.name);
								const item = comparedGroup ? comparedGroup.items[stat.metric][stat.selector] : undefined;
								stat.diffAvailable = item !== undefined;
								if (stat.diffAvailable === true) {
									stat.diff = item;
								}
							});
						});

						i++;
					}
				});

				report.builds.forEach(build => Metrics.collect(build.statistics, info.metricsMap, info.selectorsMap));
				report.builds.forEach(build => {
					let groupsHasApdex = false;
					if (build.statistics.hasOwnProperty('global')) {
						if (build.statistics.global.hasOwnProperty('apdex')) {
							build.statistics.global.apdex.metricLabel =
								info.metricsMap[build.statistics.global.apdex.metric].label;
						}
						let globalStatsCount = 'six';
						if (build.statistics.global.items.length % 5 === 0) {
							globalStatsCount = 'five';
						}
						build.statistics.global._globalStatsCount = globalStatsCount;
						build.statistics.global.items.forEach(metric => {
							const line = ',' + build.env + ',,' + metric.metric + ',' + metric.selector;
							const options = {
								project: projectAlias,
								graphs: line,
								highlightReport: report.id
							};
							metric._historyLink = this.$context.getRouteURI('project', options);
						});
					}

					if (build.statistics.hasOwnProperty("groups")) {
						build.statistics.groups.forEach(group => {
							if (group.hasOwnProperty('apdex')) {
								groupsHasApdex = true;
							}
							group.items.forEach(metric => {
								const line = ',' + build.env + ',' + group.name + ',' + metric.metric + ',' + metric.selector;
								const options = {
									project: projectAlias,
									graphs: line,
									highlightReport: report.id
								};
								metric._historyLink = this.$context.getRouteURI('project', options);
							});
						});
					}

					build.groupsHasApdex = groupsHasApdex;
				});

				return {
					builds: report.builds,
                    compared: comparedReport !== undefined
				};
			});
	}
}

module.exports = ReportStatistics;
