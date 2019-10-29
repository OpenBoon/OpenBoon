# OAuth2 Authorization Server

Adapted from [OAuth2 Authorization server](https://github.com/spring-projects/spring-security/tree/master/samples/boot/oauth2authorizationserver) sample from the Spring Security project. 

Works in conjunction with [OAuth2 Resource server](https://github.com/spring-projects/spring-security/tree/master/samples/boot/oauth2resourceserver) sample from same.  

### Workflow

Get client tokens
```
curl client-read:secret@localhost:8081/oauth/token -d grant_type=client_credentials
curl client-write:secret@localhost:8081/oauth/token -d grant_type=client_credentials
```

Make authenticated requests to the resource server

```
curl -H "Authorization: Bearer $TOKEN" localhost:8080
curl -H "Authorization: Bearer $TOKEN" localhost:8080/message
```

# Documentation and Guides

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.2.0.RELEASE/maven-plugin/)
* [Spring Security](https://docs.spring.io/spring-boot/docs/2.2.0.RELEASE/reference/htmlsingle/#boot-features-security)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.2.0.RELEASE/reference/htmlsingle/#boot-features-developing-web-applications)

### Guides
The following guides illustrate how to use some features concretely:

* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/bookmarks/)


