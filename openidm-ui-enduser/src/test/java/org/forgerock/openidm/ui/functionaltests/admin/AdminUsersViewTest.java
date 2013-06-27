package org.forgerock.openidm.ui.functionaltests.admin;

import org.forgerock.commons.ui.functionaltests.AbstractTest;
import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper.ElementType;
import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

public class AdminUsersViewTest extends AbstractTest {
	
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
