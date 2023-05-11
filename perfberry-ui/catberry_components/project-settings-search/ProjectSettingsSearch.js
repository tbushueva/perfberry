'use strict';

class ProjectSettingsSearch {
	render() {
		const isearch = parseInt(this.$context.attributes.isearch);
		const isBlank = this.$context.attributes.blank === 'true';

		return this.$context.getStoreData()
			.then(data => isBlank? {} : data.searches[isearch]);
	}

	bind() {
		return {
			click: {
				'button[name=remove-search]': this.handleRemove
			}
		};
	}

	handleRemove(event) {
		event.preventDefault();
		event.stopPropagation();

		this.$context.element.parentNode.removeChild(this.$context.element);
	}
}

module.exports = ProjectSettingsSearch;
