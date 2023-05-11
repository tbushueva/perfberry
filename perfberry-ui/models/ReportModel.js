'use strict';

const moment = require('moment');
const tz = require('moment-timezone');
const Scm = require('./ScmModel');

const df = 'MMM D, YYYY HH:mm';

class ReportModel {

	constructor(apiReport) {
		this.id = apiReport.id;
		this.projectId = apiReport.project_id;
		this._datetime = moment.utc(apiReport.created_at)
			.tz('Asia/Novosibirsk').format(df);

		if (apiReport.hasOwnProperty('label')) {
			this.label = apiReport.label;
		}
		if (apiReport.hasOwnProperty('description')) {
			this.description = apiReport.description;
		}

		this.scm = new Scm(apiReport.scm);

		this.links = apiReport.links;

		if (apiReport.hasOwnProperty('passed')) {
			this.passed = apiReport.passed;
			this._statused = true;
		}
	}
}

module.exports = ReportModel;
