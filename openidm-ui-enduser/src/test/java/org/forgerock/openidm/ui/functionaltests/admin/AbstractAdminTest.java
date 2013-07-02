package org.forgerock.openidm.ui.functionaltests.admin;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.AbstractTest;
import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper.ElementType;

public class AbstractAdminTest extends AbstractTest{

	public abstract class AdminAddUserValidationTest {
		
		protected abstract void checkAddUserViewBehavior();
		
		public final void run() {
			
			userHelper.loginAsOpenIDMAdmin();
			router.routeTo("#users/add/", true);
			selenium.waitForElement("content", "userName", ElementType.NAME);
			
			JsonNode user = jsonUtils.readJsonFromFile("/admin/newUser.json");
			forms.fillForm("content", user);
			forms.validateForm("content");
			forms.assertFormValidationPasses("content");
			
			checkAddUserViewBehavior();
			
		}
	}
	
	public abstract class AdminEditUserValidationTest {
		
		protected abstract void checkEditUserViewBehavior();
		
		public final void run() {
			
			userHelper.createDefaultUser();
			userHelper.loginAsOpenIDMAdmin();
			
			router.routeTo("#users/show/test@test.test/", true);
			router.assertUrl("#users/show/test@test.test/");
			
			forms.assertFormValidationPasses("content");
			forms.validateForm("content");
			forms.assertFormValidationPasses("content");
			
			checkEditUserViewBehavior();
			
			forms.assertFormValidationPasses("content");
			
			forms.submit("content", "saveButton");
			messages.assertInfoMessage("Profile has been updated");
			
			router.routeTo("#users/show/test@test.test/", true);
			router.assertUrl("#users/show/test@test.test/");
			
			checkEditedUserAfterSave();
		}

		protected abstract void checkEditedUserAfterSave();
	}
	
}
