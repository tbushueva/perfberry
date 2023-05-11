'use strict';

const moment = require('moment');

/*
 * This is a Catberry Cat-component file.
 * More details can be found here
 * http://catberry.org/documentation#cat-components-interface
 */

class ProjectGraphs {
	constructor() {
		this.range = {
			from: null,
			to: null
		};

		this.duration = null;

		this.timerId = null;
	}

	/**
	 * Gets data for the component's template.
	 * @returns {Promise<Object>} Promise of data.
	 */
	render() {
		return this.$context.getStoreData()
			.then(data => {
				this.projectId = data.project.id;

				const range = data.ranges.current.range ?
					data.ranges.current.range :
					data.ranges.current.from.label +
					'..' + data.ranges.current.to.label;
				const rangeTitle = data.isMultiMode ?
					range : 'KPI for ' + range.toLowerCase();
				data.title = rangeTitle +
					', refresh every ' + data.ranges.current.duration.label;

				return data;
			});
	}

	_getGraphs() {
		const values = $('.ui.graph.dropdown').dropdown('get value');
		const count = values.length / 3;
		const inputs = this.$context.element.querySelectorAll('input[name=lines]');
		let graphs = [];
		for (let i = 0; i < count; i++) {
			const graph = parseInt(inputs[i].getAttribute('data-graph'));
			if (graphs[graph] === undefined) {
				graphs[graph] = [];
			}

			const parts = values[i * 3].split(',');

			graphs[graph].push({
				search: values[2 + i * 3],
				env: values[1 + i * 3],
				group: parts[0],
				metric: parts[1],
				selector: parts[2]
			});
		}

		return graphs;
	}

	_graphsToUri(graphs) {
		return graphs
			.map(graph => graph
				.map(line => line.search + ',' + line.env + ',' + line.group + ',' + line.metric + ',' + line.selector)
				.join(';')
			);
	}

	refresh(newGraphs, fullscreenEndabled = false) {
		const graphs = this._graphsToUri(newGraphs || this._getGraphs());
		return this.$context.getStoreData()
			.then(data => {
				const route = data.isMultiMode ? 'project-graphs' : 'project';
				const newPage = this.$context.getRouteURI(route, {
					project: data.project.alias,
					from: this.range.from || data.ranges.current.from.value,
					to: this.range.to || data.ranges.current.to.value,
					refresh: this.duration || data.ranges.current.duration.value,
					fullscreen: data.fullscreenEnabled || fullscreenEndabled,
					graphs: graphs
				});
				this.$context.redirect(newPage);
			});
	}

	bind() {
		$('.ui.accordion').accordion();

		$('#rangestart').calendar({
			type: 'date',
			firstDayOfWeek: 1,
			endCalendar: $('#rangeend'),
			onChange: (date) => {
				this.range.from = moment(date).format('YYYY-MM-DDT00:00:00');
			}
		});

		$('#rangeend').calendar({
			type: 'date',
			firstDayOfWeek: 1,
			today: true,
			startCalendar: $('#rangestart'),
			onChange: (date) => {
				this.range.to = moment(date).format('YYYY-MM-DDT00:00:00');
				this.refresh();
			}
		});

		$('.ui.graph.dropdown').dropdown({
			fullTextSearch: true,
			onChange: () => {
				this.refresh();
			}
		});
		$('.ui.options.dropdown').dropdown({
			action: 'nothing'
		});

		return this.$context.getStoreData()
			.then(data => {
				$('#rangestart').calendar(
					'set date',
					data.ranges.current.from.date, true, false);
				$('#rangeend').calendar(
					'set date',
					data.ranges.current.to.date, true, false);

				const duration = moment.duration(data.ranges.current.duration.value).asMilliseconds();
				let elapsed = 0;
				this.timerId = setInterval(() => {
					$('.ui.progress').progress({
						autoSuccess: false,
						value: elapsed,
						total: duration
					});

					if (elapsed === duration) {
						this.$context.sendAction('refresh');
					} else {
						elapsed += 1000;
					}
				}, 1000);

				return {
					click: {
						'a[name=add-line]': this.handleAddLine,
						'a[name=remove]': this.handleRemoveLine,
						'a[name=remove-graph]': this.handleRemoveGraph,
						'a[name=fullscreen-graph]': this.handleFullscreenGraph,
						'a[name=change-range]': this.handleChangeRange,
						'a[name=change-refresh]': this.handleChangeRefresh,
						'button[name=add-graph]': this.handleAddGraph,
						'button[name=save]': this.handleSave
					}
				}
			});
	}

	unbind() {
		clearTimeout(this.timerId);
	}

	handleChangeRange(event) {
		this.range.from = event.target.getAttribute('data-from');
		this.range.to = event.target.getAttribute('data-to');
		this.refresh();
	}

	handleChangeRefresh(event) {
		this.duration = event.target.getAttribute('data-duration');
		this.refresh();
	}

	handleAddGraph() {
		let newGraphs = this._getGraphs();
		newGraphs.push(newGraphs[newGraphs.length - 1]);
		this.refresh(newGraphs);
	}

	handleRemoveGraph(event) {
		event.preventDefault();
		event.stopPropagation();

		const graph = parseInt(event.currentTarget.getAttribute('data-graph'));

		let newGraphs = this._getGraphs();
		newGraphs.splice(graph, 1);
		this.refresh(newGraphs);
	}

	handleFullscreenGraph(event) {
		event.preventDefault();
		event.stopPropagation();

		const graph = parseInt(event.currentTarget.getAttribute('data-graph'));

		this.refresh([this._getGraphs()[graph]], true);
	}

	handleAddLine(event) {
		event.preventDefault();
		event.stopPropagation();

		const graph = parseInt(event.currentTarget.getAttribute('data-graph'));

		let newGraphs = this._getGraphs();
		newGraphs[graph].push(newGraphs[graph][newGraphs[graph].length - 1]);
		this.refresh(newGraphs);
	}

	handleRemoveLine(event) {
		event.preventDefault();
		event.stopPropagation();

		const graph = parseInt(event.currentTarget.getAttribute('data-graph'));
		const index = parseInt(event.currentTarget.getAttribute('data-index'));

		let newGraphs = this._getGraphs();
		newGraphs[graph].splice(index, 1);
		this.refresh(newGraphs);
	}

	handleSave(event) {
		event.preventDefault();
		event.stopPropagation();

		return this.$context.getStoreData()
			.then(data => {
				const graphs = this._getGraphs()
					.map(graph => {
						return graph.map(line => {
							let item = {
								env: line.env,
								metric: line.metric,
								selector: line.selector
							};

							if (line.search !== '') {
								item.search_name = line.search;
							}

							if (line.group !== '') {
								item.group = line.group;
							}

							return item;
						});
					});

				return this.$context.sendAction('update-graphs', {
					projectId: data.project.id,
					graphs: graphs,
					isMultiMode: data.isMultiMode
				});
			})
			.then(updated => {
				if (updated) {
					this.refresh([]);
				}
			});
	}
}

module.exports = ProjectGraphs;
