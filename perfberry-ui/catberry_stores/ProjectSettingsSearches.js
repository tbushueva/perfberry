'use strict';

class ProjectSettingsSearches {

	constructor(locator) {
		this.$context.setDependency('ProjectSettings');

		this._apiClient = locator.resolve('apiClient');
	}

	load() {
		let project = {};

		return this.$context.getStoreData('Project')
			.then(data => {
				project = data.project;
				return this._apiClient.projectSearches(project.id);
			})
			.then(data => {
				return {
					project: project,
					searches: data
				}
			});
	}

	handleUpdateSearches(data) {
		return this._apiClient.updateProjectSearches(data.projectId, data.searches);
	}
}

module.exports = ProjectSettingsSearches;
