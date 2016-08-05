define([
    "jquery",
    "sinon",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/ServiceInvoker",
    "org/forgerock/openidm/ui/common/UserModel"
], function ($, sinon, Configuration, Constants, ServiceInvoker, UserModel) {
    QUnit.module('UserModel Functions');

    QUnit.test("User Model reflects appropriate policy after subsequent login (OPENIDM-5154)", function () {
        var headers = {};

        Configuration.globalData = {roles: {}};
        // stub the rest calls invoked by the UserModel to use these simple responses
        sinon.stub(ServiceInvoker, "restCall", function (options) {
            options.headers = options.headers || {};
            return $.Deferred().resolve({
                authenticationId: Constants.HEADER_PARAM_USERNAME,
                authorization: {
                    id: options.headers[Constants.HEADER_PARAM_USERNAME],
                    component: "managed/user",
                    roles: ["openidm-authorized"],
                    // openidm-admin doesn't have any protectedAttributeList values; others do
                    protectedAttributeList: options.headers[Constants.HEADER_PARAM_USERNAME] === "openidm-admin" ? [] : ["password"]
                }
            });
        });

        headers[Constants.HEADER_PARAM_USERNAME] = "openidm-admin";
        UserModel.getProfile(headers).then(function () {
            QUnit.equal(UserModel.getProtectedAttributes().length, 0, "No protected attributes for openidm-admin");
        }).then(function () {
            headers[Constants.HEADER_PARAM_USERNAME] = "bjensen";
            return UserModel.getProfile(headers);
        }).then(function () {
            QUnit.equal(UserModel.getProtectedAttributes()[0], "password", "Password is a protected attribute for bjensen");
        }).then(function () {
            ServiceInvoker.restCall.restore();
        });

    });
});
