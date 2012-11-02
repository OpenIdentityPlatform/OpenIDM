package org.forgerock.openidm.ui.functionaltests.admin;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.AbstractTest;
import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper.ElementType;
import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

public class UsersTest extends AbstractTest {
	
	@Test
	@AssertNoErrors
	public void shouldAddUser() {
		userHelper.loginAsOpenIDMAdmin();
		router.routeTo("#users/add/", true);
		
		selenium.waitForElement("content", "userName", ElementType.NAME);
		
		JsonNode user = jsonUtils.readJsonFromFile("/admin/newUser.json");
		forms.fillForm("content", user);
		forms.validateForm("content");
		forms.assertFormValidationPasses("content");
		forms.submit("content", "createButton");
		
		messages.assertInfoMessage("User has been registered successfully");
		router.assertUrl("#users/");
		
		WebElement element = selenium.getElement("content", "name", ElementType.CLASS);
		Assert.assertEquals(element.getText(), "TestName TestSurname");
	}
	
	@Test
	@AssertNoErrors
	public void shouldDeleteUser() {		
		userHelper.createDefaultUser();
		userHelper.loginAsOpenIDMAdmin();
		
		router.routeTo("#users/show/test@test.test/", true);
		WebElement deleteButton = selenium.getElement("content", "deleteButton", ElementType.NAME);
		deleteButton.click();
		
		WebElement deleteDialogButton = selenium.getElement("dialogs", "Delete", ElementType.NAME);
		deleteDialogButton.click();
		
		router.assertUrl("#users/");
		
		WebElement td = selenium.getElement("content", "td",ElementType.TAG);
		Assert.assertEquals(td.getText(), "No matching records found");
	}
	
	@Test
	@AssertNoErrors
	public void shouldUpdateUsersProfile() {
		userHelper.createDefaultUser();
		userHelper.loginAsOpenIDMAdmin();
		
		router.routeTo("#users/show/test@test.test/", true);
		
		forms.setField("content", "givenName", "John");
		forms.validateForm("content");
		forms.assertFormValidationPasses("content");
		forms.submit("content", "saveButton");
		
		messages.assertInfoMessage("Profile has been updated");
		
		router.routeTo("#users/");
		WebElement name = selenium.getElement("usersTable", "name", ElementType.CLASS);
		Assert.assertEquals(name.getText(), "John TestSurname");
	}
	
	@Test
	@AssertNoErrors
	public void shouldGoToUsersProfile() {
		userHelper.createDefaultUser();
		userHelper.loginAsOpenIDMAdmin();
		
		router.routeTo("#users/", true);
		
		WebElement tr = selenium.getElement("usersTable", "odd", ElementType.CLASS);
		tr.click();
		
		router.assertUrl("#users/show/test@test.test/");
	}
	
}
