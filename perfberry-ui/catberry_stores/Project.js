'use strict';

class Project {
	constructor(locator) {
		this._apiClient = locator.resolve('apiClient');

		this.$context.setDependency('Pages');
	}

	/**
	 * Loads data from somewhere.
	 * @returns {Object} Data object.
	 */
	load() {
        const projectAlias = this.$context.state.project;
		const tab = this.$context.state.tab || 'overview';

		let tabs = {
			overview: {
				link: this.$context.getRouteURI('project', {
					project: projectAlias
				})
			},
			reports: {
				link: this.$context.getRouteURI('project-reports', {
					project: projectAlias
				})
			},
			graphs: {
				link: this.$context.getRouteURI('project-graphs', {
					project: projectAlias
				})
			},
			settings: {
				link: this.$context.getRouteURI('project-settings', {
					project: projectAlias
				})
			}
		};
		tabs[tab].active = true;

		return Promise.all([
			this.$context.getStoreData('Pages'),
			this._apiClient.projectByAlias(projectAlias)
		])
			.then(data => {
				return {
					project: data[1],
					tabs: tabs,
					fullscreenEnabled: data[0].fullscreenEnabled
				};
			});
	}
}

module.exports = Project;
