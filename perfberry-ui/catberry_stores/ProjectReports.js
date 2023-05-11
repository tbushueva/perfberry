'use strict';

const uri = require('catberry-uri');
const ReportModel = require('../models/ReportModel');

class ProjectReports {
	constructor(locator) {
		this._apiClient = locator.resolve('apiClient');

		this.$context.setDependency('Project');
	}

	/**
	 * Loads data from somewhere.
	 * @returns {Object} Data object.
	 */
	load() {
		const encodedQuery = this.$context.state.query || '';
		const query = decodeURIComponent(encodedQuery);
		const compare = this.$context.state.comparedReportId;

		const pagination = {
			limit: 20,
			page: parseInt(this.$context.state.page) || 1
		};
		pagination.offset = pagination.limit * (pagination.page - 1);

		let project;
		return this.$context.getStoreData('Project')
			.then(data => {
				project = data.project;

				return this._apiClient.reports(project.id, pagination.limit, pagination.offset, query);
			}).then(data => {
				let reports = data.map(report => new ReportModel(report));

				pagination.showNext = pagination.offset !== 0;
				pagination.showPrev = reports.length === pagination.limit;

				if (pagination.showNext) {
					const location = this.$context.location.clone();
					if (location.query === null) {
						location.query = new uri.Query();
						location.query.values = {};
					}

					location.query.values.page = pagination.page - 1;

					pagination.nextLink = location.toString();
				}
				if (pagination.showPrev) {
					const location = this.$context.location.clone();
					if (location.query === null) {
						location.query = new uri.Query();
						location.query.values = {};
					}

					location.query.values.page = pagination.page + 1;

					pagination.prevLink = location.toString();
				}

				reports.forEach(report => {
					report._link = this.$context.getRouteURI('report', {
						project: project.alias,
						report: report.id
					});

					if (compare) {
						report.compareLink = this.$context.getRouteURI('report', {
							project: project.alias,
							report: compare,
							comparedReportId: report.id
						});
					}
				});

				return {
					query: query,
					compare: compare,
					reports: reports,
					pagination: pagination
				};
			});
	}
}

module.exports = ProjectReports;
