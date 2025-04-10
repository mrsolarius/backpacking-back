package fr.louisvolat.backpaking.service

import com.drew.imaging.ImageMetadataReader
import com.drew.lang.Rational
import com.drew.metadata.Metadata
import com.drew.metadata.exif.GpsDirectory
import fr.louisvolat.backpaking.model.Picture
import fr.louisvolat.backpaking.model.PictureVersions
import fr.louisvolat.backpaking.repository.PictureRepository
import fr.louisvolat.backpaking.repository.TravelRepository
import fr.louisvolat.backpaking.service.utils.StorageService
import fr.louisvolat.backpaking.service.utils.images.ImageConverterService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.time.LocalDateTime

private const val TRAVEL_NOT_FOUND = "Travel not found"

@Service
class PictureService(
    private val pictureRepository: PictureRepository,
    private val travelRepository: TravelRepository,
    private val storageService: StorageService,
    private val imageConverterService: ImageConverterService
) {

    @Value("\${app.upload.dir}")
    private lateinit var uploadDir: String

    @Value("\${app.root.url}")
    private lateinit var rootUrl: String

    fun getPicturesByTravelId(travelId: Long): List<Picture> {
        val travel = travelRepository.findById(travelId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, TRAVEL_NOT_FOUND) }
        return travel.travelPictures
    }

    fun getPictureById(id: Long): Picture? =
        pictureRepository.findById(id).orElse(null)

    fun savePicture(travelId: Long, file: MultipartFile): Result<Picture> {
        try {
            val travel = travelRepository.findById(travelId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, TRAVEL_NOT_FOUND) }

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

            // Création du chemin complet d'accès depuis le navigateur
            val displayRelativePath = "$uploadDir/$relativeFolderPath"

            // Création de l'objet Picture avec le chemin complet incluant le dossier quotidien et le sous-dossier
            val picture = Picture(
                rawVersion = "/$displayRelativePath/$originalFilename",
                latitude = gpsData.first.toString(),
                longitude = gpsData.second.toString(),
                altitude = gpsData.third,
                date = dateTime,
                travel = travel
            )

            // Création des versions de l'image
            val pictureVersions = mutableListOf<PictureVersions>()

            convertedImages.forEach { (type, paths) ->
                paths.forEach { (path, scale) ->
                    pictureVersions.add(
                        PictureVersions(
                            picture = picture,
                            path = "/$displayRelativePath/$path",
                            resolution = scale.toByte(),
                            versionType = type
                        )
                    )
                }
            }

            picture.versions = pictureVersions

            // Suppression du fichier temporaire
            Files.deleteIfExists(tempFile)

            if (travel.startDate.isAfter(dateTime)) {
                travel.startDate = dateTime
                travel.updatedAt = LocalDateTime.now()
                travelRepository.save(travel)
            }

            if (travel.endDate == null || travel.endDate!!.isBefore(dateTime)) {
                travel.endDate = dateTime
                travel.updatedAt = LocalDateTime.now()
                travelRepository.save(travel)
            }

            return Result.success(pictureRepository.save(picture))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun deletePicture(travelId: Long, pictureId: Long): Boolean {
        val travel = travelRepository.findById(travelId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, TRAVEL_NOT_FOUND) }

        val picture = pictureRepository.findById(pictureId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Picture not found") }

        if (picture.travel.id != travel.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Picture does not belong to this travel")
        }

        // Extraction du chemin du dossier parent (dossier quotidien/UUID)
        val path = picture.rawVersion
        val folderPath = path.substringAfterLast("$uploadDir/").substringBeforeLast('/')

        // Suppression du dossier complet contenant toutes les versions de l'image
        val deleted = storageService.deleteDirectory(folderPath)

        // Handle removing from cover picture if it's being used
        if (picture.id == travel.coverPicture?.id) {
            travel.coverPicture = null
            travel.updatedAt = LocalDateTime.now()
            travelRepository.save(travel)
        }

        if (deleted) {
            pictureRepository.delete(picture)
            return true
        }
        return false
    }

    fun setCoverPicture(travelId: Long, pictureId: Long): Boolean {
        val travel = travelRepository.findById(travelId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, TRAVEL_NOT_FOUND) }

        val picture = pictureRepository.findById(pictureId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Picture not found") }

        if (picture.travel.id != travel.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Picture does not belong to this travel")
        }

        travel.coverPicture = picture
        travel.updatedAt = LocalDateTime.now()
        travelRepository.save(travel)
        return true
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