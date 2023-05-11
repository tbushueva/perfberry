'use strict';

/*
 * This is a Catberry Cat-component file.
 * More details can be found here
 * http://catberry.org/documentation#cat-components-interface
 */

class Info {

	/**
	 * Gets data for the component's template.
	 * @returns {Promise<Object>} Promise of data.
	 */
	render() {
		return this.$context.getStoreData();
	}

	bind() {
		window.scrollTo(0, 0);

		this.tables = $('table').DataTable({
			paging: false,
			order: [],
			info: false,
			searching: false,
			fixedHeader: {
				header: true,
				headerOffset: -15
			}
		});
	}

	unbind() {
		this.tables.destroy();
	}
}

module.exports = Info;
