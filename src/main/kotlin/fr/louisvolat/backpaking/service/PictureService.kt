package fr.louisvolat.backpaking.service

import com.drew.imaging.ImageMetadataReader
import com.drew.lang.Rational
import com.drew.metadata.Metadata
import com.drew.metadata.exif.GpsDirectory
import fr.louisvolat.backpaking.model.Picture
import fr.louisvolat.backpaking.repository.PictureRepository
import fr.louisvolat.backpaking.service.utils.images.ImageConverterService
import fr.louisvolat.backpaking.service.utils.StorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.time.LocalDateTime

@Service
class PictureService(
    private val pictureRepository: PictureRepository,
    private val storageService: StorageService,
    private val imageConverterService: ImageConverterService
) {

    @Value("\${app.upload.dir}")
    private lateinit var uploadDir: String

    @Value("\${app.root.url}")
    private lateinit var rootUrl: String

    fun getAllPictures(): List<Picture> = pictureRepository.findAll()

    fun getPictureById(id: Long): Picture? =
        pictureRepository.findById(id).orElse(null)

    fun savePicture(file: MultipartFile): Result<Picture> {
        try {
            // Vérifier que le fichier est bien une image
            if (!isImageFile(file)) {
                return Result.failure(IllegalArgumentException("Not an image file"))
            }

            // Création d'un fichier temporaire pour traiter les métadonnées
            val tempFile = storageService.createTemporaryFile(file)

            // Lecture des métadonnées de l'image
            val metadata = ImageMetadataReader.readMetadata(tempFile.toFile())

            // Extraction des données GPS (latitude, longitude et altitude)
            val gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)
                ?: return Result.failure(IllegalArgumentException("L'image est incomplète ou ne contient pas d'info EXIF"))
            val gpsData = extractGpsData(gpsDirectory)
                ?: return Result.failure(IllegalArgumentException("L'image est incomplète ou ne contient pas d'info EXIF"))

            // Extraction de la date et de l'heure de prise de vue
            val dateTime = extractDateTime(metadata) ?: LocalDateTime.now()

            // Stockage du fichier dans un dossier quotidien avec sous-dossier unique
            val (relativeFolderPath, originalFilename) = storageService.storeFile(file, tempFile)

            // Conversion de l'image en différents formats WebP
            val convertedImages = imageConverterService.convertToWebP(relativeFolderPath, originalFilename)

            // Création du chemin complet d'acces depuis le navigateur
            val displayRelativePath = "$uploadDir/$relativeFolderPath"
            // Mapping des chemins des images converties en chemins relatifs
            val usableConvertedImageValue = mapConvertedImagesToRelativePaths(displayRelativePath, convertedImages)

            // Création de l'objet Picture avec le chemin complet incluant le dossier quotidien et le sous-dossier
            val picture = Picture(
                rawVersion = "$rootUrl/$displayRelativePath/$originalFilename",
                latitude = gpsData.first.toString(),
                longitude = gpsData.second.toString(),
                altitude = gpsData.third,
                date = dateTime,
                desktopVersions = usableConvertedImageValue["desktop"]?.joinToString(","),
                tabletVersions = usableConvertedImageValue["tablet"]?.joinToString(","),
                mobileVersions = usableConvertedImageValue["mobile"]?.joinToString(","),
                iconVersions = usableConvertedImageValue["icon"]?.joinToString(",")
            )

            // Suppression du fichier temporaire
            Files.deleteIfExists(tempFile)

            return Result.success(pictureRepository.save(picture))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun mapConvertedImagesToRelativePaths(
        relativeFolderPath: String,
        convertedImages: Map<String, List<String?>>
    ): Map<String, List<String?>> {
        return convertedImages.mapValues { (_, paths) ->
            paths.mapIndexed { index, path ->
                if (path != null) "$rootUrl/$relativeFolderPath/$path ${index + 1}x" else null
            }
        }
    }


    fun deletePicture(id: Long): Boolean {
        val picture = pictureRepository.findById(id).orElse(null) ?: return false

        // Extraction du chemin du dossier parent (dossier quotidien/UUID)
        val path = picture.rawVersion
        val folderPath = path.substringAfterLast("$uploadDir/").substringBeforeLast('/')

        // Suppression du dossier complet contenant toutes les versions de l'image
        val deleted = storageService.deleteDirectory(folderPath)

        if (deleted) {
            pictureRepository.delete(picture)
            return true
        }
        return false
    }

    // Vérifie que le fichier est bien une image
    private fun isImageFile(file: MultipartFile): Boolean =
        file.contentType?.startsWith("image/") == true

    // Extrait les informations GPS (latitude, longitude et altitude) depuis le répertoire GPS
    private fun extractGpsData(gpsDirectory: GpsDirectory): Triple<Double, Double, String?>? {
        val latValues = gpsDirectory.getRationalArray(GpsDirectory.TAG_LATITUDE)
        val lngValues = gpsDirectory.getRationalArray(GpsDirectory.TAG_LONGITUDE)
        if (latValues == null || lngValues == null) return null

        val lat = convertDMSToDecimal(latValues)
        val lng = convertDMSToDecimal(lngValues)
        val altitude = gpsDirectory.getRational(GpsDirectory.TAG_ALTITUDE)?.toDouble()?.toString()

        return Triple(lat, lng, altitude)
    }

    // Extrait la date de prise de vue depuis les métadonnées EXIF
    private fun extractDateTime(metadata: Metadata): LocalDateTime? {
        val exifDirectory = metadata.getFirstDirectoryOfType(com.drew.metadata.exif.ExifDirectoryBase::class.java)
        val dateTimeStr = exifDirectory?.getString(com.drew.metadata.exif.ExifDirectoryBase.TAG_DATETIME_ORIGINAL)
            ?: exifDirectory?.getString(com.drew.metadata.exif.ExifDirectoryBase.TAG_DATETIME)
        return dateTimeStr?.let { parseExifDateTime(it) }
    }

    // Convertit un tableau DMS en coordonnées décimales
    private fun convertDMSToDecimal(values: Array<Rational>): Double =
        values[0].toDouble() + values[1].toDouble() / 60.0 + values[2].toDouble() / 3600.0

    // Parse la date EXIF au format "yyyy:MM:dd HH:mm:ss"
    private fun parseExifDateTime(dateTimeStr: String): LocalDateTime {
        val parts = dateTimeStr.split(" ")
        val dateParts = parts[0].split(":")
        val timeParts = parts[1].split(":")

        return LocalDateTime.of(
            dateParts[0].toInt(),
            dateParts[1].toInt(),
            dateParts[2].toInt(),
            timeParts[0].toInt(),
            timeParts[1].toInt(),
            timeParts[2].toInt()
        )
    }
}