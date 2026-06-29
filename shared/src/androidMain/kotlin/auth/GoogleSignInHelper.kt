package com.selfguide.auth

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object GoogleSignInHelper {
    private const val RC_SIGN_IN = 9001

    fun getSignInIntent(activity: Activity, webClientId: String): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        return googleSignInClient.getSignInIntent()
    }

    suspend fun handleSignInResult(data: Intent?): String? = suspendCancellableCoroutine { cont ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            cont.resume(account?.idToken)
        } catch (e: ApiException) {
            cont.resume(null)
        }
    }
}