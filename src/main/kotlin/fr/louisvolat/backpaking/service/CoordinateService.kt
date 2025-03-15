package fr.louisvolat.backpaking.service

import fr.louisvolat.backpaking.model.Coordinate
import fr.louisvolat.backpaking.repository.CoordinateRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CoordinateService(private val coordinateRepository: CoordinateRepository) {

    fun getAllCoordinates(): List<Coordinate> {
        return coordinateRepository.findAll()
    }

    fun getCoordinateById(id: Long): Coordinate? {
        return coordinateRepository.findById(id).orElse(null)
    }

    fun saveCoordinate(latitude: String, longitude: String, date: LocalDateTime): Coordinate {
        val coordinate = Coordinate(
            latitude = latitude,
            longitude = longitude,
            date = date
        )
        return coordinateRepository.save(coordinate)
    }

    fun updateCoordinate(id: Long, latitude: String, longitude: String, date: LocalDateTime): Coordinate? {
        val coordinate = coordinateRepository.findById(id).orElse(null) ?: return null
        coordinate.latitude = latitude
        coordinate.longitude = longitude
        coordinate.date = date
        coordinate.updatedAt = LocalDateTime.now()
        return coordinateRepository.save(coordinate)
    }

    fun deleteCoordinate(id: Long): Boolean {
        val coordinate = coordinateRepository.findById(id).orElse(null) ?: return false
        coordinateRepository.delete(coordinate)
        return true
    }
}