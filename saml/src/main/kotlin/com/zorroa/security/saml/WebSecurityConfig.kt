package com.zorroa.security.saml

import org.apache.commons.httpclient.protocol.Protocol
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory
import org.apache.velocity.app.VelocityEngine
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider
import org.opensaml.saml2.metadata.provider.MetadataProvider
import org.opensaml.saml2.metadata.provider.MetadataProviderException
import org.opensaml.xml.parse.StaticBasicParserPool
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.io.FileSystemResource
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.saml.SAMLAuthenticationProvider
import org.springframework.security.saml.SAMLBootstrap
import org.springframework.security.saml.SAMLEntryPoint
import org.springframework.security.saml.SAMLLogoutFilter
import org.springframework.security.saml.SAMLLogoutProcessingFilter
import org.springframework.security.saml.SAMLProcessingFilter
import org.springframework.security.saml.SAMLWebSSOHoKProcessingFilter
import org.springframework.security.saml.context.SAMLContextProvider
import org.springframework.security.saml.context.SAMLContextProviderLB
import org.springframework.security.saml.key.JKSKeyManager
import org.springframework.security.saml.key.KeyManager
import org.springframework.security.saml.log.SAMLDefaultLogger
import org.springframework.security.saml.metadata.CachingMetadataManager
import org.springframework.security.saml.metadata.ExtendedMetadata
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate
import org.springframework.security.saml.metadata.MetadataDisplayFilter
import org.springframework.security.saml.metadata.MetadataGenerator
import org.springframework.security.saml.metadata.MetadataGeneratorFilter
import org.springframework.security.saml.parser.ParserPoolHolder
import org.springframework.security.saml.processor.HTTPPAOS11Binding
import org.springframework.security.saml.processor.HTTPPostBinding
import org.springframework.security.saml.processor.HTTPRedirectDeflateBinding
import org.springframework.security.saml.processor.HTTPSOAP11Binding
import org.springframework.security.saml.processor.SAMLBinding
import org.springframework.security.saml.processor.SAMLProcessorImpl
import org.springframework.security.saml.storage.EmptyStorageFactory
import org.springframework.security.saml.trust.httpclient.TLSProtocolConfigurer
import org.springframework.security.saml.trust.httpclient.TLSProtocolSocketFactory
import org.springframework.security.saml.util.VelocityFactory
import org.springframework.security.saml.websso.SingleLogoutProfile
import org.springframework.security.saml.websso.SingleLogoutProfileImpl
import org.springframework.security.saml.websso.WebSSOProfile
import org.springframework.security.saml.websso.WebSSOProfileConsumer
import org.springframework.security.saml.websso.WebSSOProfileConsumerHoKImpl
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl
import org.springframework.security.saml.websso.WebSSOProfileECPImpl
import org.springframework.security.saml.websso.WebSSOProfileImpl
import org.springframework.security.saml.websso.WebSSOProfileOptions
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.FilterChainProxy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.channel.ChannelProcessingFilter
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.Properties

