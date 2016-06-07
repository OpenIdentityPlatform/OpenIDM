define([
    "org/forgerock/openidm/ui/admin/selfservice/PasswordResetConfigView"
], function (PasswordResetConfigView) {
    QUnit.module('PasswordResetConfigView Tests');

    QUnit.test('Properties list filtered properly', function() {
        var props = ['password', 'notpasssword'],
            type = "resetStage",
            details = {
                password : {
                    encryption: {

                    }
                },
                notpassword : {
                    notencryption: {

                    }
                }
            },
            tempProps;

        tempProps = PasswordResetConfigView.filterPropertiesList(props, type, details);

        QUnit.equal(tempProps.length, 1, "Non-password properties are filtered out");
    });
});