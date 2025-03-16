package fr.louisvolat.backpaking.service.utils.images

import fr.louisvolat.backpaking.service.utils.StorageService
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

@Service
class ImageConverterService(
    private val storageService: StorageService,
    private val ffmpegService: FFmpegService,
    private val imageProcessor: ImageProcessor
) {
    private val logger = LoggerFactory.getLogger(ImageConverterService::class.java)

    @Value("\${app.webp.quality:80}")
    private var webpQuality: Int = 80

    @Value("\${app.image.format:jpg}")
    private var fallbackFormat: String = "jpg"

    // Pool d'exécuteurs pour le traitement parallèle
    private val executorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    )

    // Formats de conversion à générer
    private val conversionFormats = mapOf(
        "desktop" to listOf(
            FormatConfig(1920, 0, 1),
            FormatConfig(3840, 0, 2),
            FormatConfig(5760, 0, 3)
        ),
        "tablet" to listOf(
            FormatConfig(1024, 0, 1),
            FormatConfig(2048, 0, 2),
            FormatConfig(3072, 0, 3)
        ),
        "mobile" to listOf(
            FormatConfig(640, 0, 1),
            FormatConfig(1280, 0, 2),
            FormatConfig(1920, 0, 3)
        ),
        "icon" to listOf(
            FormatConfig(64, 64, 1),
            FormatConfig(128, 128, 2),
            FormatConfig(192, 192, 3)
        )
    )

    // Cache pour éviter de relire l'image originale
    private var imageCache: BufferedImage? = null
    private var lastImagePath: String? = null

    // Tuple pour les dimensions de largeur et hauteur
    data class WidthHeightTuple(val width: Int, val height: Int)

    /**
     * Convertit l'image originale en plusieurs versions
     */
    fun convertToWebP(folderRelativePath: String, originalFilename: String): Map<String, List<String>> {
        val imagePath = storageService.getFullPath(Paths.get(folderRelativePath, originalFilename).toString())
        val originalImage = loadOriginalImage(imagePath)

        val results = mutableMapOf<String, MutableList<String>>()
        val futures = mutableListOf<Future<*>>()

        // Traitement pour chaque type d'appareil
        conversionFormats.forEach { (deviceType, formats) ->
            val deviceTypeFiles = mutableListOf<String>()
            results[deviceType] = deviceTypeFiles

            // Création du dossier pour ce type d'appareil
            val deviceFolder = storageService.createSubdirectory(folderRelativePath, deviceType)

            // Traitement de chaque format pour ce type d'appareil
            formats.forEach { format ->
                val future = executorService.submit {
                    if (originalImage != null) {
                        processImageFormat(originalImage, deviceType, format, deviceFolder, deviceTypeFiles)
                    }else{
                        logger.error("Failed to load original image for $deviceType ${format.width}x${format.height}")
                    }
                }
                futures.add(future)
            }
        }

        // Attendre que toutes les tâches soient terminées
        futures.forEach { it.get() }

        return results
    }

    /**
     * Charge l'image originale, avec mise en cache
     */
    private fun loadOriginalImage(imagePath: Path): BufferedImage? {
        return if (imagePath.toString() == lastImagePath) {
            imageCache
        } else {
            val img = storageService.readImage(imagePath)
            imageCache = img
            lastImagePath = imagePath.toString()
            img
        }
    }



    /**
     * Traite un format d'image spécifique
     */
    private fun processImageFormat(
        originalImage: BufferedImage,
        deviceType: String,
        format: FormatConfig,
        deviceFolder: Path,
        deviceTypeFiles: MutableList<String>
    ) {
        try {
            val isIcon = deviceType == "icon"
            val (targetWidth, targetHeight) = imageProcessor.calculateDimensions(
                originalImage.width, originalImage.height, format.width, format.height
            )

            // Essayer d'abord la conversion WebP
            if (convertImageToWebP(originalImage, deviceType, format, deviceFolder, deviceTypeFiles, WidthHeightTuple(targetWidth, targetHeight), isIcon)) {
                return
            }

            // Si la conversion WebP échoue, utiliser le format de repli
            convertImageToFallbackFormat(originalImage, deviceType, format, deviceFolder, deviceTypeFiles, WidthHeightTuple(targetWidth,targetHeight), isIcon)
        } catch (e: Exception) {
            logger.error("Error processing image for $deviceType ${format.width}x${format.height}: ${e.message}")
        }
    }

    /**
     * Tente de convertir l'image en WebP
     * @return true si la conversion a réussi, false sinon
     */
    private fun convertImageToWebP(
        originalImage: BufferedImage,
        deviceType: String,
        format: FormatConfig,
        deviceFolder: Path,
        deviceTypeFiles: MutableList<String>,
        targetWidthHeight: WidthHeightTuple,
        isIcon: Boolean
    ): Boolean {
        val webpFilename = generateFilename(deviceType, format, "webp")
        val outputPath = deviceFolder.resolve(webpFilename)

        // Vérifier si le fichier existe déjà
        if (storageService.fileExists(outputPath)) {
            addToResultsList(deviceType, webpFilename, deviceTypeFiles)
            logger.info("File already exists, skipping conversion: $webpFilename")
            return true
        }

        // Préparer l'image temporaire
        val tempFile = storageService.createTempFile("resize-", ".jpg")

        try {
            // Préparer l'image source
            val processedImage = prepareSourceImage(originalImage, targetWidthHeight.width, targetWidthHeight.height, isIcon)

            // Enregistrer en tant que JPG temporaire
            val qualityAsFloat = webpQuality / 100f
            if (!storageService.saveAsJpeg(processedImage, tempFile, qualityAsFloat)) {
                return false
            }

            // Conversion avec FFmpeg
            if (ffmpegService.convertToWebP(tempFile, outputPath.toFile())) {
                addToResultsList(deviceType, webpFilename, deviceTypeFiles)
                logger.info("Successfully converted to WebP: $webpFilename")
                return true
            }

            return false
        } finally {
            val delete = tempFile.delete()
            if (!delete) {
                logger.error("Failed to delete temporary file: ${tempFile.absolutePath}")
            }
        }
    }

    /**
     * Convertit l'image au format de repli (JPG ou PNG)
     */
    private fun convertImageToFallbackFormat(
        originalImage: BufferedImage,
        deviceType: String,
        format: FormatConfig,
        deviceFolder: Path,
        deviceTypeFiles: MutableList<String>,
        targetWidthHeight: WidthHeightTuple,
        isIcon: Boolean
    ) {
        val fallbackFilename = generateFilename(deviceType, format, fallbackFormat)
        val fallbackPath = deviceFolder.resolve(fallbackFilename)

        // Vérifier si le fichier existe déjà
        if (storageService.fileExists(fallbackPath)) {
            addToResultsList(deviceType, fallbackFilename, deviceTypeFiles)
            logger.info("Fallback file already exists: $fallbackFilename")
            return
        }

        // Préparer l'image source
        val processedImage = prepareSourceImage(originalImage, targetWidthHeight.width, targetWidthHeight.height, isIcon)

        // Enregistrer au format de repli
        val saved = if (fallbackFormat == "png") {
            storageService.saveAsPng(processedImage, fallbackPath.toFile())
        } else {
            storageService.saveAsJpeg(processedImage, fallbackPath.toFile(), webpQuality / 100f)
        }

        if (saved) {
            addToResultsList(deviceType, fallbackFilename, deviceTypeFiles)
            logger.info("Saved as fallback format $fallbackFormat: $fallbackFilename")
        }
    }

    /**
     * Prépare l'image source (redimensionnement ou rognage)
     */
    private fun prepareSourceImage(
        originalImage: BufferedImage,
        targetWidth: Int,
        targetHeight: Int,
        isIcon: Boolean
    ): BufferedImage {
        return if (isIcon && targetWidth == targetHeight) {
            imageProcessor.cropCenterSquare(originalImage, targetWidth)
        } else {
            imageProcessor.resizeImage(originalImage, targetWidth, targetHeight)
        }
    }

    /**
     * Ajoute un fichier à la liste des résultats de manière thread-safe
     */
    private fun addToResultsList(deviceType: String, filename: String, deviceTypeFiles: MutableList<String>) {
        synchronized(deviceTypeFiles) {
            deviceTypeFiles.add("$deviceType/$filename")
        }
    }

    /**
     * Génère un nom de fichier pour l'image convertie
     */
    private fun generateFilename(deviceType: String, format: FormatConfig, extension: String): String {
        return if (deviceType == "icon") {
            "icon-${format.scale}x.$extension"
        } else {
            "${format.width}-${format.scale}x.$extension"
        }
    }

    /**
     * Classe de configuration pour les formats d'image
     */
    data class FormatConfig(val width: Int, val height: Int, val scale: Int)

    /**
     * Nettoie les ressources lors de l'arrêt de l'application
     */
    @PreDestroy
    fun cleanup() {
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
        }

        // Nettoyer le cache
        imageCache = null
        lastImagePath = null
    }
}