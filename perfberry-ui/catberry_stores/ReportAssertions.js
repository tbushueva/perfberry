'use strict';

const AssertionModel = require('../models/AssertionModel');

class ReportAssertions {

	constructor(locator) {
		this.apiClient = locator.resolve('apiClient');
		this.$context.setDependency('Report');
	}

	/**
	 * Loads data from somewhere.
	 * @returns {Object} Data object.
	 */
	load() {
		return Promise.all([
			this.$context.getStoreData('Pages'),
			this.$context.getStoreData('Report')
		])
			.then(data => {
				const reportStore = data[1];

				let requests = [ data[0].info, reportStore ];
				reportStore.report.builds.forEach(build => {
					requests.push(this.apiClient.assertions(reportStore.report.projectId, reportStore.report.id, build.id));
				});

				if (data[1].comparedReport) {
					reportStore.report.builds
						.filter(b => b.comparedBuildIndex >= 0)
						.forEach(build => {
							requests.push(this.apiClient.assertions(reportStore.report.projectId, reportStore.comparedReport.id, reportStore.comparedReport.builds[build.comparedBuildIndex].id));
						});
				}

				return Promise.all(requests);
			})
			.then(data => {
				const info = data[0];
				const reportStore = data[1];

				const offset = reportStore.report.builds.length + 2;
				let j = 0;
				const comparable = data.length > offset;

				reportStore.report.builds.forEach((build, i) => {
					const assertions = data[i + 2]
						.map(assertionData => {
							const group = assertionData.group || '';
							const selector = assertionData.selector || '';
							const line = ',' + build.env + ',' + group + ',' + assertionData.metric + ',' + selector;
							const options = {
								project: reportStore.projectAlias,
								graphs: line,
								highlightReport: reportStore.report.id
							};

							return {
								'assertion': new AssertionModel(assertionData, info),
								'historyLink': this.$context.getRouteURI('project', options)
							}
						});

					if (comparable) {
						const comparedAssertions = data[offset + build.comparedBuildIndex + j]
							.map(assertion => new AssertionModel(assertion, info));

						assertions.forEach(assertion => {
							const comparedAssertion = comparedAssertions
								.find(a =>
									a.group === assertion.assertion.group &&
									a.metric === assertion.assertion.metric &&
									a.selector === assertion.assertion.selector
								);
							if (comparedAssertion) {
								const expectedDiff = assertion.assertion.expected - comparedAssertion.expected;
								const expectedDiffSign = expectedDiff > 0 ? '+' : '';

								const actualDiff = (assertion.assertion.result.actual - comparedAssertion.result.actual).toFixed(2);
								const actualDiffSign = actualDiff > 0 ? '+' : '';

								const budgetAbsDiff = (assertion.assertion.result.budget.abs - comparedAssertion.result.budget.abs).toFixed(2);
								const budgetRelDiff = (assertion.assertion.result.budget.rel - comparedAssertion.result.budget.rel).toFixed(2);
								const budgetDiffSign = budgetAbsDiff > 0 ? '+' : '';

								assertion.diff = {
									'expected': expectedDiffSign + expectedDiff,
									'actual': actualDiffSign + actualDiff,
									'budget': {
										'abs': budgetDiffSign + budgetAbsDiff,
										'rel': budgetDiffSign + budgetRelDiff
									}
								};
							}
						});
					}

					build.assertions = assertions;
				});

				return {
					'builds': reportStore.report.builds,
					'comparable': comparable
				}
			});
	}
}

module.exports = ReportAssertions;
