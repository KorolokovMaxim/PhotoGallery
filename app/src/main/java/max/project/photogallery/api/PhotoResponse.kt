package max.project.photogallery.api

import com.google.gson.annotations.SerializedName
import max.project.photogallery.GalleryItem

class PhotoResponse {
    @SerializedName("photo")
    lateinit var galleryItems: List<GalleryItem>
}