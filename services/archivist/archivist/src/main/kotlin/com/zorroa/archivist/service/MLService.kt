package com.zorroa.archivist.service

import com.zorroa.archivist.domain.ArchivistException
import com.zorroa.archivist.util.hashFeatures
import org.apache.mxnet.Context
import org.apache.mxnet.DataBatch
import org.apache.mxnet.MXNetError
import org.apache.mxnet.Model
import org.apache.mxnet.javaapi.DType
import org.apache.mxnet.javaapi.DataDesc
import org.apache.mxnet.javaapi.Image
import org.apache.mxnet.javaapi.NDArray
import org.apache.mxnet.javaapi.Shape
import org.apache.mxnet.module.Module
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import scala.Option

@Service
interface MLService {

    fun assetToHash(bytes: ByteArray): String
}

@Service
class MXNetService(
    @Value("\${mxnet.resnet.path}") var modelPath: String
) : MLService {

    var symbol = Model.loadCheckpoint(modelPath, 0)

    /**
     * Convert a Byte Array to a MXNet Model Hash using generated attributes
     */
    override fun assetToHash(bytes: ByteArray): String {

        // Prepare data
        var nd: NDArray? = null
        try {
            nd = Image.imDecode(bytes, 1, false)
        } catch (ex: MXNetError) {
            throw(ArchivistException("File is not an image", ex))
        }

        nd = Image.imResize(nd, 224, 224)
        nd = NDArray.transpose(nd, Shape(intArrayOf(2, 0, 1)), null)[0] // HWC to CHW
        nd = NDArray.expand_dims(nd, 0, null)[0] // Add N -> NCHW
        nd = nd.asType(DType.Float32()) // Inference with Float32
        // Get Params
        val contextArray =
            arrayOf(Context.defaultCtx())

        val flatten0_output = symbol._1().internals["flatten0_output"]
        val module = Module(
            flatten0_output,
            Module.`$lessinit$greater$default$2`(),
            Module.`$lessinit$greater$default$3`(),
            contextArray,
            Module.`$lessinit$greater$default$5`(),
            Option.empty()
        )
        val dataDesc = DataDesc(
            "data",
            Shape(intArrayOf(1, 3, 224, 224)),
            DType.Float32(),
            "NCHW"
        )
        module.bind(false, false, false, dataDesc.dataDesc())
        module.setParams(symbol._2(), symbol._3(), false, true, false)
        val builder = DataBatch.Builder()
        builder.setData(nd.nd())
        module.forward(builder.build(), Option.empty())
        val outputsMerged = module.outputsMerged
        val elem = outputsMerged.toVector().getElem(0, 0)
        return hashFeatures(elem.toFloat64Array())
    }
}