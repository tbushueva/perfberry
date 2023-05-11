'use strict';

const gulp = require('gulp');
const cleanCSS = require('gulp-clean-css');
const concat = require('gulp-concat');
const del = require('del');

const cssFiles = {
    minify: [
        'node_modules/datatables.net-dt/css/jquery.dataTables.css',
        'node_modules/datatables.net-fixedheader-dt/css/fixedHeader.dataTables.css'
    ],
    concat: [
        'semantic/dist/semantic.min.css'
    ]
};

const jsFiles = {
    concat: [
        'node_modules/jquery/dist/jquery.min.js',
        'node_modules/jquery-serializejson/jquery.serializejson.min.js',
        'semantic/dist/semantic.min.js',
        'node_modules/datatables.net/js/jquery.dataTables.js',
        'node_modules/datatables.net-fixedheader/js/dataTables.fixedHeader.min.js'
    ]
};


function assets() {
	return gulp.src('semantic/dist/themes/**')
		.pipe(gulp.dest('public/static/themes/'));
}
exports.assets = assets;


function cssMinify() {
	return gulp.src(cssFiles.minify)
		.pipe(cleanCSS())
		.pipe(gulp.dest('build/css/'));
}
exports.cssMinify = cssMinify;

function cssConcat() {
	return gulp.src([...cssFiles.concat, 'build/css/*'])
		.pipe(concat('libs.css'))
		.pipe(gulp.dest('build/libs/'));
}
exports.cssConcat = cssConcat;

const css = gulp.series(cssMinify, cssConcat);
exports.css = css;

function js() {
	return gulp.src(jsFiles.concat)
		.pipe(concat('libs.js'))
		.pipe(gulp.dest('build/libs/'));
}
exports.js = js;

function copyLibs() {
	return gulp.src('build/libs/*')
		.pipe(gulp.dest('public/static/'));
}
exports.copyLibs = copyLibs;

const statics = gulp.series(gulp.parallel(css, js), copyLibs);
exports.statics = statics;


function clean() {
	return del(['build']);
}
exports.clean = clean;


const build = gulp.series(gulp.parallel(assets, statics), clean);
exports.build = build;

exports.default = build;
