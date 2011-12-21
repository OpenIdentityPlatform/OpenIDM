/*
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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.filter;

// Java Standard Edition
import java.security.Principal;
import java.util.ArrayList;  
import java.util.List;  
import java.util.Enumeration;
import java.util.Collections;
  
import javax.servlet.http.HttpServletRequest;  
import javax.servlet.http.HttpServletRequestWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserWrapper extends HttpServletRequestWrapper {

    String username = null;
    String roles = null;
    HttpServletRequest origReq = null;

    public UserWrapper(String uname, String userRoles, HttpServletRequest req) {
        super(req);
        username = uname;
        roles = userRoles;
    }

    @Override
    public Principal getUserPrincipal() {
        return new Principal() {
            @Override
            public String getName() {
                return username;
            }
        };
    }

    @Override
    public boolean isUserInRole(String role) { 
        if (roles == null || roles.indexOf(role) == -1) {
            return false;
        } 
        return true;
    }

    @Override
    public String getRemoteUser() { 
        return username;
    }

    @Override
    public String getHeader(String name){
        if (name.equalsIgnoreCase("X-OpenIDM-Roles")) {
            return roles;
        } else if (name.equalsIgnoreCase("X-OpenIDM-Password")) {
            return null;
        } else {
            return ((HttpServletRequest)getRequest()).getHeader(name);
        } 
    }

    @Override
    public Enumeration getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        names.add("X-OpenIDM-Roles");
        names.remove("X-OpenIDM-Password");
        return Collections.enumeration(names);
    }
}
