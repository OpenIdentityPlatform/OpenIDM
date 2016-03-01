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
    var _ = require("lodash");

    if (this.globals.configCache) {
        Promise.all(
            _.map(_.keys(this.globals.configCache), _.bind(function (key) {
                return this.config.reset(key);
            }, this))
        )
        .then(function() {

            if (callback) {
                callback();
            }

            return this;
        });
    } else {

        if (callback) {
            callback();
        }
        
        return this;
    }
};
