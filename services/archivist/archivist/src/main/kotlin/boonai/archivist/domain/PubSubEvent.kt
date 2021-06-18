package boonai.archivist.domain

import com.google.pubsub.v1.PubsubMessage

class PubSubEvent(val msg: PubsubMessage, val sub: String) {
    val attrs = msg.attributesMap
    val data = msg.data
}
