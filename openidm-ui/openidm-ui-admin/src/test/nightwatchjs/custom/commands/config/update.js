/*
 * @param configId {string}
 * @param config {object}
 * @param callback {function}
 *
 * example of how to use this command in tests:
 *
 * client.config.update("audit", theNewConfigObjToBeSaved , function() {
 *     whateverYouWantToHappenAfterTheConfigFileIsUpdated();
 * });
 *
 */
exports.command = function (configId, config, callback) {
    this
        .timeoutsAsyncScript(2000)
        .executeAsync(
            function (args, done) {
                var id = args[0],
                    configObj = args[1],
                    configDelegate = require("org/forgerock/openidm/ui/common/delegates/ConfigDelegate");

                configDelegate.updateEntity(id, configObj).then( function (result) {
                    done(result);
                });
            },
            /*
             * the second argument for executeAsync must be an array of arrays
             * because the args variable in the previous function disregards the outer array
             * example: if configId = "audit" and you pass in [configId, config] then args = "audit" so args[0] = "a"
             */
            [[configId, config]],
            function (result) {
                if (callback) {
                    callback(result.value);
                }
                return this;
            }
        );
};
