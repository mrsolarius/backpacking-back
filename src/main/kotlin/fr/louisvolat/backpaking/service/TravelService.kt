package fr.louisvolat.backpaking.service

import fr.louisvolat.backpaking.model.Travel
import fr.louisvolat.backpaking.model.User
import fr.louisvolat.backpaking.repository.PictureRepository
import fr.louisvolat.backpaking.repository.TravelRepository
import fr.louisvolat.backpaking.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

const val USER_NOT_FOUND = "User not found"

@Service
class TravelService(
    private val travelRepository: TravelRepository,
    private val userRepository: UserRepository,
    private val pictureRepository: PictureRepository
) {

    fun getAllTravels(): List<Travel> {
        return travelRepository.findAll()
    }
    
    fun getTravelsByUser(userId: Long): List<Travel> {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND) }
        return travelRepository.findByUser(user)
    }

    fun getTravelById(id: Long): Travel? {
        return travelRepository.findById(id).orElse(null)
    }

    // Dans TravelService.kt
    fun getTravelsByUserEmail(email: String): List<Travel> {
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND)
        return travelRepository.findByUser(user)
    }

    fun createTravel(user: User, name: String, description: String, startDate: LocalDateTime, endDate: LocalDateTime?): Travel {

        val travel = Travel(
            name = name,
            description = description,
            startDate = startDate,
            endDate = endDate,
            user = user
        )
        
        return travelRepository.save(travel)
    }

    fun updateTravel(id: Long, name: String?, description: String?, startDate: LocalDateTime?, endDate: LocalDateTime?, coverPictureId: Long?): Travel {
        val travel = travelRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found") }
        
        if (name != null) travel.name = name
        if (description != null) travel.description = description
        if (startDate != null) travel.startDate = startDate
        if (endDate != null) travel.endDate = endDate
        
        if (coverPictureId != null) {
            val picture = pictureRepository.findById(coverPictureId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Picture not found") }
                
            if (picture.travel.id != travel.id) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Picture does not belong to this travel")
            }
            
            travel.coverPicture = picture
        }
        
        travel.updatedAt = LocalDateTime.now()
        return travelRepository.save(travel)
    }

    fun deleteTravel(id: Long): Boolean {
        val travel = travelRepository.findById(id).orElse(null) ?: return false
        
        // Note: We don't need to delete pictures/coordinates manually
        // as they are configured with CascadeType.ALL and orphanRemoval = true
        
        travelRepository.delete(travel)
        return true
    }
}
