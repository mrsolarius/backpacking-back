package fr.louisvolat.backpaking.dto

import fr.louisvolat.backpaking.model.Travel
import java.time.ZonedDateTime

data class TravelDTO(
    val id: Long?,
    val name: String,
    val description: String,
    val startDate: String,
    val endDate: String?,
    val coverPictureId: Long?,
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
                startDate = travel.startDate.toString(),
                endDate = travel.endDate?.toString(),
                coverPictureId = travel.coverPicture?.id,
                userId = travel.user.id!!,
                coordinates = if (includeDetails) travel.coordinates.map { CoordinateDTO.fromEntity(it) } else null,
                pictures = if (includeDetails) travel.travelPictures.map { PictureDTO.fromEntity(it) } else null,
                createdAt = travel.createdAt.toString(),
                updatedAt = travel.updatedAt.toString()
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