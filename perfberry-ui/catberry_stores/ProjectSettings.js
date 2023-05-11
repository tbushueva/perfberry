'use strict';

class ProjectSettings {

	constructor(locator) {
		this.$context.setDependency('Project');

		this._apiClient = locator.resolve('apiClient');
	}

	load() {
		let project = {};

		const TAB_CODES_GENERAL = 'general';
		const TAB_CODES_SEARCHES = 'searches';
		const TAB_CODES_APDEX = 'apdex';
		const TAB_CODES_ASSERTIONS = 'assertions';
		const TAB_CODES_BADGES = 'badges';
		const tab = this.$context.state.tab || TAB_CODES_GENERAL;
		const tabs = {};

		return this.$context.getStoreData('Project')
			.then(data => {
				project = data.project;

				tabs[TAB_CODES_GENERAL] = {
					name: 'General',
						link: this.$context.getRouteURI('project-settings', {
						project: project.alias
					})
				};
				tabs[TAB_CODES_SEARCHES] = {
					name: 'Searches',
					link: this.$context.getRouteURI('project-settings', {
						project: project.alias,
						tab: TAB_CODES_SEARCHES
					})
				};
				tabs[TAB_CODES_APDEX] = {
					name: 'Apdex',
					link: this.$context.getRouteURI('project-settings', {
						project: project.alias,
						tab: TAB_CODES_APDEX
					})
				};
				tabs[TAB_CODES_ASSERTIONS] = {
					name: 'Assertions',
					link: this.$context.getRouteURI('project-settings', {
						project: project.alias,
						tab: TAB_CODES_ASSERTIONS
					})
				};
				tabs[TAB_CODES_BADGES] = {
					name: 'Badges',
					link: this.$context.getRouteURI('project-settings', {
						project: project.alias,
						tab: TAB_CODES_BADGES
					})
				};
				tabs[tab].active = true;

				return {
					project: project,
					tabs: tabs
				}
			});
	}
}

module.exports = ProjectSettings;
