package fr.louisvolat.backpaking.dto

import fr.louisvolat.backpaking.model.Travel
import java.time.ZoneOffset
import java.time.ZonedDateTime

data class TravelDTO(
    val id: Long?,
    val name: String,
    val description: String,
    val startDate: String,
    val endDate: String?,
    val coverPicture: PictureDTO?,
    val userId: Long,
    val coordinates: List<CoordinateDTO>?,
    val pictures: List<PictureDTO>?,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromEntity(travel: Travel, includeDetails: Boolean = false): TravelDTO {
            return TravelDTO(
                id = travel.id,
                name = travel.name,
                description = travel.description,
                startDate = travel.startDate.atZone(ZoneOffset.UTC).toString(),
                endDate = travel.endDate?.atZone(ZoneOffset.UTC)?.toString(),
                coverPicture = travel.coverPicture?.let { PictureDTO.fromEntity(it) },
                userId = travel.user.id!!,
                coordinates = if (includeDetails) travel.coordinates.map { CoordinateDTO.fromEntity(it) } else null,
                pictures = if (includeDetails) travel.travelPictures.map { PictureDTO.fromEntity(it) } else null,
                createdAt = travel.createdAt.atZone(ZoneOffset.UTC).toString(),
                updatedAt = travel.updatedAt.atZone(ZoneOffset.UTC).toString()
            )
        }
    }
}

data class CreateTravelRequest(
    val name: String,
    val description: String,
    val startDate: ZonedDateTime
)

data class UpdateTravelRequest(
    val name: String? = null,
    val description: String? = null,
    val startDate: ZonedDateTime? = null,
    val endDate: ZonedDateTime? = null,
    val coverPictureId: Long? = null
)