package com.sun.moviedb.screen.detail

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.sun.moviedb.R
import com.sun.moviedb.data.model.Category
import com.sun.moviedb.data.model.EpisodeWrapper
import com.sun.moviedb.data.model.Movie
import com.sun.moviedb.data.model.EpisodeModel
import com.sun.moviedb.utils.base.BaseFragment
import com.sun.moviedb.databinding.FragmentMovieDetailBinding
import com.sun.moviedb.screen.detail.adapter.ServerAdapter
import com.sun.moviedb.screen.detail.adapter.EpisodeAdapter
import com.sun.moviedb.MyApp
import com.sun.moviedb.screen.room.RoomFragment
import com.sun.moviedb.screen.watchMovie.WatchMovieActivity
import com.sun.moviedb.screen.watch_new.NewWatchMovieActivity
import com.sun.moviedb.utils.AppLocator
import com.sun.moviedb.utils.session.RoomSession
import com.sun.moviedb.utils.navigation.AppNavigator

class MovieDetailFragment : BaseFragment<FragmentMovieDetailBinding>(), MovieDetailContract.View {
    private lateinit var serverAdapter: ServerAdapter
    private lateinit var episodeAdapter: EpisodeAdapter
    private lateinit var presenter: MovieDetailPresenter
    private lateinit var movieInfo: Movie
    private lateinit var episodeWrappers: List<EpisodeWrapper>
    private var isFavourite = false
    private var slug: String = ""