@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    @Autowired
    lateinit var properties: SamlProperties

    @Autowired
    lateinit var samlUserDetailsServiceImpl: SAMLUserDetailsServiceImpl

    // Initialization of the velocity engine
    @Bean
    fun velocityEngine(): VelocityEngine {
        return VelocityFactory.getEngine()
    }

    // XML parser pool needed for OpenSAML parsing
    @Bean(initMethod = "initialize")
    fun parserPool(): StaticBasicParserPool {
        return StaticBasicParserPool()
    }

    @Bean(name = ["parserPoolHolder"])
    fun parserPoolHolder(): ParserPoolHolder {
        return ParserPoolHolder()
    }

    // SAML Authentication Provider responsible for validating of received SAML
    // messages
    @Bean
    fun samlAuthenticationProvider(): SAMLAuthenticationProvider {
        val samlAuthenticationProvider = ZorroaSAMLAuthenticationProvider()
        samlAuthenticationProvider.userDetails = samlUserDetailsServiceImpl
        samlAuthenticationProvider.isForcePrincipalAsString = false
        return samlAuthenticationProvider
    }

    // Provider of default SAML Context
    @Bean
    fun contextProvider(): SAMLContextProvider {
        logger.info("SAML Base URL {}", properties.baseUrl)

        val uri = URI.create(properties.baseUrl)
        val ctx = SAMLContextProviderLB()
        ctx.setScheme(uri.scheme)
        ctx.setServerName(uri.host)
        ctx.setIncludeServerPortInRequestURL(false)
        ctx.setContextPath("/")

        if (uri.scheme.endsWith("s")) {
            ctx.setServerPort(443)
        } else {
            ctx.setServerPort(80)
        }

        /**
         * This is a workaround for the fact that spring creates
         * a new session for authenticated users, which won't
         * match the anonymous session used for SAML.
         */
        ctx.setStorageFactory(EmptyStorageFactory())

        return ctx
    }

    // Logger for SAML messages and events
    @Bean
    fun samlLogger(): SAMLDefaultLogger {
        return SAMLDefaultLogger()
    }

    // SAML 2.0 WebSSO Assertion Consumer
    @Bean
    fun webSSOprofileConsumer(): WebSSOProfileConsumer {
        val consumer = WebSSOProfileConsumerImpl()
        consumer.maxAuthenticationAge = properties.maxAuthenticationAge
        return consumer
    }

    // SAML 2.0 Holder-of-Key WebSSO Assertion Consumer
    @Bean
    fun hokWebSSOprofileConsumer(): WebSSOProfileConsumerHoKImpl {
        return WebSSOProfileConsumerHoKImpl()
    }

    // SAML 2.0 Web SSO profile
    @Bean
    fun webSSOprofile(): WebSSOProfile {
        return WebSSOProfileImpl()
    }

    // SAML 2.0 Holder-of-Key Web SSO profile
    @Bean
    fun hokWebSSOProfile(): WebSSOProfileConsumerHoKImpl {
        return WebSSOProfileConsumerHoKImpl()
    }

    // SAML 2.0 ECP profile
    @Bean
    fun ecpprofile(): WebSSOProfileECPImpl {
        return WebSSOProfileECPImpl()
    }

    @Bean
    fun logoutprofile(): SingleLogoutProfile {
        return SingleLogoutProfileImpl()
    }

    // Central storage of cryptographic keys
    @Bean
    fun keyManager(): KeyManager {
        val keystore = properties.keystore
        val storeFile = FileSystemResource(File(keystore["path"]))
        val passwords = mutableMapOf<String, String>()

        passwords[keystore.getOrDefault("alias", "zorroa")] =
            keystore.getOrDefault("keyPassword", "zorroa")

        return JKSKeyManager(
            storeFile, keystore["password"],
            passwords, keystore["alias"]
        )
    }

    // Setup TLS Socket Factory
    @Bean
    fun tlsProtocolConfigurer(): TLSProtocolConfigurer {
        return TLSProtocolConfigurer()
    }

    @Bean
    fun socketFactory(): ProtocolSocketFactory {
        return TLSProtocolSocketFactory(keyManager(), null, "default")
    }

    @Bean
    fun socketFactoryProtocol(): Protocol {
        return Protocol("https", socketFactory(), 443)
    }

    @Bean
    fun socketFactoryInitialization(): MethodInvokingFactoryBean {
        val methodInvokingFactoryBean = MethodInvokingFactoryBean()
        methodInvokingFactoryBean.targetClass = Protocol::class.java
        methodInvokingFactoryBean.targetMethod = "registerProtocol"
        val args = arrayOf("https", socketFactoryProtocol())
        methodInvokingFactoryBean.setArguments(*args)
        return methodInvokingFactoryBean
    }

    @Bean
    fun defaultWebSSOProfileOptions(): WebSSOProfileOptions {
        val webSSOProfileOptions = WebSSOProfileOptions()
        webSSOProfileOptions.isIncludeScoping = false
        webSSOProfileOptions.forceAuthN = properties.forceAuthN
        return webSSOProfileOptions
    }

    // Entry point to initialize authentication, default values taken from
    // properties file
    @Bean
    fun samlEntryPoint(): SAMLEntryPoint {
        val samlEntryPoint = SAMLEntryPoint()
        samlEntryPoint.setDefaultProfileOptions(defaultWebSSOProfileOptions())
        return samlEntryPoint
    }

    // Setup advanced info about metadata
    @Bean
    fun extendedMetadata(): ExtendedMetadata {
        val extendedMetadata = ExtendedMetadata()
        extendedMetadata.isIdpDiscoveryEnabled = true
        extendedMetadata.isSignMetadata = true
        extendedMetadata.isEcpEnabled = true
        return extendedMetadata
    }

    @Bean
    @Qualifier("metadata")
    @Throws(MetadataProviderException::class, IOException::class)
    fun metadata(): CachingMetadataManager {

        val providers = mutableListOf<MetadataProvider>()
        Files.list(Paths.get("/config/saml"))
            .filter { p -> p.fileName.toString().endsWith(".properties") }
            .forEach { p ->

                logger.info("Initializing SAML : {}", p)
                val props = Properties()
                try {
                    props.load(FileInputStream(p.toFile()))
                    val uri = props.getProperty("metadataUrl")

                    val extendedMetadata = ZorroaExtendedMetadata()
                    extendedMetadata.isIdpDiscoveryEnabled = true
                    extendedMetadata.isSignMetadata = true
                    extendedMetadata.isEcpEnabled = true
                    extendedMetadata.props = props

                    try {
                        val md5 = MessageDigest.getInstance("MD5")
                        val bytes = Files.readAllBytes(Paths.get(uri))
                        val hash = md5.digest(bytes)
                    } catch (e: Exception) {
                        logger.warn("Unable to MD5 metadata")
                    }

                    val provider = FilesystemMetadataProvider(File(uri))
                    provider.parserPool = parserPool()
                    val emd = ExtendedMetadataDelegate(provider, extendedMetadata)
                    emd.isMetadataTrustCheck = false
                    emd.isMetadataRequireSignature = false
                    emd.setRequireValidMetadata(false)
                    providers.add(emd)
                } catch (e: IOException) {
                    logger.warn("Failed to open SAML file: ", e)
                } catch (e: MetadataProviderException) {
                    logger.warn("Failed to open SAML file: ", e)
                }
            }

        return CachingMetadataManager(providers)
    }

    // Filter automatically generates default SP metadata
    @Bean
    fun metadataGenerator(): MetadataGenerator {
        val baseURL = properties.baseUrl
        val metadataGenerator = MetadataGenerator()
        metadataGenerator.entityId = "$baseURL/saml/metadata"
        metadataGenerator.extendedMetadata = extendedMetadata()
        metadataGenerator.setKeyManager(keyManager())
        metadataGenerator.entityBaseURL = baseURL
        return metadataGenerator
    }

    // The filter is waiting for connections on URL suffixed with filterSuffix
    // and presents SP metadata there
    @Bean
    fun metadataDisplayFilter(): MetadataDisplayFilter {
        return MetadataDisplayFilter()
    }

    // Handler deciding where to redirect user after successful login
    @Bean
    fun successRedirectHandler(): SavedRequestAwareAuthenticationSuccessHandler {
        val successRedirectHandler = SavedRequestAwareAuthenticationSuccessHandler()
        successRedirectHandler.setDefaultTargetUrl(properties.landingPage)
        successRedirectHandler.setAlwaysUseDefaultTargetUrl(true)
        return successRedirectHandler
    }

    @Bean
    fun authenticationFailureHandler(): SimpleUrlAuthenticationFailureHandler {
        val failureHandler = SimpleUrlAuthenticationFailureHandler()
        failureHandler.setUseForward(true)
        failureHandler.setDefaultFailureUrl("/error")
        return failureHandler
    }

    @Bean
    @Throws(Exception::class)
    fun samlWebSSOHoKProcessingFilter(): SAMLWebSSOHoKProcessingFilter {
        val samlWebSSOHoKProcessingFilter = SAMLWebSSOHoKProcessingFilter()
        samlWebSSOHoKProcessingFilter.setAuthenticationSuccessHandler(successRedirectHandler())
        samlWebSSOHoKProcessingFilter.setAuthenticationManager(authenticationManager())
        samlWebSSOHoKProcessingFilter.setAuthenticationFailureHandler(authenticationFailureHandler())
        return samlWebSSOHoKProcessingFilter
    }

    // Processing filter for WebSSO profile messages
    @Bean
    @Throws(Exception::class)
    fun samlWebSSOProcessingFilter(): SAMLProcessingFilter {
        val samlWebSSOProcessingFilter = SAMLProcessingFilter()
        samlWebSSOProcessingFilter.setAuthenticationManager(authenticationManager())
        samlWebSSOProcessingFilter.setAuthenticationSuccessHandler(successRedirectHandler())
        samlWebSSOProcessingFilter.setAuthenticationFailureHandler(authenticationFailureHandler())
        return samlWebSSOProcessingFilter
    }

    @Bean
    fun metadataGeneratorFilter(): MetadataGeneratorFilter {
        return ZorroaMetadataFilter(properties.baseUrl, metadataGenerator())
    }

    // Handler for successful logout
    @Bean
    fun successLogoutHandler(): SimpleUrlLogoutSuccessHandler {
        val successLogoutHandler = SimpleUrlLogoutSuccessHandler()
        successLogoutHandler.setDefaultTargetUrl(properties.landingPage)
        return successLogoutHandler
    }

    // Logout handler terminating local session
    @Bean
    fun logoutHandler(): SecurityContextLogoutHandler {
        val logoutHandler = SecurityContextLogoutHandler()
        logoutHandler.isInvalidateHttpSession = true
        logoutHandler.setClearAuthentication(true)
        return logoutHandler
    }

    // Filter processing incoming logout messages
    // First argument determines URL user will be redirected to after successful
    // global logout
    @Bean
    fun samlLogoutProcessingFilter(): SAMLLogoutProcessingFilter {
        return SAMLLogoutProcessingFilter(
            successLogoutHandler(),
            logoutHandler()
        )
    }

    // Overrides default logout processing filter with the one processing SAML
    // messages
    @Bean
    fun samlLogoutFilter(): SAMLLogoutFilter {
        return SAMLLogoutFilter(
            successLogoutHandler(),
            arrayOf<LogoutHandler>(logoutHandler()),
            arrayOf<LogoutHandler>(logoutHandler())
        )
    }

    @Bean
    fun soapBinding(): HTTPSOAP11Binding {
        return HTTPSOAP11Binding(parserPool())
    }

    @Bean
    fun httpPostBinding(): HTTPPostBinding {
        return HTTPPostBinding(parserPool(), velocityEngine())
    }

    @Bean
    fun httpRedirectDeflateBinding(): HTTPRedirectDeflateBinding {
        return HTTPRedirectDeflateBinding(parserPool())
    }

    @Bean
    fun httpSOAP11Binding(): HTTPSOAP11Binding {
        return HTTPSOAP11Binding(parserPool())
    }

    @Bean
    fun httpPAOS11Binding(): HTTPPAOS11Binding {
        return HTTPPAOS11Binding(parserPool())
    }

    // Processor
    @Bean
    fun processor(): SAMLProcessorImpl {
        val bindings = ArrayList<SAMLBinding>()
        bindings.add(httpRedirectDeflateBinding())
        bindings.add(httpPostBinding())
        bindings.add(httpSOAP11Binding())
        bindings.add(httpPAOS11Binding())
        return SAMLProcessorImpl(bindings)
    }

    /**
     * Define the security filter chain in order to support SSO Auth by using SAML 2.0
     *
     * @return Filter chain proxy
     * @throws Exception
     */
    @Bean
    @Throws(Exception::class)
    fun samlFilter(): FilterChainProxy {
        val chains = ArrayList<SecurityFilterChain>()
        chains.add(
            DefaultSecurityFilterChain(
                AntPathRequestMatcher("/saml/login/**"),
                samlEntryPoint()
            )
        )
        chains.add(
            DefaultSecurityFilterChain(
                AntPathRequestMatcher("/saml/logout/**"),
                samlLogoutFilter()
            )
        )
        chains.add(
            DefaultSecurityFilterChain(
                AntPathRequestMatcher("/saml/metadata/**"),
                metadataDisplayFilter()
            )
        )
        chains.add(
            DefaultSecurityFilterChain(
                AntPathRequestMatcher("/saml/SSO/**"),
                samlWebSSOProcessingFilter()
            )
        )
        chains.add(
            DefaultSecurityFilterChain(
                AntPathRequestMatcher("/saml/SSOHoK/**"),
                samlWebSSOHoKProcessingFilter()
            )
        )
        chains.add(
            DefaultSecurityFilterChain(
                AntPathRequestMatcher("/saml/SingleLogout/**"),
                samlLogoutProcessingFilter()
            )
        )

        return FilterChainProxy(chains)
    }

    /**
     * Defines the web based security configuration.
     *
     * @param http It allows configuring web based security for specific http requests.
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.antMatcher("/saml/**")
            .csrf().disable()
            .httpBasic()
            .authenticationEntryPoint(samlEntryPoint())
            .and()
            .addFilterBefore(metadataGeneratorFilter(), ChannelProcessingFilter::class.java)
            .addFilterAfter(samlFilter(), BasicAuthenticationFilter::class.java)
            .authorizeRequests()
            .antMatchers("/saml/**").permitAll()
            .and()
            .logout()
            .disable()
    }

    /**
     * Sets a custom authentication provider.
     *
     * @param auth SecurityBuilder used to create an AuthenticationManager.
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun configure(auth: AuthenticationManagerBuilder) {
        auth
            .authenticationProvider(samlAuthenticationProvider())
    }

    companion object {

        private val logger = LoggerFactory.getLogger(WebSecurityConfig::class.java)

        // Initialization of OpenSAML library
        @Bean
        fun sAMLBootstrap(): SAMLBootstrap {
            return SAMLBootstrap()
        }
    }
}
