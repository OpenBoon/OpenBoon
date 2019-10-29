package com.zorroa.authserver.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore
import javax.sql.DataSource


@Configuration
class AppConfig {

    @Value("\${spring.datasource.url}")
    private val datasourceUrl: String? = null

    @Value("\${spring.database.driverClassName}")
    private val dbDriverClassName: String = "" // must be of type String, cannot be null

    @Value("\${spring.datasource.username}")
    private val dbUsername: String? = null

    @Value("\${spring.datasource.password}")
    private val dbPassword: String? = null

    @Bean
    fun dataSource(): DataSource {
        val dataSource = DriverManagerDataSource()

        dataSource.setDriverClassName(dbDriverClassName)
        dataSource.setUrl(datasourceUrl)
        dataSource.setUsername(dbUsername)
        dataSource.setPassword(dbPassword)

        return dataSource
    }

    @Bean
    fun tokenStore(): TokenStore {
        return JdbcTokenStore(dataSource())
    }
}