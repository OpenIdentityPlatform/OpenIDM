require.config({
    "packages": ["app", "libs"]
});

require(["jquery", "app/util/UIUtils" ,"app", "libs"], function($,users,products) {
	console.log("after loading");
    $(function() {
    	$(document).ready(function() {
    	//	products.a();
    	});
    });
});

