package boonai.common.service.jpa

import boonai.common.service.security.EncryptionService
import java.lang.reflect.Field
import javax.annotation.PostConstruct
import javax.persistence.EntityManagerFactory
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType
import org.hibernate.event.spi.PreInsertEvent
import org.hibernate.event.spi.PreInsertEventListener
import org.hibernate.event.spi.PreLoadEvent
import org.hibernate.event.spi.PreLoadEventListener
import org.hibernate.event.spi.PreUpdateEvent
import org.hibernate.event.spi.PreUpdateEventListener
import org.hibernate.internal.SessionFactoryImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.util.ReflectionUtils

class EncryptionListener : PreInsertEventListener, PreUpdateEventListener, PreLoadEventListener {

    @Autowired
    lateinit var encyrptionService: EncryptionService

    @Autowired
    lateinit var entityManagerFactory: EntityManagerFactory

    @PostConstruct
    private fun init() {
        val sessionFactory: SessionFactoryImpl = entityManagerFactory.unwrap(SessionFactoryImpl::class.java)
        val registry = sessionFactory.serviceRegistry.getService(
            EventListenerRegistry::class.java
        )
        registry.getEventListenerGroup<PreInsertEventListener>(EventType.PRE_INSERT).appendListener(this)
        registry.getEventListenerGroup<PreUpdateEventListener>(EventType.PRE_UPDATE).appendListener(this)
        registry.getEventListenerGroup<PreLoadEventListener>(EventType.PRE_LOAD).appendListener(this)
    }

    override fun onPreInsert(event: PreInsertEvent): Boolean {
        val state = event.state
        val propertyNames = event.persister.propertyNames
        val entity = event.entity
        encrypt(state, propertyNames, entity)
        return false
    }

    override fun onPreUpdate(event: PreUpdateEvent): Boolean {
        val state: Array<Any> = event.state
        val propertyNames: Array<String> = event.persister.propertyNames
        val entity: Any = event.entity
        encrypt(state, propertyNames, entity)
        return false
    }

    override fun onPreLoad(event: PreLoadEvent) {
        decrypt(event.entity)
    }

    fun encrypt(
        state: Array<Any>,
        propertyNames: Array<String>,
        entity: Any
    ) {
        ReflectionUtils.doWithFields(
            entity.javaClass,
            ReflectionUtils.FieldCallback { field: Field -> encryptField(field, state, propertyNames) },
            ReflectionUtils.FieldFilter {
                AnnotationUtils.findAnnotation(it, Encrypted::class.java) != null
            }
        )
    }

    private fun encryptField(
        field: Field,
        state: Array<Any>,
        propertyNames: Array<String>
    ) {
        val propertyIndex: Int = EncryptionUtils.getPropertyIndex(field.name, propertyNames)
        val currentValue = state[propertyIndex]
        check(currentValue is String) { "Encrypted annotation was used on a non-String field" }
        state[propertyIndex] = encyrptionService.encryptString(currentValue.toString(), VARIABLE)
    }

    fun decrypt(entity: Any) {
        ReflectionUtils.doWithFields(
            entity.javaClass,
            ReflectionUtils.FieldCallback { field: Field -> decryptField(field, entity) },
            ReflectionUtils.FieldFilter {
                AnnotationUtils.findAnnotation(it, Encrypted::class.java) != null
            }
        )
    }

    private fun decryptField(field: Field, entity: Any) {
        field.isAccessible = true
        val value = ReflectionUtils.getField(field, entity)
        check(value is String) { "Encrypted annotation was used on a non-String field" }

        val clearValue = encyrptionService.decryptString(value.toString(), VARIABLE)
        ReflectionUtils.setField(field, entity, clearValue)
    }

    companion object {
        /**
         * Additional encryption keys to tack on based on a specified it.
         */
        const val VARIABLE = 99
    }
}

object EncryptionUtils {

    fun getPropertyIndex(name: String, properties: Array<String>): Int {
        for (i in properties.indices) {
            if (name == properties[i]) {
                return i
            }
        }
        throw IllegalArgumentException("No property was found for name $name")
    }
}
