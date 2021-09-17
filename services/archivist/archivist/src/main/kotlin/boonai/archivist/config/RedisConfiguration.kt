package boonai.archivist.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.GenericToStringSerializer
import org.springframework.scheduling.annotation.EnableScheduling
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

@Configuration
@EnableScheduling
class RedisConfiguration {

    @Autowired
    lateinit var properties: ApplicationProperties

    @Bean
    fun redisClient(): JedisPool {
        val host = properties.getString("archivist.redis.host")
        val port = properties.getInt("archivist.redis.port")

        val config = jedisPoolConfig()

        return JedisPool(config, host, port, 10000)
    }

    private fun jedisPoolConfig(): JedisPoolConfig {
        val config = JedisPoolConfig()
        config.maxTotal = 128
        config.maxIdle = 128
        config.minIdle = 16
        config.testWhileIdle = true
        config.minEvictableIdleTimeMillis = 60000L
        config.timeBetweenEvictionRunsMillis = 3000L
        config.numTestsPerEvictionRun = 3
        config.blockWhenExhausted = true
        return config
    }

    @Bean
    fun jedisConnectionFactory(): JedisConnectionFactory? {
        val host = properties.getString("archivist.redis.host")
        val port = properties.getInt("archivist.redis.port")
        return JedisConnectionFactory(RedisStandaloneConfiguration(host, port))
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any>? {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = jedisConnectionFactory()
        template.valueSerializer = GenericToStringSerializer(Any::class.java)
        return template
    }

    @Bean
    fun redisContainer(): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.connectionFactory = jedisConnectionFactory()
        return container
    }
}
