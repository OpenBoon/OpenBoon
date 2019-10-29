package com.example.authorizationserver

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.RSAPublicKeySpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer
import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpoint
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.security.interfaces.RSAPublicKey

/**
 * An instance of Legacy Authorization Server (spring-security-oauth2) that uses a single,
 * not-rotating key and exposes a JWK endpoint.
 *
 * See
 * [
 * Spring Security OAuth Autoconfig's documentation](https://docs.spring.io/spring-security-oauth2-boot/docs/current-SNAPSHOT/reference/htmlsingle/) for additional detail
 *
 * @since 5.1
 */
@EnableAuthorizationServer
@Configuration

class AuthorizationServerConfiguration @Throws(Exception::class)
constructor(
        authenticationConfiguration: AuthenticationConfiguration,
        internal var keyPair: KeyPair,
        @param:Value("\${security.oauth2.authorizationserver.jwt.enabled:true}") internal var jwtEnabled: Boolean) : AuthorizationServerConfigurerAdapter() {

    internal lateinit var authenticationManager: AuthenticationManager

    init {

        this.authenticationManager = authenticationConfiguration.authenticationManager
    }

    @Throws(Exception::class)
    override fun configure(clients: ClientDetailsServiceConfigurer) {
        // @formatter:off
        clients.inMemory()
                .withClient("client-read")
                .authorizedGrantTypes("client_credentials")
                .secret("{noop}secret")
                .scopes("message:read")
                .accessTokenValiditySeconds(600000000)
                .and()
                .withClient("client-write")
                .authorizedGrantTypes("client_credentials")
                .secret("{noop}secret")
                .scopes("message:write")
                .accessTokenValiditySeconds(600000000)
        // @formatter:on
    }

    override fun configure(endpoints: AuthorizationServerEndpointsConfigurer) {
        // @formatter:off
        endpoints
                .authenticationManager(this.authenticationManager)
                .tokenStore(tokenStore())

        if (this.jwtEnabled) {
            endpoints
                    .accessTokenConverter(accessTokenConverter())
        }
        // @formatter:on
    }

    @Bean
    fun tokenStore(): TokenStore {
        return if (this.jwtEnabled) {
            JwtTokenStore(accessTokenConverter())
        } else {
            InMemoryTokenStore()
        }
    }

    @Bean
    fun accessTokenConverter(): JwtAccessTokenConverter {
        val converter = JwtAccessTokenConverter()
        converter.setKeyPair(this.keyPair)

        val accessTokenConverter = DefaultAccessTokenConverter()
        converter.setAccessTokenConverter(accessTokenConverter)

        return converter
    }
}

/**
 * Legacy Authorization Server (spring-security-oauth2) does not support any
 * [JWK Set](https://tools.ietf.org/html/rfc7517#section-5) endpoint.
 */
@FrameworkEndpoint
internal class JwkSetEndpoint(var keyPair: KeyPair) {

    val key: Map<String, Any>
        @GetMapping("/.well-known/jwks.json")
        @ResponseBody
        get() {
            val publicKey = this.keyPair.public as RSAPublicKey
            val key = RSAKey.Builder(publicKey).build()
            return JWKSet(key).toJSONObject()
        }
}

/**
 * An Authorization Server will more typically have a key rotation strategy, and the keys will not
 * be hard-coded into the application code.
 *
 * For simplicity, though, this sample doesn't demonstrate key rotation.
 */
@Configuration
internal class KeyConfig {
    @Bean
    fun keyPair(): KeyPair {
        try {
            val privateExponent = "3851612021791312596791631935569878540203393691253311342052463788814433805390794604753109719790052408607029530149004451377846406736413270923596916756321977922303381344613407820854322190592787335193581632323728135479679928871596911841005827348430783250026013354350760878678723915119966019947072651782000702927096735228356171563532131162414366310012554312756036441054404004920678199077822575051043273088621405687950081861819700809912238863867947415641838115425624808671834312114785499017269379478439158796130804789241476050832773822038351367878951389438751088021113551495469440016698505614123035099067172660197922333993"
            val modulus = "18044398961479537755088511127417480155072543594514852056908450877656126120801808993616738273349107491806340290040410660515399239279742407357192875363433659810851147557504389760192273458065587503508596714389889971758652047927503525007076910925306186421971180013159326306810174367375596043267660331677530921991343349336096643043840224352451615452251387611820750171352353189973315443889352557807329336576421211370350554195530374360110583327093711721857129170040527236951522127488980970085401773781530555922385755722534685479501240842392531455355164896023070459024737908929308707435474197069199421373363801477026083786683"
            val exponent = "65537"

            val publicSpec = RSAPublicKeySpec(BigInteger(modulus), BigInteger(exponent))
            val privateSpec = RSAPrivateKeySpec(BigInteger(modulus), BigInteger(privateExponent))
            val factory = KeyFactory.getInstance("RSA")
            return KeyPair(factory.generatePublic(publicSpec), factory.generatePrivate(privateSpec))
        } catch (e: Exception) {
            throw IllegalArgumentException(e)
        }

    }
}

