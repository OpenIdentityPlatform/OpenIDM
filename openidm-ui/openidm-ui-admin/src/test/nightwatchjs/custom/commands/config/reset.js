/*
 * @param configId {string}
 * @param callback {function}
 *
 * example of how to use this command in tests:
 *
 * client.config.reset("audit", function() {
 *     whateverYouWantToHappenAfterTheConfigFileIsReturnedToItsOriginalState();
 * });
 *
 */
exports.command = function (configId, callback) {
    var _ = require('lodash');

    if (this.globals.configCache && this.globals.configCache[configId]) {
        this.config.update(configId, this.globals.configCache[configId], _.bind(function () {
            console.log(configId + " has been successfully reset...");

            //after resetting the config on the server remove it from the cache
            delete this.globals.configCache[configId];

            if (callback) {
                callback();
            }

            return this;

        }, this));
    } else {

        if (callback) {
            callback();
        }
        
        return this;
    }
};
