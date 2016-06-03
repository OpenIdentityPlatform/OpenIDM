exports.command = function (callback) {
    console.log("Deleting LDAP connector...");

    this
    .timeoutsAsyncScript(1000)
    .executeAsync(function(prov, done) {
        // create LDAP connector
        window.$.ajax({
            method: 'DELETE',
            url: '/openidm/config/provisioner.openicf/ldap',
            beforeSend: function(xhr) {
                xhr.setRequestHeader("Content-Type","application/json");
                xhr.setRequestHeader("Accept","application/json");
                xhr.setRequestHeader("Cache-Control","no-cache");
            }
        });
        done(true);
    }, ['test'], callback);
};
