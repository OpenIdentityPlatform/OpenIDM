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

/*global define, window */

define("org/forgerock/openidm/ui/admin/dashboard/widgets/UserRelationshipWidget", [
    "jquery",
    "underscore",
    "bootstrap",
    "selectize",
    "d3",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate"
], function($, _, bootstrap,
            selectize,
            d3,
            AbstractView,
            ResourceDelegate) {
    var widgetInstance = {},
        Widget = AbstractView.extend({
            noBaseTemplate: true,
            template: "templates/admin/dashboard/widgets/UserRelationshipWidgetTemplate.html",
            model: {},
            events: {
                "click .toggle-view-btn": "toggleButtonChange"
            },
            toggleButtonChange: function(event) {
                var target = $(event.target);

                if(target.hasClass("fa")) {
                    target = target.parents(".btn");
                }

                this.$el.find(".toggle-view-btn").toggleClass("active", false);
                target.toggleClass("active", true);
            },

            render: function(args, callback) {
                var searchList,
                    _this = this;

                this.element = args.element;
                this.model = {};

                this.parentRender(_.bind(function(){
                    this.$el.find("#findUserRelationship").selectize({
                        valueField: "userName",
                        searchField: "userName",
                        maxOptions: 10,
                        create: false,
                        onChange: function(value) {
                            _this.$el.find("#userRelationshipNoDetails").hide();
                            _this.$el.find("#userRelationshipDisplay").show();

                            _this.setupRelationshipChart(this.options[value]);
                        },
                        render: {
                            option: function(item, selectizeEscape) {
                                var fields = _.pick(item, ["userName", "displayName"]),
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
                                return "<div>" +escape(item.userName) +"</div>";
                            }
                        },
                        load: function(query, selectizeCallback) {
                            if (!query.length || query.length < 2) {
                                return callback();
                            }

                            //TODO Dynamic build list off of relationship from schema
                            ResourceDelegate.searchResource("userName sw \"" +query +"\"&_fields=*,roles/*,devices/*,manager/*&_pageSize=10", "managed/user").then(function(response) {
                                if(response) {
                                    searchList = response.result;

                                    selectizeCallback(response.result);
                                } else {
                                    searchList = [];

                                    selectizeCallback();
                                }
                            });
                        }
                    });

                    if(callback) {
                        callback();
                    }
                }, this));
            },

            /*
             Example data set:
             var nodeSet = [
             {id: "N1", name: "Smith, John", fixed: true, x: 10, y: 0, type: "Main", hlink: ""},
             {id: "N2", name: "Product 2", type: "Type", hlink: ""},
             {id: "N3", name: "Cee, Washing D", type: "Type", hlink: ""},
             {id: "N4", name: "Product 4", type: "Type", hlink: ""},
             ];


             var linkSet = [
             {sourceId: "N1", linkName: "Relationship 1", targetId: "N2"},
             {sourceId: "N3", linkName: "Relationship 2", targetId: "N1"},
             {sourceId: "N4", linkName: "Relationship 3", targetId: "N1"}
             ];
             */
            setupRelationshipChart: function(details) {
                var focalNodeID = "mainNode",
                    nodeSet = [{
                        id: "mainNode",
                        name: details.displayName,
                        x: 10,
                        y: 0,
                        fixed: true,
                        type: "User",
                        hlink: "#resource/managed/user/edit/" + details._id
                    }],
                    linkSet = [],
                    nodeCounter = 0,
                    tempRef;

                this.$el.find("#roundRelationshipGraph").empty();

                 //For now this is hardcoded to display user, manager, role, and device

                if(details.manager) {
                    nodeSet.push({
                        id: "N" + nodeCounter,
                        name: details.manager.displayName,
                        type: "Manager",
                        hlink: "#resource/managed/user/edit/" +details.manager._id
                    });

                    nodeCounter++;
                }

                if(details.roles) {
                    _.each(details.roles, function (item) {
                        nodeSet.push({
                            id: "N" + nodeCounter,
                            name: item.name,
                            type: "Role",
                            hlink: "#resource/managed/role/edit/" +item._id
                        });

                        nodeCounter++;
                    });
                }

                /*
                Device Examples:

                details.devices = [
                    {
                        deviceName : "IPad",
                        _ref : "location/location"
                    },
                    {
                        deviceName : "Mac Book",
                        _ref : "location/location"
                    },
                    {
                        deviceName : "Bluetooth Mouse",
                        _ref : "location/location"
                    }
                ]
                */

                if(details.devices) {
                    _.each(details.devices, function (item) {
                        tempRef = item._ref.split("/");

                        nodeSet.push({
                            id: "N" + nodeCounter,
                            name: item.deviceName,
                            type: "Device",
                            hlink: "#resource/managed/" +tempRef[1] +"/edit/" +tempRef[2]
                        });

                        nodeCounter++;
                    });
                }

                _.each(nodeSet, function(item) {
                    if(item.sourceId !== "userNode") {
                        linkSet.push({
                            sourceId: "mainNode",
                            linkName: item.type,
                            secondaryNode: false,
                            targetId: item.id
                        });
                    }
                });



                //Multi layer example keep for future development
                /*
                nodeSet.push({
                    id: "N20",
                    name: "Create Agile Board",
                    type: "Assignment",
                    hlink: ""
                });

                nodeSet.push({
                    id: "N21",
                    name: "Order Issues",
                    type: "Assignment",
                    hlink: ""
                });

                nodeSet.push({
                    id: "N22",
                    name: "John Smith",
                    type: "User",
                    hlink: ""
                });

                nodeSet.push({
                    id: "N23",
                    name: "Steve Steveson",
                    type: "User",
                    hlink: ""
                });

                nodeSet.push({
                    id: "N24",
                    name: "Hank Hankerson",
                    type: "User",
                    hlink: ""
                });

                nodeSet.push({
                    id: "N25",
                    name: "Bilbo Baggins",
                    type: "User",
                    hlink: ""
                });

                linkSet.push({
                    sourceId: "N1",
                    linkName: "Assignment",
                    secondaryNode: true,
                    targetId: "N20"
                });

                linkSet.push({
                    sourceId: "N1",
                    linkName: "Assignment",
                    secondaryNode: true,
                    targetId: "N21"
                });

                linkSet.push({
                    sourceId: "N1",
                    linkName: "User",
                    secondaryNode: true,
                    targetId: "N22"
                });

                linkSet.push({
                    sourceId: "N1",
                    linkName: "User",
                    secondaryNode: true,
                    targetId: "N23"
                });

                linkSet.push({
                    sourceId: "N1",
                    linkName: "User",
                    secondaryNode: true,
                    targetId: "N24"
                });

                linkSet.push({
                    sourceId: "N1",
                    linkName: "User",
                    secondaryNode: true,
                    targetId: "N25"
                });
                */
                this.updateRoundChart(focalNodeID, nodeSet, linkSet, "#roundRelationshipGraph", "colorScale20c");
            },

            typeMouseOver: function() {
                var thisObject = d3.select(this),
                    typeValue = thisObject.attr("type_value"),
                    strippedTypeValue = typeValue.replace(/ /g, "_"),
                    selectedBullet = d3.selectAll(".legendBullet-" + strippedTypeValue),
                    nodeSize = 12, //Pixel size of circles
                    bulletSize = 8, //Pixel size for bullets in legend
                    highlightColor = "#519387",
                    font = "bold 12px Arial",
                    legendTextSelector,
                    selectedLegendText,
                    nodeTextSelector,
                    selectedNodeText,
                    nodeCircleSelector,
                    selectedCircle,
                    focalNodeCircleSelector,
                    selectedFocalNodeCircle,
                    focalNodeType,
                    focalNodeTextSelector,
                    selectedFocalNodeText,
                    focalNodeTextType;

                selectedBullet.style("fill", highlightColor);
                selectedBullet.attr("r", bulletSize);

                legendTextSelector = ".legendText-" + strippedTypeValue;
                selectedLegendText = d3.selectAll(legendTextSelector);
                selectedLegendText.style("font", font);
                selectedLegendText.style("fill", highlightColor);

                nodeTextSelector = ".nodeText-" + strippedTypeValue;
                selectedNodeText = d3.selectAll(nodeTextSelector);
                selectedNodeText.style("font", font);
                selectedNodeText.style("fill", highlightColor);

                nodeCircleSelector = ".nodeCircle-" + strippedTypeValue;
                selectedCircle = d3.selectAll(nodeCircleSelector);
                selectedCircle.style("fill", highlightColor);
                selectedCircle.style("stroke", highlightColor);
                selectedCircle.attr("r", nodeSize);

                focalNodeCircleSelector = ".focalNodeCircle";
                selectedFocalNodeCircle = d3.selectAll(focalNodeCircleSelector);

                focalNodeType = selectedFocalNodeCircle.attr("type_value");

                if (typeValue === focalNodeType){
                    selectedFocalNodeCircle.style("stroke", highlightColor);
                    selectedFocalNodeCircle.style("fill", "White");
                }

                focalNodeTextSelector = ".focalNodeText";
                selectedFocalNodeText = d3.selectAll(focalNodeTextSelector);
                focalNodeTextType = selectedFocalNodeText.attr("type_value");

                if (typeValue === focalNodeTextType) {
                    selectedFocalNodeText.style("fill", highlightColor);
                    selectedFocalNodeText.style("font", font);
                }
            },

            typeMouseOut: function() {
                var thisObject = d3.select(this),
                    typeValue = thisObject.attr("type_value"),
                    colorValue = thisObject.attr("color_value"),
                    strippedTypeValue = typeValue.replace(/ /g, "_"),
                    selectedBullet = d3.selectAll( ".legendBullet-" + strippedTypeValue),
                    nodeSize = 12, //Pixel size of circles
                    font = "normal 12px Arial",
                    legendTextSelector,
                    selectedLegendText,
                    nodeTextSelector,
                    selectedNodeText,
                    nodeCircleSelector,
                    selectedCircle,
                    focalNodeCircleSelector,
                    selectedFocalNodeCircle,
                    focalNodeType,
                    focalNodeTextSelector,
                    selectedFocalNodeText;

                selectedBullet.style("fill", colorValue);
                selectedBullet.attr("r", 6);

                legendTextSelector = ".legendText-" + strippedTypeValue;
                selectedLegendText = d3.selectAll(legendTextSelector);
                selectedLegendText.style("font", font);
                selectedLegendText.style("fill", "");

                nodeTextSelector = ".nodeText-" + strippedTypeValue;
                selectedNodeText = d3.selectAll(nodeTextSelector);
                selectedNodeText.style("font", font);
                selectedNodeText.style("fill", "");

                nodeCircleSelector = ".nodeCircle-" + strippedTypeValue;
                selectedCircle = d3.selectAll(nodeCircleSelector);
                selectedCircle.style("fill", "White");
                selectedCircle.style("stroke", colorValue);
                selectedCircle.attr("r", nodeSize);

                focalNodeCircleSelector = ".focalNodeCircle";
                selectedFocalNodeCircle = d3.selectAll(focalNodeCircleSelector);
                focalNodeType = selectedFocalNodeCircle.attr("type_value");

                if (typeValue === focalNodeType){
                    selectedFocalNodeCircle.style("stroke", colorValue);
                    selectedFocalNodeCircle.style("fill", "White");
                }

                focalNodeTextSelector = ".focalNodeText";
                selectedFocalNodeText = d3.selectAll(focalNodeTextSelector);
                selectedFocalNodeText.style("fill", "");
                selectedFocalNodeText.style("font", font);
            },

            nodeMouseOver: function() {
                var thisObject = d3.select(this),
                    typeValue = thisObject.attr("type_value"),
                    strippedTypeValue = typeValue.replace(/ /g, "_"),
                    focalNode = "mainNode",
                    selectedBullet,
                    legendTextSelector,
                    selectedLegendText,
                    highlightColor = "#519387",
                    font = "bold 12px Arial",
                    largeNodeSize = 65,
                    smallNodeSize = 15;

                //Increase circle size for better highlight visability
                d3.select(this).select("circle").transition()
                    .duration(250)
                    .attr("r", function(d) {
                        if(d.id === focalNode) {
                            return largeNodeSize;
                        } else {
                            return smallNodeSize;
                        }
                    });

                d3.select(this).select("text").transition()
                    .style("font", font);

                selectedBullet = d3.selectAll(".legendBullet-" + strippedTypeValue);
                selectedBullet.style("fill", highlightColor);

                //Increase the bullet node size on highlight
                selectedBullet.attr("r", 1.2 * 6);

                legendTextSelector = ".legendText-" + strippedTypeValue;

                selectedLegendText = d3.selectAll(legendTextSelector);
                selectedLegendText.style("font", font);
                selectedLegendText.style("fill", highlightColor);
            },

            nodeMouseOut: function() {
                var thisObject = d3.select(this),
                    typeValue = thisObject.attr("type_value"),
                    colorValue = thisObject.attr("color_value"),
                    strippedTypeValue = typeValue.replace(/ /g, "_"),
                    focalNode = "mainNode",
                    nodeSize = 12, //Pixel size of circles
                    centerNodeSize = 45, //Pixel size of circles
                    selectedBullet,
                    font = "normal 12px Arial",
                    legendTextSelector,
                    selectedLegendText;

                d3.select(this).select("circle").transition()
                    .duration(250)
                    .attr("r", function(d, i) { if(d.id === focalNode) {
                        return centerNodeSize;
                    } else {
                        return nodeSize;
                    }
                });

                d3.select(this).select("text").transition()
                    .duration(250)
                    .style("font", font)
                    .attr("fill", "Black");

                selectedBullet = d3.selectAll(".legendBullet-" + strippedTypeValue);
                selectedBullet.style("fill", colorValue);
                selectedBullet.attr("r", 6);

                legendTextSelector = ".legendText-" + strippedTypeValue;

                selectedLegendText = d3.selectAll(legendTextSelector);
                selectedLegendText.style("font", font);
                selectedLegendText.style("fill", "Black");

            },

            updateRoundChart: function (focalNode, nodeSet, linkSet, selectString) {
                var colorScale = d3.scale.category20c(),
                    width = 1024,
                    height = 600,
                    centerNodeSize = 45, //Pixel size of circles
                    nodeSize = 12, //Pixel size of circles
                    color_hash = [],
                    _this = this,
                    sortedKeys,
                    svgCanvas,
                    node_hash,
                    force,
                    link,
                    node,
                    linkText;

                //Create a hash that maps colors to types
                nodeSet.forEach(function(d) {
                    color_hash[d.type] = "";
                });

                sortedKeys = _.keys(color_hash).sort();

                sortedKeys.forEach(function(d, i) {
                    color_hash[d] = colorScale(i);
                });

                nodeSet.forEach(function(d) {
                    d.color = color_hash[d.type];
                });

                //Create a canvas
                svgCanvas = d3.select(selectString)
                    .append("svg:svg")
                    .attr("width", width)
                    .attr("height", height)
                    .append("svg:g")
                    .attr("class", "focalNodeCanvas")
                    .attr("transform", "translate(" + width/2 + "," + height/2 + ")");

                node_hash = [];

                //Create a hash that allows access to each node by its id
                nodeSet.forEach(function(d) {
                    node_hash[d.id] = d;
                });

                //Append the source object node and the target object node to each link records...
                linkSet.forEach(function(d) {
                    d.source = node_hash[d.sourceId];
                    d.target = node_hash[d.targetId];

                    if (d.sourceId === focalNode) {
                        d.direction = "OUT";
                    } else {
                        d.direction = "IN";
                    }
                });

                //Create a force layout and bind nodes and links
                force = d3.layout.force()
                    .nodes(nodeSet)
                    .links(linkSet)
                    .charge(-1000)
                    .gravity(0.01)
                    .friction(0.2)
                    .linkStrength(9)
                    .linkDistance( function(d) {
                        if(!d.secondaryNode) {
                            if (width < height) {
                                return (25 + width) * 0.35;
                            } else {
                                return (25 + height) * 0.35;
                            }
                        } else {
                            return 150;
                        }
                    })
                    .on("tick", function() {
                        link.attr("x1", function(d) { return d.source.x; })
                            .attr("y1", function(d) { return d.source.y; })
                            .attr("x2", function(d) { return d.target.x; })
                            .attr("y2", function(d) { return d.target.y; });

                        node.attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });

                        linkText.attr("x", function(d) {
                            if (d.target.x > d.source.x) { return (d.source.x + (d.target.x - d.source.x)/2); }
                            else { return (d.target.x + (d.source.x - d.target.x)/2); }
                        })
                            .attr("y", function(d) {
                                if (d.target.y > d.source.y) { return (d.source.y + (d.target.y - d.source.y)/2); }
                                else { return (d.target.y + (d.source.y - d.target.y)/2); }
                            });
                    })
                    .start();

                //Draw lines for links between nodes
                link = svgCanvas.selectAll(".gLink")
                    .data(force.links())
                    .enter().append("g")
                    .attr("class", "gLink")
                    .append("line")
                    .attr("class", "link")
                    .style("stroke", "#ccc")
                    .attr("x1", function(d) { return d.source.x; })
                    .attr("y1", function(d) { return d.source.y; })
                    .attr("x2", function(d) { return d.target.x; })
                    .attr("y2", function(d) { return d.target.y; });

                //Create nodes
                node = svgCanvas.selectAll(".node")
                    .data(force.nodes())
                    .enter().append("g")
                    .attr("class", "node")
                    .attr("type_value", function(d) {
                        return d.type;
                    })
                    .attr("color_value", function(d) {
                        return color_hash[d.type];
                    })
                    .on("mouseover", _this.nodeMouseOver)
                    .on("mouseout", _this.nodeMouseOut)
                    .call(force.drag)
                    .append("a")
                    .attr("xlink:href", function(d) {
                        return d.hlink;
                    });

                //Append circles to nodes
                node.append("circle")
                    .attr("r", function(d) {
                        if (d.id === focalNode) {
                            return centerNodeSize;
                        } else {
                            return nodeSize;
                        }
                    })
                    .style("fill", "White") //Make the nodes hollow looking
                    .attr("type_value", function(d) {
                        return d.type;
                    })
                    .attr("color_value", function(d) {
                        return color_hash[d.type];
                    })
                    .attr("class", function(d) {
                        var str = d.type,
                            strippedString = str.replace(/ /g, "_");

                        if (d.id === focalNode) {
                            return "focalNodeCircle";
                        } else {
                            return "nodeCircle-" + strippedString;
                        }
                    })
                    .style("stroke-width", 5) //Give the node strokes some thickness
                    .style("stroke", function(d, i) {
                        return color_hash[d.type];
                    })
                    .call(force.drag);

                //Append text to nodes
                node.append("text")
                    .attr("x", function(d) { if (d.id === focalNode) { return 0; } else {return 20;} } )
                    .attr("y", function(d) { if (d.id === focalNode) { return 0; } else {return -10;} } )
                    .attr("text-anchor", function(d) { if (d.id === focalNode) {return "middle";} else {return "start";} })
                    .attr("font-family", "Arial, Helvetica, sans-serif")
                    .style("font", "normal 12px Arial")
                    .attr("fill", "Black")
                    .style("fill", function(d) { return color_hash[d]; })
                    .attr("type_value", function(d) { return d.type; })
                    .attr("color_value", function(d) { return color_hash[d.type]; })
                    .attr("class", function(d) {
                        var str = d.type,
                            strippedString = str.replace(/ /g, "_");

                        if (d.id === focalNode) {
                            return "focalNodeText";
                        } else {
                            return "nodeText-" + strippedString;
                        }
                    })
                    .attr("dy", ".35em")
                    .text(function(d) {
                        return d.name;
                    });

                //Append text to link edges
                linkText = svgCanvas.selectAll(".gLink")
                    .data(force.links())
                    .append("text")
                    .attr("font-family", "Arial, Helvetica, sans-serif")
                    .attr("x", function(d) {
                        if (d.target.x > d.source.x) {
                            return (d.source.x + (d.target.x - d.source.x)/2);
                        } else {
                            return (d.target.x + (d.source.x - d.target.x)/2);
                        }
                    })
                    .attr("y", function(d) {
                        if (d.target.y > d.source.y) {
                            return (d.source.y + (d.target.y - d.source.y)/2);
                        } else {
                            return (d.target.y + (d.source.y - d.target.y)/2);
                        }
                    })
                    .attr("fill", "Black")
                    .style("font", "normal 12px Arial")
                    .attr("dy", ".35em")
                    .text(function(d) {
                        return d.linkName;
                    });

                //Print legend title
                svgCanvas.append("text").attr("class","region")
                    .text("Data Types:")
                    .attr("x", -1 * (width/2 - 10))
                    .attr("y", (-height/7 * 3))
                    .style("fill", "Black")
                    .style("font", "bold 12px Arial")
                    .attr("text-anchor","start");

                //Plot the bullet circle
                svgCanvas.selectAll("focalNodeCanvas")
                    .data(sortedKeys).enter().append("svg:circle") // Append circle elements
                    .attr("cx", -1*(width/2 - 25))
                    .attr("cy", function(d, i) {
                        return (i * 20 - height/7 * 3 + 20);
                    })
                    .attr("stroke-width", ".5")
                    .style("fill", function(d) {
                        return color_hash[d];
                    })
                    .attr("r", 6)
                    .attr("color_value", function(d) {
                        return color_hash[d];
                    })
                    .attr("type_value", function(d) {
                        return d;
                    })
                    .attr("index_value", function(d, i) {
                        return "index-" + i;
                    })
                    .attr("class", function(d) {
                        var str = d,
                            strippedString = str.replace(/ /g, "_");

                        return "legendBullet-" + strippedString;
                    })
                    .on('mouseover', _this.typeMouseOver)
                    .on("mouseout", _this.typeMouseOut);

                //Create legend text that acts as label keys
                svgCanvas.selectAll("a.legend_link")
                    .data(sortedKeys)
                    .enter().append("svg:a")
                    .append("text")
                    .attr("text-anchor", "center")
                    .attr("x", -1 * (width/2 - 40))
                    .attr("y", function(d, i) { return (i * 20 - height/7 * 3 + 20); } )
                    .attr("dx", 0)
                    .attr("dy", "4px") //Controls padding to place text in alignment with bullets
                    .text(function(d) {
                        return d;
                    })
                    .attr("color_value", function(d) {
                        return color_hash[d];
                    })
                    .attr("type_value", function(d) {
                        return d;
                    })
                    .attr("index_value", function(d, i) {
                        return "index-" + i;
                    })
                    .attr("class", function(d) {
                        var str = d,
                            strippedString = str.replace(/ /g, "_");

                        return "legendText-" + strippedString;
                    })
                    .style("fill", "Black")
                    .style("font", "normal 12px Arial")
                    .on('mouseover', _this.typeMouseOver)
                    .on("mouseout", _this.typeMouseOut);
            }
        });

    widgetInstance.generateWidget = function(loadingObject, callback) {
        var widget = {};

        $.extend(true, widget, new Widget());

        widget.render(loadingObject, callback);

        return widget;
    };

    return widgetInstance;
});