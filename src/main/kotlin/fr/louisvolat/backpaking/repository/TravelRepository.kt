package fr.louisvolat.backpaking.repository

import fr.louisvolat.backpaking.model.Travel
import fr.louisvolat.backpaking.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TravelRepository : JpaRepository<Travel, Long> {
    fun findByUser(user: User): List<Travel>
}