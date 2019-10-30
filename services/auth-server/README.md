# OAuth2 Server

Spring Docs

https://docs.spring.io/spring-security-oauth2-boot/docs/current/reference/htmlsingle/#specifying-a-client-and-secret

This is currently single tenant only.  Need a Multi-tenant JWT singer like this one but better.

```
https://github.com/thomasdarimont/spring-boot-2-keycloak-oauth-example/blob/feature/mulit-tenancy/src/main/java/demo/SpringBoot2App.java#L127
```

## Client Credentials Grant

Obtaining a client credentials grant. 

```
 curl -u clientId:secret -X POST "localhost:9090/oauth/token?grant_type=client_credentials"
```

Now use the token in an archivist request:

```
curl -XGET localhost:8080/api/v1/authed/client -H "Authorization: Bearer <token>"
```

## Password Credentials Grant

Obtaining a password credentials Grant.

```
curl -u clientId:secret -X POST "localhost:9090/oauth/token?grant_type=password&username=user&password=pass"
```

Now use the token in an archivist request:

```
curl -XGET localhost:8080/api/v1/authed/user -H "Authorization: Bearer <token>"
```
