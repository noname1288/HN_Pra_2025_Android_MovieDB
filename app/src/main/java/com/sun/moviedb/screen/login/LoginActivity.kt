package com.sun.moviedb.screen.login

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import com.sun.moviedb.data.repository.auth.AuthRepositoryImpl
import com.sun.moviedb.databinding.ActivityLoginBinding
import com.sun.moviedb.screen.MainActivity
import com.sun.moviedb.utils.base.BaseActivity

class LoginActivity : BaseActivity<ActivityLoginBinding>(), LoginContract.View {

    private lateinit var presenter: LoginContract.Presenter
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    private val TAG = "LoginActivity"

    override fun getViewBinding(): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun initView() {
        super.initView()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        binding.loginButton.setOnClickListener {
            presenter.onLoginButtonClicked()
        }
    }

    override fun initData() {
        super.initData()
        val authRepository = AuthRepositoryImpl(applicationContext)
        presenter = LoginPresenterImpl(authRepository)
        presenter.attachView(this)

            googleSignInLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == RESULT_OK) {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        try {
                            val account = task.getResult(ApiException::class.java)
                            presenter.handleGoogleSignInResult(account, null)
                        } catch (e: ApiException) {
                            presenter.handleGoogleSignInResult(null, e)
                        }
                    } else {
                        Log.w(
                            TAG,
                            "Google Sign In Canceled or Failed, Result Code: ${result.resultCode}"
                        )
                        showGoogleSignInFailed("Google Sign-In was cancelled or failed.")
                    }
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
    }

    override fun showLoading(isLoading: Boolean) {
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showLoginSuccess(firebaseUser: FirebaseUser) {
        Toast.makeText(
            this,
            "Authentication Successful. User: ${firebaseUser.displayName}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun showLoginError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun launchGoogleSignInActivity(signInIntent: Intent) {
        if (::googleSignInLauncher.isInitialized) {
            googleSignInLauncher.launch(signInIntent)
            Log.d(TAG, "Google Sign-In Intent launched by Activity.")
        } else {
            Log.e(TAG, "googleSignInLauncher not initialized in Activity!")
            showLoginError("Error initiating login. Please try again.")
        }
    }

    override fun showGoogleSignInFailed(message: String) {
        Toast.makeText(this, "Google Sign In Failed: $message", Toast.LENGTH_SHORT).show()
    }
}
