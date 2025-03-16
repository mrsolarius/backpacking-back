package fr.louisvolat.backpaking.service.utils.images

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.awt.RenderingHints
import java.awt.image.BufferedImage

@Service
class ImageProcessor {
    private val logger = LoggerFactory.getLogger(ImageProcessor::class.java)

    /**
     * Redimensionne une image aux dimensions spécifiées
     * @param image Image source
     * @param width Largeur cible
     * @param height Hauteur cible
     * @return Image redimensionnée
     */
    fun resizeImage(image: BufferedImage, width: Int, height: Int): BufferedImage {
        // Si les dimensions sont identiques, pas besoin de redimensionner
        if (width == image.width && height == image.height) {
            return image
        }

        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g2d = result.createGraphics()

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.drawImage(image, 0, 0, width, height, null)
        g2d.dispose()

        return result
    }

    /**
     * Calcule les dimensions cibles en préservant le ratio d'aspect
     * @param originalWidth Largeur originale
     * @param originalHeight Hauteur originale
     * @param targetWidth Largeur cible
     * @param targetHeight Hauteur cible (0 pour calculer automatiquement)
     * @return Paire de dimensions cibles (largeur, hauteur)
     */
    fun calculateDimensions(originalWidth: Int, originalHeight: Int, targetWidth: Int, targetHeight: Int): Pair<Int, Int> {
        var finalWidth = targetWidth
        var finalHeight = targetHeight

        // Si la hauteur est 0, on la calcule en préservant le ratio
        if (finalHeight == 0) {
            val ratio = finalWidth.toDouble() / originalWidth
            finalHeight = (originalHeight * ratio).toInt()
        }

        return Pair(finalWidth, finalHeight)
    }

    /**
     * Rogne une image au centre pour obtenir un carré
     * @param image Image source
     * @param targetSize Taille du carré cible
     * @return Image carrée rognée et redimensionnée
     */
    fun cropCenterSquare(image: BufferedImage, targetSize: Int): BufferedImage {
        val sourceWidth = image.width
        val sourceHeight = image.height

        // Court-circuit si l'image est déjà un carré de la bonne taille
        if (sourceWidth == sourceHeight && sourceWidth == targetSize) {
            return image
        }

        // Déterminer la dimension la plus petite pour faire un carré
        val size = minOf(sourceWidth, sourceHeight)

        // Calculer les coordonnées de rognage pour centrer l'image
        val x = (sourceWidth - size) / 2
        val y = (sourceHeight - size) / 2

        // Rogner l'image en carré
        val cropped = image.getSubimage(x, y, size, size)

        // Si la taille cible est la même que la taille rognée, pas besoin de redimensionner
        if (size == targetSize) {
            return cropped
        }

        // Redimensionner au format d'icône cible
        return resizeImage(cropped, targetSize, targetSize)
    }
}