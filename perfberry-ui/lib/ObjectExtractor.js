exports.extract = function(obj, path) {
	for (let i = 0, paths = path.split('.'), len = paths.length; i < len; i++) {
		obj = obj[paths[i]];
	}

	return obj;
};
