/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.forgerock.openidm.http;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Interface to configure security
 *
 * @author aegloff
 */
public interface SecurityConfigurator {

    /**
     * Let the security configurator apply its configuration 
     * @param context the component context of the main bundle
     * @param httpContext the shared http context to configure
     */
    void activate(HttpService httpService, HttpContext httpContext,  ComponentContext context);

    /**
     * Deactivate security configurators if present to cleanup
     * @param context the component context of the main bundle
     * @param httpContext the shared http context to configure
     */
    void deactivate(HttpService httpService, HttpContext httpContext,  ComponentContext context);
}
