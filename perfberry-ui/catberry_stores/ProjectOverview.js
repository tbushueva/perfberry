'use strict';

const moment = require('moment');
const tz = require('moment-timezone');

class ProjectOverview {
	constructor(locator) {
		this._apiClient = locator.resolve('apiClient');

		/**
		 * Current config.
		 * @type {Object}
		 * @private
		 */
		this._config = locator.resolve('config');

		this.$context.setDependency('Project');
	}

	/**
	 * Loads data from somewhere.
	 * @returns {Object} Data object.
	 */
	load() {
		const dateTimeFormat = this._config.dateTimeFormat;

		let project;
		return this.$context.getStoreData('Project')
			.then(data => {
				project = data.project;
				return this._apiClient.reports(project.id, 10);
			}).then(data => {
				const builds = data;

				builds.forEach(build => {
					build._link = this.$context.getRouteURI('report', {
						project: project.alias,
						report: build.id
					});

					build._dateTime = moment.utc(build.created_at).tz('Asia/Novosibirsk').format(dateTimeFormat);

					if (build.scm.hasOwnProperty('vcs')) {
						build.scm.vcs.shortRevision = build.scm.vcs.revision.substr(0, 8);
					}

					if (build.hasOwnProperty('passed')) {
						build._statused = true;
					}
				});

				return {
					project: project,
					builds: builds
				};
			});
	}
}

module.exports = ProjectOverview;
