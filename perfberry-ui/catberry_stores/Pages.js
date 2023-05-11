'use strict';

const InfoModel = require('../models/InfoModel');

class Pages {
	constructor(locator) {
		this.apiClient = locator.resolve('apiClient');
	}

	load() {
		const title = 'Reports',
			page = this.$context.state.page,
			prefixes = {
				home: 'Home',
				info: 'Info',
				project: 'Project',
				projects: 'Projects',
				report: 'Report'
			};

		let result = {
			title: prefixes[page] + ' — ' + title,
			isActive: {},
			fullscreenEnabled: this.$context.state.fullscreen === 'true',
			menu: {
				left: [{
					section: 'Projects',
					link: '/projects',
					isActive: page === 'projects',
					breadcrumb: []
				}],
				right: [{
					section: 'Info',
					link: '/info',
					isActive: page === 'info'
				}, {
					section: 'API',
					link: 'https://perfberry-api.2gis.ru'
				}]
			}
		};

		if (page === 'project' || page === 'report') {
			const projectCode = this.$context.state.project;
			result.menu.left[0].breadcrumb.push({
				section: projectCode,
				link: '/projects/' + projectCode,
				isActive: page === 'project'
			});
		}
		if (page === 'report') {
			const projectCode = this.$context.state.project;
			const report = this.$context.state.report;
			result.menu.left[0].breadcrumb.push({
				section: report,
				link: '/projects/' + projectCode + '/reports/' + report,
				isActive: true
			});
		}

		result.isActive[page] = true;

		return this.apiClient.info()
			.then(data => {
				result.info = new InfoModel(data);

				return result;
			});
	}
}

module.exports = Pages;
