package fr.louisvolat.backpaking.repository

import fr.louisvolat.backpaking.model.Coordinate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CoordinateRepository : JpaRepository<Coordinate, Long>