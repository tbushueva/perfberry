'use strict';

class ProjectSettingsApdexItem {

	render() {
		const i = parseInt(this.$context.attributes.i);
		const isBlank = this.$context.attributes.blank === 'true';

		return this.$context.getStoreData()
			.then(data => {
				let groups = data.meta.groups.map(g => {
					return {
						title: g,
						name: g
					};
				});
				groups.unshift({
					title: 'Global'
				});

				let searches = data.searches.slice();
				searches.forEach(s => s.title = s.name);
				searches.unshift({
					title: 'All reports'
				});

				let blankApdex = {
					metric: 'rt',
					t: 0.1,
					f: 0.4
				};
				let apdex = isBlank ? blankApdex : data.apdex[i];

				apdex.groups = groups;
				apdex.searches = searches;

				if (apdex.hasOwnProperty('group')) {
					const group = groups.find(g => g.name === apdex.group);

					if (group !== undefined) {
						apdex._group = group;
					}
				} else {
					apdex._group = groups[0];
				}

				if (apdex.hasOwnProperty('search_name')) {
					const search = searches.find(s => s.name === apdex.search_name);

					if (search !== undefined) {
						apdex.search = search;
					}
				} else {
					apdex.search = searches[0];
				}

				return apdex;
			});
	}

	bind() {
		const groupDropdown = this.$context.element
			.querySelector('.ui.dropdown[data-name="apdex-group"]');
		$(groupDropdown).dropdown({
			fullTextSearch: true
		});

		const searchDropdown = this.$context.element
			.querySelector('.ui.dropdown[data-name="apdex-search"]');
		$(searchDropdown).dropdown();

		const optionsDropdown = this.$context.element
			.querySelector('.ui.dropdown[data-name="apdex-options"]');
		$(optionsDropdown).dropdown();

		return {
			input: {
				'input[data-name="apdex-t"]': this.handleThresholdChange,
				'input[data-name="apdex-f"]': this.handleThresholdChange
			},
			click: {
				'a[name=remove-rule]': this.handleRemove
			}
		};
	}

	handleThresholdChange(event) {
		const value = event.target.value;
		const kind = event.target.getAttribute('data-name').split('-')[1];

		this.$context.element
			.querySelectorAll('span[data-name="apdex-' + kind + '"]')
			.forEach(e => e.innerText = value);
	}

	handleRemove(event) {
		event.preventDefault();
		event.stopPropagation();

		this.$context.element.parentNode.removeChild(this.$context.element);
	}
}

module.exports = ProjectSettingsApdexItem;
