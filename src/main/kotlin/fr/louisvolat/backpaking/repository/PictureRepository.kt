package fr.louisvolat.backpaking.repository

import fr.louisvolat.backpaking.model.Picture
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PictureRepository : JpaRepository<Picture, Long>