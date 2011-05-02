/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 *
 * $Id$
 */

package org.forgerock.openidm.provisioner.openicf.commons;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class Id {
    private LinkedList<String> elements;
    private boolean systemObject = false;

    public Id(String id) {
        parseId(id);
    }

    public Id(String id, String newId) {
        parseId(id);
        if (null == getLocalId()) {
            elements.add(newId);
        } else {
            elements.set(elements.size() - 1, newId);
        }
    }


    private void parseId(String id) {
        Pattern MY_PATTERN = Pattern.compile("/([^/]*)");
        elements = new LinkedList<String>();
        Matcher m = MY_PATTERN.matcher(id);
        boolean afterBaseContext = false;
        while (m.find()) {
            String s = m.group(0);
            if (!afterBaseContext) {
                if ("/system".equals(s)) {
                    afterBaseContext = true;
                    systemObject = true;
                } else if ("/managed".equals(s)) {
                    afterBaseContext = true;
                }
            } else {
                elements.add(s.substring(1));
            }

        }
    }


    public String getLocalId() {
        if (systemObject && elements.size() > 2) {
            return elements.get(2);
        } else if (!systemObject && elements.size() > 1) {
            return elements.get(1);
        }
        return null;
    }

    public String getObjectType() {
        if (systemObject && elements.size() > 1) {
            return elements.get(1);
        } else if (!systemObject && !elements.isEmpty()) {
            return elements.get(0);
        }
        return null;
    }

    public List<String> getElements() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public String toString() {
        try {
        URI base = new URI("/");
            for (String element: elements) {
                base.resolve(element);
                base.toString();
            }
        } catch (URISyntaxException e){

        }
        return "/URISyntaxException";
    }
}
