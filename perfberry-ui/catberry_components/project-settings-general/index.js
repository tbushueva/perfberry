'use strict';

class ProjectSettingsGeneral {

	render() {
		return this.$context.getStoreData();
	}

	bind() {
		return {
			submit: {
				'form': this.handleSaveProject
			}
		};
	}

	handleSaveProject(event) {
		event.preventDefault();
		event.stopPropagation();

		this.$context.element.querySelectorAll('.visible.message')
			.forEach(m => m.classList.remove('visible'));
		this.$context.element.querySelector('button.primary')
			.classList.add('loading');

		const form = this.$context.element.querySelector('form'),
			data = $(form).serializeJSON({skipFalsyValuesForTypes: ["string", "null"]});

		return this.$context.sendAction('update-project', data)
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
}

module.exports = ProjectSettingsGeneral;
