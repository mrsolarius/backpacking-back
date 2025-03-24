package fr.louisvolat.backpaking.service

import fr.louisvolat.backpaking.model.Coordinate
import fr.louisvolat.backpaking.repository.CoordinateRepository
import fr.louisvolat.backpaking.repository.TravelRepository
import fr.louisvolat.backpaking.util.findByIdOrThrow
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

private const val TRAVEL_NOT_FOUND = "Travel not found"

@Service
class CoordinateService(
    private val coordinateRepository: CoordinateRepository,
    private val travelRepository: TravelRepository
) {

    fun getCoordinatesByTravelId(travelId: Long): List<Coordinate> {
        val travel = travelRepository.findByIdOrThrow(travelId)
        return travel.coordinates
    }

    fun saveCoordinate(travelId: Long, latitude: String, longitude: String, date: LocalDateTime): Coordinate {
        val travel = travelRepository.findById(travelId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, TRAVEL_NOT_FOUND) }

        val coordinate = Coordinate(
            latitude = latitude,
            longitude = longitude,
            date = date,
            travel = travel
        )

        // Mise à jour des dates de début et de fin du voyage
        if (travel.startDate.isAfter(date)) {
            travel.startDate = date
            travel.updatedAt = LocalDateTime.now()
            travelRepository.save(travel)
        }

        if (travel.endDate == null || travel.endDate!!.isBefore(date)) {
            travel.endDate = date
            travel.updatedAt = LocalDateTime.now()
            travelRepository.save(travel)
        }

        return coordinateRepository.save(coordinate)
    }

    fun deleteCoordinate(travelId: Long, coordinateId: Long): Boolean {
        val travel = travelRepository.findById(travelId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, TRAVEL_NOT_FOUND) }

        val coordinate = coordinateRepository.findById(coordinateId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Coordinate not found") }

        if (coordinate.travel.id != travel.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Coordinate does not belong to this travel")
        }

        coordinateRepository.delete(coordinate)
        return true
    }
}