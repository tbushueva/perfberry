'use strict';

/*
 * This is a Catberry Cat-component file.
 * More details can be found here
 * http://catberry.org/documentation#cat-components-interface
 */

class ReportAssertions {

	constructor() {
		this.tables = null;
	}

	/**
	 * Gets data for the component's template.
	 * @returns {Promise<Object>} Promise of data.
	 */
	render() {
		return this.$context.getStoreData();
	}

	bind() {
		this.tables = $('table').DataTable({
			paging: false,
			info: false,
			searching: false,
			fixedHeader: {
				header: true,
				headerOffset: -13
			}
		});
	}

	unbind() {
		this.tables.destroy();
	}
}

module.exports = ReportAssertions;
