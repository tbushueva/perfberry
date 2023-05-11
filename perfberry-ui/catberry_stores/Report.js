'use strict';

const ReportModel = require('../models/ReportModel');
const BuildModel = require('../models/BuildModel');

class Report {
	constructor(locator) {
		this._apiClient = locator.resolve('apiClient');

		/**
		 * Current config.
		 * @type {Object}
		 * @private
		 */
		this._config = locator.resolve('config');
	}

	/**
	 * Loads data from somewhere.
	 * @returns {Object} Data object.
	 */
	load() {
		const projectAlias = this.$context.state.project;
		const reportId = this.$context.state.report;
		const comparedReportId = this.$context.state.comparedReportId;
		let comparedReport;
		const tab = this.$context.state.tab;

		let tabs = {
			showCompare: tab === 'statistics' || tab === 'assertions',
			statistics: {
				link: this.$context.getRouteURI('report', {
					project: projectAlias,
					report: reportId,
					comparedReportId: comparedReportId
				})
			},
			assertions: {
				link: this.$context.getRouteURI('report-assertions', {
					project: projectAlias,
					report: reportId,
					comparedReportId: comparedReportId
				})
			},
			transactions: {
				link: this.$context.getRouteURI('report-transactions', {
					project: projectAlias,
					report: reportId
				})
			},
			graphs: {
				link: this.$context.getRouteURI('report-graphs', {
					project: projectAlias,
					report: reportId
				})
			},
			edit: {
				link: this.$context.getRouteURI('report-edit', {
					project: projectAlias,
					report: reportId
				})
			}
		};
		tabs[tab].active = true;

		return this._apiClient.projectByAlias(projectAlias)
			.then(project => {
				const requests = [
					this._apiClient.report(project.id, reportId),
					this._apiClient.builds(project.id, reportId)
				];

				if (comparedReportId) {
					requests.push(this._apiClient.report(project.id, comparedReportId));
					requests.push(this._apiClient.builds(project.id, comparedReportId));
				}

				return Promise.all(requests);
			})
			.then(data => {
				const report = new ReportModel(data[0]);
				report.builds = data[1].map(build => new BuildModel(build));

				if (comparedReportId) {
					comparedReport = new ReportModel(data[2]);
					comparedReport.builds = data[3].map(build => new BuildModel(build));
				}

				report.builds.forEach(build => {
					if (comparedReportId) {
						build.comparedBuildIndex =
							comparedReport.builds
								.map(b => b.env)
								.indexOf(build.env);
						build.comparable = build.comparedBuildIndex !== -1;
					}
				});

				const compareLink = this.$context.getRouteURI('project-reports', {
					project: projectAlias,
					comparedReportId: report.id
				});

				return {
					report: report,
					compareLink: compareLink,
					comparedReport: comparedReport,
					projectAlias: projectAlias,
					tabs: tabs
				};
			});
	}
}

module.exports = Report;
