# Zorroa SAML Module

## Build and Installation

```
mvn clean package
cp target/saml-1.0.jar <archivist_root>/lib/ext
```

## Configuration

### Application.properties Setting

Archivist server configuration is done application.properties

##### The SAML base return URL
archivist.security.saml.baseUrl = http://localhost:8066

##### Enable SAML discovery
archivist.security.saml.discovery = true

##### SAML keystore path
archivist.security.saml.keystore.path = file://${archivist.path.home}/config/saml/keystore.jks

##### SAML keystore password
archivist.security.saml.keystore.password = zorroa

##### SAML key alias
archivist.security.saml.keystore.alias = zorroa

##### SAML key password
archivist.security.saml.keystore.keyPassword = zorroa

##### SAML import permissions/groups from SAML
archivist.security.saml.permissions.import = true

### Configuring an IDP

To configure an IDP you need to drop a <idpname>.properties file in <archivist_root>/config/saml
for each IDP.

Example properties file.

```
# A  human readable/user recognizable name.
label=Zorroa Okta

# The name of the SAML attribute where permission groups can be found
groupAttr=groups

# A unique ID to identify permissions coming from this service.  Don't change this once it is set.
authSourceId=zorroa-okta

# If no permission type prefix is set (eg: okta::manager), default to this value.
permissionType=okta

# The URL or file path where the SAML metadata can be found
metadataUrl=config/saml/okta-idp.xml

# An optional image to display on the IDP selection page
imageUrl=A URL to an image
```
