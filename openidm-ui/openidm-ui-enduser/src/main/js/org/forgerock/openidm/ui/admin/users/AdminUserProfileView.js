/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define, $, form2js, _, js2form, document, window, d3 */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/users/AdminUserProfileView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/components/ConfirmationDialog",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/user/delegates/CountryStateDelegate",
    "org/forgerock/openidm/ui/user/delegates/RoleDelegate",
    "org/forgerock/openidm/ui/admin/linkedView/LinkedView",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate"
], function(AbstractView,
            validatorsManager,
            uiUtils,
            userDelegate,
            eventManager,
            constants,
            conf,
            confirmationDialog,
            router,
            countryStateDelegate,
            roleDelegate,
            LinkedView,
            ResourceDelegate) {
    var AdminUserProfileView = AbstractView.extend({
        template: "templates/admin/AdminUserProfileTemplate.html",
        events: {
            "click input[name=saveButton]": "formSubmit",
            "click input[name=deleteButton]": "deleteUser",
            "click input[name=backButton]": "back",
            "onValidate": "onValidate",
            "change select[name='country']": "loadStates",
            "change select[name='stateProvince']": "selectState"
        },

        formSubmit: function(event) {
            event.preventDefault();

            if(validatorsManager.formValidated(this.$el)) {
                var data = form2js(this.$el.find("form")[0], '.', false),
                    self = this,
                    oldUserName;
                delete data.lastPasswordSet;
                delete data.oldEmail;
                oldUserName = data.oldUserName;
                delete data.oldUserName;

                if(data.manager) {
                    data.manager = this.resourcePath +"/" +data.manager;
                }

                data.roles = this.$el.find("input[name=roles]:checked").map(function(){return $(this).val();}).get();

                userDelegate.patchUserDifferences(this.editedUser, data, _.bind(function() {
                    if(oldUserName === conf.loggedUser.userName && data.userName !== conf.loggedUser.userName) {
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
                        eventManager.sendEvent(constants.EVENT_LOGOUT);
                        return;
                    }

                    userDelegate.getForUserName(data.userName, _.bind(function(user) {
                        self.editedUser = user;
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
                        router.routeTo(router.configuration.routes.adminUserProfile, {args: [data.userName], trigger : true});

                        this.linkedView.render({"id": user._id});
                        self.reloadData();
                    },this));

                },this), null, null, { "forbidden": { status: "403", event: constants.EVENT_USER_UPDATE_POLICY_FAILURE } });
            }
        },

        editedUser: {},

        render: function(userName, callback) {
            userName = userName[0].toString();
            this.data.host = constants.host;

            $.when(
                userDelegate.getForUserName(userName),
                roleDelegate.getAllRoles(),
                ResourceDelegate.getSchema(["managed", "user"])
            ).then(
                _.bind(function(user, roles, schema) {
                    var managedRoleMap = _.chain(roles.result)
                        .map(function (r) { return [r._id, r.properties.name || r._id]; })
                        .object()
                        .value();

                    this.editedUser = user;
                    this.schema = schema;

                    this.data.user = user;
                    this.data.roles = _.extend({}, conf.globalData.userRoles, managedRoleMap);
                    this.data.profileName = user.givenName + ' ' + user.sn;

                    if(schema.properties.manager) {
                        this.resourcePath =  schema.properties.manager.resourceCollection.path;
                        this.searchableManagerFields = schema.properties.manager.resourceCollection.query.fields;


                        ResourceDelegate.searchResource('manager sw "' +this.resourcePath +"/" +this.editedUser._id +'"', this.resourcePath).then(_.bind(function(reports) {
                            this.data.reports = reports.result;

                            if (this.editedUser.manager) {
                                ResourceDelegate.readResource("/openidm/" + this.resourcePath, this.editedUser.manager.replace(this.resourcePath + "/", "")).then(_.bind(function (manager) {
                                    this.completeRender(user, roles, schema, userName, callback, manager);
                                }, this));
                            } else {
                                this.completeRender(user, roles, schema, userName, callback, null);
                            }
                        }, this));
                    } else {
                        this.resourcePath = "";
                        this.searchableManagerFields = [];

                        this.completeRender(user, roles, schema, userName, callback, "");
                    }


                }, this),

                function() {
                    eventManager.sendEvent(constants.ROUTE_REQUEST, { routeName: "404", trigger: false, args: [window.location.hash]} );
                }
            );
        },
        completeRender: function(user, roles, schema, userName, callback, manager) {
            this.parentRender(_.bind(function() {
                this.linkedView = new LinkedView();
                this.linkedView.element = "#linkedViewBody";

                this.$el.find("input[name=oldUserName]").val(this.editedUser.userName);
                validatorsManager.bindValidators(this.$el.find("form"), userDelegate.baseEntity + "/" + this.data.user._id, _.bind(function () {
                    this.reloadData();

                    this.$el.find("#manager").selectize({
                        valueField: '_id',
                        labelField: 'userName',
                        searchField: 'userName',
                        create: false,
                        preload: true,
                        render: {
                            item: function(item, escape) {
                                return '<div>' +
                                    '<span class="manager-title">' +
                                    '<span class="manager-fullname"><i class="fa fa-user"></i>' + escape(item.givenName) +' ' +escape(item.sn) + '<span class="manager-email caption"> (' + escape(item.mail) + ')</span></span>' +
                                    '</span>' +
                                    '</div>';
                            },
                            option: function(item, escape) {
                                return '<div>' +
                                    '<span class="manager-title">' +
                                    '<span class="manager-fullname"><i class="fa fa-user"></i>' + escape(item.givenName) +' ' +escape(item.sn) + '<span class="manager-username caption"> (' + escape(item.userName) + ')</span></span>' +
                                    '</span>' +
                                    '<span class="caption manager-email">' + escape(item.mail) + '</span>' +

                                    '</div>';
                            }
                        },
                        load: _.bind(function(query, callback) {
                            var queryFilter = null;

                            if (!query.length) {
                                return callback();
                            }

                            _.each(this.searchableManagerFields, function(field){
                                if(queryFilter === null) {
                                    queryFilter = field +' sw "' +query +'"';
                                } else {
                                    queryFilter = queryFilter + ' or ' +field +' sw "' +query +'"';
                                }
                            });

                            ResourceDelegate.searchResource(queryFilter, this.resourcePath).then(function(search) {
                                    callback(search.result);
                                },
                                function(){
                                    callback();
                                }
                            );

                        }, this)
                    });

                    if(manager) {
                        this.$el.find("#manager")[0].selectize.addOption(manager);
                        this.$el.find("#manager")[0].selectize.setValue(manager._id);
                    }

                    this.linkedView.render({"id": user._id}, callback);

                    $('#reportsHolder .table tbody tr[data-href]').bind("click", function(){
                        document.location = $(this).attr('data-href');
                    });


                    if(this.data.reports !== undefined && this.data.reports.length > 0){
                        this.loadTree();
                    }

                }, this));
            }, this));
        },
        loadTree: function() {
            var treeData = {
                    "name" : this.editedUser.givenName + " " +this.editedUser.sn,
                    "parent" : "null",
                    "url" : constants.host +"/openidmui/index.html#users/show/" +this.editedUser.userName +"/",
                    "children" : [

                    ]
                },
                margin = {
                    top: 20,
                    right: 120,
                    bottom: 20,
                    left: 120
                },
                width = 1024 - margin.right - margin.left,
                height = 500 - margin.top - margin.bottom,
                i = 0,
                tree = d3.layout.tree().size([height, width]),
                diagonal = d3.svg.diagonal().projection(function(d) {
                    return [d.y, d.x];
                }),
                svg = d3.select("#reportsGraphBody").append("svg")
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
                        .text(function(data) { return data.name; })
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

            _.each(this.data.reports, function(item){
                treeData.children.push({
                    "name" : item.givenName + " " +item.sn,
                    "parent" : "null",
                    "url" : constants.host +"/openidmui/index.html#users/show/" +item.userName +"/"
                });
            });

            root = treeData;

            update(root);
        },
        loadStates: function() {
            var country = this.$el.find('select[name="country"]').val();

            if(country) {
                this.$el.find("select[name='country'] > option:first").text("");

                countryStateDelegate.getAllStatesForCountry(country, _.bind(function(states) {
                    uiUtils.loadSelectOptions(states, $("select[name='stateProvince']"), true, _.bind(function() {
                        if(this.editedUser.stateProvince) {
                            this.$el.find("select[name='stateProvince'] > option:first").text("");
                            this.$el.find("select[name='stateProvince']").val(this.editedUser.stateProvince);
                        }
                    }, this));
                }, this));
            } else {
                this.$el.find("select[name='stateProvince']").emptySelect();
                this.$el.find("select[name='country'] > option:first").text($.t("common.form.pleaseSelect"));
                this.$el.find("select[name='stateProvince'] > option:first").text($.t("common.form.pleaseSelect"));
            }
        },

        selectState: function() {
            var state = $('#profile select[name="stateProvince"]').val();

            if(state) {
                this.$el.find("select[name='stateProvince'] > option:first").text("");
            } else {
                this.$el.find("select[name='stateProvince'] > option:first").text($.t("common.form.pleaseSelect"));
            }
        },

        reloadData: function() {
            js2form(this.$el.find("form")[0], this.editedUser);

            this.$el.find("input[name=saveButton]").val($.t("common.form.update"));
            this.$el.find("input[name=deleteButton]").val($.t("common.form.delete"));
            this.$el.find("input[name=backButton]").val($.t("common.form.back"));
            this.$el.find("input[name=oldUserName]").val(this.editedUser.userName);

            _.each(this.editedUser.roles, _.bind(function(v) {
                this.$el.find("input[name=roles][value='"+v+"']").prop('checked', true);
            }, this));

            validatorsManager.validateAllFields(this.$el.find("form"));

            countryStateDelegate.getAllCountries(_.bind(function(countries) {
                uiUtils.loadSelectOptions(countries, $("select[name='country']"), true, _.bind(function() {
                    if(this.editedUser.country) {
                        this.$el.find("select[name='country'] > option:first").text("");
                        this.$el.find("select[name='country']").val(this.editedUser.country);

                        this.loadStates();
                    }
                }, this));
            }, this));
        },

        deleteUser: function() {
            confirmationDialog.render("Delete user",
                $.t("openidm.ui.admin.users.AdminUserProfileView.profileWillBeDeleted", { postProcess: 'sprintf', sprintf: [this.editedUser.userName] }),
                $.t("common.form.delete"), _.bind(function() {

                    eventManager.sendEvent(constants.EVENT_PROFILE_DELETE_USER_REQUEST, {userId: this.editedUser._id});
                }, this));
        },

        back: function() {
            router.routeTo(router.configuration.routes.adminUsers, {trigger: true});
        }
    });

    return new AdminUserProfileView();
});