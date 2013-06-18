package org.forgerock.openidm.ui.functionaltests.admin;

import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper.ElementType;
import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AdminModifyUserTest extends AbstractAdminTest {
	
	@Test
	@AssertNoErrors
	public void shouldUpdateUsersFirstName() {
		new AdminEditUserValidationTest() {
			@Override
			protected void checkEditUserViewBehavior() {
				forms.setField("content", "givenName", "John");
			}
			@Override
			protected void checkEditedUserAfterSave() {
				Assert.assertEquals("John", forms.getFieldValue("content", "givenName"));
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void shouldUpdateUsersEmail() {
		new AdminEditUserValidationTest() {
			@Override
			protected void checkEditUserViewBehavior() {
				forms.setField("content", "email", "some@email.ok");
			}
			@Override
			protected void checkEditedUserAfterSave() {
				Assert.assertEquals("some@email.ok", forms.getFieldValue("content", "email"));
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void shouldUpdateUsersActiveStatus() {
		new AdminEditUserValidationTest() {
			@Override
			protected void checkEditUserViewBehavior() {
				forms.setField("content", "accountStatus", "inactive");
			}
			@Override
			protected void checkEditedUserAfterSave() {
				Assert.assertEquals("Inactive", forms.getSelectDisplayValue("content", "accountStatus"));
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void shouldUpdateUsersUsername() {
		userHelper.createDefaultUser();
		userHelper.loginAsOpenIDMAdmin();
		
		router.routeTo("#users/show/test@test.test/", true);
		router.assertUrl("#users/show/test@test.test/");
		
		forms.assertFormValidationPasses("content");
		
		forms.setField("content", "userName", "xxx@test");
		forms.submit("content", "saveButton");
		messages.assertInfoMessage("Profile has been updated");
		
		router.routeTo("#users/show/test@test.test/", true);
		
		router.assertUrl("#users/show/test@test.test/");
		WebElement element = selenium.getElement("content", "h1", ElementType.TAG);
		Assert.assertEquals(element.getText(), "Page not found");
		
		router.routeTo("#users/show/xxx@test/", true);
		router.assertUrl("#users/show/xxx@test/");
		Assert.assertEquals("xxx@test", forms.getFieldValue("content", "userName"));
	}
	
	@Test
	@AssertNoErrors
	public void shouldRemoveUser() {
		userHelper.createDefaultUser();
		userHelper.loginAsOpenIDMAdmin();
		
		router.routeTo("#users/show/test@test.test/", true);
		router.assertUrl("#users/show/test@test.test/");
		
		forms.assertFormValidationPasses("content");
		
		forms.submit("content", "deleteButton");
		dialogsHelper.assertActionButtonEnabled("Delete");
		
		forms.submit("dialogs", "Delete");
		
		messages.assertInfoMessage("User has been deleted");
		router.assertUrl("#users/");
		
		try {
			userHelper.login("test@test.test", "tesT#1#Test");
			Assert.fail();
		} catch (IllegalArgumentException e) {
		}
		
	}
	
	@Test
	@AssertNoErrors
	public void shouldUpdateUsersPassword() {
		userHelper.createDefaultUser();
		userHelper.loginAsOpenIDMAdmin();
		
		router.routeTo("#users/test@test.test/change_password/", true);
		router.assertUrl("#users/test@test.test/change_password/");
		
		forms.assertFormValidationError("dialogs");
		
		forms.setField("dialogs", "password", "Aa12345!");
		forms.setField("dialogs", "passwordConfirm", "Aa12345!");
		
		forms.assertFormValidationPasses("dialogs");
		dialogsHelper.assertActionButtonEnabled("Update");
		
		forms.submit("dialogs", "Update");
		messages.assertInfoMessage("Security data has been changed");
		router.assertUrl("#users/show/test@test.test/");
		
		try {
			userHelper.login("test@test.test", "tesT#1#Test");
			Assert.fail();
		} catch (IllegalArgumentException e) {
			userHelper.login("test@test.test", "Aa12345!");
		}
	}
	
	@Test
	@AssertNoErrors
	public void shouldNotAllowToDeleteYourself() {
		userHelper.createDefaultAdminUser();
		userHelper.loginAsDefaultAdminUser();
		
		router.routeTo("#users/admin@test.test/change_password/", true);
		router.assertUrl("#users/admin@test.test/change_password/");
		
		forms.assertFormValidationError("dialogs");
		
		forms.submit("content", "deleteButton");
		dialogsHelper.assertActionButtonEnabled("Delete");
		
		forms.submit("dialogs", "Delete");
		
		messages.assertErrorMessage("You can't delete yourself");
	}
	
}
