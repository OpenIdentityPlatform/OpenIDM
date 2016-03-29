module.exports = {
        baseUrl: "http://localhost:8080/",
        adminUser: {
            userName: "openidm-admin",
            password: "openidm-admin"
        },
        nonAdminUser: {
            userName: "bjensen",
            password: "Passw0rd",
            givenName: "Barbara",
            sn: "Jensen",
            mail: "bjensen@example.com"
        },
        // a useful method so you can quickly load a page in the same domain without
        // having to load the full UI.
        loadEmptyPage: function (client) {
            client.url(function (response) {
                if (response.value.indexOf(client.globals.baseUrl) === -1) {
                    return client.url(client.globals.baseUrl + "templates/common/EmptyTemplate.html");
                }
            });
            return client;
        },
        createUser: function (client, callback) {
            return client.globals.loadEmptyPage(client)
                .timeoutsAsyncScript(1000)
                .executeAsync(function (args, done) {
                    var makeRequest = function () {
                        $.ajax({
                            "type": "PUT",
                            "headers" : {
                                "X-OpenIDM-Username": args.admin.userName,
                                "X-OpenIDM-Password": args.admin.password,
                                "X-OpenIDM-NoSession": "true",
                                "Content-type": "application/json"
                            },
                            "url": args.baseUrl + "openidm/managed/user/" + args.user.userName,
                            "data": JSON.stringify(args.user)
                        }).then(done);
                    }
                    if (typeof $ === "undefined" || typeof $.fn === "undefined") {
                        var scr = document.createElement('script');
                            scr.setAttribute('src', args.baseUrl + 'libs/jquery-2.1.1-min.js');
                            scr.type = 'text/javascript';
                            scr.onload = makeRequest;
                            document.body.appendChild(scr);
                    } else {
                        makeRequest();
                    }
                },
                [{
                    admin: client.globals.adminUser,
                    user: client.globals.nonAdminUser,
                    baseUrl: client.globals.baseUrl
                }],
                callback
            );
        },
        deleteUser: function (client, callback) {
            return client.executeAsync(
                function (args, done) {
                    var makeRequest = function () {
                        $.ajax({
                            "type": "DELETE",
                            "headers" : {
                                "X-OpenIDM-Username": args.admin.userName,
                                "X-OpenIDM-Password": args.admin.password,
                                "X-OpenIDM-NoSession": "true",
                                "Content-type": "application/json",
                                "If-Match": "*"
                            },
                            "url": args.baseUrl + "openidm/managed/user/" + args.user.userName
                        }).then(done);
                    };

                    if (typeof $ === "undefined" || typeof $.fn === "undefined") {
                        var scr = document.createElement('script');
                            scr.setAttribute('src', args.baseUrl + 'libs/jquery-2.1.1-min.js');
                            scr.type = 'text/javascript';
                            scr.onload = makeRequest;
                            document.body.appendChild(scr);
                    } else {
                        makeRequest();
                    }
                },
                [{
                    admin: client.globals.adminUser,
                    user: client.globals.nonAdminUser,
                    baseUrl: client.globals.baseUrl
                }],
                callback
            );
        },
        // gets a session-jwt cookie without having to go through the normal UI login process
        setSession: function (client, credentials, callback) {
            return client.globals.loadEmptyPage(client)
                .timeoutsAsyncScript(1000)
                .executeAsync(
                    function (args, done) {
                        var makeRequest = function () {
                            $.ajax({
                                "type": "GET",
                                "headers" : {
                                    "X-OpenIDM-Username": args.credentials.userName,
                                    "X-OpenIDM-Password": args.credentials.password
                                },
                                "url": args.baseUrl + "openidm/info/login"
                            }).then(done);
                        }

                        if (typeof $ === "undefined") {
                            var scr = document.createElement('script');
                                scr.setAttribute('src', args.baseUrl + 'libs/jquery-2.1.1-min.js');
                                scr.type = 'text/javascript';
                                scr.onload = makeRequest;
                                document.body.appendChild(scr);
                        } else {
                            makeRequest();
                        }
                    },
                    [{
                        credentials: credentials,
                        baseUrl: client.globals.baseUrl
                    }],
                    callback
                );
        },
        endSession: function (client, callback) {
            client.deleteCookie("session-jwt", callback);
        }
};
