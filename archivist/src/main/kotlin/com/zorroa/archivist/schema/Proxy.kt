package com.zorroa.common.schema

import com.fasterxml.jackson.annotation.JsonIgnore

data class Proxy(
        var id: String,
        var width: Int,
        var height: Int,
        /**
         * Some older versions don't have a mimetype but they
         * are all image/jpeg, so we'll default to that.
         */
        var mimetype: String = "image/jpeg",
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
    fun getClosest(width: Int, height: Int, type: String = "image"): Proxy? {
        if (proxies == null) {
            return null
        }

        var result: Proxy? = null
        val dim = width + height
        var distance = Integer.MAX_VALUE

        return try {
            for (p in proxies!!) {
                if (!p.mimetype.startsWith(type, ignoreCase = true)) {
                    continue
                }
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
    fun getLargest(type: String="image"): Proxy? {
        return proxies?.stream()
                ?.filter { p-> p.mimetype.startsWith(type, ignoreCase = true) }
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
    fun getSmallest(type: String="image"): Proxy? {
        return proxies?.stream()
                ?.filter { p-> p.mimetype.startsWith(type, ignoreCase = true) }
                ?.sorted{ o1, o2 -> Integer.compare(o1.width, o2.width) }
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
    fun atLeastThisSize(minDim: Int, type: String="image"): Proxy? {
        return proxies?.stream()
                ?.filter { p -> (p.width >= minDim || p.height >= minDim)
                        && p.mimetype.startsWith(type, ignoreCase = true) }
                ?.sorted {o1, o2 -> Integer.compare(o1.width, o2.width) }
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
    fun thisSizeOrBelow(minDim: Int, type: String="image"): Proxy? {
        return proxies?.stream()
                ?.filter { p -> (p.width <= minDim || p.width <= minDim)
                        && p.mimetype.startsWith(type, ignoreCase = true) }
                ?.sorted { o1, o2 -> Integer.compare(o2.width, o1.width) }
                ?.findFirst()
                ?.orElse(null)
    }

}
