package fr.louisvolat.backpaking.dto

import fr.louisvolat.backpaking.model.Picture


data class PictureDTO(
    val id: Long?,
    val path: String,
    val latitude: String,
    val longitude: String,
    val altitude: String?,
    val date: String,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromEntity(picture: Picture): PictureDTO {
            return PictureDTO(
                id = picture.id,
                path = picture.path,
                latitude = picture.latitude,
                longitude = picture.longitude,
                altitude = picture.altitude,
                date = picture.date.toString(),
                createdAt = picture.createdAt.toString(),
                updatedAt = picture.updatedAt.toString()
            )
        }
    }
}