# <img alt="OpenIDM Logo" src="https://github.com/OpenIdentityPlatform/OpenIDM/raw/master/logo.png" width="300"/>
[![Latest release](https://img.shields.io/github/release/OpenIdentityPlatform/OpenIDM.svg)](https://github.com/OpenIdentityPlatform/OpenIDM/releases)
[![Build](https://github.com/OpenIdentityPlatform/OpenIDM/actions/workflows/build.yml/badge.svg)](https://github.com/OpenIdentityPlatform/OpenIDM/actions/workflows/build.yml)
[![Deploy](https://github.com/OpenIdentityPlatform/OpenIDM/actions/workflows/deploy.yml/badge.svg)](https://github.com/OpenIdentityPlatform/OpenIDM/actions/workflows/deploy.yml)
[![Issues](https://img.shields.io/github/issues/OpenIdentityPlatform/OpenIDM.svg)](https://github.com/OpenIdentityPlatform/OpenIDM/issues)
[![Last commit](https://img.shields.io/github/last-commit/OpenIdentityPlatform/OpenIDM.svg)](https://github.com/OpenIdentityPlatform/OpenIDM/commits/master)
[![License](https://img.shields.io/badge/license-CDDL-blue.svg)](https://github.com/OpenIdentityPlatform/OpenIDM/blob/master/LICENSE.md)
[![Downloads](https://img.shields.io/github/downloads/OpenIdentityPlatform/OpenIDM/total.svg)](https://github.com/OpenIdentityPlatform/OpenIDM/releases)
[![Docker](https://img.shields.io/docker/pulls/openidentityplatform/openidm.svg)](https://hub.docker.com/r/openidentityplatform/openidm)
[![Gitter](https://img.shields.io/gitter/room/nwjs/nw.js.svg)](https://gitter.im/OpenIdentityPlatform/OpenIDM)
[![Top language](https://img.shields.io/github/languages/top/OpenIdentityPlatform/OpenIDM.svg)](https://github.com/OpenIdentityPlatform/OpenIDM)
[![Code size in bytes](https://img.shields.io/github/languages/code-size/OpenIdentityPlatform/OpenIDM.svg)](https://github.com/OpenIdentityPlatform/OpenIDM)


OpenIDM enables you to consolidate multiple identity sources for policy and workflow-based management. OpenIDM can 
consume, transform and feed data to external sources so that you maintain control over the identities of users, 
devices and other objects.

OpenIDM provides a modern UI experience that allows you to manage your data without writing a single line of code. The 
standard RESTful interfaces also offer ultimate flexibility so that you can customize and develop the product to fit the
requirements of your deployment.

## License
This project is licensed under the [Common Development and Distribution License (CDDL)](https://github.com/OpenIdentityPlatform/OpenIDM/blob/master/LICENSE.md). 

## Downloads 
* [OpenIDM ZIP](https://github.com/OpenIdentityPlatform/OpenIDM/releases)
* [OpenIDM Docker](https://hub.docker.com/r/openidentityplatform/openidm/) (All OS) 

Java 1.8+ required

## How-to build
For windows use:
```bash
git config --system core.longpaths true
```

```bash
git clone --recursive  https://github.com/OpenIdentityPlatform/OpenIDM.git
mvn clean verify -f OpenIDM
```

## How-to run after build
```bash
unzip OpenIDM/openidm-zip/target/openidm-*.zip
./opendm/startup.sh
```
Wait for the message **OpenIDM ready** and go:

* User self service UI: http://localhost:8080/ (openidm-admin/openidm-admin)
* Admin UI: http://localhost:8080/admin/ (openidm-admin/openidm-admin)
* Apache Felix UI: http://localhost:8080/system/console/ (admin/admin)

## Support and Mailing List Information
* OpenIDM Community Wiki: https://github.com/OpenIdentityPlatform/OpenIDM/wiki
* OpenIDM Community Mailing List: open-identity-platform-openidm@googlegroups.com
* OpenIDM Community Archive: https://groups.google.com/d/forum/open-identity-platform-openidm
* OpenIDM Community on Gitter: https://gitter.im/OpenIdentityPlatform/OpenIDM
* OpenIDM Commercial support RFP: support@3a-systems.ru (English, Russian)

## Contributing
Please, make [Pull request](https://github.com/OpenIdentityPlatform/OpenIDM/pulls)

## Thanks for OpenIDM 🥰
* Sun Identity Manager
* Forgerock OpenIDM
