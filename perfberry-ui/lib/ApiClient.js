'use strict';

class ApiClient {

	/**
	 * Creates a new instance of the ApiClient.
	 * @param {ServiceLocator} locator Catberry's service locator.
	 */
	constructor(locator) {

		/**
		 * Current config.
		 * @type {Object}
		 * @private
		 */
		this._config = locator.resolve('config');

		this._uhr = locator.resolve('uhr');
	}

	request(method, path, query, timeout) {
		const data = query || {};
		const options = {
			timeout: timeout || 3000,
			data: data,
			headers: {
				'Content-Type': 'application/json'
			}
		};
		const apiEndpoint = this._config.apiEndpoint;
		return this._uhr[method](apiEndpoint + path, options)
			.catch(err => console.error(err));
	}

	projects() {
		const path = '/v1/projects';
		return this.request('get', path)
			.then(result => result.content);
	}

	projectByAlias(alias) {
		const path = '/v1/projects';

		let query = {
			alias: alias
		};

		return this.request('get', path, query)
			.then(result => result.content[0]);
	}

	projectOverview(projectId) {
		const path = '/v1/projects/' + projectId + '/overview';
		return this.request('get', path)
			.then(result => result.content);
	}

	projectSearches(projectId) {
		const path = '/v1/projects/' + projectId + '/searches';
		return this.request('get', path)
			.then(result => result.content);
	}

	projectAssertions(projectId) {
		const path = '/v1/projects/' + projectId + '/assertions';
		return this.request('get', path)
			.then(result => result.content);
	}

	projectGraphs(projectId) {
		const path = '/v1/projects/' + projectId + '/graphs';
		return this.request('get', path)
			.then(result => result.content);
	}

	projectApdex(projectId) {
		const path = '/v1/projects/' + projectId + '/apdex';
		return this.request('get', path)
			.then(result => result.content);
	}

	projectMeta(projectId) {
		const path = '/v1/projects/' + projectId + '/meta';
		return this.request('get', path)
			.then(result => result.content);
	}

	updateProject(project) {
		const path = '/v1/projects/' + project.id;
		return this.request('put', path, JSON.stringify(project))
			.then(result => result.status.code === 204);
	}

	updateProjectOverview(projectId, items) {
		const path = '/v1/projects/' + projectId + '/overview';
		return this.request('put', path, JSON.stringify(items))
			.then(result => result.status.code === 204);
	}

	updateProjectSearches(projectId, items) {
		const path = '/v1/projects/' + projectId + '/searches';
		return this.request('put', path, JSON.stringify(items))
			.then(result => result.status.code === 204);
	}

	updateProjectAssertions(projectId, items) {
		const path = '/v1/projects/' + projectId + '/assertions';
		return this.request('put', path, JSON.stringify(items))
			.then(result => result.status.code === 204);
	}

	updateProjectGraphs(projectId, items) {
		const path = '/v1/projects/' + projectId + '/graphs';
		return this.request('put', path, JSON.stringify(items))
			.then(result => result.status.code === 204);
	}

	updateProjectApdex(projectId, items) {
		const path = '/v1/projects/' + projectId + '/apdex';
		return this.request('put', path, JSON.stringify(items))
			.then(result => result.status.code === 204);
	}

	badges(projectId) {
		return {
			static: this._config.apiEndpoint + '/v1/projects/' + projectId + '/badges/static',
			status: this._config.apiEndpoint + '/v1/projects/' + projectId + '/badges/status',
			stats: this._config.apiEndpoint + '/v1/projects/' + projectId + '/badges/stats'
		};
	}

	reports(projectId, limit = 20, offset = 0, searchQuery) {
		const path = '/v1/projects/' + projectId + '/reports';

		let query = {
			limit: limit,
			offset: offset
		};

		if (searchQuery !== undefined) {
			query.query = searchQuery;
		}

		return this.request('get', path, query)
			.then(result => result.content);
	}

	buildsStats(projectId, options = {}) {
		const path = '/v1/projects/' + projectId + '/history';

		return this.request('get', path, options, 15000)
			.then(result => result.content);
	}

	report(projectId, reportId) {
		const path = '/v1/projects/' + projectId + '/reports/' + reportId;

		return this.request('get', path)
			.then(result => result.content);
	}

	updateReport(projectId, reportId, report) {
		const path = '/v1/projects/' + projectId + '/reports/' + reportId;
		return this.request('patch', path, JSON.stringify(report))
			.then(result => result.status.code === 204);
	}

	deleteReport(projectId, reportId) {
		const path = '/v1/projects/' + projectId + '/reports/' + reportId;
		return this.request('delete', path)
			.then(result => result.status.code === 204);
	}

	builds(projectId, reportId) {
		const path = '/v1/projects/' + projectId + '/reports/' + reportId + '/builds';

		return this.request('get', path)
			.then(result => result.content);
	}

	statistics(projectId, reportId, buildId) {
		const path = '/v1/projects/' + projectId + '/reports/' + reportId + '/builds/' + buildId + '/statistics';

		return this.request('get', path)
			.then(result => result.content);
	}

	assertions(projectId, reportId, buildId) {
		const path = '/v1/projects/' + projectId + '/reports/' + reportId + '/builds/' + buildId + '/assertions';

		return this.request('get', path)
			.then(result => result.content);
	}

	transactions(projectId, reportId, buildId, limit, group, query, status, code) {
		const path = '/v1/projects/' + projectId + '/reports/' + reportId + '/builds/' + buildId + '/transactions';

		const params = {
			limit: limit || 100
		};

		if (group !== undefined) {
			params.group = group;
		}
		if (query !== undefined && query !== '') {
			params.query = query;
		}
		if (status !== undefined) {
			params.status = status;
		}
		if (code !== undefined) {
			params.code = code;
		}

		return this.request('get', path, params, 30000)
			.then(result => result.content);
	}

	graphs(projectId, reportId, buildId, group, hasError) {
		const path = '/v1/projects/' + projectId + '/reports/' + reportId + '/builds/' + buildId + '/graphs';

		let query = {};
		if (group !== undefined) {
			query.group = group;
		}
		if (hasError !== undefined) {
			query.has_error = hasError;
		}

		return this.request('get', path, query, 30000)
			.then(result => result.content);
	}

	buildStatisticsDiff(originalId, comparedId) {
		const path = '/v1/diffs/builds/' + originalId + '/' + comparedId + '/statistics';

		return this.request('get', path)
			.then(result => result.content);
	}

	info() {
		const path = '/v1/info';
		return this.request('get', path)
			.then(result => result.content);
	}
}

module.exports = ApiClient;
