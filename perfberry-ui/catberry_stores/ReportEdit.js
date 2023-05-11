'use strict';

class ReportEdit {
	constructor(locator) {
		this.$context.setDependency('Report');

		this._apiClient = locator.resolve('apiClient');
	}

	/**
	 * Loads data from somewhere.
	 * @returns {Object} Data object.
	 */
	load() {
		const projectAlias = this.$context.state.project;

		return this.$context.getStoreData('Report')
			.then(data => {

				return {
					report: data.report,
					project: {
						alias: projectAlias
					}
				};
			});
	}

	handleEdit(data) {
		const projectId = data.projectId;
		delete data.projectId;

		const reportId = data.reportId;
		delete data.reportId;

		return this._apiClient.updateReport(projectId, reportId, data);
	}

	handleDelete(data) {
		return this._apiClient.deleteReport(data.projectId, data.reportId);
	}
}

module.exports = ReportEdit;
