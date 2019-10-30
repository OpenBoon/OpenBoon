package com.zorroa.auth

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl
import javax.sql.DataSource

@Configuration
class DatabaseConfiguration {
    
    @Bean
    @Autowired
    fun userDetailsService(dataSource: DataSource): UserDetailsService {
        val userDetailsService = JdbcDaoImpl()
        userDetailsService.setDataSource(dataSource)
        return userDetailsService
    }

    @Bean
    fun getDataSource(): DataSource {
        val dataSourceBuilder = DataSourceBuilder.create()
        dataSourceBuilder.driverClassName("org.h2.Driver")
        dataSourceBuilder.url("jdbc:h2:mem:test")
        dataSourceBuilder.username("SA")
        dataSourceBuilder.password("")
        return dataSourceBuilder.build()
    }
}