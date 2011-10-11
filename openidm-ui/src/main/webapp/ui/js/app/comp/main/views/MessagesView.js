define(function() {
	var obj = {};

	obj.addErrorMessage = function(msg, divid) {
		var msgData = {
				type : 'errorMessage',
				msg : msg,
				divid : divid
		};

		obj.addMessage(msgData);
	};

	obj.addInfoMessage = function(msg, divid) {
		var msgData = {
				type : 'confirmMessage',
				msg : msg,
				divid : divid
		};

		obj.addMessage(msgData);
	};

	obj.addMessage = function(msgData) {
		$(msgData.divid).append("<div class='" + msgData.type + " radious'>" + msgData.msg + "</div>");
		$(msgData.divid+" > div:last").fadeIn(500).delay(2000).fadeOut(500);
	};

	return obj;
});

