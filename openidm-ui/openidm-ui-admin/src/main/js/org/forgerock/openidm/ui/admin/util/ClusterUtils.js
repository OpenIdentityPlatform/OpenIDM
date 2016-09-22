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

define([
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/openidm/ui/admin/delegates/ClusterDelegate",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/openidm/ui/admin/util/SchedulerUtils",
    "bootstrap-dialog",
    "moment",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, _,handlebars,
    ClusterDelegate,
    SchedulerDelegate,
    SchedulerUtils,
    BootstrapDialog,
    moment,
    UIUtils
) {

    var obj = {};

    obj.getClusterData = function () {
        //get the info for all the nodes in the cluster
        return ClusterDelegate.getNodes().then((cluster) => {
            return this.getDetailsForNodes(cluster.results);
        });
    };
    /**
     * This function takes a list of nodes, looks for any jobs that may be running
     * on any of those nodes, matches nodes with jobs, and returns an array of node objects
     * with a "runningJobs" array attached
     *
     * @param {array} clusterNodes - an array of node objects to gather details on
     * @returns {promise}
     **/
    obj.getDetailsForNodes = function (clusterNodes) {
        //pluck out the nodeIds into an array
        var nodeIds = _.map(clusterNodes,"instanceId");
        //get all the scheduler/job triggers that have been picked up by any of these nodes
        //and return an array of node objects with info about any currently running jobs on that node
        return SchedulerDelegate.getSchedulerTriggersByNodeIds(nodeIds).then((jobs) => {
            //loop over each clusterNode
            return _.map(clusterNodes, (node) => {
                var runningJobs = [];
                //loop over all jobs, match up the ones for this node,
                //and add them to the runningJobs array
                _.each(jobs, (job) => {
                    if (job.triggers[0] && job.triggers[0].nodeId === node.instanceId) {
                        runningJobs.push({
                            link: "scheduler/edit/" + job._id + "/",
                            typeData: SchedulerUtils.getScheduleTypeData(job)
                        });
                    }
                });
                //convert startup and shutdown into readable dates
                node.startup = moment(node.startup).format('MMMM Do YYYY, h:mm:ss a');
                if (node.shutdown) {
                    node.shutdown = moment(node.shutdown).format('MMMM Do YYYY, h:mm:ss a');
                }
                //add the runningJobs and jobCount to the node object
                return _.extend(node, {
                    runningJobs : runningJobs,
                    jobCount: runningJobs.length
                });
            });
        });
    };

    /**
    * This function opens a bootstrap dialog with info on a cluster node
    *
    * @param {object} node - an object representing a node example:
        {
            "instanceId": "node1",
            "startup": "October 11th 2016, 4:45:13 pm",
            "state": "running",
            "shutdown": "",
            "runningJobs": [
                {
                    "link": "scheduler/edit/2dc25cd0-3c26-4552-b2a4-3f4f34e1e3e9/",
                    "typeData": {
                        "type": "genericScript",
                        "display": "Script",
                        "meta": "java.lang.Thread.sleep(25000); ",
                        "metaSource": "script.source"
                    }
                },
                {
                    "link": "scheduler/edit/845bdeb9-d7d7-427a-9cd6-cf56677a3353/",
                    "typeData": {
                        "type": "genericScript",
                        "display": "Script",
                        "meta": "java.lang.Thread.sleep(50000); ",
                        "metaSource": "script.source"
                    }
                }
            ],
            "jobCount": 2
        }
    **/
    obj.openNodeDetailDialog = function (node) {
        UIUtils.preloadPartial("partials/util/_clusterNodeDetail.html").then( () => {
            var details = $(handlebars.compile("{{> util/_clusterNodeDetail }}")(node));

            BootstrapDialog.show({
                title: node.instanceId,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: details,
                size: BootstrapDialog.SIZE_WIDE,
                buttons: [
                    {
                        label: $.t('common.form.close'),
                        id: "clusterNodeDetailDialogCloseBtn",
                        action: function(dialogRef){
                            dialogRef.close();
                        }
                    }
                ]
            });
        });
    };

    return obj;
});
