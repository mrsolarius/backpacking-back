package fr.louisvolat.backpaking.dto

import fr.louisvolat.backpaking.model.Coordinate
import java.time.LocalDateTime
import java.time.ZonedDateTime

data class CoordinateDTO(
    val id: Long?,
    val latitude: String,
    val longitude: String,
    val date: LocalDateTime,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromEntity(coordinate: Coordinate): CoordinateDTO {
            return CoordinateDTO(
                id = coordinate.id,
                latitude = coordinate.latitude,
                longitude = coordinate.longitude,
                date = coordinate.date,
                createdAt = coordinate.createdAt.toString(),
                updatedAt = coordinate.updatedAt.toString()
            )
        }
    }
}

data class CreateCoordinateRequest(
    val latitude: String,
    val longitude: String,
    val date: ZonedDateTime
)

data class CreateCoordinatesRequest(
    val coordinates: List<CreateCoordinateRequest>
)

data class CreateCoordinateResponseConfirm(
    val savedCoordinate: Long,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime
)