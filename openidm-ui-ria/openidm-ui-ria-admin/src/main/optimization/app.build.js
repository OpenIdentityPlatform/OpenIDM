({
	appDir : "../../../target/classes",
	baseUrl : ".",
	dir : "../../../target/minified",
	mainConfigFile : '../js/main.js',
	modules : [ {
		name : "main",
		excludeShallow : [ 
		    "config/AppConfiguration",
		    "config/ValidatorsConfiguration",
		    "config/process/AdminConfig",
		    "config/process/UserConfig",
		    "config/process/CommonConfig",
		    "mustache",
		    "backbone",
		    "underscore",
		    "spin",
		    "js2form",
		    "form2js",
		    "contentflow"
		]
	} ]
})