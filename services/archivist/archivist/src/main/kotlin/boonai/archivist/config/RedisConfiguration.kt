package boonai.archivist.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.GenericToStringSerializer
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

@Configuration
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
        return JedisConnectionFactory(jedisPoolConfig())
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any>? {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = jedisConnectionFactory()
        template.valueSerializer = GenericToStringSerializer(Any::class.java)
        return template
    }

    @Bean
    fun redisContainer(
        @Qualifier("project-topic") projectTopic: PatternTopic,
        @Qualifier("project-listener") projectListener: MessageListener
    ): RedisMessageListenerContainer? {
        val container = RedisMessageListenerContainer()
        container.connectionFactory = jedisConnectionFactory()
        container.addMessageListener(projectListener, projectTopic)
        return container
    }

    @Bean("project-topic")
    fun topic(): PatternTopic {
        return PatternTopic("project/*")
    }
}
