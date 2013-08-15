/*global require, define, module, $, QUnit, window*/
define([
    "org/forgerock/commons/ui/user/LoginView",
    "sinon",
    "mocks/lessRequests",
    "mocks/openidm/invalidLoginCredentials"
], function (
    LoginView,
    sinon,
    lessRequests,
    invalidLoginCredentials) {

        return {
            executeAll: function (server) {

                module("Login Tests",{
                    setup: function(){
                        lessRequests(server);
                    },
                    teardown: function(){
                        delete window.location.hash;
                    }
                });

                QUnit.test("Invalid Login Credentials", function () {

                    invalidLoginCredentials(server);

                    LoginView.render();

                    server.respond();
                    
                    QUnit.ok($('[name="login"]').length, "Login Input Displayed");
                    
                    $("[name=login]").val("foo").trigger("keyup");
                    
                    $("[name=password]").val("bar").trigger("keyup");

                    $(':submit').trigger("click");

                    server.respond();

                    QUnit.ok($(".errorMessage:contains('Login/password combination is invalid')").length === 1, "Invalid Credentials");


                });

            }
        };

});