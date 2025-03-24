package fr.louisvolat.backpaking.controller

import fr.louisvolat.backpaking.dto.CreateTravelRequest
import fr.louisvolat.backpaking.dto.TravelDTO
import fr.louisvolat.backpaking.dto.UpdateTravelRequest
import fr.louisvolat.backpaking.security.annotation.SecuredMethode
import fr.louisvolat.backpaking.service.TravelService
import fr.louisvolat.backpaking.service.USER_NOT_FOUND
import fr.louisvolat.backpaking.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/travels")
class TravelController(private val travelService: TravelService, private val userService: UserService) {

    @GetMapping
    fun index(): List<TravelDTO> {
        return travelService.getAllTravels().map { TravelDTO.fromEntity(it) }
    }
    
    @GetMapping("/user/{userId}")
    fun getByUser(@PathVariable userId: Long): List<TravelDTO> {
        return travelService.getTravelsByUser(userId).map { TravelDTO.fromEntity(it) }
    }

    @SecuredMethode
    @GetMapping("/mine")
    fun getMyTravels(@RequestParam(defaultValue = "false") includeDetails: Boolean): List<TravelDTO> {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.name

        return travelService.getTravelsByUserEmail(email)
            .map { TravelDTO.fromEntity(it, includeDetails) }
    }

    @GetMapping("/{id}")
    fun show(@PathVariable id: Long, @RequestParam(defaultValue = "false") includeDetails: Boolean): ResponseEntity<TravelDTO> {
        val travel = travelService.getTravelById(id)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)

        return ResponseEntity(TravelDTO.fromEntity(travel, includeDetails), HttpStatus.OK)
    }

    @SecuredMethode
    @PostMapping
    fun store(
        @Valid @RequestBody request: CreateTravelRequest
    ): ResponseEntity<TravelDTO> {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.name
        val user = userService.getUserByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND)
        val travel = travelService.createTravel(
            user = user,
            name = request.name,
            description = request.description,
            startDate = request.startDate.toLocalDateTime(),
            endDate = null
        )

        return ResponseEntity(TravelDTO.fromEntity(travel), HttpStatus.CREATED)
    }

    @SecuredMethode
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTravelRequest
    ): ResponseEntity<TravelDTO> {
        val travel = travelService.updateTravel(
            id = id,
            name = request.name,
            description = request.description,
            startDate = request.startDate?.toLocalDateTime(),
            endDate = request.endDate?.toLocalDateTime(),
            coverPictureId = request.coverPictureId
        )

        return ResponseEntity(TravelDTO.fromEntity(travel), HttpStatus.OK)
    }

    @SecuredMethode
    @DeleteMapping("/{id}")
    fun destroy(@PathVariable id: Long): ResponseEntity<String> {
        val success = travelService.deleteTravel(id)

        return if (success) {
            ResponseEntity("Travel deleted", HttpStatus.OK)
        } else {
            ResponseEntity("Travel not found", HttpStatus.NOT_FOUND)
        }
    }
}
