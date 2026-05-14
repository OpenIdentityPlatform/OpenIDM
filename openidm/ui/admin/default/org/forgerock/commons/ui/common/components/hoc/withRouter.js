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

/**
 * @module org/forgerock/commons/ui/common/components/hoc/withRouter
 */
define(["lodash", "react", "org/forgerock/commons/ui/common/main/Router"], function (_, React, Router) {
    function getDisplayName(WrappedComponent) {
        return WrappedComponent.displayName || WrappedComponent.name || "Component";
    }

    /**
     * A HoC (higher-order component) that wraps another component to provide `this.props.router`.
     * Pass in your component and it will return the wrapped component.
     * <p/>
     * Accompanying prop type can be found within the
     * {@link module:org/forgerock/commons/ui/common/components/hoc/withRouterPropType|withRouterPropType} module.
     * @param  {ReactComponent} WrappedComponent Component to wrap
     * @returns {ReactComponent} Wrapped component
     * @example
     * import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter"
     *
     * class MyReactComponent extends Component { ... }
     *
     * export default withRouter(MyReactComponent)
     */
    var exports = function exports(WrappedComponent) {
        var WithRouter = React.createClass({
            render: function render() {
                var route = Router.currentRoute,
                    params = Router.extractParameters(route, Router.getURIFragment()),
                    paramsWithDefaults = Router.applyDefaultParameters(route, params),
                    router = {
                    /**
                     * TODO: params should be a key/value pair provided by the router, however the router provides
                     * an array and we must address params in thier position. A router change is required to provide
                     * named parameters making the views less fragile. http://tiny.cc/8wgk8x
                     */
                    params: _.map(paramsWithDefaults, function (param) {
                        if (!param) {
                            return "";
                        }

                        return decodeURIComponent(param);
                    })
                };

                return React.createElement(WrappedComponent, _.extend({}, this.props, { router: router }));
            }
        });

        WithRouter.displayName = "withRouter(" + getDisplayName(WrappedComponent) + ")";
        WithRouter.WrappedComponent = WrappedComponent;

        return WithRouter;
    };

    return exports;
});
