package fr.louisvolat.backpaking.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.imageio.ImageIO
import javax.imageio.IIOImage
import javax.imageio.ImageWriteParam
import org.slf4j.LoggerFactory

@Service
class StorageService {
    private val logger = LoggerFactory.getLogger(StorageService::class.java)

    @Value("\${app.upload.dir}")
    private lateinit var uploadDir: String

    @Value("\${app.webp.quality:80}")
    private var webpQuality: Int = 80

    // Crée un fichier temporaire à partir du fichier uploadé
    fun createTemporaryFile(file: MultipartFile): Path =
        Files.createTempFile("upload-", "-" + file.originalFilename).also {
            file.transferTo(it.toFile())
        }

    // Stocke le fichier dans un dossier quotidien avec un sous-dossier unique par image
    fun storeFile(file: MultipartFile, tempFile: Path): Pair<String, String> {
        val originalFilename = file.originalFilename!!
        val extension = originalFilename.substringAfterLast('.', "")

        // Génération d'un UUID pour le sous-dossier de l'image
        val imageUuid = UUID.randomUUID().toString()

        // Création du chemin basé sur la date du jour (YYYY-MM-DD)
        val today = LocalDate.now()
        val dateFolder = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Structure des dossiers: uploadDir/YYYY-MM-DD/UUID/
        val relativePath = "$dateFolder/$imageUuid"
        val imageFolderPath = Paths.get(uploadDir, dateFolder, imageUuid)

        // Création des dossiers s'ils n'existent pas
        if (!Files.exists(imageFolderPath)) {
            Files.createDirectories(imageFolderPath)
        }

        // Nom du fichier original
        val filename = "original.$extension"
        val destinationFile = imageFolderPath.resolve(filename)

        // Copie du fichier temporaire vers sa destination finale
        Files.copy(tempFile, destinationFile)

        // Retourne le chemin relatif du dossier et le nom du fichier
        return Pair(relativePath, filename)
    }

    // Vérifie si un fichier existe et le supprime
    fun deleteFile(relativePath: String): Boolean {
        try {
            val filePath = Paths.get(uploadDir, relativePath)
            return Files.deleteIfExists(filePath)
        } catch (e: Exception) {
            logger.error("Failed to delete file: ${e.message}")
            return false
        }
    }

    // Supprime un dossier et tout son contenu
    fun deleteDirectory(relativePath: String): Boolean {
        try {
            val dirPath = Paths.get(uploadDir, relativePath)
            if (Files.exists(dirPath)) {
                Files.walk(dirPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.delete(it) }
                return true
            }
            return false
        } catch (e: Exception) {
            logger.error("Failed to delete directory: ${e.message}")
            return false
        }
    }

    // Obtient le chemin complet à partir d'un chemin relatif
    fun getFullPath(relativePath: String): Path {
        return Paths.get(uploadDir, relativePath)
    }

    // Crée un sous-dossier s'il n'existe pas déjà
    fun createSubdirectory(relativePath: String, subdirectory: String): Path {
        val subfolderPath = Paths.get(uploadDir, relativePath, subdirectory)
        if (!Files.exists(subfolderPath)) {
            Files.createDirectories(subfolderPath)
        }
        return subfolderPath
    }

    // Vérifie si un fichier existe
    fun fileExists(fullPath: Path): Boolean {
        return Files.exists(fullPath)
    }

    // Lire une image depuis le disque
    fun readImage(imagePath: Path): BufferedImage {
        ImageIO.setUseCache(false)
        return ImageIO.read(imagePath.toFile())
    }

    // Créer un fichier temporaire pour le traitement
    fun createTempFile(prefix: String, suffix: String): File {
        return File.createTempFile(prefix, suffix)
    }

    // Enregistre une image au format JPEG
    fun saveAsJpeg(image: BufferedImage, outputFile: File, quality: Float): Boolean {
        return try {
            ImageIO.setUseCache(false)

            val jpgWriter = ImageIO.getImageWritersByFormatName("jpeg").next()
            val writeParam = jpgWriter.defaultWriteParam
            writeParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
            writeParam.compressionQuality = quality

            val outputStream = FileOutputStream(outputFile)
            val ios = ImageIO.createImageOutputStream(outputStream)

            jpgWriter.output = ios
            jpgWriter.write(null, IIOImage(image, null, null), writeParam)

            ios.close()
            outputStream.close()
            jpgWriter.dispose()

            true
        } catch (e: Exception) {
            logger.error("Error saving as JPEG: ${e.message}")
            false
        }
    }

    // Enregistre une image au format PNG
    fun saveAsPng(image: BufferedImage, outputFile: File): Boolean {
        return try {
            ImageIO.setUseCache(false)
            ImageIO.write(image, "png", outputFile)

            true
        } catch (e: Exception) {
            logger.error("Error saving as PNG: ${e.message}")
            false
        }
    }
}