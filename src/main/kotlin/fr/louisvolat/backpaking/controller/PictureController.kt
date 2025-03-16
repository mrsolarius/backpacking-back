package fr.louisvolat.backpaking.controller

import fr.louisvolat.backpaking.model.Picture
import fr.louisvolat.backpaking.security.annotation.SecuredMethode
import fr.louisvolat.backpaking.service.PictureService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/pictures")
class PictureController(private val pictureService: PictureService) {

    @GetMapping
    fun index(): List<Picture> {
        return pictureService.getAllPictures()
    }

    @SecuredMethode
    @PostMapping
    fun store(@RequestParam("picture") file: MultipartFile): ResponseEntity<String> {
        val result = pictureService.savePicture(file)

        return result.fold(
            onSuccess = { ResponseEntity("Success", HttpStatus.OK) },
            onFailure = {
                val errorMessage = it.message ?: "Unknown error"
                val status = if (errorMessage.contains("missing exif info"))
                    HttpStatus.INTERNAL_SERVER_ERROR
                else
                    HttpStatus.BAD_REQUEST
                ResponseEntity(errorMessage, status)
            }
        )
    }

    @GetMapping("/{id}")
    fun show(@PathVariable id: Long): ResponseEntity<Picture> {
        val picture = pictureService.getPictureById(id)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)

        return ResponseEntity(picture, HttpStatus.OK)
    }

    @SecuredMethode
    @DeleteMapping("/{id}")
    fun destroy(@PathVariable id: Long): ResponseEntity<String> {
        val success = pictureService.deletePicture(id)

        return if (success) {
            ResponseEntity("Picture deleted", HttpStatus.OK)
        } else {
            ResponseEntity("Picture not found", HttpStatus.NOT_FOUND)
        }
    }
}