// Copyright 2011 Mark Cavage, Inc.  All rights reserved.
define(function (require) {
var module = {};

var util = require('./util');
var escape = require('./escape').escape;

var Filter = require('./filter');

var Protocol = require('./protocol');



///--- API

function ApproximateFilter(options) {
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
  options.type = Protocol.FILTER_APPROX;
  Filter.call(this, options);

  var self = this;
  Object.defineProperty(this, 'json', { get: function () {
      return {
      type: 'ApproximateMatch',
      attribute: self.attribute || undefined,
      value: self.value || undefined
      };
    }
  });  
}
util.inherits(ApproximateFilter, Filter);
module.exports = ApproximateFilter;


ApproximateFilter.prototype.toString = function () {
  return '(' + escape(this.attribute) + '~=' + escape(this.value) + ')';
};


ApproximateFilter.prototype.matches = function (target) {
  if (typeof (target) !== 'object')
    throw new TypeError('target (object) required');

  var matches = false;
  if (target.hasOwnProperty(this.attribute)) {
    var tv = target[this.attribute];
    if (Array.isArray(tv)) {
      matches = (tv.indexOf(this.value) != -1);
    } else {
      matches = (this.value === target[this.attribute]);
    }
  }

  return matches;
};


ApproximateFilter.prototype.parse = function (ber) {

  this.attribute = ber.readString().toLowerCase();
  this.value = ber.readString();

  return true;
};


ApproximateFilter.prototype._toBer = function (ber) {

  ber.writeString(this.attribute);
  ber.writeString(this.value);

  return ber;
};

return module.exports;
});
