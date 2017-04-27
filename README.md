# OpenIDM Community Edition 2.1.2

Provisioning users, devices, and things is a repetitive and potentially time-consuming task that has a significant impact on security and user access. Ensuring the right access to the right service (or user, or device) is the essential step in Identity Management. It’s critical for you to correctly manage roles and entitlements assigned to users, devices, or things, based on your organizational need and structure (such as job function, title, and geography) and assign and remove entitlements and resources consistently and rapidly provided.

OpenIDM provides a responsive framework that can be deployed on-premises, in the cloud, or in hybrid environments where it can:

- Manage previously disparate data repositories, network applications, and user data stores anywhere in the infrastructure stack.
- Use the ForgeRock Open Connector Framework and its flexible workflow engine to provision and assign relationships to users.
- Easily customize and manage the registration and provisioning of users.

### About the Community Version

ForgeRock have been developing and commercially supporting OpenIDM since its birth in 2011. This version was originally released to ForgeRock customers in Feb 2013, and is now being released as our Community Edition with CDDL binary licensing which enables the downloadable binaries to be used in production.

To find out about the enterprise release of the ForgeRock platform [here][ForgeRock Identity Platform].

## Getting Started with OpenIDM

Binary Downloads are available via the GitHub releases page for the project [here][Binary Downloads].

ForgeRock provide a comprehensive set of documents for OpenIDM. They maybe found [here][OpenIDM 2.1.2 Docs] and [here][OpenIDM 2.1.0 Docs].

The [Installation Guide] introduces you to to OpenIDM and provides some samples you can explore that will help you to to get to know it better.

## Issues

Issues are handled via the [GitHub issues page for the project][GitHub Issues].

## How to Collaborate

Collaborate by:

- [Reporting an issue][GitHub Issues]
- [Fixing an issue][Help Wanted Issues]
- [Contributing to the Wiki][Project Wiki]

Code collaboration is done by creating an issue, discussing the changes in the issue. When the issue's been agreed then, fork, modify, test and submit a pull request. 

## Licensing

The Code and binaries are covered under the [CDDL 1.0 license](https://forgerock.org/cddlv1-0/). Essentially you may use the release binaries in production at your own risk. 

#### Legal Disclaimer Bit
All components herein are provided AS IS and without a warranty of any kind by ForgeRock or any licensors of such code.  ForgeRock and all licensors of such code disclaims all liability or obligations of such code and any use, distribution or operation of such code shall be exclusively subject to the licenses contained in each file and recipient is obligated to read and understand each such license before using the Software.  Notwithstanding anything to the contrary, ForgeRock has no obligations for the use or support of such code under any ForgeRock license agreement.

## How do I build it?

Best built on linux or OS X. Builds are possible on Windows, but more of a challenge. 

#### Set Up Your Environment

The below combination of versions are known to work;

Software          | Version
------------------|--------
Apache Maven      | 3.0.5  
JDK version       | Oracle JDK 1.6.0_45
Git               | 1.7.6 or above

Set your JAVA_HOME environment to point to the correct JDK so that Maven picks it up. `mvn --version` should output the correct versions;

```
Apache Maven 3.0.5
Maven home: /usr/share/maven
Java version: 1.6.0_45, vendor: Sun Microsystems Inc.
Java home: /usr/lib/jvm/jdk1.6.0_45/jre
```

**WARNING:** *When building the openidm-workflow-activiti module maven pulls the activiti dependency from the alfresco public repository. The SSH handshake fails with the following error;*
```
java.lang.RuntimeException: Could not generate DH keypair: Prime size must be multiple of 64, 
and can only range from 512 to 1024 (inclusive)
```

*This can be avoided by making the jre use ECDHC rather than DHC to perform the handshake. This may be done by adding the following line to `$JAVA_HOME/jre/lib/security/java.security`:*

`jdk.tls.disabledAlgorithms=DHE`

#### Build The Project

1. Clone the repository, or Fork it and clone your Fork if you want to create pull requests:
`git clone https://github.com/ForgeRock/openidm-community-edition-1.2.git`
2. `cd openidm-community-edition-1.2`
3. `mvn clean install`


### Modifying the GitHub Project Page

The [OpenIDM Community Edition project page][Project Page] is published via the `gh-pages` branch, which contains all the usual artifacts to create the web page. The GitHub page is served up directly from this branch by GitHub.


# All the Links

- [GitHub Project]
- [Project Wiki]
- [GitHub Issues]
- [Binary Downloads]
- [Help Wanted Issues]
- [OpenIDM 2.1.2 Docs]
- [OpenIDM 2.1.0 Docs]
- [Installation Guide]
- [ForgeRock Identity Platform]

[Project Page]:https://forgerock.github.io/openidm-community-edition-2.1.2/
[GitHub Project]:https://github.com/ForgeRock/openidm-community-edition-2.1.2
[GitHub Issues]:https://github.com/ForgeRock/openidm-community-edition-2.1.2/issues
[Binary Downloads]:https://github.com/ForgeRock/openidm-community-edition-2.1.2/releases
[Help Wanted Issues]:https://github.com/ForgeRock/openidm-community-edition-2.1.2/labels/help%20wanted
[Getting Started Guide]:https://backstage.forgerock.com/docs/openam/11.0.0/getting-started
[Project Wiki]:https://github.com/ForgeRock/openidm-community-edition-2.1.2/wiki
[ForgeRock Identity Platform]:https://www.forgerock.com/platform/
[OpenIDM 2.1.0 Docs]:https://backstage.forgerock.com/docs/openidm/2.1.0
[OpenIDM 2.1.2 Docs]:https://backstage.forgerock.com/docs/openidm/2.1.2
[Installation Guide]:https://backstage.forgerock.com/docs/openidm/2.1.0/install-guide