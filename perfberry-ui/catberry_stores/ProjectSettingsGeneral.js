'use strict';

class ProjectSettingsGeneral {

	constructor(locator) {
		this._apiClient = locator.resolve('apiClient');
	}

	//TODO fix: store will be use cached data after modify and next tabs explore
	load() {
		return this.$context.getStoreData('Project');
	}

	handleUpdateProject(data) {
		return this._apiClient.updateProject(data);
	}
}

module.exports = ProjectSettingsGeneral;
