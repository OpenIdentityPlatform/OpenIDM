/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.maintenance.impl;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.forgerock.json.resource.Router.uriTemplate;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resources;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.resource.Router;
import org.forgerock.http.routing.RoutingMode;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentInstance;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test the Maintenance Service
 */
public class MaintenanceServiceTest {
    
    private MaintenanceService maintenanceService = new MaintenanceService();
    
    private Router router = new Router();
    private final Connection connection = Resources.newInternalConnection(router);

    @BeforeMethod
    public void setUp() {
        List<Component> testComponents = new ArrayList<Component>();
        testComponents.add(new TestComponent(1, "test.component.one"));
        testComponents.add(new TestComponent(2, "test.component.two"));
        testComponents.add(new TestComponent(3, "test.component.three"));
        maintenanceService.scrService = new TestScrService(testComponents);
        maintenanceService.setMaintenanceModeComponents(new String[] {
                "test.component.one",
                "test.component.two"
        });
        router.addRoute(RoutingMode.EQUALS, uriTemplate("maintenance"), maintenanceService);
    }
    
    @Test
    public void testMaintenanceMode() throws Exception {
        ActionRequest enableAction = Requests.newActionRequest("maintenance", "enable");
        assertThat(connection.action(new RootContext(), enableAction).getJsonContent()
                .get("maintenanceEnabled").asBoolean());
        assertThat(maintenanceService.scrService.getComponent(1).getState() == Component.STATE_DISABLED);
        assertThat(maintenanceService.scrService.getComponent(2).getState() == Component.STATE_DISABLED);
        assertThat(maintenanceService.scrService.getComponent(3).getState() == Component.STATE_ACTIVE);
        
        ActionRequest disableAction = Requests.newActionRequest("maintenance", "disable");
        assertThat(!connection.action(new RootContext(), disableAction).getJsonContent()
                .get("maintenanceEnabled").asBoolean());
        assertThat(maintenanceService.scrService.getComponent(1).getState() == Component.STATE_ACTIVE);
        assertThat(maintenanceService.scrService.getComponent(2).getState() == Component.STATE_ACTIVE);
        assertThat(maintenanceService.scrService.getComponent(3).getState() == Component.STATE_ACTIVE);
    }


    /**
     * A testing implementation of {@link ScrService}.
     */
    class TestScrService implements ScrService {

        public List<Component> components = new ArrayList<Component>();
        
        public TestScrService(List<Component> components) {
            this.components = components;
        }

        @Override
        public Component[] getComponents() {
            return components.toArray(new Component[components.size()]);
        }

        @Override
        public Component getComponent(long componentId) {
            for (Component component : components) {
                if (component.getId() == componentId) {
                    return component;
                }
            }
            return null;
        }

        @Override
        public Component[] getComponents(String componentName) {
            List<Component> componentsToReturn = new ArrayList<Component>();
            for (Component component : components) {
                if (component.getName().equals(componentName)) {
                    componentsToReturn.add(component);
                }
            }
            if (componentsToReturn.size() > 0) {
                return componentsToReturn.toArray(new Component[componentsToReturn.size()]);
            }
            return null;
        }

        @Override
        public Component[] getComponents(Bundle bundle) {
            List<Component> componentsToReturn = new ArrayList<Component>();
            for (Component component : components) {
                if (component.getBundle().getBundleId() == bundle.getBundleId()) {
                    componentsToReturn.add(component);
                }
            }
            if (componentsToReturn.size() > 0) {
                return componentsToReturn.toArray(new Component[componentsToReturn.size()]);
            }
            return null;
        }
    }
    
    class TestComponent implements Component {
        
        private long id;
        private String name;
        private int state;
        
        TestComponent(long id, String name) {
            this.id = id;
            this.name = name;
            state = Component.STATE_ACTIVE;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getState() {
            return state;
        }

        @Override
        public Bundle getBundle() {
            return null;
        }

        @Override
        public String getFactory() {
            return null;
        }

        @Override
        public boolean isServiceFactory() {
            return false;
        }

        @Override
        public String getClassName() {
            return null;
        }

        @Override
        public boolean isDefaultEnabled() {
            return false;
        }

        @Override
        public boolean isImmediate() {
            return false;
        }

        @Override
        public String[] getServices() {
            return null;
        }

        @Override
        public Dictionary getProperties() {
            return null;
        }

        @Override
        public Reference[] getReferences() {
            return null;
        }

        @Override
        public ComponentInstance getComponentInstance() {
            return null;
        }

        @Override
        public String getActivate() {
            return null;
        }

        @Override
        public boolean isActivateDeclared() {
            return false;
        }

        @Override
        public String getDeactivate() {
            return null;
        }

        @Override
        public boolean isDeactivateDeclared() {
            return false;
        }

        @Override
        public String getModified() {
            return null;
        }

        @Override
        public String getConfigurationPolicy() {
            return null;
        }

        @Override
        public void enable() {
            state = Component.STATE_ACTIVE;
        }

        @Override
        public void disable() {
            state = Component.STATE_DISABLED;
        }
        
    }
}
