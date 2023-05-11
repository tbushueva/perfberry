'use strict';

const moment = require('moment');
const tz = require('moment-timezone');

class ReportTransactions {

	constructor(locator) {
		this.$context.setDependency('Report');

		this._config = locator.resolve('config');

		this._apiClient = locator.resolve('apiClient');
	}

	parseValue(v) {
		const valueParts = parseFloat(v).toFixed(2).split('.');
		return {
			integer: valueParts[0],
			fraction: valueParts[1]
		}
	}

	isJsonString(str) {
		try {
			JSON.parse(str);
		} catch (e) {
			return false;
		}
		return true;
	}

	/**
	 * Loads data from somewhere.
	 * @returns {Object} Data object.
	 */
	load() {
		let builds = [];
		let meta = {};

		const filterLimit = parseInt(this.$context.state.limit) || 100;
		let limitFilters = [
			{
				size: 100
			},
			{
				size: 500
			},
			{
				size: 1000
			}
		];
		limitFilters.forEach(filter => {
			if (filter.size === filterLimit) {
				filter.active = true;
			}
		});

		const statusFilters = [
			{
				status: 'all'
			}
		];
		const filterStatus = this.$context.state.status || 'all';

		const codeFilters = [
			{
				code: 'all'
			}
		];
		const filterCode = this.$context.state.code || 'all';

		const filterGroupEncoded = this.$context.state.group === 'Global' ? undefined : this.$context.state.group;
		const filterGroup = filterGroupEncoded ? filterGroupEncoded.replace(/\+/g, ' ') : filterGroupEncoded;

		const filterQuery = this.$context.state.query;

		const filters = {
			limit: limitFilters,
			query: filterQuery,
			status: statusFilters,
			code: codeFilters,
			active: {
				group: filterGroup || 'Global'
			}
		};

		return this.$context.getStoreData('Pages')
			.then(data => {
				meta = data.info;

				return this.$context.getStoreData('Report');
			})
			.then(data => {
				builds = data.report.builds;

				const requests = builds.map(build =>
					this._apiClient.transactions(
						data.report.projectId,
						data.report.id,
						build.id,
						filterLimit,
						filterGroup,
						filterQuery,
						filterStatus === 'all' ? undefined : filterStatus,
						filterCode === 'all' ? undefined : filterCode
					)
				);

				return Promise.all(requests);
			})
			.then(data => {
				builds.forEach((build, i) => {
					build.transactions = data[i];
					build._transactionsTotal = build.transactions.items.length;

					build.transactions.filters.statuses.forEach(status => {
						if (!statusFilters.find(f => f.status === status)) {
							statusFilters.push({
								status: status
							});
						}
					});
					statusFilters.forEach(filter => {
						if (filter.status === filterStatus) {
							filter.active = true;
						}
					});

					build.transactions.filters.codes.forEach(code => {
						if (!codeFilters.find(f => f.code === code.toString())) {
							codeFilters.push({
								code: code.toString()
							});
						}
					});
					codeFilters.forEach(filter => {
						if (filter.code === filterCode) {
							filter.active = true;
						}
					});

					build.transactions.filters.groups.unshift('Global');

					build._metricHeaders = [];
					build.transactions.items.forEach((transaction, i) => {
						transaction._datetime = moment.utc(transaction.datetime).tz('Asia/Novosibirsk').format(this._config.dateTimeFormat + ':ss.SSS');

						if (transaction.status === 'OK') {
							transaction._statusSuccessful = true;
						}

						if (transaction.hasOwnProperty('query') && transaction.query !== '') {
							transaction.queryLabel =
								transaction.query
									.replace(/(http[s]?:\/\/)?([^\/\s]+)(.*)/g, '$3') || '/';
						}

						if (transaction.hasOwnProperty('payload')) {
							if (this.isJsonString(transaction.payload)) {
								transaction.payload = JSON.stringify(JSON.parse(transaction.payload), 4, ' ');
							}
						}

						const metrics = [];
						for (let metric in transaction.metrics) metrics.push({
							metric: metric,
							value: transaction.metrics[metric]
						});
						transaction.metrics = metrics;

						transaction.metrics.forEach(metric => {
							if (i === 0) {
								let header = {
									header: meta.metricsMap[metric.metric].label,
									title: meta.metricsMap[metric.metric].name
								};

								if (meta.metricsMap[metric.metric].hasOwnProperty('unit')) {
									header.unit = meta.metricsMap[metric.metric].unit;
								}

								build._metricHeaders.push(header);
							}

							metric._parts = this.parseValue(metric.value);
						});
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
