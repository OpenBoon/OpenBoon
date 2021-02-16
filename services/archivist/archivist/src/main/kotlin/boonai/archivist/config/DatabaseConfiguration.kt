package boonai.archivist.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.Properties
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

@EnableTransactionManagement
@Configuration
class DatabaseConfiguration {

    @Bean
    fun entityManagerFactory(dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val emf = LocalContainerEntityManagerFactoryBean()
        emf.dataSource = dataSource
        emf.setPackagesToScan("com.zorroa.archivist.domain", "com.zorroa.zmlp.service.jpa")

        val ad = HibernateJpaVendorAdapter()
        emf.jpaVendorAdapter = ad
        emf.setJpaProperties(additionalProperties())

        return emf
    }

    @Bean
    fun transactionManager(emf: EntityManagerFactory): PlatformTransactionManager {
        val txm = JpaTransactionManager()
        txm.entityManagerFactory = emf
        return txm
    }

    @Bean
    fun exceptionTranslation(): PersistenceExceptionTranslationPostProcessor {
        return PersistenceExceptionTranslationPostProcessor()
    }

    fun additionalProperties(): Properties {
        val props = Properties()
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
        return props
    }
}
