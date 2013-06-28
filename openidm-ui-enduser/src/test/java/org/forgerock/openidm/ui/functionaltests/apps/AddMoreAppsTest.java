package org.forgerock.openidm.ui.functionaltests.apps;

import javax.inject.Inject;

import org.forgerock.commons.ui.functionaltests.AbstractTest;
import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper.ElementType;
import org.forgerock.openidm.ui.functionaltests.helpers.*;
import org.forgerock.openidm.ui.functionaltests.helpers.ApplicationHelper.ApplicationState;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

public class AddMoreAppsTest extends AbstractTest{
	
	@Inject
	protected ApplicationHelper applicationHelper;

	@Test
	public void testAddAppWithRequiredAcceptance() {
		userHelper.createDefaultUser();
		userHelper.loginAsDefaultUser();
		router.goToAddMoreApps(true);
		router.assertUrl("#applications/addmore/");
		
		WebElement draggedElement = applicationHelper.getApplication("appsTable", "Salesforce");
		WebElement containerToDrop = selenium.getElement("content", "userAppsView", ElementType.ID);
		
		applicationHelper.assertApplicationNotExists("userAppsView", "Salesforce");
		applicationHelper.assertApplicationExists("appsTable", "Salesforce");
		selenium.dragAndDrop(draggedElement, containerToDrop);
		applicationHelper.assertApplicationExists("userAppsView", "Salesforce");
		applicationHelper.assertApplicationExists("appsTable", "Salesforce");
		
		applicationHelper.assertApplicationInState("appsTable", "Salesforce", ApplicationState.PENDING);
	}
	
}
