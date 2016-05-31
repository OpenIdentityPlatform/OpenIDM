var request = require("request"),
    _ = require("lodash");

module.exports = {
    baseUrl: "http://localhost:8080/openidm/config",
    headers: {
        "Content-Type": "application/json",
        "X-OpenIDM-Username": "openidm-admin",
        "X-OpenIDM-Password": "openidm-admin"
    },
    configCache: [],
    log: function(msg) {
        console.log("\x1b[34m", String.fromCharCode(parseInt('279f',16)) + ' ' + msg);
    },

    /**
     * Utility method used by other helper methods that makes the rest calls
     * and returns a promise.
     * @param {String} Method - The request method you want to send.
     * @param {String} Config Id - The id of the config you want to call.
     * @param {Object} Body - Payload you want to send to the server.
     * @returns {Promise}
     */
    makeConfigRequest: function(method, configId, body) {
        var options = {
            url: [this.baseUrl, configId].join("/"),
            headers: this.headers,
            method: method
        };
        if (body) {
            if (typeof body === "string") {
                options.body = body;
            } else {
                options.body = JSON.stringify(body);
            }
        }
        return new Promise(function(resolve, reject) {
            request(options, function(err, resp) {
                resolve(resp);
                if (err) { reject(err); }
            });
        });
    },

    /**
     * Reads the specified config and stores a copy of it in the config cache.
     * Will only add one version of config at a time.
     * @param {String} Config Id - The id of the config you want to read.
     * @param {Function} callback - What you want to do once the promise resolves.
     * @returns {String|Callback} - Will either return the config or invoke callback with config as arg
     */
    read: function(configId, callback) {
        this.log("Reading " + configId + " configuraton...");
        var req = this.makeConfigRequest("GET", configId);
        return req
            .then(function(response) {
                // add config to the cache if one doesn't already exist
                if (!_.find(this.configCache, {configId: configId})) {
                    this.configCache.push({configId: configId, config: response.body});
                }
                return response.body;
            }.bind(this))
            .then(function(body) {
                if (callback) { return callback(body); }
                return body;
            });
    },

    /**
     * Updates the specified config
     * @param {String} Config Id - The id of the config you want to update.
     * @param {Object} Body - Payload you want to send to the server.
     * @param {Function} callback - What you want to do once the promise resolves.
     * @param {Boolean} - Read first? - Specify whether you want to read/save a copy of the config prior to update
     * @returns {String|Callback} - Will either return the config or invoke callback with config as arg
     */
    update: function(configId, newConfig, callback, readFirst) {
        var _update = function(configId, newConfig, callback) {
            var req = this.makeConfigRequest("PUT", configId, newConfig);
            return req.then(
                function(resp) {
                    if (callback) { return callback(resp.body); }
                    return resp.body;
                },
                function(err) {
                    throw err;
                }
            );
        }.bind(this);

        if (readFirst) {
            return this.read(configId, function() {
                _update(configId, newConfig, callback);
            });
        } else {
            return _update(configId, newConfig, callback);
        }

    },

    /**
     * Searches config cache for specified config and sends that version to the server
     * @param {String} Config Id - The id of the config you want to reset.
     * @param {Function} callback - What you want to do once the promise resolves.
     * @returns {String|Callback} - Will either return the config or invoke callback with config as arg
     */
    reset: function(configId, callback) {
        var configCache = this.configCache,
            cachedConfig = _.find(configCache, {configId: configId});
        if (cachedConfig) {
            return this.makeConfigRequest("PUT", configId, cachedConfig.config)
                .then(
                    function(response) {
                        this.log(configId + " has been successfully reset");

                        //after resetting the config on the server remove it from the cache
                        this.configCache = _.reject(configCache, {configId: configId});
                        if (callback) { return callback(response.body); }
                        return response.body;

                    }.bind(this),
                    function(error) {
                        throw error;
                    }
            );
        } else {
            if (callback) { callback(false); }
            return false;
        }
    },

    /**
     * Iterates over config cache and resets each one in the collection
     * @param {Function} callback - What you want to do once the promise resolves.
     * @returns {Array|Callback} - Will either return array of reset configs or invoke callback with array of configs as arg
     */
    resetAll: function(callback) {
        var configCache = this.configCache;//, keys, resets;
        Promise.all(
            configCache.map(function(config) {
                return this.reset(config.configId);
            }, this)
        )
        .then(
            function(responses) {
                if (callback) { return callback(responses); }
                return responses;
            },
            function(error) { throw error; }
        );
    }
};
