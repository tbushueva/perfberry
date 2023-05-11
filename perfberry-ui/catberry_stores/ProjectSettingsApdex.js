'use strict';

class ProjectSettingsApdex {

	constructor(locator) {
		this.$context.setDependency('ProjectSettings');

		this._apiClient = locator.resolve('apiClient');
	}

	load() {
		let project = {};

		return this.$context.getStoreData('Project')
			.then(data => {
				project = data.project;
				return Promise.all([
					this._apiClient.projectMeta(project.id),
					this._apiClient.projectSearches(project.id),
					this._apiClient.projectApdex(project.id)
				]);
			})
			.then(data => {
				const meta = data[0];
				const searches = data[1];

				return {
					project: project,
					meta: meta,
					searches: searches,
					apdex: data[2]
				}
			});
	}

	handleUpdateApdex(data) {
		return this._apiClient.updateProjectApdex(data.projectId, data.apdex);
	}
}

module.exports = ProjectSettingsApdex;
