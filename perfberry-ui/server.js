'use strict';

const http = require('http');
const path = require('path');

// configuration
const config = require('./config/environment.json');
const isRelease = process.argv.length === 3 ?	process.argv[2] === 'release' : undefined;
config.publicPath = path.join(__dirname, 'public');
config.server.port = config.server.port || 3000;
config.isRelease = isRelease === undefined ? config.isRelease : isRelease;

// catberry application
const catberry = require('catberry');
const cat = catberry.create(config); // the Catberry application object
cat.events.on('ready', () => {
	const logger = cat.locator.resolve('logger');
	logger.info(`Ready to handle incoming requests on port ${config.server.port}`);
});

// register Catberry plugins needed on the server
const templateEngine = require('catberry-handlebars');
templateEngine.register(cat.locator);

const loggerPlugin = require('catberry-logger');
loggerPlugin.register(cat.locator);

const uhrPlugin = require('catberry-uhr');
uhrPlugin.register(cat.locator);

const ApiClient = require('./lib/ApiClient');
// when you have created an instance of the Catberry application
// you can register anything you want in the Service Locator.
// last "true" value means that the instance of your service is a singleton
cat.locator.register('apiClient', ApiClient, true);

// web server
const express = require('express');
const app = express();

const serveStatic = require('serve-static');
app.use(serveStatic(config.publicPath, {
	extensions: ['txt']
}));

app.use(cat.getMiddleware()); // Catberry app as a middleware

const errorhandler = require('errorhandler');
app.use(errorhandler());

http
	.createServer(app)
	.listen(config.server.port);
