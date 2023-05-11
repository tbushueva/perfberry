'use strict';

const ChildComponent = require('../../lib/ChildComponent');

class ProjectSettingsAssertion extends ChildComponent {

	bind() {
		const groupDropdown = this.$context.element
			.querySelector('.ui.dropdown[data-name="assertion-group"]');
		$(groupDropdown).dropdown({
			fullTextSearch: true
		});

		const metricsDropdown = this.$context.element
			.querySelector('.ui.dropdown[data-name="assertion-metric"]');
		$(metricsDropdown).dropdown();

		const searchDropdown = this.$context.element
			.querySelector('.ui.dropdown[data-name="assertion-search"]');
		$(searchDropdown).dropdown();

		const buildDropdown = this.$context.element
			.querySelector('.ui.dropdown[data-name="assertion-build"]');
		$(buildDropdown).dropdown();

		const conditionDropdown = this.$context.element
			.querySelector('.ui.dropdown[data-name="assertion-condition"]');
		$(conditionDropdown).dropdown();

		const optionsDropdown = this.$context.element
			.querySelector('.ui.dropdown[data-name="assertions-options"]');
		$(optionsDropdown).dropdown();

		return {
			click: {
				'a[name=remove-rule]': this.handleRemove
			}
		};
	}

	handleRemove(event) {
		event.preventDefault();
		event.stopPropagation();

		this.$context.element.parentNode.removeChild(this.$context.element);
	}
}

module.exports = ProjectSettingsAssertion;
