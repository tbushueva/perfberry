'use strict';

class ScmModel {

	/**
	 * @param {String} apiScm.vcs.reference
	 * @param {String} apiScm.vcs.revision
	 * @param {String} [apiScm.vcs.title]
	 * @param {Object} apiScm.parameters
	 */
	constructor(apiScm) {
		if (apiScm.hasOwnProperty('vcs')) {
			this.vcs = apiScm.vcs;

			this.vcs.shortRevision = this.vcs.revision.substr(0, 8);
		}

		this.parameters = apiScm.parameters;
		this.hasParameters = Object.keys(this.parameters).length > 0;
	}
}

module.exports = ScmModel;
