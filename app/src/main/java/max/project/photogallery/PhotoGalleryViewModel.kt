package max.project.photogallery

import android.app.Application
import androidx.lifecycle.*

class PhotoGalleryViewModel(private val app: Application) : AndroidViewModel(app) {

    val galleryItemLiveData: LiveData<List<GalleryItem>>


    val searchTerm: String
        get() = mutableSearchTerm.value ?: ""

    private val flickrFetch = FlickrFetch()
    private val mutableSearchTerm = MutableLiveData<String>()

    init {

        mutableSearchTerm.value = QueryPreference.getStoredQuery(app)

        galleryItemLiveData = Transformations.switchMap(mutableSearchTerm){searchTerm ->
            if(searchTerm.isBlank()){
                flickrFetch.fetchPhotos()
            }else{
                flickrFetch.searchPhotos(searchTerm)
            }

        }
    }

    fun fetchPhotos(query: String =""){
        QueryPreference.setStoredQuery(app , query)
        mutableSearchTerm.value = query
    }

}