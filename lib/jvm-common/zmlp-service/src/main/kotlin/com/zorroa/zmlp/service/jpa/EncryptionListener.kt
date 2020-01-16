package com.zorroa.zmlp.service.jpa

import com.zorroa.zmlp.service.storage.SystemStorageService
import org.hibernate.event.spi.PreInsertEvent
import org.hibernate.event.spi.PreInsertEventListener
import org.hibernate.event.spi.PreLoadEvent
import org.hibernate.event.spi.PreLoadEventListener
import org.hibernate.event.spi.PreUpdateEvent
import org.hibernate.event.spi.PreUpdateEventListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EncryptionListener : PreInsertEventListener, PreUpdateEventListener, PreLoadEventListener {

    @Autowired
    lateinit var fieldEncryption: FieldEncryption

    override fun onPreInsert(event: PreInsertEvent): Boolean {
        val state = event.state
        val propertyNames = event.persister.propertyNames
        val entity = event.entity
        fieldEncryption.encrypt(state, propertyNames, entity)
        return false
    }

    override fun onPreUpdate(event: PreUpdateEvent): Boolean {
        val state: Array<Any> = event.state
        val propertyNames: Array<String> = event.persister.propertyNames
        val entity: Any = event.entity
        fieldEncryption.encrypt(state, propertyNames, entity)
        return false
    }

    override fun onPreLoad(event: PreLoadEvent) {
        fieldEncryption.decrypt(event.entity)
    }
}
