define(["app/comp/main/views/BreadcrumbsView"], function(view) {
	var obj = {};

	obj.view = view;

	obj.set = function(pageName) {
		console.info('setting page name');
		obj.view.setCrumb(pageName);
	};

	obj.init = function() {
		obj.view.getHomeButton().bind('click', function(event) {
			event.preventDefault();

			//TODO cycle dependency
			require("app/comp/main/controllers/MainCtrl").clearContent();
		});
	}

	return obj;

});

