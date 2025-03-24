package fr.louisvolat.backpaking.controller

import fr.louisvolat.backpaking.dto.CoordinateDTO
import fr.louisvolat.backpaking.dto.CreateCoordinateResponseConfirm
import fr.louisvolat.backpaking.dto.CreateCoordinatesRequest
import fr.louisvolat.backpaking.security.annotation.SecuredMethode
import fr.louisvolat.backpaking.service.CoordinateService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.ZoneId

@RestController
@RequestMapping("/api/travels/{travelId}/coordinates")
class CoordinateController(private val coordinateService: CoordinateService) {

    @GetMapping
    fun index(@PathVariable travelId: Long): List<CoordinateDTO> {
        return coordinateService.getCoordinatesByTravelId(travelId).map { CoordinateDTO.fromEntity(it) }
    }

    @SecuredMethode
    @PostMapping
    fun store(
        @PathVariable travelId: Long,
        @Valid @RequestBody request: CreateCoordinatesRequest
    ): CreateCoordinateResponseConfirm {
        val savedCoordinates = mutableListOf<CoordinateDTO>()

        request.coordinates.forEach { coordinateReq ->
            val coordinate = coordinateService.saveCoordinate(
                travelId,
                coordinateReq.latitude,
                coordinateReq.longitude,
                coordinateReq.date.toLocalDateTime()
            )
            savedCoordinates.add(CoordinateDTO.fromEntity(coordinate))
        }

        val maxDate = savedCoordinates.maxByOrNull { it.date }?.date
        val minDate = savedCoordinates.minByOrNull { it.date }?.date

        if (maxDate == null || minDate == null) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        }

        return CreateCoordinateResponseConfirm(
            savedCoordinate = savedCoordinates.size.toLong(),
            startDate = minDate.atZone(ZoneId.of("UTC")),
            endDate = maxDate.atZone(ZoneId.of("UTC")),
        )
    }

    @SecuredMethode
    @DeleteMapping("/{id}")
    fun destroy(@PathVariable travelId: Long, @PathVariable id: Long): ResponseEntity<String> {
        val success = coordinateService.deleteCoordinate(travelId, id)

        return if (success) {
            ResponseEntity("Coordinate deleted", HttpStatus.OK)
        } else {
            ResponseEntity("Coordinate not found", HttpStatus.NOT_FOUND)
        }
    }
}