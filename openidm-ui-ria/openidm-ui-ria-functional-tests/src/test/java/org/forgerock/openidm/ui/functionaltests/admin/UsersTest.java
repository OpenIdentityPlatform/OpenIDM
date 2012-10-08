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
		//selenium.waitForElement("content", "Add user", ElementType.LINK_TEXT);
		
		JsonNode user = jsonUtils.readJsonFromFile("/admin/newUser.json");
		forms.fillForm("content", user);
		forms.validateForm("content");
		forms.assertFormValidationPasses("content");
		forms.submit("content", "register");
		
		messages.assertInfoMessage("User has been registered successfully");
		router.assertUrl("#users/");
		
		WebElement element = selenium.getElement("content", "name", ElementType.CLASS);
		Assert.assertEquals(element.getText(), "TestName TestSurname");
	}
	
}
