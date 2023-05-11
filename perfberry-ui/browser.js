'use strict';

// this config will be replaced by `./config/browser.json` when building
// because of `browser` field in `package.json`
const config = require('./config/environment.json');

// catberry application
const catberry = require('catberry');
const cat = catberry.create(config);

// register Catberry plugins needed in a browser
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

// starts the application when DOM is ready
cat.startWhenReady();
