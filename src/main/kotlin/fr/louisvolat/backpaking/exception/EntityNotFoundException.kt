// Dans un fichier Exceptions.kt
package fr.louisvolat.backpaking.exception

import fr.louisvolat.backpaking.model.Picture
import fr.louisvolat.backpaking.model.Travel
import fr.louisvolat.backpaking.model.User
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
open class EntityNotFoundException(entityType: String, id: Any) :
    RuntimeException("$entityType with id $id not found")

// Versions spécifiques pour plus de clarté
class TravelNotFoundException(id: Long) :
    EntityNotFoundException(Travel::class.simpleName.toString() , id)

class UserNotFoundException(id: Any) :
    EntityNotFoundException(User::class.simpleName.toString(), id)

class PictureNotFoundException(id: Long) :
    EntityNotFoundException(Picture::class.simpleName.toString(), id)