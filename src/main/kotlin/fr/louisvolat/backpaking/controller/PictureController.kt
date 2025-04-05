package fr.louisvolat.backpaking.controller

import fr.louisvolat.backpaking.dto.PictureDTO
import fr.louisvolat.backpaking.security.annotation.SecuredMethode
import fr.louisvolat.backpaking.service.PictureService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/travels/{travelId}/pictures")
class PictureController(private val pictureService: PictureService) {

    @GetMapping
    fun index(@PathVariable travelId: Long): List<PictureDTO> {
        return pictureService.getPicturesByTravelId(travelId).map { PictureDTO.fromEntity(it) }
    }

    @SecuredMethode
    @PostMapping("/{id}/set-as-cover")
    fun setCoverPicture(@PathVariable travelId: Long, @PathVariable id: Long): ResponseEntity<String> {
        val success = pictureService.setCoverPicture(travelId, id)

        return if (success) {
            ResponseEntity("Cover picture set successfully", HttpStatus.OK)
        } else {
            ResponseEntity("Failed to set cover picture", HttpStatus.BAD_REQUEST)
        }
    }

    @SecuredMethode
    @PostMapping
    fun store(
        @PathVariable travelId: Long,
        @RequestParam("picture") file: MultipartFile
    ): ResponseEntity<PictureDTO> {
        val result = pictureService.savePicture(travelId, file)

        return result.fold(
            onSuccess = { ResponseEntity(PictureDTO.fromEntity(result.getOrThrow()), HttpStatus.OK) },
            onFailure = {
                throw ResponseStatusException(
                    if (it.message?.contains("missing exif info") == true) HttpStatus.INTERNAL_SERVER_ERROR else HttpStatus.BAD_REQUEST,
                    it.message ?: "Unknown error",
                    it
                )
            }
        )
    }

    @GetMapping("/{id}")
    fun show(@PathVariable travelId: Long, @PathVariable id: Long): ResponseEntity<PictureDTO> {
        val picture = pictureService.getPictureById(id)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)

        if (picture.travel.id != travelId) {
            return ResponseEntity(HttpStatus.FORBIDDEN)
        }

        return ResponseEntity(PictureDTO.fromEntity(picture), HttpStatus.OK)
    }

    @SecuredMethode
    @DeleteMapping("/{id}")
    fun destroy(@PathVariable travelId: Long, @PathVariable id: Long): ResponseEntity<String> {
        val success = pictureService.deletePicture(travelId, id)

        return if (success) {
            ResponseEntity("Picture deleted", HttpStatus.OK)
        } else {
            ResponseEntity("Picture not found", HttpStatus.NOT_FOUND)
        }
    }
}