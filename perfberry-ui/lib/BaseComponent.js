'use strict';

class BaseComponent {

	bind() {
		const loaders = this.$context.element.querySelectorAll('.loader');

		for (let i = 0; i < loaders.length; i++) {
			loaders[i].style.display = 'none';
		}
	}
}

module.exports = BaseComponent;
