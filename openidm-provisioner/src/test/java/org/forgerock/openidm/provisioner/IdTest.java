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

        idString = "system/xml/account/af352880-73e0-11e0-a1f0-0800200c9a66";
        id = new Id(idString);
        Assert.assertEquals(id.getSystemName(), "xml");
        Assert.assertEquals(id.getObjectType(), "account");
        Assert.assertEquals(id.getLocalId(), "af352880-73e0-11e0-a1f0-0800200c9a66");

        idString = "system/Active Directory/User type/<GUID=500ac9a4c732df45bde6e5dbc784de04>";
        id = new Id(idString);
        Assert.assertEquals(id.getSystemName(), "Active Directory");
        Assert.assertEquals(id.getObjectType(), "User type");
        Assert.assertEquals(id.getLocalId(), "<GUID=500ac9a4c732df45bde6e5dbc784de04>");

        idString = "system/Active+Directory/User+type/%3CGUID%3D500ac9a4c732df45bde6e5dbc784de04%3E";

        Assert.assertEquals(id.getQualifiedId().toString(),idString);

        id = new Id(idString);
        Assert.assertEquals(id.getSystemName(), "Active Directory");
        Assert.assertEquals(id.getObjectType(), "User type");
        Assert.assertEquals(id.getLocalId(), "<GUID=500ac9a4c732df45bde6e5dbc784de04>");
    }

    @Test
    public void testResolveLocalId() throws Exception {
        Id actual = new Id("system/xml/account/");
        String expected = "xml/account/simple";
        Assert.assertEquals(actual.resolveLocalId("simple").getId().toString(), expected);
        expected = "xml/account/http%3A%2F%2Fopenidm.forgerock.org%2Fopenidm%2Fmanaged%2Fuser%2F480ab4b0-764f-11e0-a1f0-0800200c9a66";
        Assert.assertEquals(actual.resolveLocalId("http://openidm.forgerock.org/openidm/managed/user/480ab4b0-764f-11e0-a1f0-0800200c9a66").getId().toString(), expected);
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

}
