'use strict';

class ReportTransactions {

	constructor() {
		this.tables = null;
	}

	/**
	 * Gets data for the component's template.
	 * @returns {Promise<Object>} Promise of data.
	 */
	render() {
		return this.$context.getStoreData();
	}

	bind() {
		$('#filters .ui.checkbox').checkbox({
			onChange: () => {
				$('#filters').submit();
			}
		});

		$('.ui.dropdown').dropdown({
			fullTextSearch: true,
			onChange: () => {
				$('#filters').submit();
			}
		});

		$(this.$context.element.querySelectorAll('.ui.accordion')).accordion();

		this.tables = $('table').DataTable({
			paging: false,
			info: false,
			searching: false,
			fixedHeader: {
				header: true,
				headerOffset: -13
			}
		});

		return {
			click: {
				'#filters a': this.handleClear,
			},
			keyup: {
				'input': this.handleSearch
			}
		}
	}

	unbind() {
		this.tables.destroy();
	}

	handleClear(event) {
		event.preventDefault();
		event.stopPropagation();

		$('.ui.dropdown').dropdown('set selected', 'Global');

		$(this.$context.element.querySelectorAll('#filters .ui.checkbox'))
			.checkbox('uncheck');
	}

	handleSearch(event) {
		event.preventDefault();
		event.stopPropagation();

		if (event.keyCode === 13 || event.which === 13) {
			$('#filters').submit();
		}
	}
}

module.exports = ReportTransactions;
