package fr.louisvolat.backpaking.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.TimeUnit

@Service
class FFmpegService {
    private val logger = LoggerFactory.getLogger(FFmpegService::class.java)

    @Value("\${app.webp.quality:80}")
    private var webpQuality: Int = 80 // Qualité par défaut de 80%

    /**
     * Convertit une image en WebP à l'aide de FFmpeg
     * @param inputFile Fichier d'entrée
     * @param outputFile Fichier de sortie
     * @return true si la conversion a réussi, false sinon
     */
    fun convertToWebP(inputFile: File, outputFile: File): Boolean {
        try {
            // Commande FFmpeg optimisée pour la conversion en WebP
            val command = arrayOf(
                "ffmpeg",
                "-i", inputFile.absolutePath,
                "-c:v", "libwebp",
                "-quality", webpQuality.toString(),
                "-lossless", "0",
                "-compression_level", "6",
                "-preset", "picture", // Optimisation pour les images statiques
                "-threads", Runtime.getRuntime().availableProcessors().toString(), // Utilisation de tous les cœurs disponibles
                "-y", // Remplacer le fichier existant
                outputFile.absolutePath
            )

            val process = ProcessBuilder(*command)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            // Attendre que le processus se termine
            val completed = process.waitFor(15, TimeUnit.SECONDS) // Timeout de 15 secondes
            if (!completed) {
                process.destroyForcibly()
                logger.error("FFmpeg process timed out")
                return false
            }

            return process.exitValue() == 0
        } catch (e: Exception) {
            logger.error("Error using FFmpeg for WebP conversion: ${e.message}")
            return false
        }
    }
}