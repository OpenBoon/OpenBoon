package boonai.archivist.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.util.Collections

@Configuration
@EnableSwagger2
@ConfigurationProperties(prefix = "swagger")
class SwaggerConfig : WebMvcConfigurer {

    @Value("\${swagger.title}")
    lateinit var title: String
    @Value("\${swagger.description}")
    lateinit var description: String
    @Value("\${swagger.version}")
    lateinit var version: String
    @Value("\${swagger.termOfService}")
    lateinit var termOfService: String

    @Value("\${swagger.contactCompanyName}")
    lateinit var contactCompanyName: String
    @Value("\${swagger.contactCompanyWebsite}")
    lateinit var contactCompanyWebsite: String
    @Value("\${swagger.contactCompanySupportEmail}")
    lateinit var contactCompanySupportEmail: String

    @Value("\${swagger.restBasePackage}")
    lateinit var restBasePackage: String

    @Value("\${swagger.licenseName}")
    lateinit var licenseName: String
    @Value("\${swagger.licenseUrl}")
    lateinit var licenseUrl: String

    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage(restBasePackage))
            .paths(PathSelectors.any())
            .build()
            .apiInfo(getApiInfo())
    }

    private fun getApiInfo(): ApiInfo {

        return ApiInfo(
            title,
            description,
            version,
            termOfService,
            Contact(contactCompanyName, contactCompanyWebsite, contactCompanySupportEmail),
            licenseName,
            licenseUrl,
            Collections.emptyList()
        )
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {

        registry.addResourceHandler("swagger-ui.html")
            .addResourceLocations("classpath:/META-INF/resources/")

        registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/")

        registry.addResourceHandler("**/**").addResourceLocations("/dist/")
    }

    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.favorPathExtension(false)
    }
}
