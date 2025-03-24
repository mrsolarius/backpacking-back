package fr.louisvolat.backpaking.util

import fr.louisvolat.backpaking.exception.EntityNotFoundException
import org.springframework.data.repository.CrudRepository
import java.util.Optional

inline fun <reified T, ID : Any> CrudRepository<T, ID>.findByIdOrThrow(id: ID): T {
    val entityName = T::class.simpleName ?: "Entity"
    return findById(id).orElseThrow {
        EntityNotFoundException(entityName, id.toString())
    }
}

// Extension pour Optional qui utilise également la réflexion
inline fun <reified T> Optional<T>.orThrowNotFound(id: Any): T {
    val entityName = T::class.simpleName ?: "Entity"
    return this.orElseThrow { EntityNotFoundException(entityName, id) }
}