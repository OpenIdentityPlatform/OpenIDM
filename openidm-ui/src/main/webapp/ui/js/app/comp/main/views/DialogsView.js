define(function() {
	var obj = {};

	obj.show = function() {
		$("#dialog").fadeIn(300, function() {
			$(this).show();
		});
	}

	obj.close = function() {
		$("#dialog").fadeOut(300, function() {
			$(this).hide();
		});
	}

	obj.setContent = function(content) {
		$("#dialogContent").html(content);
	}

	obj.setActions = function(actions) {
		$("#dialogActions").html(actions);
	}

	obj.setWidth = function(width) {
		$("#dialogContainer").css('width', width);
	}

	obj.setHeight = function(height) {
		$("#dialogContainer").css('height', height + 50);
		$("#dialogContent").css('height', height);
	}

	return obj;
});

