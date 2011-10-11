define(["app/comp/main/controllers/DialogsCtrl"], function(dialogsCtrl) {
	
	var obj = {};
	
	obj.dialog = dialogsCtrl;

	obj.getConfirmButton = function() {
		return $("#dialog input[name='dialogOk']");
	};

	obj.getCloseButton = function() {
		return $('#dialogClose');
	},

	obj.getEmailInput = function() {
		return $("#dialog input[name='forgottenEmail']");
	},

	obj.getPasswordInput = function() {
		return $("#dialog input[name='password']");
	},
	
	obj.getPasswordConfirmInput = function() {
		return $("#dialog input[name='passwordConfirm']");
	},
	
	obj.getFgtnSecurityQuestion = function() {
		return $("#fgtnSecurityQuestion");
	},

	obj.getFgtnSecurityAnswer = function() {
		return $("#dialog input[name='fgtnSecurityAnswer']");
	},

	obj.getFgtnEmailDiv = function() {
		return $("#fgtnEmailDiv");
	},

	obj.getFgtnAnswerDiv = function() {
		return $("#fgtnAnswerDiv");
	},

	obj.getPasswordResultDiv = function() {
		return $("#fgtnPasswordResult");
	},

	obj.getPasswordResetLink = function() {
		return $("#passwordResetLink");
	},

	obj.showPassword = function (val) {
		obj.getPasswordResultDiv().text("");
		obj.getPasswordResultDiv().append("<div class='field'>" + val + "</div>");
	},

	obj.reply = function() {
		console.log("View reply");
	},
	
	obj.show = function(callback) {
		self = this;

		console.log("showing forgotten password dialog");
		
		$.ajax({
			type: "GET",
			url: "js/app/comp/user/templates/ForgottenPasswordTemplate.html",
			dataType: "html",
			success: function(data) {
				self.dialog.setContent(data);
				self.dialog.setActions("<input type='button' name='dialogClose' id='dialogClose' class='button gray floatRight' value='Close' /><input type='button' name='dialogOk' id='dialogOk' class='button gray floatRight' value='Change password' />");
				self.dialog.setWidth(800);
				self.dialog.setHeight(210);
				self.dialog.show();
				callback();
			},
			error: callback
		});
	};

	obj.close = function() {
		obj.dialog.close();
	},

	obj.enableSaveButton = function() {
		obj.getConfirmButton().removeClass('gray').addClass('orange');
	},

	obj.disableSaveButton = function() {
		obj.getConfirmButton().removeClass('orange').addClass('gray');
	},
	
	obj.showEmail = function() {
		obj.getFgtnAnswerDiv().hide();
		obj.getFgtnEmailDiv().show();
		obj.dialog.setHeight(210);

	},
	
	obj.showAnswer = function() {
	  obj.getFgtnAnswerDiv().show();
	  obj.dialog.setHeight(380);
	},
		
	console.log("Forgotten Password View created");
	return obj;
	
});

