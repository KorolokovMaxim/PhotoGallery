package max.project.photogallery


import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.squareup.picasso.Picasso
import java.util.concurrent.TimeUnit


private const val TAG = "PhotoGalleryFragment"
private const val POLL_WORK = "POLL_WORK"

class PhotoGalleryFragment : VisibleFragment() {

    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var photoGalleryViewModel: PhotoGalleryViewModel
    //  private lateinit var thumbnailDownloader: ThumbnailDownloader<PhotoHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
        setHasOptionsMenu(true)

        photoGalleryViewModel = ViewModelProvider(this).get(PhotoGalleryViewModel::class.java)
        val responseHandler = Handler(Looper.myLooper()!!)
        /*   thumbnailDownloader = ThumbnailDownloader(responseHandler) { photoHandler, bitmap ->
               val drawable = BitmapDrawable(resources, bitmap)
               photoHandler.bindDrawable(drawable)
           }

           lifecycle.addObserver(thumbnailDownloader.fragmentLifecycleObserver)*/


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        /*  viewLifecycleOwner.lifecycle.addObserver(
               thumbnailDownloader.viewLifecycleObserver
           )*/
        val view = inflater.inflate(R.layout.fragment_photo_gallery, container, false)

        photoRecyclerView = view.findViewById(R.id.photo_recycler_view)
        photoRecyclerView.layoutManager = GridLayoutManager(context, 3)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        /* viewLifecycleOwner.lifecycle.removeObserver(
             thumbnailDownloader.viewLifecycleObserver
         )*/
    }


    override fun onDestroy() {
        super.onDestroy()
        /* lifecycle.removeObserver(thumbnailDownloader.fragmentLifecycleObserver)*/
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_photo_gallery, menu)

        val searchItem: MenuItem = menu.findItem(R.id.menu_item_search)
        val searchView = searchItem.actionView as SearchView

        searchView.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    Log.d(TAG, "QueryTextSubmit: $query")
                    photoGalleryViewModel.fetchPhotos(query)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    Log.d(TAG, "QueryTextChange: $newText")
                    return false
                }

            })

            setOnSearchClickListener {
                searchView.setQuery(photoGalleryViewModel.searchTerm, false)
            }
        }

        val toggleItem = menu.findItem(R.id.menu_item_toggle_polling)
        val isPolling = QueryPreference.isPolling(requireContext())
        val toggleItemTitle = if (isPolling) {
            R.string.stop_polling
        } else {
            R.string.start_polling
        }
        toggleItem.setTitle(toggleItemTitle)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_clear -> {
                photoGalleryViewModel.fetchPhotos("")
                true
            }

            R.id.menu_item_toggle_polling -> {
                val isPolling = QueryPreference.isPolling(requireContext())
                if (isPolling) {
                    WorkManager.getInstance().cancelUniqueWork(POLL_WORK)
                    QueryPreference.setPolling(requireContext(), false)
                } else {
                    val constrains = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .build()

                    val periodicRequest = PeriodicWorkRequest
                        .Builder(PoolWorker::class.java, 15, TimeUnit.MINUTES)
                        .setConstraints(constrains)
                        .build()

                    WorkManager.getInstance().enqueueUniquePeriodicWork(
                        POLL_WORK,
                        ExistingPeriodicWorkPolicy.KEEP,
                        periodicRequest
                    )
                    QueryPreference.setPolling(requireContext() , true)
                }
                activity?.invalidateOptionsMenu()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoGalleryViewModel.galleryItemLiveData.observe(
            viewLifecycleOwner,
            Observer { galleryItems ->
                photoRecyclerView.adapter = PhotoAdapter(galleryItems)
            }
        )
    }

    private inner class PhotoHolder(private val itemImageView: ImageView) :
        RecyclerView.ViewHolder(itemImageView) , View.OnClickListener {
        //val bindDrawable: (Drawable) -> Unit = itemImageView::setImageDrawable

        private lateinit var galleryItem: GalleryItem
        init {
            itemView.setOnClickListener(this)
        }
        fun bindGalleryItem(item: GalleryItem) {
            Picasso.get()
                .load(item.url)
                .placeholder(R.drawable.placeholder)
                .into(itemImageView)

            galleryItem = item
        }
        override fun onClick(v: View?) {
            val intent = PhotoPageActivity.newIntent(requireContext() , galleryItem.photoPageUri)
            startActivity(intent)
        }
    }

    private inner class PhotoAdapter(private val galleryItems: List<GalleryItem>) :
        RecyclerView.Adapter<PhotoHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {

            val view =
                layoutInflater.inflate(
                    R.layout.list_item_gallery,
                    parent,
                    false
                ) as ImageView

            return PhotoHolder(view)

        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val galleryItem = galleryItems[position]

           /* val placeholder: Drawable = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bill_up_close
            ) ?: ColorDrawable()*/

            holder.bindGalleryItem(galleryItem)

        }

        override fun getItemCount(): Int = galleryItems.size

    }

    companion object {
        fun newInstance() = PhotoGalleryFragment()
    }


}