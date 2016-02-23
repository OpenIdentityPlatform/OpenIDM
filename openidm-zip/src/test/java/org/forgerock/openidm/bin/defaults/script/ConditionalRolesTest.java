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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.bin.defaults.script;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.javascript.RhinoScriptEngineFactory;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.script.scope.Function;
import org.forgerock.script.scope.Parameter;
import org.forgerock.script.source.DirectoryContainer;
import org.forgerock.services.context.RootContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.script.ScriptException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests conditionalRoles.js
 */
public class ConditionalRolesTest {
    private static final String ID = "_id";
    private static final String REV = "_rev";
    private static final String MAIL = "mail";
    private static final String GIVEN_NAME = "givenName";
    private static final String SN = "sn";
    private static final String DESCRIPTION = "description";
    private static final String USER_NAME = "userName";
    private static final String TELEPHONE = "telephoneNumber";
    private static final String STATUS = "accountStatus";
    private static final String CONDITIONAL_GRANTS = "conditionalGrants";
    private static final String DIRECT_GRANTS = "directGrants";
    private static final String ROLES = "roles";
    private static final String REF = "_ref";
    private static final String REF_PROPERTIES = "_refProperties";
    private static final String GRANT_TYPE = "_grantType";
    private static final String CONDITIONAL = "conditional";
    private static final String ROLE_GRANT_ID = "566";
    private static final String NAME = "name";
    private static final String CONDITION = "condition";
    private ScriptRegistryImpl scriptRegistry;

    interface RoleState {
        String getRoleCondition();
        String getRoleDescription();
    }

    enum Role implements RoleState {
        NONE {
            public String getRoleCondition() {
                return "false";
            }

            public String getRoleDescription() {
                return "no condition";
            }
        }, MAIL_CO_COM {
            public String getRoleCondition() {
                return "/mail co 'com'";
            }

            public String getRoleDescription() {
                return "does mail contain com";
            }

        }, MAIL_CO_EXAMPLE {
            public String getRoleCondition() {
                return "/mail co 'example'";
            }

            public String getRoleDescription() {
                return "does mail contain example";
            }

        }, MAIL_CO_GOOGLE {
            public String getRoleCondition() {
                return "/mail co 'google'";
            }

            public String getRoleDescription() {
                return "does mail contain google";
            }
        }};

    enum GrantType { DIRECT, CONDITIONAL };

    @BeforeClass
    public void initScriptRegistry() throws IOException, URISyntaxException, ScriptException {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(getLanguageName(), getConfiguration());
        scriptRegistry = new ScriptRegistryImpl(configuration,
                Collections.<ScriptEngineFactory>singleton(new RhinoScriptEngineFactory()), null, null);
        URL scriptContainer = getScriptContainer("/bin/defaults/script/");
        Assert.assertNotNull(scriptContainer);
        scriptRegistry.addSourceUnit(new DirectoryContainer("bin/defaults/script", scriptContainer));
        URL lodashContainer = getScriptContainer("/scriptLibs/");
        Assert.assertNotNull(lodashContainer);
        scriptRegistry.addSourceUnit(new DirectoryContainer("scriptLibs", lodashContainer));
        Map<String, Object> console = new HashMap<>();
        console.put("log", new Function<Void>() {
            @Override
            public Void call(Parameter scope, Function<?> callback, Object... arguments) throws ResourceException, NoSuchMethodException {
                if (arguments.length > 0) {
                    if (arguments[0] instanceof String) {
                        System.out.println((String) arguments[0]);
                    }
                }
                return null;
            }
        });
        scriptRegistry.put("console", console);
    }

    protected Map<String, Object> getConfiguration() {
        Map<String, Object> configuration = new HashMap<>();
        return configuration;
    }

    protected String getLanguageName() {
        return RhinoScriptEngineFactory.LANGUAGE_NAME;
    }

    protected URL getScriptContainer(String name) {
        return ConditionalRolesTest.class.getResource(name);
    }

    public ScriptRegistry getScriptRegistry() {
        return scriptRegistry;
    }


