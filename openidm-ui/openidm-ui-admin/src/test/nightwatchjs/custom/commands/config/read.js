/*
 * @param configId {string}
 * @param callback {function}
 *
 * example of how to use this command in tests:
 *
 * client.config.read("audit" , function(config) {
 *     var theConfigIJustRead = config;
 *
 *     whateverYouWantToDoAfterTheConfigurationHasBeenRead();
 * });
 *
 */
exports.command = function (configId, callback) {
    console.log("Reading " + configId + " configuraton...");

    this
        .timeoutsAsyncScript(2000)
        .executeAsync(
            function (args, done) {
                var configDelegate = require("org/forgerock/openidm/ui/common/delegates/ConfigDelegate");

                //this may look weird...passing the "args" array into readEntity but
                //for some reason args is actually what args[0] should be
                configDelegate.readEntity(args).then( function (result) {
                    done(result);
                });
            },
            /*
             * NOTE: the args variable in the previous function disregards the outer array
             * if configId = "audit" and you pass in [configId] then args = "audit" so args[0] = "a"
             */
            [configId],
            function (result) {
                var _ = require('lodash');
                
                //check for existence of configCache in globals
                if (!this.globals.configCache) {
                    this.globals.configCache = {};
                }

                //check for existence of configCache for this configId
                //if not there set it...this is the original version to be used when reset is called
                if (!this.globals.configCache[configId]) {
                    this.globals.configCache[configId] = _.cloneDeep(result.value);
                }

                if (callback) {
                    callback(result.value);
                }

                return this;
            }
        );
};
