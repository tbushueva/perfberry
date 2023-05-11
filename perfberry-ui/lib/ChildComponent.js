'use strict';

const ObjectExtractor = require('./ObjectExtractor');

class ChildComponent {

	render() {
		return this.$context.getStoreData()
			.then(data => {
				const ed = ObjectExtractor.extract(data, this.$context.attributes.path);
				ed._parentPath = this.$context.attributes.path;
				return ed;
			});
	}
}

module.exports = ChildComponent;
