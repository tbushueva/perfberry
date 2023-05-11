'use strict';

class Projects {
	constructor(locator) {
		this.apiClient = locator.resolve('apiClient');
	}

	/**
	 * Loads data from somewhere.
	 * @returns {Object} Data object.
	 */
	load() {
		return this.apiClient.projects();
	}
}

module.exports = Projects;
