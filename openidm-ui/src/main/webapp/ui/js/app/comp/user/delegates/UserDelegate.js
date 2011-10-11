define(["app/util/Constants"],function(constants) {

	var obj = {};

	obj.getUser = function(id, successCallback, errorCallback) {
		console.info("getting user[id=" + id + "]");

		$.ajax({
			url: util.constants.host + "/openidm/managed/user/" + id,
			dataType: 'json',
			success: successCallback,
			error: errorCallback
		});
	};

	obj.addUser = function(user, successCallback, errorCallback) {
		console.info("adding user[" + JSON.stringify(user) + "]");

		$.ajax({
			type: "PUT",
			url: util.constants.host + "/openidm/managed/user/" + user.email,
			dataType: "json",
			contentType: 'application/json; charset=utf-8',
			data: JSON.stringify(user),
			success: successCallback,
			error: errorCallback
		});
	};

	obj.changeField = function(id, field, value, successCallback, errorCallback) {
		console.info("changing " + field);

		$.ajax({
			type: "POST",
			url: util.constants.host + "/openidm/managed/user/?_action=patch&_query-id=for-username&uid=" + id,
			dataType: "json",
			contentType: 'application/json; charset=utf-8',
			data: '[{"replace":"/' + field + '","value":"' + value + '"}]',
			success: successCallback,
			error: errorCallback
		});
	}

	obj.updateUser = function(oldUserData, newUserData, successCallback, errorCallback, noChangesCallback) {
		console.info("updating user");

		var differences = this.getDifferences(oldUserData, newUserData);
		if(differences.length==0){
			console.log("Brak zmian");
			this.noChangesCallback();
			return;
		}
		var dataString = this.buildDataString(differences);

		$.ajax({
			type: "POST",
			url: util.constants.host + "/openidm/managed/user/?_action=patch&_query-id=for-username&uid=" + oldUserData.email,
			dataType: "json",
			contentType: 'application/json; charset=utf-8',
			data: dataString,
			success: successCallback,
			error: errorCallback
		});
	}
	

	obj.getDifferences = function(oldObject, newObject) {
		var result = new Array();
		var fieldContents;
		for ( var field in newObject) {
			fieldContents = newObject[field];
			if ( typeof (fieldContents) != "function") {
				var newValue = newObject[field];
				var oldValue = oldObject[field];
				if((newValue!="" || oldValue) && newValue != oldValue){
					result.push([field,newValue,oldValue]);
				}
			}
		}
		return result;
	}
	
	obj.buildDataString = function(differences){
		var oneChangeTemplete = '{"replace":"/field","value":"newValue"}';
		var dataString="[";
		for (var i = 0; i < differences.length; i++) {
			if(i>0){
				dataString+=','
			}
			diff = differences[i];
			 var temp = oneChangeTemplete.replace('field', diff[0]);
			temp = temp.replace('newValue', diff[1]);
			dataString+=temp;
		}
		dataString+="]";
		return dataString;
	},

	obj.getAnswer = function(id, successCallback, errorCallback) {
		console.info("getting answer of user[id=" + id + "]");
		$.ajax({
			url: "js/app/tmp/secquestions.json",
			dataType: 'json',
			success: successCallback,
			error: errorCallback
		});
	};
	
	return obj;
});



