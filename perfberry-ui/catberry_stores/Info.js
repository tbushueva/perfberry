'use strict';

class Info {

	constructor() {
		this.$context.setDependency('Pages');
	}

	load() {
		return this.$context.getStoreData('Pages')
			.then(data => data.info);
	}
}

module.exports = Info;
