package org.forgerock.openidm.provisioner.openicf.commons;

import org.testng.Assert;
import org.testng.annotations.Test;

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
        assertThat(id.getElements()).hasSize(2).containsSequence("xml", "account");
        Assert.assertEquals(id.getObjectType(), "account");
        Assert.assertNull(id.getLocalId());

        idString = "http://openidm.forgerock.org/openidm/system/xml/account/af352880-73e0-11e0-a1f0-0800200c9a66";
        id = new Id(idString);
        assertThat(id.getElements()).hasSize(3).containsSequence("xml", "account", "af352880-73e0-11e0-a1f0-0800200c9a66");
        Assert.assertEquals(id.getObjectType(), "account");
        Assert.assertEquals(id.getLocalId(), "af352880-73e0-11e0-a1f0-0800200c9a66");
    }
}
