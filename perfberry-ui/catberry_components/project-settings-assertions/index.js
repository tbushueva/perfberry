'use strict';

class ProjectSettingsAssertions {

	render() {
		return this.$context.getStoreData();
	}

	bind() {
		return {
			submit: {
				'form': this.handleSaveAssertions
			},
			click: {
				'button[name=add-assertion]': this.handleAddAssertion
			}
		};
	}

	handleSaveAssertions(event) {
		event.preventDefault();
		event.stopPropagation();

		this.$context.element.querySelectorAll('.visible.message')
			.forEach(m => m.classList.remove('visible'));
		this.$context.element.querySelector('button.primary')
			.classList.add('loading');

		const form = this.$context.element.querySelector('form');
		const data = $(form).serializeJSON();

		// Because skipFalsyValuesForFields doesn't properly work for empty
		// properties in objects in array.
		data.assertions.forEach(rule => {
			if (rule.group === '') {
				delete rule.group;
			}
			if (rule.build_name === '') {
				delete rule.build_name;
			}
			if (rule.search_name === '') {
				delete rule.search_name;
			}
			const metricParts = rule.metric.split(':');
			rule.metric = metricParts[0];
			rule.selector = metricParts[1];
		});

		return this.$context.sendAction('update-assertions', data)
			.then(updated => {
				this.$context.element.querySelector('button.primary')
					.classList.remove('loading');

				if (updated) {
					this.$context.element.querySelector('.success.message')
						.classList.add('visible');
				} else {
					this.$context.element.querySelector('.error.message')
						.classList.add('visible');
				}
			}).catch(() => {
				this.$context.element.querySelector('.error.message')
					.classList.add('visible');
			});
	}

	handleAddAssertion(event) {
		event.preventDefault();
		event.stopPropagation();

		return this.$context.createComponent('project-settings-assertion', {
			'cat-store': 'ProjectSettingsAssertions',
			path: 'blankAssertion'
		})
			.then(c => {
				this.$context.element.querySelector('#assertions').appendChild(c);
			});
	}
}

module.exports = ProjectSettingsAssertions;
