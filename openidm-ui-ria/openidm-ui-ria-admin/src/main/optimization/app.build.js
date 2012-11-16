({
	appDir : "../../../target/classes",
	baseUrl : ".",
	dir : "../../../target/minified",
	mainConfigFile : '../js/main.js',
	preserveLicenseComments: false,
	modules : [ {
		name : "main",
		excludeShallow : [ 
		    "config/AppConfiguration",
		    "config/ValidatorsConfiguration",
		    "config/process/AdminConfig",
		    "config/process/UserConfig",
		    "config/process/CommonConfig",
		    "mustache",
		    "i18next",
		    "backbone",
		    "underscore",
		    "spin",
		    "js2form",
		    "form2js",
		    "contentflow",
		    "spin",
		    "dataTable",
		    "jqueryui",
		    "doTimeout",
		    "handlebars",
		    "moment"
		]
	} ]
})