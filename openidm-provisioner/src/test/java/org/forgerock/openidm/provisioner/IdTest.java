package org.forgerock.openidm.provisioner;

import org.forgerock.openidm.objset.ObjectSetException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class IdTest {

    @Test
    public void testId() throws Exception {
        String idString = "system/xml/account";
        Id id = new Id(idString);
        Assert.assertEquals(id.getSystemName(), "xml");
        Assert.assertEquals(id.getObjectType(), "account");
        Assert.assertNull(id.getLocalId());

//        idString = "http://openidm.forgerock.org/openidm/system/xml/account/af352880-73e0-11e0-a1f0-0800200c9a66";
//        id = new Id(idString);
        idString = "system/xml/account/af352880-73e0-11e0-a1f0-0800200c9a66";
        id = new Id(idString);
        Assert.assertEquals(id.getSystemName(), "xml");
        Assert.assertEquals(id.getObjectType(), "account");
        Assert.assertEquals(id.getLocalId(), "af352880-73e0-11e0-a1f0-0800200c9a66");
    }

    @Test
    public void testResolveLocalId() throws Exception {
        Id actual = new Id("system/xml/account/");
        URI expected = new URI("xml/account/simple");
        Assert.assertEquals(actual.resolveLocalId("simple"), expected);
        expected = new URI("xml/account/http%3a%2f%2fopenidm.forgerock.org%2fopenidm%2fmanaged%2fuser%2f480ab4b0-764f-11e0-a1f0-0800200c9a66");
        Assert.assertEquals(actual.resolveLocalId("http://openidm.forgerock.org/openidm/managed/user/480ab4b0-764f-11e0-a1f0-0800200c9a66"), expected);
    }

    @Test(expectedExceptions = ObjectSetException.class)
    public void testMalformedURLPrefix() throws Exception {
        new Id("/system/xml/");
    }

    @Test(expectedExceptions = ObjectSetException.class)
    public void testExpectObjectId() throws Exception {
        Id id = new Id("/system/xml/");
        id.expectObjectId();
    }

//    @Test
//    public void testGetId() throws Exception {
//
//    }
//
//    @Test
//    public void testEscapeUid() throws Exception {
//
//    }

}
