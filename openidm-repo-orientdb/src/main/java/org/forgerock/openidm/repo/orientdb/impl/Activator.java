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
 */
package org.forgerock.openidm.repo.orientdb.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.storage.OStorage;

public class Activator implements BundleActivator {
     public static ODatabaseDocumentTx db;
 
     public void start(BundleContext context) {
         // Temporary example embedding DB 
         // For now output lifecycle status to the console via standard out 
         String dbURL = "local:./db/openidm";
         db = new ODatabaseDocumentTx(dbURL); 
         if (db.exists()) {
             System.out.println("Opening " + dbURL);
             db.open("admin", "admin"); 
         } else { 
             System.out.println("DB does not exist, creating " + dbURL);
             db.create(); 

             // Test creating a given 'class' / document type
             OSchema schema = db.getMetadata().getSchema();
             OClass user = schema.createClass("User", OStorage.CLUSTER_TYPE.PHYSICAL); 
             schema.save(); 
         }
         System.out.println("Repository Started");
     }

     public void stop(BundleContext context) {
         db.close();
         System.out.println("Repository Stopped");
     }
}