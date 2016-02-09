/*
 * @param callback {function}
 * 
 * example of how to use this command in tests:
 * 
 * client.config.resetAll(function() {
 *     whateverYouWantToHappenAfterAllTheConfigFilesAreReturnedToTheirOriginalState();
 * });
 * 
 */
exports.command = function (callback) {
    var _ = require("lodash"),
        promCount = 0,
        configCount;
    
    if (this.globals.configCache) {
        configCount = _.keys(this.globals.configCache).length;
        _.each(this.globals.configCache, _.bind(function (val,key) {
            promCount++;
            this.config.reset(key, _.bind(function () {
                if (promCount === configCount) {
                    callback();
                }
            }, this));
        }, this));
    } else {
        callback();
    }
    
    return this;
}