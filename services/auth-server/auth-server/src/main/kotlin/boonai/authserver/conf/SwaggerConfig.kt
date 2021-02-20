package boonai.authserver.conf

import java.util.Collections
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@ConfigurationProperties(prefix = "swagger")
class SwaggerConfigurationProperties {
    var title: String? = null
    var description: String? = null
    var version: String? = null
    var termOfService: String? = null
    var contactCompanyName: String? = null
    var contactCompanyWebsite: String? = null
    var contactCompanySupportEmail: String? = null
    var restBasePackage: String? = null
    var licenseUrl: String? = null
    var licenseName: String? = null
}

@Configuration
@EnableSwagger2
class SwaggerConfig {

    @Autowired
    lateinit var config: SwaggerConfigurationProperties

    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage(config.restBasePackage))
            .paths(PathSelectors.any())
            .build()
            .apiInfo(getApiInfo())
    }

    private fun getApiInfo(): ApiInfo {

        return ApiInfo(
            config.title,
            config.description,
            config.version,
            config.termOfService,
            Contact(config.contactCompanyName, config.contactCompanyWebsite, config.contactCompanySupportEmail),
            config.licenseName,
            config.licenseUrl,
            Collections.emptyList()
        )
    }
}
