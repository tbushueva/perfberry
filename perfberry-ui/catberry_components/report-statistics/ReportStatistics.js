'use strict';

const PAGE_SIZE = 14;
const APDEX_RATINGS = [
	{
		range: {
			from: 0.94,
			to: 1.0.toFixed(2)
		},
		label: 'Excellent',
		color: 'green'
	}, {
		range: {
			from: 0.85,
			to: 0.93
		},
		label: 'Good',
		color: 'olive'
	}, {
		range: {
			from: 0.7.toFixed(2),
			to: 0.84
		},
		label: 'Fair',
		color: 'yellow'
	}, {
		range: {
			from: 0.5.toFixed(2),
			to: 0.69
		},
		label: 'Poor',
		color: 'orange'
	}, {
		range: {
			from: 0.0.toFixed(2),
			to: 0.49
		},
		label: 'Unacceptable',
		color: 'red'
	}
];

class ReportStatistics {

	constructor(locator) {
        this._config = locator.resolve('config');

		this.page = 1;
		this.tables = null;
	}

	/**
	 * Gets data for the component's template.
	 * @returns {Promise<Object>} Promise of data.
	 */
	render() {
		return this.$context.getStoreData()
			.then(data => {
				data.builds.forEach(build => {
					if (build.statistics.global.hasOwnProperty('apdex')) {
						if (build.statistics.global.apdex.samples === 0) {
							build.statistics.global.apdex.isNoSamples = true;
						} else {
							if (build.statistics.global.apdex.samples < 100) {
								build.statistics.global.apdex.isSmallGroup = true;
							}

							build.statistics.global.apdex.value =
								build.statistics.global.apdex.value.toFixed(2);
							const apdex = build.statistics.global.apdex.value;
							const rating = APDEX_RATINGS
								.find(r => r.range.from <= apdex && apdex <= r.range.to);
							build.statistics.global.apdex.rating = rating.label.toLowerCase();
							build.statistics.global.apdex.color = rating.color;
						}
					}

					build.statistics.groups.forEach(group => {
						if (group.hasOwnProperty('apdex')) {
							if (group.apdex.samples === 0) {
								group.apdex.isNoSamples = true;
							} else {
								if (group.apdex.samples < 100) {
									group.apdex.isSmallGroup = true;
								}

								group.apdex.value = group.apdex.value.toFixed(2);
							}
						}
					});
				});

				data.apdexRatings = APDEX_RATINGS;

				return data;
			});
	}

	bind() {
		this.tables = $('table[data-name="group-statistics"]').DataTable({
			paging: false,
			info: false,
			searching: false,
			fixedHeader: {
				header: true,
				headerOffset: -13
			}
		});
		this.refreshPagination();

		// Enable filtering requests.
		$('input.filterable').keyup(event => {
			var data = $(event.target).val();
			var rows = $('table.filterable tbody').find('tr').hide();
			if (data) {
				var requests = $('table.filterable tbody').find('tr td.name');
				requests.filter(":contains('" + data + "')").parent().show();
			} else {
				rows.show();
			}
		});

		return {
			click: {
				'a[name=pager]': this.handlePager
			}
		}
	}

	unbind() {
		this.tables.destroy();
	}

	handlePager(event) {
		event.preventDefault();
		event.stopPropagation();

		const direction = event.target.getAttribute('data-direction');

		this.setPage(direction);
	}

	setPage(direction) {
		switch(direction) {
			case 'next':
				this.page++;
				break;
			case 'back':
				this.page--;
				break;
		}
		this.refreshPagination();
	}

	refreshPagination() {
		const rows = this.$context.element.querySelectorAll('table[data-name="group-statistics"] tr');
		const columns = rows.length > 0 ? rows[0].querySelectorAll('th,td').length - 1 : 0;
		const hasRemainder = columns % PAGE_SIZE > 0;
		const pages = hasRemainder ? parseInt(columns / PAGE_SIZE) + 1 : columns / PAGE_SIZE;

		if (this.page === 1) {
			$(this.$context.element.querySelectorAll('a[data-direction=back]')).hide();
		} else {
			$(this.$context.element.querySelectorAll('a[data-direction=back]')).show();
		}

		if (this.page === pages) {
			$(this.$context.element.querySelectorAll('a[data-direction=next]')).hide();
		} else {
			$(this.$context.element.querySelectorAll('a[data-direction=next]')).show();
		}

		this.$context.element.querySelectorAll('table[data-name="group-statistics"] tr').forEach(row => {
			row.querySelectorAll('th,td').forEach((cell, i) => {
				if (i > (this.page - 1) * PAGE_SIZE && i <= this.page * PAGE_SIZE) {
					$(cell).show();
				} else if (i !== 0) {
					$(cell).hide();
				}
			});
		});

		this.tables.columns.adjust().draw(false);
	}
}

module.exports = ReportStatistics;