    private val watchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val left = result.data?.getBooleanExtra(RoomFragment.HAS_ROOM, false) ?: false
            if (left) {
                val message = result.data?.getStringExtra(RoomFragment.MESSAGE_AFTER_LEFT_ROOM)
                    ?: "Bạn đã rời phòng"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                /**
                 * clear room session, clear current member node
                 * */
                var currentRoomId = RoomSession.roomId ?: ""
                if (currentRoomId.isNotEmpty())
                    presenter.deleteCurrentMember(currentRoomId)
                RoomSession.roomId = null
            }
        }
    }

    private val TAG = "MovieDetailFragment"

    private val userId: String by lazy {
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMovieDetailBinding {
        return FragmentMovieDetailBinding.inflate(inflater, container, false)
    }

    override fun initData() {
        super.initData()

        val app = requireActivity().application as MyApp

        presenter = MovieDetailPresenter(
            app.movieRepository,
            AppLocator.roomRepository,
            AppLocator.memberRepository
        )
        presenter.attachView(this)


        showLoading(true)
        slug = arguments?.getString(KEY_SLUG) ?: ""
        presenter.getDetail(slug)
    }

    override fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) ViewGroup.VISIBLE else ViewGroup.GONE
        binding.frOverlay.visibility = if (isLoading) ViewGroup.VISIBLE else ViewGroup.GONE
    }

    override fun showError(message: String) {
        showLoading(false)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onGetDetailSuccess(
        movie: Movie,
        episodeWrappers: List<EpisodeWrapper>
    ) {
        this.movieInfo = movie
        this.episodeWrappers = episodeWrappers
        Log.d(TAG, "Movie: $movie")
        Log.d(TAG, "Episodes: $episodeWrappers")

        showLoading(false)
        setUI()
        setEpsListView()
        onStartFromBeginningButtonClicked()
        onFavoriteButtonClicked()
        onWatchNowButtonClicked()
        onChillWithFriendButtonClicked()
        onBackButtonClicked()
        onInviteFriendButtonClicked()
        presenter.checkFavorite(movieInfo.id, userId)
    }

    override fun onAddSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showLoading2(isLoading: Boolean) {
        if (isLoading)
            binding.progressBar2.visibility = ViewGroup.VISIBLE
        else
            binding.progressBar2.visibility = ViewGroup.GONE
    }

    override fun onCheckFavorite(isFavorite: Boolean) {
        isFavourite = isFavorite
        updateFavoriteIcon(isFavourite)
    }

    override fun onMovieToFirebaseSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun updateFavoriteIcon(isFav: Boolean) {
        binding.btnFav.setImageResource(
            if (isFav) R.drawable.ic_favorite_red_24 else R.drawable.ic_favorite_24
        )
    }

    private fun setUI() {
        Glide.with(requireContext())
            .load(movieInfo.thumbUrl)
            .into(binding.imgThumb)
        binding.tvTitle.text = movieInfo.name
        binding.tvOriginName.text = movieInfo.originName
        binding.tvDescription.text = movieInfo.content
        binding.tvTime.text = movieInfo.time
        binding.tvCate.text = getListCate(movieInfo.category)
        binding.tvQuality.text = movieInfo.quality
        binding.tvLang.text = movieInfo.lang
        binding.tvStatus.text = movieInfo.episodeCurrent
        binding.tvYear.text = movieInfo.year.toString()
    }

    private fun setEpsListView() {
        serverAdapter = ServerAdapter(episodeWrappers) { episodes ->
            setupEpisodeAdapter(episodes)
        }

        binding.rvListEps.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL, false
        )
        binding.rvListEps.adapter = serverAdapter

        if (episodeWrappers.isNotEmpty()) {
            /* *
            * Auto select the first serverdata
            * */
            serverAdapter.selectFirstItem()
        }
    }

    private fun setupEpisodeAdapter(serverData: List<EpisodeModel>) {
        binding.progressBar2.visibility = ViewGroup.VISIBLE

        episodeAdapter = EpisodeAdapter(serverData) { item ->
            // Handle click on server data
//            Toast.makeText(requireContext(), "Link m3u8: $item", Toast.LENGTH_SHORT).show()
//            val intent = Intent(requireContext(), WatchMovieActivity::class.java).apply {
//                putExtra(WatchMovieActivity.ARG_M3U8_LINK, item)
//                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            }
//            startActivity(intent)

            navigateToWatchMovie(item)

        }

        binding.rvListServerData.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL, false
        )
        binding.rvListServerData.adapter = episodeAdapter

        binding.progressBar2.visibility = ViewGroup.GONE

    }

    private fun onStartFromBeginningButtonClicked() {
        binding.btnStartZero.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Start watching from zero",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun onFavoriteButtonClicked() {
        binding.btnFav.setOnClickListener {
            presenter.onFavClicked(movieInfo, isFavourite, userId)
        }
    }

    private fun onWatchNowButtonClicked() {

        binding.btnWatchNow.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Watch from current episode + position",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun onChillWithFriendButtonClicked() {
        binding.btnWatchWithFriends.setOnClickListener {
            presenter.createRoom(movieInfo)

            if (RoomSession.roomId != null)
                presenter.addCurrentMember(RoomSession.roomId!!)
            else {
                Log.d(TAG, "Room ID is null -> Fail to create new Room")
                Toast.makeText(
                    requireContext(),
                    "Room ID is null -> Fail to create new Room",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            /* *
            * Default: Watch movie from the first episode
            * */

            val firstEpisode = episodeWrappers.firstOrNull()
            if (firstEpisode != null && firstEpisode.serverData.isNotEmpty()) {
                val firstServerData = firstEpisode.serverData.firstOrNull()
                val roomId = RoomSession.roomId ?: ""
                if (firstServerData != null && roomId.isNotEmpty()) {
                    val intent = Intent(requireContext(), WatchMovieActivity::class.java).apply {
                        putExtra(WatchMovieActivity.ARG_M3U8_LINK, firstServerData.linkM3u8)
                        putExtra(WatchMovieActivity.ARG_ROOM_ID, roomId)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    watchLauncher.launch(intent)
                }
            }
        }
    }

    private fun onBackButtonClicked() {
        binding.btnBack.setOnClickListener {
            AppNavigator.safeBack()
        }
    }

    private fun onInviteFriendButtonClicked() {}

    /**
     * support to show category chip in UI
     * get 2 items of categories
     * if only one category, return that category name
     * */
    private fun getListCate(cates: List<Category> = emptyList()): String {
        if (cates.size == 1) return cates[0].name
        if (cates.size >= 2) {
            val res = StringBuilder()
            res.append(cates[0].name).append(" - ").append(cates[1].name)
            return res.toString()
        }
        return "Unknown"
    }

    private fun navigateToWatchMovie (videoUrl : String){
        val intent = Intent(requireContext(), NewWatchMovieActivity::class.java).apply {
            putExtra(NewWatchMovieActivity.M3U8_LINK, videoUrl)
            putExtra(NewWatchMovieActivity.OBJECT_MOVIE, movieInfo)
        }

        startActivity(intent)
    }

    companion object {
        private const val KEY_SLUG = "slug"
        fun newInstance(slug: String): MovieDetailFragment {
            val fragment = MovieDetailFragment()
            val args = Bundle().apply {
                putString(KEY_SLUG, slug)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }
}

