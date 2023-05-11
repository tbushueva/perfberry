'use strict';

class ProjectSettingsBadges {

	constructor(locator) {
		this._apiClient = locator.resolve('apiClient');
	}

	load() {
		return this.$context.getStoreData('Project')
			.then(data => {
				return {
					project: data.project,
					badges: this._apiClient.badges(data.project.id)
				}
			});
	}
}

module.exports = ProjectSettingsBadges;
