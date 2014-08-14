// Copyright 2011 Mark Cavage, Inc.  All rights reserved.
define(function (require) {
var module = {};

var util = require('./util');
var escape = require('./escape').escape;

var Filter = require('./filter');

var Protocol = require('./protocol');



///--- API

function LessThanEqualsFilter(options) {
  if (typeof (options) === 'object') {
    if (typeof (options.attribute) !== 'string')
      throw new TypeError('options.attribute (string) required');
    if (typeof (options.value) !== 'string')
      throw new TypeError('options.value (string) required');
    this.attribute = options.attribute;
    this.value = options.value;
  } else {
    options = {};
  }

  options.type = Protocol.FILTER_LE;
  Filter.call(this, options);

  var self = this;
  Object.defineProperty(this, 'json', { get: function () {
      return {
        type: 'LessThanEqualsMatch',
        attribute: self.attribute || undefined,
        value: self.value || undefined
      };
    }
  });    
}
util.inherits(LessThanEqualsFilter, Filter);
module.exports = LessThanEqualsFilter;


LessThanEqualsFilter.prototype.toString = function () {
  return '(' + escape(this.attribute) + '<=' + escape(this.value) + ')';
};


LessThanEqualsFilter.prototype.matches = function (target) {
  if (typeof (target) !== 'object')
    throw new TypeError('target (object) required');

  if (target.hasOwnProperty(this.attribute)) {
    var value = this.value;
    return Filter.multi_test(
      function (v) { return value >= v; },
      target[this.attribute]);
  }

  return false;
};


LessThanEqualsFilter.prototype.parse = function (ber) {

  this.attribute = ber.readString().toLowerCase();
  this.value = ber.readString();

  return true;
};


LessThanEqualsFilter.prototype._toBer = function (ber) {

  ber.writeString(this.attribute);
  ber.writeString(this.value);

  return ber;
};

return module.exports;
});