    @DataProvider(name="UserConditionalRolesUpdate")
    Object[][] resourcePaths() {
        return new Object[][] {
                { baseUser(), systemConditionalRoles(), getUserRoleGrants(null, null) },
                { userWithRoleGrant(Role.MAIL_CO_EXAMPLE, GrantType.CONDITIONAL), systemConditionalRoles(), getUserRoleGrants(Role.MAIL_CO_EXAMPLE, null) },
                { userWithRoleGrant(Role.MAIL_CO_COM, GrantType.DIRECT), systemConditionalRoles(), getUserRoleGrants(null, Role.MAIL_CO_COM)}
        };
    }

//    @Test(dataProvider = "UserConditionalRolesUpdate")
    public void testUserConditionalRoles(JsonValue user, JsonValue existingConditionalRoles, JsonValue userRoleGrants) throws ScriptException {
        JsonValue scriptName = json(object(
                field("type", "text/javascript"),
                field("source", "require('roles/conditionalRoles').evaluateConditionalRoles(user, rolesPropName, " +
                        "existingConditionalRoles, userRoleGrants);")
        ));
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(scriptName);
        assertThat(scriptEntry).isNotNull();
        Script script = scriptEntry.getScript(new RootContext());
        script.put("user", user);
        script.put("rolesPropName", ROLES);
        script.put("existingConditionalRoles", existingConditionalRoles);
        script.put("userRoleGrants", userRoleGrants);
        script.eval();
        assertThat(roleGrantsCorrect(user)).isTrue();
    }

    private static boolean userContainsRole(JsonValue user, String... roleRefs) {
        List<Map> roles = user.get(ROLES).asList(Map.class);
        for (Map role : roles) {
            for (String roleRef : roleRefs) {
                if (role.get(REF).toString().contains(roleRef)) {
                    return true;
                }
            }
        }
        return false;
    }

    private JsonValue baseUser() {
        int ndx = 5;
        return json(object(
                field(ID, "user" + ndx),
                field(REV, "1"),
                field(MAIL, "user" + ndx + "@example.com"),
                field(GIVEN_NAME, "fooUser" + ndx),
                field(SN, "user" + ndx + "sn"),
                field(DESCRIPTION, "description of fooUser " + ndx),
                field(USER_NAME, "user" + ndx + "@example.com"),
                field(TELEPHONE, "1234" + ndx),
                field(STATUS, "active")
        ));
    }

    private JsonValue userWithRoleGrant(Role role, GrantType grantType) {
        JsonValue baseUser = baseUser();
        if (Role.NONE != role) {
            baseUser.put(ROLES, getRoleGrant(role, grantType));
        }
        return baseUser;
    }

    private Map<String, Object> getRoleGrant(Role role, GrantType grantType) {
        return json(object(
                field(REF, "managed/role/" + role.name()),
                field(REF_PROPERTIES, getRoleGrantRefProperties(grantType)
        ))).asMap();
    }

    private Map<String, Object> getRoleGrantRefProperties(GrantType grantType) {
        JsonValue refProperties = json(object(
                field(REV, 1),
                field(ID, ROLE_GRANT_ID)));
        if (GrantType.CONDITIONAL == grantType) {
            refProperties.add(GRANT_TYPE, CONDITIONAL);
        }
        return refProperties.asMap();
    }

    private Map<String, Object> getRole(Role role) {
        return json(object(
                field(ID, role.name()),
                field(REV, "1"),
                field(NAME, "role " + role.name()),
                field(CONDITION, role.getRoleCondition()),
                field(DESCRIPTION, role.getRoleDescription()))).asMap();
    }

    private boolean roleGrantsCorrect(JsonValue user) {
        return userContainsRole(user, Role.MAIL_CO_COM.name(), Role.MAIL_CO_EXAMPLE.name()) &&
                !userContainsRole(user, Role.MAIL_CO_GOOGLE.name()) ;
    }

    private JsonValue getUserRoleGrants(Role conditionalRole, Role directRole) {
        return json(object(
                field(CONDITIONAL_GRANTS, conditionalRole != null ?
                        Collections.singletonList(getRoleGrant(conditionalRole, GrantType.CONDITIONAL)) : Collections.emptyList()),
                field(DIRECT_GRANTS, directRole != null ?
                        Collections.singletonList(getRoleGrant(directRole, GrantType.DIRECT)) : Collections.emptyList())));
    }

    private JsonValue systemConditionalRoles() {
        List<Map<String, Object>> roles = new ArrayList<>(3);
        roles.add(getRole(Role.MAIL_CO_COM));
        roles.add(getRole(Role.MAIL_CO_EXAMPLE));
        roles.add(getRole(Role.MAIL_CO_GOOGLE));
        return new JsonValue(roles);
    }
}
