/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.recon;

/**
 * Configuration constants for testing. Here are embedded valid configurations
 * for {@link org.forgerock.openidm.recon.impl.ReconciliationConfiguration} and
 * {@link org.forgerock.openidm.sync.impl.MappingConfiguration}
 */
public class ConfigurationConstants {

    /**
     * Valid {@link org.forgerock.openidm.recon.impl.ReconciliationConfiguration}
     *
     * @return recon config string
     */
    public static String reconciliationTestConfig() {
        return "{\n" +
                "    \"reconciliation\" : {\n" +
                "\n" +
                "        \"reconciliation-configurations\" : [\n" +
                "            {\n" +
                "                \"name\" : \"a policy configuration name\",\n" +
                "                \"sourceObject\" : \"a system object ref\",\n" +
                "                \"targetObject\" : \"a system object ref\",\n" +
                "                \"policyName\" : \"a policy name\",\n" +
                "                \"reconciliationType\" : \"full|incremental\",\n" +
                "                \"enabled\" : false\n" +
                "            }\n" +
                "        ],\n" +
                "\n" +
                "        \"policies\" : [\n" +
                "\n" +
                "            {\n" +
                "                \"name\" : \"a policy name\",\n" +
                "                \"batchRecords\" : 100,\n" +
                "                \"errorsBeforeFailing\" : 50,\n" +
                "                \"serviceAccount\" : \"reconUser\",\n" +
                "                \"detectNativeChangesToAccounts\" : false,\n" +
                "                \"preReconciliationScript\" : \"script for workflow\",\n" +
                "                \"perObjectScript\" : \"script for workflow\",\n" +
                "                \"postReconciliationScript\" : \"script for workflow\",\n" +
                "                \"correlationScript\" : \"script for correlation\",\n" +
                "                \"correlationQuery\" : \"query id \",\n" +
                "                \"filterScript\" : \"filter script\",\n" +
                "\n" +
                "                \"situations\" : {\n" +
                "                    \"confirmed\" : \"script\",\n" +
                "                    \"deleted\" : \"script\",\n" +
                "                    \"unmatched\" : \"script\",\n" +
                "                    \"unassigned\" : \"script\"\n" +
                "                },\n" +
                "\n" +
                "                \"actionMap\" : {\n" +
                "                    \"testAction\" : \"org.forgerock.openidm.recon.TestAction\"\n" +
                "                }\n" +
                "\n" +
                "            }\n" +
                "\n" +
                "        ]\n" +
                "\n" +
                "    }\n" +
                "}";
    }

    /**
     * Valide {@link org.forgerock.openidm.sync.impl.MappingConfiguration}
     *
     * @return object synchronization string
     */
    public static String objectSynchronizationTestConfig() {
        return "{\n" +
                "    \"objectSynchronization\" : {\n" +
                "\n" +
                "        \"mappings\": [\n" +
                "\n" +
                "            {\n" +
                "                \"name\" : \"a map name\",\n" +
                "                \"sourceObject\" : \"a system object path\",\n" +
                "                \"targetObject\" : \"a system object\",\n" +
                "                \"synchrony\" : \"synchronous\",\n" +
                "                \"qualifier\" : \"script\",\n" +
                "                \"query\" : \"script or string ??? old remove\",\n" +
                "                \"namedQuery\" : \"a named query\",\n" +
                "\n" +
                "                \"propertyMappings\" : [\n" +
                "                    {\n" +
                "                        \"sourcePath\" : \"source_property_path\",\n" +
                "                        \"targetPath\" : \"target_property_path\" ,\n" +
                "                        \"script\" : {\n" +
                "    \"type\": \"text/javascript\",\n" +
                "    \"source\": \"println(sourcevalue)\",\n" +
                "    \"sharedScope\": true \n" +
                "}\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"sourcePath\" : \"source_property_path\",\n" +
                "                        \"targetPath\" : \"target_property_path\",\n" +
                "                        \"script\" : {\n" +
                "    \"type\": \"text/javascript\",\n" +
                "    \"source\": \"println(sourcevalue)\",\n" +
                "    \"sharedScope\": true \n" +
                "}\n" +
                "                    }\n" +
                "                ]\n" +
                "\n" +
                "            }\n" +
                "\n" +
                "        ]\n" +
                "\n" +
                "    }\n" +
                "}";
    }
}
