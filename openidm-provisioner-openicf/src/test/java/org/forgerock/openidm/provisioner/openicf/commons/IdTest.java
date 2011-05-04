package org.forgerock.openidm.provisioner.openicf.commons;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URI;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class IdTest {
    @Test
    public void testId() throws Exception {
        String idString = "http://openidm.forgerock.org/openidm/system/xml/account";
        Id id = new Id(idString);
        Assert.assertEquals(id.getObjectType(), "account");
        Assert.assertNull(id.getLocalId());

//        idString = "http://openidm.forgerock.org/openidm/system/xml/account/af352880-73e0-11e0-a1f0-0800200c9a66";
//        id = new Id(idString);
        idString = "system/xml/account/af352880-73e0-11e0-a1f0-0800200c9a66";
        id = new Id(idString);
        Assert.assertEquals(id.getObjectType(), "account");
        Assert.assertEquals(id.getLocalId(), "af352880-73e0-11e0-a1f0-0800200c9a66");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMalformedURLPrefix() throws Exception {
        new Id("/system/xml/account/");
    }

    @Test
    public void testResolveLocalId() throws Exception {
        Id actual = new Id("system/xml/account/");
        URI expected = new URI("system/xml/account/simple");
        Assert.assertEquals(actual.resolveLocalId("simple"), expected);
        expected = new URI("system/xml/account/http%3a%2f%2fopenidm.forgerock.org%2fopenidm%2fmanaged%2fuser%2f480ab4b0-764f-11e0-a1f0-0800200c9a66");
        Assert.assertEquals(actual.resolveLocalId("http://openidm.forgerock.org/openidm/managed/user/480ab4b0-764f-11e0-a1f0-0800200c9a66"), expected);
    }
}
