// Copyright 2011 Mark Cavage, Inc.  All rights reserved.
define(function (require) {
var module = {};

var util = require('./util');
var escape = require('./escape').escape;

var Filter = require('./filter');

var Protocol = require('./protocol');



///--- API

function GreaterThanEqualsFilter(options) {
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

  options.type = Protocol.FILTER_GE;
  Filter.call(this, options);

  var self = this;
  Object.defineProperty(this, 'json', { get: function () {
      return {
        type: 'GreaterThanEqualsMatch',
        attribute: self.attribute || undefined,
        value: self.value || undefined
      };
    }
  });    
}
util.inherits(GreaterThanEqualsFilter, Filter);
module.exports = GreaterThanEqualsFilter;


GreaterThanEqualsFilter.prototype.toString = function () {
  return '(' + escape(this.attribute) + '>=' + escape(this.value) + ')';
};


GreaterThanEqualsFilter.prototype.matches = function (target) {
  if (typeof (target) !== 'object')
    throw new TypeError('target (object) required');

  if (target.hasOwnProperty(this.attribute)) {
    var value = this.value;
    return Filter.multi_test(
      function (v) { return value <= v; },
      target[this.attribute]);
  }

  return false;
};


GreaterThanEqualsFilter.prototype.parse = function (ber) {

  this.attribute = ber.readString().toLowerCase();
  this.value = ber.readString();

  return true;
};


GreaterThanEqualsFilter.prototype._toBer = function (ber) {

  ber.writeString(this.attribute);
  ber.writeString(this.value);

  return ber;
};

return module.exports;
});