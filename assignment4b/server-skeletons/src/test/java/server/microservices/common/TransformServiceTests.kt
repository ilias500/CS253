package server.microservices.common

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.test.context.ContextConfiguration
import server.common.Components
import server.common.Constants
import server.common.model.TransformedImage
import server.main.ImageFactory
import server.main.MainApplication
import server.main.MainService

/**
 * These use mocking to isolate and test only the service component.
 */
@EnableDiscoveryClient(autoRegister = false)
@AutoConfigureMockMvc
@SpringBootTest
@ContextConfiguration(classes = [Components::class, MainApplication::class, MainService::class])
class TransformServiceTests {

    @BeforeEach
    fun beforeEach() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `constructor initialized HashMap`() {
        val service = spyk(TransformService())
        Assertions.assertThat(service.mTransformMap).hasSize(3)
    }

    @Test
    fun `applyTransform() calls transforms helpers`() {
        val imageBytes = ImageFactory.randomImageBytes()
        val service = TransformService()
        val transforms = mockk<Transforms>().also { service.transforms = it }
        val expected = ImageFactory.randomTransformedImages(3).also {
            it[0].transformName = Constants.Service.GRAYSCALE_TRANSFORM
            it[1].transformName = Constants.Service.SEPIA_TRANSFORM
            it[2].transformName = Constants.Service.TINT_TRANSFORM
        }

        every { transforms.grayScale(any(), any()) } answers { firstArg() }
        every { transforms.sepia(any(), any()) } answers { firstArg() }
        every { transforms.tint(any(), any(), any(), any(), any()) } answers { firstArg() }

        for (transformedImage in expected) {
            Assertions.assertThat(
                service.applyTransform(
                    transformedImage.imageName,
                    transformedImage.transformName,
                    imageBytes
                )
            ).isEqualTo(
                TransformedImage(
                    transformedImage.imageName,
                    transformedImage.transformName,
                    imageBytes
                )
            )
        }

        verify(exactly = 1) {
            transforms.grayScale(any(), any())
            transforms.sepia(any(), any())
            transforms.tint(any(), any(), any(), any(), any())
        }
    }
}