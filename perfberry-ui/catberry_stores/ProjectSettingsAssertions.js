'use strict';

class ProjectSettingsAssertions {

	constructor(locator) {
		this.$context.setDependency('ProjectSettings');

		this._apiClient = locator.resolve('apiClient');
	}

	load() {
		let info = {};
		let project = {};

		return Promise.all([
			this.$context.getStoreData('Pages'),
			this.$context.getStoreData('Project')
		])
			.then(data => {
				info = data[0].info;
				project = data[1].project;
				return Promise.all([
					this._apiClient.projectMeta(project.id),
					this._apiClient.projectSearches(project.id),
					this._apiClient.projectAssertions(project.id)
				]);
			})
			.then(data => {
				const meta = data[0];
				const searches = data[1];

				const groups = meta.groups.map(group => {
					return {
						title: group,
						name: group
					};
				});
				groups.unshift({
					title: 'Global'
				});

				const metrics = [];
				for (let metric in meta.metrics) {
					const metricTitle = info.metricsMap[metric].label;
					meta.metrics[metric].forEach(selector => {
						const title = `${metricTitle} ${info.selectorsMap[selector].label}`;
						metrics.push({
							title: title,
							name: `${metric}:${selector}`
						});
					});
				}

				const buildNames = meta.envs.map(env => {
					return {
						title: env,
						name: env
					};
				});
				buildNames.unshift({
					title: 'All builds'
				});

				const asearches = searches.slice();
				asearches.forEach(s => s.title = s.name);
				asearches.unshift({
					title: 'All reports'
				});

				const conditions = info.conditions.map(condition => {
					return {
						title: condition.label,
						name: condition.condition
					}
				});

				const assertions = data[2];
				const blankMetric = Object.keys(meta.metrics)[0] || info.metrics[1].metric;
				const blankSelector = meta.metrics.hasOwnProperty(blankMetric) ?
					meta.metrics[blankMetric][0] : info.selectors[1].selector;
				const blankAssertion = {
					metric: blankMetric,
					selector: blankSelector,
					condition: conditions[0].name,
					expected: 100
				};

				assertions.unshift(blankAssertion);
				assertions.forEach(assertion => {
					assertion.groups = groups;
					assertion.metrics = metrics;
					assertion.builds = buildNames;
					assertion.searches = asearches;
					assertion.conditions = conditions;

					if (assertion.hasOwnProperty('group')) {
						const group = groups.find(g => g.name === assertion.group);

						if (group !== undefined) {
							assertion._group = group;
						}
					} else {
						assertion._group = groups[0];
					}

					assertion._metric = metrics.find(metric => metric.name === `${assertion.metric}:${assertion.selector}`);

					if (assertion.hasOwnProperty('search_name')) {
						const search = asearches.find(s => s.name === assertion.search_name);

						if (search !== undefined) {
							assertion.search = search;
						}
					} else {
						assertion.search = asearches[0];
					}

					if (assertion.hasOwnProperty('build_name')) {
						const build = buildNames.find(b => b.name === assertion.build_name);

						if (build !== undefined) {
							assertion.build = build;
						}
					} else {
						assertion.build = buildNames[0];
					}

					assertion._condition = conditions.find(c => c.name === assertion.condition);

					assertion.unit = info.metricsMap[assertion.metric].unit || ' ';
				});

				return {
					project: project,
					blankAssertion: assertions.shift(),
					assertions: assertions
				}
			});
	}

	handleUpdateAssertions(data) {
		return this._apiClient.updateProjectAssertions(data.projectId, data.assertions);
	}
}

module.exports = ProjectSettingsAssertions;
