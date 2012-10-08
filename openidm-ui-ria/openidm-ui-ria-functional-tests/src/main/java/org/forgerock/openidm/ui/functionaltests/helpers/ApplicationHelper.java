package org.forgerock.openidm.ui.functionaltests.helpers;

import javax.inject.Inject;

import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper;
import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper.ElementType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;
import org.testng.Assert;

@Component
public class ApplicationHelper {
	
	@Inject
	protected SeleniumHelper selenium;
	
	public WebElement getApplication(String containerId, String appName) {
		try {
			WebElement element = selenium.getElement(containerId, "id('"+containerId+"')//li[contains(.,'" + appName + "')]", ElementType.XPATH);
			return element;
		} catch (Exception e) {
			return null;
		}
	}

	public void assertApplicationExists(final String containerId, final String appName) {
		selenium. new AssertionWithTimeout() {
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				return getApplication(containerId, appName) != null;
			}
			@Override
			protected String getAssertionFailedMessage() {
				return "Application "+ appName + " does not exists in container "+ containerId;
			}
		}.checkAssertion();
	}

	public void assertApplicationNotExists(final String containerId, final String appName) {
		selenium. new AssertionWithTimeout() {
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				return getApplication(containerId, appName) == null;
			}
			@Override
			protected String getAssertionFailedMessage() {
				return "Application "+ appName + " exists in container "+ containerId;
			}
		}.checkAssertion();
	}

	public void assertApplicationInState(final String containerId, final String appName, ApplicationState applicationState) {
		assertApplicationExists(containerId, appName);
		String failMessage = "Application is not in " + applicationState + " state";
		Assert.assertTrue(getApplication(containerId, appName).getAttribute("class").contains(applicationState.getStateClassName()), failMessage );
	}
	
	public enum ApplicationState {
		PENDING("ui-state-needsApproval");
		
		private String stateClassName;
		
		private ApplicationState(String stateClassName) {
			this.stateClassName = stateClassName;
		}

		public String getStateClassName() {
			return stateClassName;
		}
	}

}
