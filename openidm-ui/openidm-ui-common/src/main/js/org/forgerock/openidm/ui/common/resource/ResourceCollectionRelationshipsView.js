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
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/common/resource/ResourceCollectionRelationshipsView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/openidm/ui/common/util/ResourceCollectionUtils",
    "org/forgerock/commons/ui/common/util/ModuleLoader"
], function($, _, AbstractView, constants, resourceDelegate, resourceCollectionUtils, ModuleLoader) {
    var ResourceCollectionRelationshipsView = AbstractView.extend({
            template: "templates/admin/resource/ResourceCollectionRelationshipsViewTemplate.html",
            noBaseTemplate: true,
            events: {
                "change [name=viewType]": "toggleView",
                "click .resourceListItem": "showResource"
            },
            toggleView: function(e) {
                this.$el.find(".listTab").toggle();
                this.$el.find(".graphTab").toggle();
            },
            render: function (args, callback) {
                this.element = args.element;
                this.data.prop = args.prop;
                this.data.schema = args.schema;

                $.when(
                    resourceDelegate.searchResource(
                            args.prop.propName + ' eq "' + args.prop.parentId + '"&_pageSize=100&_sortKeys=' + args.prop.resourceCollection.query.fields[0],
                            args.prop.resourceCollection.path
                    ),
                    ModuleLoader.load("d3")
                ).then(_.bind(function(qry, d3) {
                    var schema = this.data.schema.properties;

                    this.d3 = d3;
                    this.data.headerValues = resourceCollectionUtils.getHeaderValues(this.data.prop.resourceCollection.query.fields, schema);

                    this.data.relatedTo = _.map(qry[0].result, _.bind(function(relationship) {
                        return _.pick(relationship, "_id", this.data.prop.resourceCollection.query.fields);
                    }, this));

                    this.parentRender(_.bind(function() {
                        if(this.data.relatedTo.length) {
                            this.parentRender(_.bind(function() {
                                this.loadTree();
                                if(callback) {
                                    callback();
                                }
                            }, this));
                        } else {
                            this.$el.closest(".container").find(".tab-menu").find("#tabHeader_relationship-" + this.data.prop.propName).hide();
                        }
                        if(callback) {
                            callback();
                        }
                    }, this));
                }, this));
            },
            showResource: function(e) {
                resourceCollectionUtils.showResource($(e.target).closest(".resourceListItem").attr("resourcePath"));
            },
            loadTree: function() {
                var treeData = {
                        "name" : resourceCollectionUtils.getDisplayText(this.data.prop, this.data.prop.parentValue),
                        "parent" : "null",
                        "children" : [

                        ]
                    },
                    margin = {
                        top: 20,
                        right: 120,
                        bottom: 20,
                        left: 350
                    },
                    width = 1024 - margin.right - margin.left,
                    height = 500 - margin.top - margin.bottom,
                    i = 0,
                    tree = this.d3.layout.tree().size([height, width]),
                    diagonal = this.d3.svg.diagonal().projection(function(d) {
                        return [d.y, d.x];
                    }),
                    svg = this.d3.select("#reportsGraphBody-" + this.data.prop.propName ).append("svg")
                        .attr("width", width + margin.right + margin.left)
                        .attr("height", height + margin.top + margin.bottom)
                        .append("g")
                        .attr("transform", "translate(" + margin.left + "," + margin.top + ")"),
                    root = null,
                    update = function(source) {
                        var nodes = tree.nodes(root).reverse(),
                            links = tree.links(nodes),
                            nodeEnter,
                            node,
                            link;

                        //Normalize for fixed-depth.
                        nodes.forEach(function(data) { data.y = data.depth * 180; });

                        //Declare the nodes
                        node = svg.selectAll("g.node").data(nodes, function(data) {
                            if(!data.id) {
                                data.id = ++i;
                            }

                            return data.id;
                        });

                        //Enter the nodes.
                        nodeEnter = node.enter().append("g")
                            .attr("class", "node")
                            .attr("transform", function(data) {
                                return "translate(" + data.y + "," + data.x + ")";
                            });

                        //Add Circles
                        nodeEnter.append("circle")
                            .attr("r", 10)
                            .style("fill", "#fff");

                        //Add Text
                        nodeEnter.append("svg:a")
                            .attr("xlink:href", function(data){return data.url;})
                            .append("text")
                            .attr("x", function(data) {
                                return data.children || data._children ? -13 : 13;
                            })
                            .attr("dy", ".35em")
                            .attr("text-anchor", function(data) {
                                return data.children || data._children ? "end" : "start";
                            })
                            .html(function(data) { return data.name; })
                            .style("fill-opacity", 1);

                        //Generate the paths
                        link = svg.selectAll("path.link").data(links, function(d) {
                            return d.target.id;
                        });

                        //Add the paths
                        link.enter().insert("path", "g")
                            .attr("class", "link")
                            .attr("d", diagonal);
                    };

                _.each(this.data.relatedTo, _.bind(function(item){
                    treeData.children.push({
                        "name" : resourceCollectionUtils.getDisplayText(this.data.prop, item),
                        "parent" : "null",
                        "url" : "#resource/" + this.data.prop.resourceCollection.path + "/edit/" + item._id +"/"
                    });
                }, this));

                root = treeData;

                update(root);
            }
        });

    return ResourceCollectionRelationshipsView;
});
