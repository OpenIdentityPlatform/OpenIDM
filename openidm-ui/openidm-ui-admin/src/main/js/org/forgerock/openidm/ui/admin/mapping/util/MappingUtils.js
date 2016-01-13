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
 * Copyright 2011-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/openidm/ui/common/delegates/SearchDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "selectize",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate"
], function($, _,
             Handlebars,
             searchDelegate,
             configDelegate,
             resourceDelegate,
             UIUtils,
             router,
             constants,
             selectize,
             AdminUtils,
             schedulerDelegate) {

    var obj = {};

    obj.buildObjectRepresentation = function(objToRep, props){
        var propVals = [];

        _.each(props, _.bind(function(prop, i){
            var objRepEl = $("<span>"),
                wrapper = $("<div>");
            if(objToRep[prop]){
                objRepEl.text(Handlebars.Utils.escapeExpression(objToRep[prop])).attr("title", prop);
            }
            if(i === 0){
                objRepEl.addClass("objectRepresentationHeader");
            } else {
                objRepEl.addClass("objectRepresentation");
            }
            wrapper.append(objRepEl);
            propVals.push(wrapper.html());
        }, this));

        return propVals.join("<br/>");
    };

    obj.setupSampleSearch = function(el, mapping, autocompleteProps, selectSuccessCallback){
        var searchList,
            selectedItem;

        el.selectize({
            valueField: autocompleteProps[0],
            searchField: autocompleteProps,
            maxOptions: 10,
            create: false,
            onChange: function() {
                selectSuccessCallback(selectedItem);
            },
            render: {
                option: function(item, selectizeEscape) {
                    var fields = _.pick(item, autocompleteProps),
                        element = $('<div class="fr-search-option"></div>'),
                        counter = 0;

                    _.forIn(fields, function(value, key) {
                        if(counter === 0) {
                            $(element).append('<div class="fr-search-primary">' +selectizeEscape(value) +'</div>');
                        } else {
                            $(element).append('<div class="fr-search-secondary text-muted">' +selectizeEscape(value) +'</div>');
                        }

                        counter++;
                    }, this);

                    return element.prop('outerHTML');
                },
                item: function(item, escape) {
                    selectedItem = item;

                    return "<div>" +escape(item[autocompleteProps[0]]) +"</div>";
                }
            },
            load: function(query, callback) {
                if (!query.length || query.length < 2 || !autocompleteProps.length) {
                    return callback();
                }

                searchDelegate.searchResults(mapping.source, autocompleteProps, query).then(function(response) {
                    if(response) {
                        searchList = response;
                        callback([response]);
                    } else {
                        searchList = [];

                        callback();
                    }
                });
            }
        });
    };

    obj.readOnlySituationalPolicy = function(policies){
        return _.reduce(policies, function(memo, val){
            return memo && val.action === "ASYNC";
        }, true);
    };
    /**
    * This function gathers child info about a mapping then pops up a confirmDialog
    * with a list of any associated managed/assignments or scheduledTasks to be
    * deleted if the process is continued.
    *
    * @param mappingName {string}
    * @param syncMappings {object} - the current state of sync.json mappings property
    * @param successCallback {function} - action to take after the delete process has been completed
    */
    obj.confirmDeleteMapping = function(mappingName, syncConfig, successCallback) {
        obj.getMappingChildren(mappingName).then(_.bind(function (mappingChildren) {
            var dialogText = $("<div>").append($.t("templates.mapping.confirmDeleteMapping", {"mappingName": mappingName, "mappingChildren": mappingChildren.display }));

            AdminUtils.confirmDeleteDialog(dialogText, _.bind(function(){
                obj.deleteMapping(mappingName, mappingChildren, syncConfig).then(() => {
                    successCallback();
                });
            }, this));
        }, this));
    };
    /**
    * This function deletes a mapping and it's child data
    *
    * @param mappingName {string}
    * @param mappingChildren {array}
    * @param syncConfigMappings {object} - current sync.json mappings array
    */
    obj.deleteMapping = function(mappingName, mappingChildren, syncConfigMappings) {
        var newSyncConfigMappings = _.filter(syncConfigMappings, function(mapping) {
            /*
                if there are other mappings with this mapping set as the "links"
                property remove the "links" property of those mappings
                essentially disassociating them with the mapping being deleted
            */
            if (mapping.links && mapping.links === mappingName) {
                delete mapping.links;
            }
            return mapping.name !== mappingName;
        }, this);

        return obj.deleteMappingChildren(mappingName, mappingChildren).then(function () {
            return configDelegate.updateEntity("sync", {"mappings": newSyncConfigMappings});
        });
    };
    /**
    * This function takes a mappingName, finds all managed/assigments and scheduled tasks
    * associated with that mapping, builds a display list of these items to be used in a
    * confirmDialog, and returns an object :
    *   {
    *       scheduledTasks : resultsOfScheduledTasksQuery
    *       assigments : resultsOfAssignmentsQuery
    *       display : htmlStringListingAssignmentsAndScheduledTasks
    *   }
    *
    * @param mappingName {string}
    * @returns {object}
    */
    obj.getMappingChildren = function (mappingName) {
        var scheduleQuery = schedulerDelegate.getReconSchedulesByMappingName(mappingName),
            assignmentQuery = resourceDelegate.searchResource(encodeURIComponent('mapping eq "' + mappingName + '"'), "managed/assignment"),
            scheduledTasksPartialPromise = UIUtils.preloadPartial("partials/mapping/_mappingScheduledTasks.html"),
            mappingAssignmentsPartialPromise = UIUtils.preloadPartial("partials/mapping/_mappingAssignments.html");

        return $.when(scheduleQuery,assignmentQuery, scheduledTasksPartialPromise, mappingAssignmentsPartialPromise).then((scheduledTasks, assignments) => {
            var mappingChildren = {
                scheduledTasks : scheduledTasks,
                assignments : assignments[0].result,
                display: ""
            };

            mappingChildren.display += "<div class='well'>";
            mappingChildren.display += Handlebars.compile("{{> mapping/_mappingScheduledTasks}}")({ scheduledTasks : mappingChildren.scheduledTasks });
            mappingChildren.display += Handlebars.compile("{{> mapping/_mappingAssignments}}")({ assignments : mappingChildren.assignments });
            mappingChildren.display += "</div>";

            return mappingChildren;
        });
    };
    /**
    * This function deletes a mapping's managed/assignments, scheduledTasks, and links
    *
    * @param mappingName {string}
    * @param mappingChildren {object}
    * @returns {promise}
    */
    obj.deleteMappingChildren = function (mappingName, mappingChildren) {
        var promise,
            scheduledTasksPromise = (schedule) => {
                return schedulerDelegate.deleteSchedule(schedule._id);
            },
            assignmentsPromise = (assignment) => {
                return resourceDelegate.deleteResource("/" + constants.context + "/managed/assignment", assignment._id);
            },
            concatPromise = (action) => {
                if (!promise) {
                    //no promise exists so create it
                    promise = action();
                } else {
                    //promise exists now "concat" a new "then" onto the original promise
                    promise = promise.then(() => {
                        return action();
                    });
                }
            };

        //delete this mapping's scheduleTasks
        _.each(mappingChildren.scheduledTasks, function (schedule) {
            concatPromise(() => {
                return scheduledTasksPromise(schedule);
            });
        });
        //delete this mapping's assignments
        _.each(mappingChildren.assignments, function (assignment) {
            concatPromise(() => {
                return assignmentsPromise(assignment);
            });
        });
        //delete this mapping's links
        concatPromise(() => {
            return resourceDelegate.serviceCall({
                "type": "POST",
                "serviceUrl": "/openidm/repo/links",
                "url":  "?_action=command&commandId=delete-mapping-links&mapping=" + mappingName
            });
        });

        return promise;
    };

    return obj;
});
