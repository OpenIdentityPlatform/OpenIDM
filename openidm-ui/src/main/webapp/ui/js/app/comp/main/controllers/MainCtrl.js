define(["app/comp/user/controllers/LoginCtrl","app/comp/main/controllers/BreadcrumbsCtrl"], function (loginCtrl, breadcrumbsCtrl) {
	
	console.debug("mainctrl lc=" + loginCtrl);
	
	$(document).ready(function() {
		loginCtrl.init();
		breadcrumbsCtrl.init();
	});

	var obj = {};

	obj.clearContent = function() {
		$("#content").fadeOut(100, function() {
			$(this).html('');
		});
	}

	return obj;

});


