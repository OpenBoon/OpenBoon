package com.zorroa.common.schema

class LinkSchema : HashMap<String, MutableSet<Any>>() {

    fun addLink(type: String, id: Any) {
        var set = this[type]
        if (set == null) {
            set = mutableSetOf(type)
            this[type] = set
        }
        else {
            set.add(id)
        }
    }
}
