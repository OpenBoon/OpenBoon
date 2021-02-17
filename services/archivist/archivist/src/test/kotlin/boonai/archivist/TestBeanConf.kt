package boonai.archivist

import com.google.cloud.automl.v1.Model
import com.google.longrunning.Operation
import com.google.longrunning.OperationsClient
import com.google.protobuf.Any
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import boonai.archivist.repository.AutomlDao
import boonai.archivist.service.AutomlClientWrapper
import boonai.archivist.service.AutomlService
import boonai.archivist.service.AutomlServiceImpl
import boonai.archivist.service.ModelService
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("test")
@Configuration
class TestBeanConf {

    @Bean
    @Primary
    @Autowired
    fun automlService(automlDao: AutomlDao, modelService: ModelService): AutomlService {
        val service = AutomlServiceImpl(automlDao)
        service.modelService = modelService
        val spy = Mockito.spy(service)

        val mockClient = Mockito.mock(AutomlClientWrapper::class.java)
        val mockOpClient = Mockito.mock(OperationsClient::class.java)
        val mockOp = Mockito.mock(Operation::class.java)

        doReturn(mockClient).whenever(spy).automlClient()

        whenever(mockClient.getOperationsClient()).thenReturn(mockOpClient)
        whenever(mockOpClient.getOperation(any())).thenReturn(mockOp)
        whenever(mockOp.name).thenReturn("/foo/bar")
        whenever(mockOp.done).thenReturn(true)

        val model = Model.newBuilder()
            .setName("/model/name")
            .build()
        val any = Any.pack(model)
        whenever(mockOp.response).thenReturn(any)

        return spy
    }
}
