# Archivist

The Archivist is the hub for data store and client communication.  It is responsible
for exposing REST endpoints to client tools and controlling a group of Analyst nodes.

## Shared Data

The Archivist and its Analyst nodes rely on a shared filesystem between them.  This
shared file system contains these top level entries:

   * backups - backups of the embedded archivist DB
   * certs - SSL public keys, if SSL is configured
   * docs - documentation for the server, SDK, and any installed plugins.
   * exports - temporary storage for export packages.
   * logs - logs for batch jobs
   * models - data files needed by analysts
   * ofs - the root of the object file system, a permanent storage mechanism for proxy data.
   * plugins - installed plugins

## SSL Configuration

By default the Zorroa system ships configured for unsecured HTTP.  To enable HTTPS you
must first decide if your doing a self signed or signed SSL cert.

  * just a note, the key-store-password listed below is incorrect.  correct key-store-password
    has been placed into LastPass



## Self Signed Cert

To generate a self signed cert keystore, use Java's keytool.

```
keytool -genkey -alias zorroa -storetype PKCS12 -keyalg RSA -keysize 2048 -keystore zorroa.p12 -validity 3650
```

Remember the password you provided when creating the keystore.

Copy this resulting zorroa.p12 to both the 'archivist/certs' and 'analyst/certs'

In both the archivist and analyst configuration properties you must enable SSL and provide the location of the keystore.
Make sure to set the 'server.ssl.key-store-password' property to the password you used when creating the keystore.

```
server.ssl.enabled = true
server.ssl.key-store = certs/zorroa.p12
server.ssl.key-store-password = zorroa
server.ssl.key-store-type = PKCS12
server.ssl.key-alias = zorroa
```

Remember to update the Analyst(s) application.properties to utilize HTTPS when talking to the archivist.

```
analyst.master.host = https://archivist:8066
```

### Self Signed Python Client

The Python client cannot read Java's keystore format, so to support the python client in this configuration
you must export the public key into a PEM file and store it in 'shared/certs/zorroa.pem'

To export the cert in PEM format, use the openssl tool.

```
openssl pkcs12 -in zorroa.p12 -nokeys -out zorroa.pem
```

Set the BOONAI_CERT_PATH environment variable to the absolute path containing the zorroa.pem file,
along with the archivist URL.  Note the trailing / in the URL.

```
export BOONAI_ARCHIVIST_URL="https://archivist:8066/"
export BOONAI_CERT_PATH="/vol/zorroa/shared/certs"
```

## Signed Cert

Once you have obtained all relevant files from your digital cert provider you must convert them into
the pkcs12 format using OpenSSL.

The official Zorroa.com cert can be found here:

   * https://github.com/Zorroa/ssl-cert

```
openssl pkcs12 -export -in zorroa.crt -inkey zorroa.key -out zorroa.p12 -name zorroa.com -CAfile intermediate_sha2.crt -caname root
```

When generating the .p12 file, you will be asked for a password, which must match the 
server.ssl.key-store-password application property listed below.

Similar to self signed, you then add this to both your Archivist and Analyst application.properties.

```
server.ssl.enabled = true
server.ssl.key-store = certs/zorroa.p12
server.ssl.key-store-password = zorroa
server.ssl.key-store-type = PKCS12
server.ssl.key-alias = zorroa
```

Remember to update the Analyst(s) application.properties to utilize HTTPS when talking to the archivist.  With a signed cert, you must set the host name to the FQDN.

```
analyst.master.host = https://archivist.zorroa.com:8066
```

## Messaging Service Configuration
The archivist can be configured to publish messages to messaging queue such as Google Pub/Sub or RabbitMQ. The currently
supported technologies are below.

### Google Pub/Sub
To enable set `archivist.messaging-service.type=pubsub` in the application.properties file.

#### Config Options
| Property | Description | Required | Default |
| -------- | ----------- | -------- | ------- | 
| archivist.messaging-service.topicId | Sets the pub/sub topic to publish messages to. |  | archivist-actions |

#### Messages:

#### Assets Deleted
Emitted each time an asset is deleted.

Attributes:
- action = assets-deleted
- organization = <ORGANIZATION_ID>

Data:
```
{"ids": ["<ASSET_ID>"]}
```

