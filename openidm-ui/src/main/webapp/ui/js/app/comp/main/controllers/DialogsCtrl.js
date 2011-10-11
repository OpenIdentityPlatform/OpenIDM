define(["app/comp/main/views/DialogsView"], function (dialogsView) {
	var obj = {}

	obj.view = dialogsView;

	obj.show = function() {
		console.log("show dialog");
		obj.view.show();
	}

	obj.close = function() {
		console.log("close dialog");
		obj.view.close();
	}

	obj.setContent = function(content) {
		obj.view.setContent(content);
	}

	obj.setActions = function(actions) {
		obj.view.setActions(actions)
	}

	obj.setWidth = function(width) {
		obj.view.setWidth(width);
	}

	obj.setHeight = function(height) {
		obj.view.setHeight(height);
	}

	return obj;
})

