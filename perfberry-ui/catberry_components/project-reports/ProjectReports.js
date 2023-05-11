'use strict';

const uri = require('catberry-uri');

class ProjectReports {

	/**
	 * Gets data for the component's template.
	 * @returns {Promise<Object>} Promise of data.
	 */
	render() {
		return this.$context.getStoreData();
	}

	handleSearch(event) {
		event.preventDefault();
		event.stopPropagation();

		const query = event.target.elements[0].value;
		const location = this.$context.location.clone();

		if (query === '') {
			location.query = null;
		} else {
			if (location.query === null) {
				location.query = new uri.Query();
				location.query.values = {};
			}
			location.query.values.query = encodeURIComponent(query);
		}

		return this.$context.redirect(location.toString());
	}

	bind() {
		return {
			submit: {
				'form': this.handleSearch
			}
		};
	}

	unbind() {
		window.scrollTo(0, 0);
	}
}

module.exports = ProjectReports;
