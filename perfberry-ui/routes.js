'use strict';

module.exports = [
	{
		name: 'report-edit',
		expression: '/projects/:project[Pages,Report,ReportEdit]/reports/:report[Pages,Report]/edit',
		map: function (state) {
			state.Pages.page = 'report';
			state.Report.tab = 'edit';
			return state;
		}
	},
	{
		name: 'report-graphs',
		expression: '/projects/:project[Pages,Report]/reports/:report[Pages,Report]/graphs' +
		'?status=:status[ReportGraphs]&group=:group[ReportGraphs]',
		map: function (state) {
			state.Pages.page = 'report';
			state.Report.tab = 'graphs';
			return state;
		}
	},
	{
		name: 'report-transactions',
		expression: '/projects/:project[Pages,Report]/reports/:report[Pages,Report]/transactions' +
		'?limit=:limit[ReportTransactions]' +
		'&group=:group[ReportTransactions]' +
		'&query=:query[ReportTransactions]' +
		'&status=:status[ReportTransactions]' +
		'&code=:code[ReportTransactions]',
		map: function (state) {
			state.Pages.page = 'report';
			state.Report.tab = 'transactions';
			return state;
		}
	},
	{
		name: 'report-assertions',
		expression: '/projects/:project[Pages,Report]/reports/:report[Pages,Report]/assertions' +
			'?compare=:comparedReportId[Report]',
		map: function (state) {
			state.Pages.page = 'report';
			state.Report.tab = 'assertions';
			return state;
		}
	},
	{
		name: 'report',
		expression: '/projects/:project[Pages,Report]/reports/:report[Pages,Report]' +
		'?compare=:comparedReportId[Report,ReportStatistics]',
		map: function (state) {
			state.Pages.page = 'report';
			state.Report.tab = 'statistics';
			return state;
		}
	},
	{
		name: 'project-graphs',
		expression: '/projects/:project[Pages,Project,ProjectGraphs]/graphs' +
		'?from=:from[ProjectGraphs]&to=:to[ProjectGraphs]' +
		'&refresh=:refresh[ProjectGraphs]&graphs=:graphs[ProjectGraphs]' +
		'&fullscreen=:fullscreen[Pages]',
		map: function (state) {
			state.Pages.page = 'project';
			state.Project.tab = 'graphs';
			return state;
		}
	},
	{
		name: 'project-settings',
		expression: '/projects/:project[Pages,Project]/settings?tab=:tab[ProjectSettings]',
		map: function (state) {
			state.Pages.page = 'project';
			state.Project.tab = 'settings';
			return state;
		}
	},
	{
		name: 'project-reports',
		expression: '/projects/:project[Pages,Project]/reports' +
		'?page=:page[ProjectReports]&query=:query[ProjectReports]&compare=:comparedReportId[ProjectReports]',
		map: function (state) {
			state.Pages.page = 'project';
			state.Project.tab = 'reports';
			return state;
		}
	},
	{
		name: 'project',
		expression: '/projects/:project[Pages,Project,ProjectGraphs]' +
		'?tab=:tab[Project]' +
		'&from=:from[ProjectGraphs]&to=:to[ProjectGraphs]' +
		'&refresh=:refresh[ProjectGraphs]&graphs=:graphs[ProjectGraphs]' +
		'&highlightReport=:highlightReport[ProjectGraphs]',
		map: function (state) {
			state.Pages.page = 'project';
			return state;
		}
	},
	{
		expression: '/projects',
		map: function (state) {
			state.Pages = {
				page: 'projects'
			};
			return state;
		}
	},
	{
		expression: '/info',
		map: function (state) {
			state.Pages = {
				page: 'info'
			};
			return state;
		}
	},
	{
		expression: '/',
		map: function (state) {
			state.Pages = {
				page: 'home'
			};
			return state;
		}
	}
];
