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
 * Copyright 2015-2016 ForgeRock AS.
 */

define(["jquery", "underscore", "backbone", "backbone.paginator", "org/forgerock/commons/ui/common/main/AbstractModel", "org/forgerock/commons/ui/common/main/ServiceInvoker", "org/forgerock/commons/ui/common/components/Messages"], function ($, _, Backbone, BackbonePaginator, AbstractModel, ServiceInvoker, Messages) {
    /**
     * @exports org/forgerock/commons/ui/common/main/AbstractCollection
     *
     * Extending PageableCollection to support CREST-specific paging details
     *
     */
    return BackbonePaginator.extend({
        model: AbstractModel,
        /**
           this setPageSize overrides the default implementation in BackbonePaginator so that it
           can be called without necessarily triggering a .fetch()
           @param {number} pageSize - number of records to fetch, per page
           @param {object} options - any options necessary to provide
           @param {boolean} options.fetch - if false, will suppress the fetch call and instead merely update the state
        */
        setPageSize: function setPageSize(pageSize, options) {
            if (_.isObject(options) && options.fetch === false) {
                this.state.pageSize = pageSize;
            } else {
                return BackbonePaginator.prototype.setPageSize.call(this, pageSize, options);
            }
        },
        /**
            The only two type values supported here are offset and cookie. If anything else is
            passed in, it will default to offset.
        */
        setPagingType: function setPagingType(type) {
            this.state.pagingType = type === "offset" || type === "cookie" ? type : "offset";
            return this;
        },
        getPagingType: function getPagingType() {
            return this.state.pagingType || "offset";
        },

        /**
            The three policy values supported here are ESTIMATE, NONE and EXACT. If anything else is
            passed in, it will default to NONE
        */
        setTotalPagedResultsPolicy: function setTotalPagedResultsPolicy(policy) {
            this.state.totalPagedResultsPolicy = _.indexOf(["ESTIMATE", "NONE", "EXACT"], policy) !== -1 ? policy : "NONE";
            return this;
        },

        setSorting: function setSorting(sortKey, order) {
            if (order === 1) {
                return BackbonePaginator.prototype.setSorting.call(this, "-" + sortKey, order);
            } else {
                return BackbonePaginator.prototype.setSorting.call(this, sortKey, order);
            }
        },

        hasPrevious: function hasPrevious() {
            return this.getPagingType() === "offset" && this.state.currentPage >= 1;
        },
        hasNext: function hasNext() {
            return this.getPagingType() === "cookie" && this.state.pagedResultsCookie !== null ||
            // when we don't have a total, assume there are more results
            this.getPagingType() === "offset" && this.state.totalRecords === null || this.getPagingType() === "offset" && this.state.totalRecords >= (this.state.currentPage + 1) * this.state.pageSize;
        },
        sync: function sync(method, collection, options) {
            if (method === "read") {
                // BackbonePaginator seems to insist that this field be included anytime sorting is performed.
                delete options.data.order;
                options.processData = false;

                options.error = function (response) {
                    Messages.addMessage({
                        type: Messages.TYPE_DANGER,
                        response: response
                    });
                };

                if (_.isObject(options.data)) {
                    // read calls will be GET requests, so any "data" sent will be URL parameters
                    options.data = $.param(options.data);
                }

                return ServiceInvoker.restCall(options);
            } else {
                return BackbonePaginator.prototype.sync.apply(this, arguments);
            }
        },
        getFirstPage: function getFirstPage() {
            this.state.pagedResultsCookie = null;
            this.state.pagedResultsOffset = 0;
            this.state.currentPage = 0;
            return BackbonePaginator.prototype.getFirstPage.apply(this, arguments);
        },
        getLastPage: function getLastPage() {
            if (this.getPagingType() === "offset" && this.state.totalRecords !== null && this.state.totalRecords > 0) {
                this.state.pagedResultsCookie = null;
                this.state.currentPage = Math.floor((this.state.totalRecords - 1) / this.state.pageSize);
                this.state.pagedResultsOffset = this.state.currentPage * this.state.pageSize;
                return BackbonePaginator.prototype.getLastPage.apply(this, arguments);
            } else {
                // nothing else we can really do here, so fail over to getting the first page
                return this.getFirstPage();
            }
        },
        getNextPage: function getNextPage() {
            if (this.getPagingType() === "cookie") {
                if (this.state.pagedResultsCookie === null) {
                    return this.getFirstPage();
                }
                this.state.pagedResultsOffset = null;
            } else {
                this.state.pagedResultsCookie = null;
                this.state.pagedResultsOffset = (this.state.currentPage + 1) * this.state.pageSize;
            }
            return BackbonePaginator.prototype.getNextPage.apply(this, arguments);
        },
        getPreviousPage: function getPreviousPage() {
            if (!this.hasPrevious()) {
                return this.getFirstPage();
            }
            // this only works with offset-based paging
            this.state.pagedResultsCookie = null;
            this.state.pagedResultsOffset = (this.state.currentPage - 1) * this.state.pageSize;
            return BackbonePaginator.prototype.getPreviousPage.apply(this, arguments);
        },
        getPage: function getPage(pageNumber) {
            if (_.isFinite(pageNumber)) {
                this.state.currentPage = pageNumber;
                // jumping to a specific page is only possible with offset
                this.state.pagedResultsOffset = this.state.pageSize * pageNumber;
            }
            return BackbonePaginator.prototype.getPage.apply(this, arguments);
        },
        parseState: function parseState(resp) {
            if (this.getPagingType() === "cookie" && resp.pagedResultsCookie !== null) {
                this.state.pagedResultsCookie = resp.pagedResultsCookie;
            } else {
                this.state.pagedResultsCookie = null;
            }

            // totalPagedResults may not be defined in the response, depending on the policy
            this.state.totalRecords = _.isFinite(resp.totalPagedResults) && resp.totalPagedResults > -1 ? resp.totalPagedResults : null;

            if (!this.state.totalPages && this.state.totalRecords) {
                this.state.totalPages = Math.ceil(this.state.totalRecords / this.state.pageSize);
            } else {
                this.state.totalPages = null;
            }
        },
        parseRecords: function parseRecords(resp) {
            return resp.result;
        },
        state: {
            pagedResultsCookie: null,
            pagedResultsOffset: 0,
            firstPage: 0,
            pageSize: 10,
            pagingType: "offset",
            totalPagedResultsPolicy: "NONE"
        },
        queryParams: {
            currentPage: null,
            totalPages: null,
            totalRecords: null,
            _totalPagedResultsPolicy: function _totalPagedResultsPolicy() {
                if (this.state.totalPagedResultsPolicy === "NONE") {
                    return null;
                }
                return this.state.totalPagedResultsPolicy;
            },
            _pagedResultsOffset: function _pagedResultsOffset() {
                if (this.state.pagedResultsOffset === 0 || !_.isFinite(this.state.pagedResultsOffset)) {
                    return null;
                }
                return this.state.pagedResultsOffset;
            },
            _pagedResultsCookie: function _pagedResultsCookie() {
                return this.state.pagedResultsCookie;
            },
            pageSize: "_pageSize",
            sortKey: "_sortKeys"
        }
    });
});
