// Copyright 2011 Mark Cavage, Inc.  All rights reserved.
define(function (require) {
var module = {};

var util = require('./util');
var escape = require('./escape').escape;

var Filter = require('./filter');

var Protocol = require('./protocol');



///--- API

function EqualityFilter(options) {
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
  options.type = Protocol.FILTER_EQUALITY;
  Filter.call(this, options);

  var self = this;
  Object.defineProperty(this, 'json', { get: function () {
      return {
        type: 'EqualityMatch',
        attribute: self.attribute || undefined,
        value: self.value || undefined
      };
    }
  });    
}
util.inherits(EqualityFilter, Filter);
module.exports = EqualityFilter;


EqualityFilter.prototype.toString = function () {
  return '(' + escape(this.attribute) + '=' + escape(this.value) + ')';
};


EqualityFilter.prototype.matches = function (target) {
  if (typeof (target) !== 'object')
    throw new TypeError('target (object) required');

  var self = this;

  if (target.hasOwnProperty(this.attribute)) {
    var value = this.value;
    return Filter.multi_test(
      function (v) {
        if (self.attribute === 'objectclass')
          v = v.toLowerCase();
        return value === v;
      },
      target[this.attribute]);
  }

  return false;
};


EqualityFilter.prototype.parse = function (ber) {

  this.attribute = ber.readString().toLowerCase();
  this.value = ber.readString();

  if (this.attribute === 'objectclass')
    this.value = this.value.toLowerCase();

  return true;
};


EqualityFilter.prototype._toBer = function (ber) {

  ber.writeString(this.attribute);
  ber.writeString(this.value);

  return ber;
};

return module.exports;
});