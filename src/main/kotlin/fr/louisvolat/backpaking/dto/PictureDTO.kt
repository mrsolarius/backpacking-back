package fr.louisvolat.backpaking.dto

import fr.louisvolat.backpaking.model.Picture
import fr.louisvolat.backpaking.model.PictureVersions
import java.time.ZoneOffset


data class PictureDTO(
    val id: Long?,
    val path: String,
    val latitude: String,
    val longitude: String,
    val altitude: String?,
    val date: String,
    val createdAt: String,
    val updatedAt: String,
    val versions: Map<String, List<PictureVersionsDTO>>
) {
    companion object {
        fun fromEntity(picture: Picture): PictureDTO {
            return PictureDTO(
                id = picture.id,
                path = picture.rawVersion,
                latitude = picture.latitude,
                longitude = picture.longitude,
                altitude = picture.altitude,
                date = picture.date.atZone(ZoneOffset.UTC).toString(),
                createdAt = picture.createdAt.atZone(ZoneOffset.UTC).toString(),
                updatedAt = picture.updatedAt.atZone(ZoneOffset.UTC).toString(),
                versions = picture.versions
                    .groupBy { it.versionType }
                    .mapValues { (_, versions) ->
                        versions.map { PictureVersionsDTO.fromEntity(it) }
                    }
            )
        }
    }
}

data class PictureVersionsDTO(
    val id: Long?,
    val pictureId: Long,
    val path: String,
    val resolution: Byte,
    val versionType: String,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromEntity(pictureVersion: PictureVersions): PictureVersionsDTO {
            return PictureVersionsDTO(
                id = pictureVersion.id,
                pictureId = pictureVersion.picture.id!!,
                path = pictureVersion.path,
                resolution = pictureVersion.resolution,
                versionType = pictureVersion.versionType,
                createdAt = pictureVersion.createdAt.atZone(ZoneOffset.UTC).toString(),
                updatedAt = pictureVersion.updatedAt.atZone(ZoneOffset.UTC).toString()
            )
        }
    }
}