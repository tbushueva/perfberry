'use strict';

class AssertionModel {

	constructor(apiAssertion, info) {
		this.group = apiAssertion.group || 'Global';

		this.metric = info.metricsMap[apiAssertion.metric].label;
		this.unit = info.metricsMap[apiAssertion.metric].unit;
		this.spaced = info.metricsMap[apiAssertion.metric].spaced;

		if (apiAssertion.hasOwnProperty('selector')) {
			this.selector = info.selectorsMap[apiAssertion.selector].label + ' of';
		}

		this.condition = info.conditionsMap[apiAssertion.condition].label;

		this.expected = apiAssertion.expected;

		if (apiAssertion.hasOwnProperty('result')) {
			const actual = apiAssertion.result.actual.toFixed(2);
			const budgetAbs = info.metricsMap[apiAssertion.metric].inverted ?
				(this.expected - actual).toFixed(2) :
				(actual - this.expected).toFixed(2);
			const budgetRel = (this.expected ?
				budgetAbs / this.expected * 100 : 0).toFixed(2);

			this.result = {
				'actual': actual,
				'passed': apiAssertion.result.passed,
				'budget': {
					'abs': budgetAbs,
					'rel': budgetRel
				}
			};
		}
	}
}

module.exports = AssertionModel;
