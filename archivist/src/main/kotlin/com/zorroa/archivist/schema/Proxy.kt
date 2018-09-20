package com.zorroa.common.schema

import com.fasterxml.jackson.annotation.JsonIgnore

data class Proxy(
        var stream: String?=null,
        var mimeType: String?,
        var width: Int,
        var height: Int,
        var id: String? = null,
        var format: String? = null
)

data class ProxySchema (
        var proxies: MutableList<Proxy>? = null,
        var tinyProxy : MutableList<String>? = null
) {

    /**
     * Get closest.
     *
     * @return
     */
    @JsonIgnore
    fun getClosest(width: Int, height: Int): Proxy? {
        if (proxies == null) {
            return null
        }

        var result: Proxy? = null
        val dim = width + height
        var distance = Integer.MAX_VALUE

        return try {
            for (p in proxies!!) {
                val pDim = p.width + p.height
                val diff = Math.abs(pDim - dim)
                if (diff < distance) {
                    distance = diff
                    result = p
                }
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Return the largest proxy.
     *
     * @return
     */
    @JsonIgnore
    fun size(): Int {
        return if (proxies == null) 0 else proxies!!.size
    }


    /**
     * Return the largest proxy.
     *
     * @return
     */
    @JsonIgnore
    fun getLargest(): Proxy? {
        return proxies?.stream()
                ?.sorted { o1, o2 -> Integer.compare(o2.width, o1.width) }
                ?.findFirst()
                ?.orElse(null)
    }

    /**
     * Return the smallest proxy.
     *
     * @return
     */
    @JsonIgnore
    fun getSmallest(): Proxy? {
        return proxies?.stream()
                ?.sorted({o1, o2 -> Integer.compare(o2.width, o1.width)})
                ?.findFirst()
                ?.orElse(null)
    }


    /**
     * Return the first proxy greater than or equal to the minimum dimension.  If there is problem loading
     * the proxy IOException is thrown.
     *
     * @param minDim
     * @return
     */
    @JsonIgnore
    fun atLeastThisSize(minDim: Int): Proxy? {
        return proxies?.stream()
                ?.filter { p -> p.width >= minDim || p.height >= minDim }
                ?.sorted({o1, o2 -> Integer.compare(o2.width, o1.width)})
                ?.findFirst()
                ?.orElse(null)
    }

    /**
     * Return the first proxy less than or equal to the minimum dimension.  If there is problem loading
     * the proxy IOException is thrown.
     *
     * @param minDim
     * @return
     */
    @JsonIgnore
    fun thisSizeOrBelow(minDim: Int): Proxy? {
        return proxies?.stream()
                ?.filter { p -> p.width <= minDim || p.width <= minDim }
                ?.sorted { o1, o2 -> Integer.compare(o2.width, o1.width) }
                ?.findFirst()
                ?.orElse(null)
    }

}
