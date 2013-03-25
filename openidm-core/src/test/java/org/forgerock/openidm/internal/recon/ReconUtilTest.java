/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.internal.recon;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Resource;
import org.testng.Assert;
import org.testng.annotations.Test;
import static org.forgerock.openidm.internal.recon.ReconUtil.*;

import java.util.HashMap;
import java.util.Map;


/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class ReconUtilTest {
    @Test
    public void testTriplet() throws Exception {
        Resource source = new Resource("USER1", "0", new JsonValue(null));

        Resource target = new Resource("User2", "1", new JsonValue(new HashMap<String, Object>(1)));

        Map<String, Object> linkMap = new HashMap<String, Object>(3);
        linkMap.put(LINK_SOURCE_ID_FIELD, "user1");
        linkMap.put(LINK_TARGET_ID_FIELD, "User2");

        Triplet triplet = Triplet.fromLink(linkMap).triplet();
        triplet.source().setResource(source,false);
        triplet.target().setResource(target, true);

        Assert.assertEquals(triplet.source().getId(),"user1");
        Assert.assertEquals(triplet.target().getId(),"User2");
        Assert.assertEquals(triplet.source().getId(), triplet.link().sourceId());
        Assert.assertEquals(triplet.target().getId(), triplet.link().targetId());
        Assert.assertTrue(triplet.link().linkType() == null);

        System.out.println(tripletToJSON(triplet));
    }
}
