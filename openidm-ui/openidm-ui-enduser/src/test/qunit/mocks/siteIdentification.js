/*global require, define*/
define([
    "text!templates/common/MediumBaseTemplate.html"
], function () {

    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "templates/common/MediumBaseTemplate.html"
        ],
        deps = arguments;

    return function (server) {

        _.each(staticFiles, function (file, i) {
            server.respondWith(
                "GET",
                new RegExp(file.replace(/([\/\.\-])/g, "\\$1") + "$"),
                [
                    200,
                    { },
                    deps[i]
                ]
            );
        });

        server.respondWith(
            "GET",   
            "/openidm/endpoint/siteIdentification?_queryId=siteIdentification&login=openidm-admin",
            [
                200, 
                { },
                "{\"result\":[{\"siteImage\":\"images/passphrase/report.png\",\"passPhrase\":\"human\"}],\"resultCount\":1,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}"
            ]
        );

    };

});
