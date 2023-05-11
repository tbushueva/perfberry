'use strict';

class InfoModel {

	/**
	 * @param {Object[]} apiInfo.metrics
	 * @param {String} apiInfo.metrics[].metric
	 * @param {Object[]} apiInfo.selectors
	 * @param {String} apiInfo.selectors[].selector
	 * @param {Object[]} apiInfo.conditions
	 * @param {String} apiInfo.conditions[].condition
	 */
	constructor(apiInfo) {
		this.metrics = apiInfo.metrics;
		this.metricsMap = {};
		apiInfo.metrics.forEach(m => this.metricsMap[m.metric] = m);

		this.selectors = apiInfo.selectors;
		this.selectorsMap = {};
		apiInfo.selectors.forEach(s => this.selectorsMap[s.selector] = s);

		this.conditions = apiInfo.conditions;
		this.conditionsMap = {};
		this.conditions.forEach(c => this.conditionsMap[c.condition] = c);
	}
}

module.exports = InfoModel;
