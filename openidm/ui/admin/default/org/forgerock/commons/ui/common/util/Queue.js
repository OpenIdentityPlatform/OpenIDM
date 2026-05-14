"use strict";

/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

define(["lodash"], function (_) {
  /**
   * Provides a generic, sharable queue (FIFO) mechanism.
   * @exports org/forgerock/commons/ui/common/util/Queue
   */

  /**
   * Constructor. Takes an optional array for initializing the queue with values
   *
   * @param {array} initialValues - optional array of values to start queuing.
   */
  var obj = function obj(initialValues) {
    this._values = _.isArray(initialValues) ? initialValues : [];
    return this;
  };

  /**
   * Put a new item in the queue
   *
   * @param {Object} value - any arbitrary value to insert into the queue
   */
  obj.prototype.add = function (value) {
    this._values.push(value);
  };

  /**
   * Remove and return the head of the queue
   *
   * @returns {Object} whatever is on the head of the queue, or undefined if nothing is available
   */
  obj.prototype.remove = function () {
    return this._values.shift(1);
  };

  /**
   * Return the head of the queue without removing it
   *
   * @param {string} queueName - name of queue to remove from
   * @returns {Object} whatever is on the head of the queue, or undefined if nothing is available
   */
  obj.prototype.peek = function () {
    return this._values[0];
  };

  return obj;
});
