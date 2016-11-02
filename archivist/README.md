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

