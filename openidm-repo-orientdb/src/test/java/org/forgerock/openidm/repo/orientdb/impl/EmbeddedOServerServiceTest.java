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


import com.orientechnologies.orient.server.plugin.OServerPlugin;
import java.util.HashMap;
import java.util.Map;
import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.*;
import static org.testng.Assert.*;

public class EmbeddedOServerServiceTest {
    
    @AfterTest
    public void automaticBackupHandlerConfigTest() throws Exception {
        Map automaticBackupMap = new HashMap();
        automaticBackupMap.put("enabled", true);
        automaticBackupMap.put("targetDirectory", "./target/backups");
        automaticBackupMap.put("targetFile", "${DBNAME}-${DATE:yyyyMMddHHmmss}.zip");
        automaticBackupMap.put("firsttime", "23:59:00");
        automaticBackupMap.put("delay", "1d");

        Map embeddedServerMap = new HashMap();
        embeddedServerMap.put("enabled", true);
        embeddedServerMap.put("automaticBackup", automaticBackupMap);
        
        Map map = new HashMap();
        map.put("dbUrl", "plocal:./target/backuptestdb");
        map.put("embeddedServer", embeddedServerMap);
        
        JsonValue config = new JsonValue(map);
        EmbeddedOServerService embeddedOServerService = new EmbeddedOServerService();
        embeddedOServerService.activate(config);
        assertNotNull(embeddedOServerService.orientDBServer);
        OServerPlugin plugin = embeddedOServerService.orientDBServer.getPlugin("automaticBackup");
        assertNotNull(plugin);
        embeddedOServerService.deactivate();
    }
}
