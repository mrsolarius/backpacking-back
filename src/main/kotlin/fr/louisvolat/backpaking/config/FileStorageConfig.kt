package fr.louisvolat.backpaking.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Path
import java.nio.file.Paths

@Configuration
class FileStorageConfig : WebMvcConfigurer {

    @Value("\${app.upload.dir}")
    private lateinit var uploadDir: String

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val uploadPath: Path = Paths.get(uploadDir)
        val uploadAbsolutePath = uploadPath.toFile().absolutePath

        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:$uploadAbsolutePath/")
    }
}