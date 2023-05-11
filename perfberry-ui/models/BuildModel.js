'use strict';

const moment = require('moment');
const tz = require('moment-timezone');
const Scm = require('./ScmModel');

const df = 'MMM D, YYYY HH:mm';

class BuildModel {

	constructor(apiBuild) {
		this.id = apiBuild.id;
		this.env = apiBuild.env;
		this._createdAt = moment.utc(apiBuild.created_at)
			.tz('Asia/Novosibirsk').format(df);

		if (apiBuild.hasOwnProperty('label')) {
			this.label = apiBuild.label;
		}
		if (apiBuild.hasOwnProperty('description')) {
			this.description = apiBuild.description;
		}

		this.scm = new Scm(apiBuild.scm);

		this.links = apiBuild.links;

		if (apiBuild.hasOwnProperty('passed')) {
			this.passed = apiBuild.passed;
			this._statused = true;
		}
	}
}

module.exports = BuildModel;
