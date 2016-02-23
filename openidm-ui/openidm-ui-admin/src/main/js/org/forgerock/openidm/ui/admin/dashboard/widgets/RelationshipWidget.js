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

/*global define, window */

define("org/forgerock/openidm/ui/admin/dashboard/widgets/RelationshipWidget", [
    "jquery",
    "underscore",
    "bootstrap",
    "selectize",
    "d3",
    "org/forgerock/openidm/ui/common/dashboard/widgets/AbstractWidget",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate"
], function($, _, bootstrap,
            selectize,
            d3,
            AbstractWidget,
            ResourceDelegate) {
    var widgetInstance = {},
        Widget = AbstractWidget.extend({
            template: "templates/admin/dashboard/widgets/RelationshipWidgetTemplate.html",
            model : {
                "overrideTemplate" : "dashboard/widget/_relationshipConfig"
            },
            widgetSettings: function (event) {
                AbstractWidget.prototype.widgetSettings.call(this,event);
                /*
                 * had to do this as there is no facility to handle a checkbox in widgetSettings
                 * must wait for .toggleBoolean to be on the dom
                 */
                setTimeout( function () {
                    $(".toggleBoolean").click(function (e) {
                        var toggle = $(e.target);
                        if (toggle.prop("checked")) {
                            $("#displaySubRelationships").val(true);
                        } else {
                            $("#displaySubRelationships").val(false);
                        }
                    });
                }, 500);
            },

            widgetRender: function(args, callback) {
                var _this = this;
                
                /*
                 * if baseObject does not exist use widget defaults for baseOjbect and searchFields settings
                 */
                if (!this.data.baseObject) {
                    this.data.baseObject = args.widget.defaultObject;
                    this.data.searchFields = [];
                    if (args.widget.searchProperty) {
                        this.data.searchFields = args.widget.searchProperty.split(",");
                    }
                    this.data.displaySubRelationships = args.widget.displaySubRelationships;
                }

                this.partials.push("partials/dashboard/widget/_relationshipConfig.html");
                ResourceDelegate.getSchema(["managed", this.data.baseObject]).then( _.bind(function (schema) {
                    /*
                     * get the schema for baseObject then figure out which properties of this object are either
                     * type relationship or type array of relationships
                     */
                    this.data.relationshipProps = this.getRelationshipProps(schema.properties);
                    /*
                     * if there are no searchFields defined then set the searchFields array to 
                     * a single string value containing the second value in the schema order
                     * this assumes that "_id" is the value of schema.order[0]
                     * TODO figure out a better way of handling this
                     */
                    if (schema.order && !this.data.searchFields) {
                        this.data.searchFields = [schema.order[1]];
                    }
                    
                    this.data.schema = schema;
                    
                    this.parentRender(_.bind(function(){
                        /*
                         * build the selectize search field
                         */
                        this.$el.find("#findRelationship").selectize({
                            /*
                             * had to use something for valueField that would always be available here
                             */
                            valueField: _this.data.searchFields[0],
                            searchField: _this.data.searchFields,
                            maxOptions: 10,
                            create: false,
                            onChange: function(value) {
                                _this.$el.find("#roundRelationshipGraphStatus h2").text($.t("dashboard.relationshipWidget.gatheringRelationshipData"));
                                _this.getResourceRenderChart(this.options[value]._id);
                            },
                            render: {
                                option: function(item, selectizeEscape) {
                                    var element = $('<div class="fr-search-option"></div>'),
                                        counter = 0;
    
                                    _.each(_this.data.searchFields, function(key) {
                                        if(counter === 0) {
                                            $(element).append('<div class="fr-search-primary">' +selectizeEscape(item[key]) +'</div>');
                                        } else {
                                            $(element).append('<div class="fr-search-secondary text-muted">' +selectizeEscape(item[key]) +'</div>');
                                        }
    
                                        counter++;
                                    }, this);
    
                                    return element.prop('outerHTML');
                                },
                                item: function(item, escape) {
                                    var txtArr = [];
                                    
                                    _.each(_this.data.searchFields, function (field) {
                                        txtArr.push(escape(item[field]));
                                    });
                                    
                                    return "<div>" + txtArr.join(" / ") +"</div>";
                                }
                            },
                            load: function(query, selectizeCallback) {
                                var queryFilter = ResourceDelegate.queryStringForSearchableFields(_this.data.searchFields, query);
                                
                                if (!query.length || query.length < 2) {
                                    return selectizeCallback();
                                }
                                
                                ResourceDelegate.searchResource(queryFilter, "managed/" + _this.data.baseObject).then(function(response) {
                                    if(response && response.result.length > 0) {
                                        selectizeCallback(response.result);
                                    } else {
                                        selectizeCallback();
                                    }
                                });
                            }
                        });
                        
                        if (_this.data.resourceUrl) {
                            _this.getResourceRenderChart();
                        }
    
                        if(callback) {
                            callback();
                        }
                    }, this));
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
                var _this = this,
                    focalNodeID = "mainNode",
                    displayTextArray = [],
                    nodeSet,
                    linkSet = [],
                    nodeCounter = 0,
                    tempRef,
                    titleNodeId,
                    getNodeData = function (item, itemType, itemObject , resourceCollection) {
                        var nameTextArr = [],
                            refreshSettings = {
                                searchFields : []
                            },
                            returnText = "";
                        
                        _.each(resourceCollection, function (rc) {
                            var pathArr = rc.path.split("/"),
                                resourceType = pathArr[0],
                                resourceObject = pathArr[1];
                            
                            if (resourceType === itemType && resourceObject === itemObject) {
                                /*
                                 * refreshSettings are for the click event on the circle which refreshes the 
                                 * relationship chart view with the perspective of the object being clicked
                                 */
                                refreshSettings.baseObject = resourceObject;
                                refreshSettings.searchFields = rc.query.fields;
                                /*
                                 * from the list of fields defined in the query.fields array for this resource collection
                                 * build the nameTextArr to be used for the name property of this node
                                 */
                                _.each(rc.query.fields, function (field) {
                                    nameTextArr.push(item[field]);
                                });
                            }
                        });
                        
                        if (nameTextArr[0]) {
                            returnText = nameTextArr.join(" / ");
                        }
                        
                        return {
                            refreshSettings : refreshSettings,
                            text: returnText
                        };
                    },
                    /*
                     * addNode adds a node on the first level of relationship data for the item being passed in
                     * 
                     * if displaySubRelationships is enabled it loops over all of the relationship properties of
                     * whatever object the item comes from (i.e. managed/user or managed/role) and adds a subnode for
                     * each of the values from that relationship
                     */
                    addNode = function (prop, item, resourceCollection) {
                        var tempRef = item._ref.split("/"),
                            itemType = tempRef[0],
                            itemObject = tempRef[1],
                            itemId = tempRef[2],
                            resourceCollections,
                            secondarySourceId,
                            nodeData = getNodeData(item, itemType, itemObject, resourceCollection);
                        
                        if (nodeData.text && nodeData.text.length) {
                            nodeSet.push({
                                id: "N" + nodeCounter,
                                sourceId: titleNodeId,
                                name: nodeData.text,
                                type: prop.title || prop.propName,
                                hlink: "#resource/" + itemType + "/" + itemObject +"/edit/" + itemId,
                                resourceUrl: item._ref,
                                refreshSettings: nodeData.refreshSettings
                            });
                            
                            secondarySourceId = "N" + nodeCounter;

                            nodeCounter++;
                            
                            if (prop.items) {
                                resourceCollections = prop.items.resourceCollection;
                            } else {
                                resourceCollections = prop.resourceCollection;
                            }
                            _.each(resourceCollections , function (resourceCollection) {
                                var schema = _.where(_this.data.schema.allSchemas, { name : resourceCollection.path.split("/")[1] }),
                                    relProps = [];
                                
                                if(schema[0]) {
                                    relProps = _this.getRelationshipProps(schema[0].schema.properties);
                                }
                                
                                _.each(relProps, function (relProp) {
                                    var addSubNode = function (item) {
                                            var subItemRef = item._ref.split("/"),
                                                subItemType = subItemRef[0],
                                                subItemObject = subItemRef[1],
                                                subResourceCollections,
                                                subNodeData,
                                                refreshSettings;
                                            
                                            if (relProp.items) {
                                                subResourceCollections = relProp.items.resourceCollection;
                                            } else {
                                                subResourceCollections = relProp.resourceCollection;
                                            }
                                        
                                            subNodeData = getNodeData(item, subItemType, subItemObject, subResourceCollections);
                                            refreshSettings = nodeData.refreshSettings;
                                            
                                            nodeSet.push({
                                                id: "SN" + nodeCounter,
                                                sourceId: secondarySourceId,
                                                name: subNodeData.text,
                                                type: (prop.title || prop.propName) + "-" + (relProp.title || relProp.propName),
                                                hlink: "#",
                                                resourceUrl: item._ref,
                                                refreshSettings: subNodeData.refreshSettings
                                            });
                                            
                                            nodeCounter++;
                                        };
                                    
                                    if (item[relProp.propName]) {
                                        if (_.isArray(item[relProp.propName])) {
                                            _.each(item[relProp.propName], function (subSubItem) {
                                                addSubNode(subSubItem);
                                            });
                                        } else {
                                            addSubNode(item[relProp.propName]);
                                        }
                                    }
                                });
                            });
                        }
                    };

                _.each(this.data.searchFields, function (field) {
                    if (details[field]) {
                        displayTextArray.push(details[field]);
                    }
                });
                
                /*
                 * create the nodeSet with the origin being the mainNode
                 */
                nodeSet = [{
                    id: "mainNode",
                    name: displayTextArray.join(" / "),
                    x: 10,
                    y: 0,
                    fixed: true,
                    type: this.data.schema.title,
                    hlink: "#resource/managed/" + this.data.baseObject + "/edit/" + details._id
                }];
                
                this.$el.find("#roundRelationshipGraph").empty();
                /*
                 * loop over all the relationship props and add nodes for all the property values
                 * or in the case of arrays of relationships all the array item property values
                 */
                _.each(this.data.relationshipProps, function (prop) {
                    var propKey;
                    
                    if (details[prop.propName]) {
                        if (details[prop.propName].length || (prop.type === "relationship" && !_.isEmpty(details[prop.propName]))) {
                            /*
                             * if the none of the values in the array of relationships come from managed/whatever then do not add a titleNode
                             */
                            if (!(prop.type === "array" && _.reject(details[prop.propName], function (item) { return item._ref.indexOf("repo/internal") === 0; }).length === 0)) {
                                titleNodeId = "SN" + nodeCounter;
                                nodeSet.push({
                                    id: titleNodeId,
                                    sourceId: "mainNode",
                                    name: prop.title || prop.propName,
                                    type: prop.title || prop.propName,
                                    intermediateNode: true
                                });
                            }
                        }
                        
                        if (prop.type === "array") {
                            /*
                             * loop over all the the relationships in this property's data set
                             * then add a node for each of those relationships
                             */
                            _.each(details[prop.propName], function (item) {
                                addNode(prop, item, prop.items.resourceCollection);
                            });
                        } else {
                            addNode(prop, details[prop.propName], prop.resourceCollection);
                        }
                    }
                });

                /*
                 * loop over all the nodes that are not the mainNode and create a link for each
                 */
                _.each(nodeSet, function(item) {
                    linkSet.push({
                        sourceId: item.sourceId || "mainNode",
                        linkName: "",//item.type, thought this cluttered up the display
                        secondaryNode: (item.sourceId) ? true : false,
                        targetId: item.id,
                        linkType: item.type
                    });
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
                */
                this.updateRoundChart(focalNodeID, nodeSet, linkSet, "#roundRelationshipGraph", "colorScale20c");
            },
            /*
             * builds a query to get back all the fields needed to display a
             * resource's relationship chart
             * this function is called when the user clicks on a circle and the widget
             * is refreshed with the view of the new baseObject
             */
            getResourceRenderChart : function (id) {
                var resourceUrl,
                    fields = "?_fields=*";
                
                if (id) {
                    resourceUrl = "managed/" + this.data.baseObject + "/" + id;
                }
                
                if (this.data.resourceUrl) {
                    resourceUrl = this.data.resourceUrl;
                }
                _.each(this.data.relationshipProps, _.bind(function (prop) {
                
                    fields += "," + prop.propName + "/*";

                    if (this.data.displaySubRelationships === "true") {
                        fields += this.addResourceCollectionProps(prop);
                    }
                }, this));
                
                resourceUrl += fields;
                
                ResourceDelegate.getResource(resourceUrl).then(_.bind(function (response) {
                    this.setupRelationshipChart(response);
                }, this));
            },
            /*
             * returns a list of properties from schema properties with type relationship or type array with itemtype relationship
             */
            getRelationshipProps : function (properties) {
                return _.chain(properties)
                            .map(function (val, key) {
                                return _.extend(val, { propName: key });
                            })
                            .filter(function (prop) {
                                return prop.type === "relationship" || (prop.type === "array" && prop.items.type === "relationship");
                            })
                            .value();
            },
            /*
             * builds up a list of resource collection properties to add to the _fields property of a read or query
             */
            addResourceCollectionProps : function (prop) {
                var resourceCollections,
                    fields = ""; 
                
                if (prop.items) {
                    resourceCollections = prop.items.resourceCollection;
                } else {
                    resourceCollections = prop.resourceCollection;
                }
                _.each(resourceCollections , _.bind(function (resourceCollection) {
                    var schema = _.where(this.data.schema.allSchemas, { name : resourceCollection.path.split("/")[1] }),
                        relProps = [];
                    
                    if(schema[0]) {
                        relProps = this.getRelationshipProps(schema[0].schema.properties);
                    }
                    
                    _.each(relProps, function (relProp) {
                        if (prop.type === "relationship") {
                            fields += "," + prop.propName + "/" + relProp.propName + "/*";
                        } else {
                            fields += "," + prop.propName + "/*/" + relProp.propName + "/*";
                        }
                    });
                }, this));
                
                return fields;
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
            /*
             * this function runs when checkboxes in the legend are clicked 
             * to turn on and off different sets of relationships
             */
            dataTypeToggle: function () {
                var thisObject = d3.select(this).select("input"),
                    type_value = thisObject.attr("type_value"),
                    checked = $(thisObject[0]).prop('checked'),
                    id = thisObject.attr("id"),
                    nodes = $('.node[type_value|="' + type_value + '"]'),//circles
                    links = $('.linkType-' + type_value.replace(/ /g, "_")),//lines connected to main node
                    sublinks = $("[class|='linkType-" + type_value.replace(/ /g, "_") + "']");//lines connected to sub nodes
                
                if (checked) {
                    nodes.show();
                    links.show();
                    sublinks.show();
                } else {
                    nodes.hide();
                    links.hide();
                    sublinks.hide();
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
                    centerNodeSize = 30, //Pixel size of circles
                    nodeSize = 8, //Pixel size of circles
                    color_hash = [],
                    _this = this,
                    sortedKeys,
                    svgCanvas,
                    node_hash,
                    force,
                    link,
                    node,
                    linkText, 
                    mainNodeType;

                //Create a hash that maps colors to types
                nodeSet.forEach(function(d) {
                    if (d.id === "mainNode") {
                        mainNodeType = d.type;
                    }
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
                                return (25 + width) * 0.25;
                            } else {
                                return (25 + height) * 0.25;
                            }
                        } else {
                            return 100;
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
                    .style("stroke", "#ccc")
                    .attr("x1", function(d) { return d.source.x; })
                    .attr("y1", function(d) { return d.source.y; })
                    .attr("x2", function(d) { return d.target.x; })
                    .attr("y2", function(d) { return d.target.y; })
                    .attr("class", function (d) { return "linkType-" + d.linkType.replace(/ /g, "_") + " link"; } );

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
                    .call(force.drag);

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
                    .call(force.drag)
                    .on("dblclick", function(d) {
                        if (d.id !== "mainNode" && !d.intermediateNode) {
                            _this.data.baseObject = d.refreshSettings.baseObject;
                            _this.data.searchFields = d.refreshSettings.searchFields;
                            _this.data.resourceUrl = d.resourceUrl;
                            _this.widgetRender({});
                        }
                        return false;
                    });

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
                    })
                    .on("click", function(d) {
                        location.hash = d.hlink;
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
                
                //add checkboxes for each data type
                svgCanvas.selectAll("input.legend_checkbox")
                    .data(sortedKeys).enter()
                    .append("foreignObject")
                    .attr("x", -1 * (width/2 - 1))
                    .attr("y", function(d, i) { return (i * 20 - height/7 * 3 + 8); } )
                    .attr("width", 10)
                    .attr("height", 10)
                    .html(function (d) {
                        var str = d,
                            strippedString = str.replace(/ /g, "_"),
                            id = "legendCheckbox-" + strippedString,
                            type_value = d,
                            parent_type = "";
                        
                        if (type_value.split("-").length > 1) {
                            parent_type = type_value.split("-")[0];
                        }
                        
                        if (d === mainNodeType) {
                            return "";
                        }
                        
                        return "<input type='checkbox' class='legendCheckbox' id='" + id + "' type_value='" + type_value + "' parent_type='" + parent_type + "' checked/>";
                    })
                    .on("click", _this.dataTypeToggle);

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
                    .enter()
                    .append("svg:a")
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