define(["app/comp/main/views/MessagesView"], function (messagesView) {
	var obj = {};

	obj.view = messagesView;

	obj.displayMessage = function(type, message) {
		console.info('displaing message');

		if (type == 'info') {
			obj.view.addInfoMessage(message, "#messages");
		} else {
			obj.view.addErrorMessage(message, "#messages");
		}
	};

	obj.displayMessageOn = function(type, message, divid) {
		console.info('displaing message');

		if (type == 'info') {
			obj.view.addInfoMessage(message, divid);
		} else {
			obj.view.addErrorMessage(message, divid);
		}
	};


	return obj;
});

