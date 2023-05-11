'use strict';

/*
 * This is a Catberry Cat-component file.
 * More details can be found here
 * http://catberry.org/documentation#cat-components-interface
 */

class ReportEdit {

	/**
	 * Gets data for the component's template.
	 * @returns {Promise<Object>} Promise of data.
	 */
	render() {
		return this.$context.getStoreData();
	}

	bind() {
		return {
			submit: {
				'form[name=edit]': this.handleEditReport,
				'form[name=delete]': this.handleDeleteReport
			}
		};
	}

	handleEditReport(event) {
		event.preventDefault();
		event.stopPropagation();

		const form = this.$context.element.querySelector('form[name=edit]'),
			data = $(form).serializeJSON({skipFalsyValuesForTypes: ["string", "null"]});

		const projectAlias = data.project.alias;
		delete data.project.alias;

		const reportId = data.reportId;

		return this.$context.sendAction('edit', data)
			.then(updated => {
				if (updated) {
					const url = this.$context.getRouteURI('report', {
						project: projectAlias,
						report: reportId,
					});
					this.$context.redirect(url);
				}
			});
	}

	handleDeleteReport(event) {
		event.preventDefault();
		event.stopPropagation();

		const form = this.$context.element.querySelector('form[name=delete]'),
			data = $(form).serializeJSON();

		return this.$context.sendAction('delete', data)
			.then(deleted => {
				if (deleted) {
					const url = this.$context.getRouteURI('project', {
						project: data.project.alias
					});
					this.$context.redirect(url);
				}
			});
	}
}

module.exports = ReportEdit;
