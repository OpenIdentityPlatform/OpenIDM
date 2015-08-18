/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2014 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.orientdb.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.server.plugin.OServerPlugin;
import java.util.Map;
import org.forgerock.json.JsonValue;
import org.testng.annotations.*;
import static org.testng.Assert.*;

public class EmbeddedOServerServiceTest {
    
    @AfterTest
    public void automaticBackupHandlerConfigTest() throws Exception {
        String automaticBackupConfig = "{"
                + "\"dbUrl\" : \"plocal:./target/backuptestdb\","
                + "\"embeddedServer\" : {"
                + "    \"automaticBackup\" : {"
                + "        \"enabled\" : true,"
                + "        \"targetDirectory\" : \"./target/backups\","
                + "        \"targetFile\" : \"${DBNAME}-${DATE:yyyyMMddHHmmss}.zip\","
                + "        \"firsttime\" : \"23:59:00\","
                + "        \"delay\" : \"1d\""
                + "    },"
                + "    \"enabled\" : true,"
                + "    \"overrideConfig\" : {"
                + "        \"network\" : {"
                + "            \"listeners\" : {"
                + "                \"binary\" : {"
                + "                    \"ipAddress\" : \"0.0.0.0\","
                + "                    \"portRange\" : \"2424-3434\""
                + "                },"
                + "                \"http\" : {"
                + "                    \"ipAddress\" : \"127.0.0.1\","
                + "                    \"portRange\" : \"2480-3480\""
                + "                }"
                + "            }"
                + "        }"
                + "    }"
                + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JsonValue config = new JsonValue((Map)mapper.readValue(automaticBackupConfig, Map.class));
        EmbeddedOServerService embeddedOServerService = new EmbeddedOServerService();
        embeddedOServerService.activate(config);
        assertNotNull(embeddedOServerService.orientDBServer);
        OServerPlugin plugin = embeddedOServerService.orientDBServer.getPlugin("automaticBackup");
        assertNotNull(plugin);
        embeddedOServerService.deactivate();
    }
}