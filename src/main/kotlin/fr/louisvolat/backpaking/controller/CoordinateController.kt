package fr.louisvolat.backpaking.controller

import fr.louisvolat.backpaking.dto.CreateCoordinateResponseConfirm
import fr.louisvolat.backpaking.dto.CreateCoordinatesRequest
import fr.louisvolat.backpaking.model.Coordinate
import fr.louisvolat.backpaking.security.annotation.SecuredMethode
import fr.louisvolat.backpaking.service.CoordinateService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.ZoneId

@RestController
@RequestMapping("/api/coordinates")
class CoordinateController(private val coordinateService: CoordinateService) {

    @GetMapping
    fun index(): List<Coordinate> {
        return coordinateService.getAllCoordinates()
    }

    @SecuredMethode
    @PostMapping
    fun store(@Valid @RequestBody request: CreateCoordinatesRequest): CreateCoordinateResponseConfirm {
        val savedCoordinates = mutableListOf<Coordinate>()

        request.coordinates.forEach { coordinateReq ->
            val coordinate = coordinateService.saveCoordinate(
                coordinateReq.latitude,
                coordinateReq.longitude,
                coordinateReq.date.toLocalDateTime()
            )
            savedCoordinates.add(coordinate)
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
    fun destroy(@PathVariable id: Long): ResponseEntity<String> {
        val success = coordinateService.deleteCoordinate(id)

        return if (success) {
            ResponseEntity("Coordinate deleted", HttpStatus.OK)
        } else {
            ResponseEntity("Coordinate not found", HttpStatus.NOT_FOUND)
        }
    }
}