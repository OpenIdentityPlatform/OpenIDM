define(function() {
	var obj = {};

	obj.setCrumb = function(pageName) {

		$("#nav-content span:last").fadeOut(100, function() { 
			$(this).html(pageName).fadeIn(100);
		}); 
		
	};
  
	obj.getHomeButton = function() {
		return $("#home_link");
	}

	return obj;

});


