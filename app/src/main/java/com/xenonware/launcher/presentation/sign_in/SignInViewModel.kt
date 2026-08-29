package com.xenonware.launcher.presentation.sign_in

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xenonware.launcher.data.SharedPreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class SignInEvent {
    object SignedInSuccessfully : SignInEvent()
}

class SignInViewModel(
    application: Application,
    private val sharedPreferenceManager: SharedPreferenceManager = SharedPreferenceManager(application)
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    private val _signInEvent = MutableStateFlow<SignInEvent?>(null)
    val signInEvent = _signInEvent.asStateFlow()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "is_user_logged_in") {
            val isLoggedIn = prefs.getBoolean(key, false)
            _state.update { it.copy(isSignInSuccessful = isLoggedIn) }
        }
    }

    init {
        sharedPreferenceManager.registerListener(preferenceListener)
        val isLoggedIn = sharedPreferenceManager.isUserLoggedIn
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        _state.update { 
            it.copy(
                isSignInSuccessful = isLoggedIn && currentUser != null,
                userData = currentUser?.let { user ->
                    UserData(
                        userId = user.uid,
                        username = user.displayName,
                        profilePictureUrl = user.photoUrl?.toString(),
                        email = user.email ?: ""
                    )
                }
            ) 
        }
    }

    override fun onCleared() {
        sharedPreferenceManager.unregisterListener(preferenceListener)
        super.onCleared()
    }

    fun updateSignInState(userData: UserData?) {
        _state.update { 
            it.copy(
                isSignInSuccessful = userData != null,
                userData = userData
            ) 
        }
    }

    fun onSignInResult(result: SignInResult) {
        _state.update {
            if (result.data != null) {
                sharedPreferenceManager.isUserLoggedIn = true
                _signInEvent.value = SignInEvent.SignedInSuccessfully
                it.copy(isSignInSuccessful = true, signInError = null, userData = result.data)
            } else {
                it.copy(isSignInSuccessful = false, signInError = result.errorMessage)
            }
        }
    }

    fun resetState() {
        _state.update { SignInState() }
        sharedPreferenceManager.isUserLoggedIn = false
        _signInEvent.value = null
    }

    class SignInViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SignInViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SignInViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
