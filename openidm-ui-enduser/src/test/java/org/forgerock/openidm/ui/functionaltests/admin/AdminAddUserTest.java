package org.forgerock.openidm.ui.functionaltests.admin;

import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper.ElementType;
import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AdminAddUserTest extends AbstractAdminTest {
	
	@Test
	@AssertNoErrors
	public void shouldPassValidationDefaultScenario() {
		new AdminAddUserValidationTest(){
			@Override
			protected void checkAddUserViewBehavior() {
				//Do nothing - just check if default test scenario passes
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void shouldNotPassValidationOnEmptyUsername() {
		new AdminAddUserValidationTest(){
			@Override
			protected void checkAddUserViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "userName", "");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void shouldNotPassValidationOnEmptyEmail() {
		new AdminAddUserValidationTest(){
			@Override
			protected void checkAddUserViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "email", "");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void shouldNotPassValidationOnNotValidEmail() {
		new AdminAddUserValidationTest(){
			@Override
			protected void checkAddUserViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "email", "qwe");
				fieldShouldBeNotValidAfterChange("content", "email", "qwe@");
				fieldShouldBeNotValidAfterChange("content", "email", "qwe@x.plllll");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void shouldNotPassValidationOnEmptyFirstName() {
		new AdminAddUserValidationTest(){
			@Override
			protected void checkAddUserViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "givenName", "");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void shouldNotPassValidationOnEmptyLastName() {
		new AdminAddUserValidationTest(){
			@Override
			protected void checkAddUserViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "familyName", "");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void shouldNotPassValidationOnEmptyPhoneNumber() {
		new AdminAddUserValidationTest(){
			@Override
			protected void checkAddUserViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "phoneNumber", "");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void shouldNotPassValidationOnNotValidPhoneNumber() {
		new AdminAddUserValidationTest(){
			@Override
			protected void checkAddUserViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "phoneNumber", "adfsae");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void shouldNotPassValidationOnExistingUsername() {
		userHelper.createDefaultUser();
		new AdminAddUserValidationTest(){
			@Override
			protected void checkAddUserViewBehavior() {
				forms.setField("content", "userName", "test@test.test");
				forms.assertValidationError("content", "userName");
				forms.assertFormValidationError("content");
			}
		}.run();
	}
	
	
	@Test
	@AssertNoErrors
	public void shouldNotPassValidationOnInvaliPassword() {
		new AdminAddUserValidationTest(){
			@Override
			protected void checkAddUserViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "password", "");

				fieldShouldBeNotValidAfterChange("content", "password", "aa12345!");
				fieldShouldBeNotValidAfterChange("content", "password", "Aaaaaaa!");
				fieldShouldBeNotValidAfterChange("content", "password", "Aa1234!");
				fieldShouldBeNotValidAfterChange("content", "password", "Aa12345");
				
				fieldShouldBeValidAfterChangeButFormCanBeNotValid("content", "password", "Aa12345!");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void shouldNotPassValidationOnInvalidConfirmPassword() {
		new AdminAddUserValidationTest(){
			@Override
			protected void checkAddUserViewBehavior() {
				forms.setField("content", "password", "");
				forms.assertFormFieldHasValue("content", "password", "");
				
				forms.setField("content", "passwordConfirm", "");
				forms.assertFormFieldHasValue("content", "passwordConfirm", "");
				forms.assertValidationError("content", "passwordConfirm");
				
				forms.setField("content", "password", "XXX");
				forms.assertFormFieldHasValue("content", "password", "XXX");
				
				forms.setField("content", "passwordConfirm", "xxX");
				forms.assertFormFieldHasValue("content", "passwordConfirm", "xxX");
				forms.assertValidationError("content", "passwordConfirm");
				
				forms.setField("content", "passwordConfirm", "XXX");
				forms.assertFormFieldHasValue("content", "passwordConfirm", "XXX");
				forms.assertValidationPasses("content", "passwordConfirm");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void shouldAddUser() {
		new AdminAddUserValidationTest(){
			@Override
			protected void checkAddUserViewBehavior() {
				forms.submit("content", "createButton");
				messages.assertInfoMessage("User has been registered successfully");
				router.assertUrl("#users/");
				WebElement element = selenium.getElement("content", "name", ElementType.CLASS);
				Assert.assertEquals(element.getText(), "TestName TestSurname");
				
				userHelper.logout();
				try{
					userHelper.login("test", "tesT#1#Test");
					router.goToProfile(true);
					router.assertUrl("#profile/");
				} catch (IllegalArgumentException e) {
					Assert.fail("Invalid credentials: " + e);
				}
			}
		}.run();
	}
}
