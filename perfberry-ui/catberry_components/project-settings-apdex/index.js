'use strict';

class ProjectSettingsApdex {

	render() {
		return this.$context.getStoreData();
	}

	bind() {
		return {
			submit: {
				'form': this.handleSaveApdex
			},
			click: {
				'button[name=add-apdex]': this.handleAddApdex
			}
		};
	}

	handleSaveApdex(event) {
		event.preventDefault();
		event.stopPropagation();

		this.$context.element.querySelectorAll('.visible.message')
			.forEach(m => m.classList.remove('visible'));
		this.$context.element.querySelector('button.primary')
			.classList.add('loading');

		const form = this.$context.element.querySelector('form');
		let data = $(form).serializeJSON();
		data.apdex.forEach(rule => {
			if (rule.search_name === '') {
				delete rule.search_name;
			}
			if (rule.group === '') {
				delete rule.group;
			}
		});

		return this.$context.sendAction('update-apdex', data)
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

	handleAddApdex(event) {
		event.preventDefault();
		event.stopPropagation();

		return this.$context.createComponent('project-settings-apdex-item', {
			'cat-store': 'ProjectSettingsApdex',
			blank: true
		})
			.then(c => {
				this.$context.element.querySelector('#apdex').appendChild(c);
			});
	}
}

module.exports = ProjectSettingsApdex;
