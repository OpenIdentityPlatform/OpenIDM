// Copyright 2011 Mark Cavage, Inc.  All rights reserved.
define(function (require) {
var module = {};

var util = require('./util');

var Filter = require('./filter');

var Protocol = require('./protocol');

///--- API

function NotFilter(options) {
  if (typeof (options) === 'object') {
    if (!options.filter || !(options.filter instanceof Filter))
      throw new TypeError('options.filter (Filter) required');

  } else {
    options = {};
  }

  this.filter = options.filter || {};
  options.type = Protocol.FILTER_NOT;
  Filter.call(this, options);

  var self = this;
  Object.defineProperty(this, 'json', { get: function () {
      return {
        type: 'Not',
        filter: self.filter
      };
    }
  }); 
}
util.inherits(NotFilter, Filter);

module.exports = NotFilter;


NotFilter.prototype.addFilter = function (f) {
  if (!(f instanceof Filter))
    throw new TypeError('filter (Filter) required');
  this.filter = f;
};

NotFilter.prototype.toString = function () {
  return '(!' + this.filter.toString() + ')';
};


NotFilter.prototype.matches = function (target) {
  return !this.filter.matches(target);
};


NotFilter.prototype._toBer = function (ber) {

  return this.filter.toBer(ber);
};

return module.exports;
});