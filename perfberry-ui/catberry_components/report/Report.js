'use strict';

const BaseComponent = require('../../lib/BaseComponent');

/*
 * This is a Catberry Cat-component file.
 * More details can be found here
 * http://catberry.org/documentation#cat-components-interface
 */

class Report extends BaseComponent {

	/**
	 * Gets data for the component's template.
	 * @returns {Promise<Object>} Promise of data.
	 */
	render() {
		return this.$context.getStoreData();
	}
}

module.exports = Report;
