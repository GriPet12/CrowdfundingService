import com.gripet12.crowdfundingService.model.Image
import com.gripet12.crowdfundingService.service.ImageService

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("images")
@CrossOrigin(origins = ["\${frontend.url}"])
class ImageController(val imageService: ImageService) {

    @PostMapping
    fun uploadImage(@RequestParam("image") file: MultipartFile): ResponseEntity<String> {
        val image = Image(
            data = file.bytes
        )
        imageService.saveImage(image)
        return ResponseEntity.ok("Файл збережено!")
    }
}
