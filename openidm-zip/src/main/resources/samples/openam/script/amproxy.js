
/*global _, JSON */

load("samples/openam/script/libs/json2.js");
load("samples/openam/script/libs/underscore.js");

java.lang.System.out.println(request);

var openamServer = "jake.forgerock.com:8081",
    passThroughURL = request.id.replace(/^amproxy/, ''),
    map = {
      "_url" : "http://" + openamServer + passThroughURL,
      "_method" : request.parent.method,
      "_body": JSON.stringify(request.value),
      "_content-type": "application/json"
    },
    urlArgs = [],
    newHeaders = _.clone(request.parent.headers);

_.each(request.params, function (value, key) {
    urlArgs.push(encodeURIComponent(key) + "=" + encodeURIComponent(value));
});
newHeaders.Host = openamServer;
map._url += "?" + urlArgs.join("&");
map._headers = newHeaders;
    
java.lang.System.out.println(JSON.stringify(map));

openidm.action("external/rest", map);


//(function () { return request; }());