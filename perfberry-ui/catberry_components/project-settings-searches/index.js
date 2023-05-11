'use strict';

class ProjectSettingsSearches {

	render() {
		return this.$context.getStoreData();
	}

	bind() {
		return {
			submit: {
				'form': this.handleSaveSearches
			},
			click: {
				'button[name=add-search]': this.handleAddSearch
			}
		};
	}

	handleSaveSearches(event) {
		event.preventDefault();
		event.stopPropagation();

		this.$context.element.querySelectorAll('.visible.message')
			.forEach(m => m.classList.remove('visible'));
		this.$context.element.querySelector('button.primary')
			.classList.add('loading');

		const form = this.$context.element.querySelector('form'),
			data = $(form).serializeJSON({skipFalsyValuesForTypes: ["string", "null"]});

		return this.$context.sendAction('update-searches', data)
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

	handleAddSearch(event) {
		event.preventDefault();
		event.stopPropagation();

		return this.$context.createComponent('project-settings-search', {
			'cat-store': 'ProjectSettingsSearches',
			blank: true
		})
			.then(c => {
				this.$context.element.querySelector('#searches').appendChild(c);
			});
	}
}

module.exports = ProjectSettingsSearches;
