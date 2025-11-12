package com.sun.moviedb.screen

import com.sun.moviedb.R
import com.sun.moviedb.utils.base.BaseActivity
import com.sun.moviedb.databinding.ActivityMainBinding
import com.sun.moviedb.utils.navigation.AppNavigator
import com.sun.moviedb.utils.navigation.NavDestination

class MainActivity : BaseActivity<ActivityMainBinding>() {
    private val TAG = "MainActivity"

    override fun getViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initView() {
        /* *
        * Initalize an instance of AppNavigator to handle navigation
        * */
        AppNavigator.init(
            supportFragmentManager,
            binding.fragmentContainer.id,
            binding.bottomNavigation
        )

        /* *
        * bottom navigation bar attach to
        * */
        AppNavigator.attachNavVisibilityListener()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    AppNavigator.navigateTo(NavDestination.HomeScreen)
                    true
                }
                R.id.nav_filter -> {
                    AppNavigator.navigateTo(NavDestination.FilterScreen)
                    true
                }
                R.id.nav_notification -> {
                    AppNavigator.navigateTo(NavDestination.NotificationScreen)
                    true
                }
                R.id.nav_favorite -> {
                    AppNavigator.navigateTo(NavDestination.FavoriteMovieScreen)
                    true
                }
                else -> false
            }
        }
        // Set default selected item (start destination)
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    override fun initData() {
        super.initData()


    }

    override fun onStart() {
        super.onStart()
        android.util.Log.d(TAG, "onStart called")
    }

    override fun onRestart() {
        super.onRestart()
        android.util.Log.d(TAG, "onRestart called")
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d(TAG, "onResume called")
    }

    override fun onPause() {
        android.util.Log.d(TAG, "onPause called")
        super.onPause()
    }

    override fun onStop() {
        android.util.Log.d(TAG, "onStop called")
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d(TAG, "onDestroy called")
    }

    override fun onSaveInstanceState(outState: android.os.Bundle) {
        android.util.Log.d(TAG, "onSaveInstanceState called")
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: android.os.Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        android.util.Log.d(TAG, "onRestoreInstanceState called")
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        android.util.Log.d(TAG, "onConfigurationChanged called: $newConfig")
    }
}
