'use strict';

class ReportTransactions {

	constructor(locator) {
		this._apiClient = locator.resolve('apiClient');
		this.$context.setDependency('Report');
	}

	/**
	 * Loads data from somewhere.
	 * @returns {Object} Data object.
	 */
	load() {
		let builds = [];
		let meta = {};

		let statusFilters = [
			{
				code: 'all',
				label: 'all',
				hasError: undefined
			},
			{
				code: 'successful',
				label: 'successful',
				hasError: false
			},
			{
				code: 'failed',
				label: 'failed',
				hasError: true
			}
		];
		const filterStatus = this.$context.state.status || 'all';
		statusFilters.forEach(filter => {
			if (filter.code === filterStatus) {
				filter.active = true;
			}
		});

		const filters = {
			status: statusFilters
		};

		const filterGroupEncoded = this.$context.state.group === 'Global' ? undefined : this.$context.state.group;
		const filterGroup = filterGroupEncoded ? filterGroupEncoded.replace(/\+/g, ' ') : filterGroupEncoded;

		return this.$context.getStoreData('Pages')
			.then(data => {
				meta = data.info;

				return this.$context.getStoreData('Report');
			})
			.then(data => {
				builds = data.report.builds;

				const requests = builds.map(build =>
					this._apiClient.graphs(
						data.report.projectId,
						data.report.id,
						build.id,
						filterGroup,
						filters.status.find(s => s.active).hasError));

				return Promise.all(requests);
			})
			.then(data => {
				builds.forEach((build, i) => {
					build.graphs = data[i].map((graph, graphIndex) => {
						graph.filters.groups.unshift('Global');

						graph.series.forEach(series => {
							series.name = meta.selectorsMap[series.selector].label;
						});

						return {
							data: graph,
							container: 'graph-' + build.id + '-' + graphIndex,
							group: graph.hasOwnProperty('group') ? graph.group : 'Global',
							unit: meta.metricsMap[graph.metric].unit || '',
							title: meta.metricsMap[graph.metric].label
						};
					});
				});

				return {
					filters: filters,
					builds: builds
				};
			});
	}
}

module.exports = ReportTransactions;
