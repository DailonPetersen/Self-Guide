package com.selfguide.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.selfguide.auth.GoogleSignInHelper
import com.selfguide.di.AuthEventNotifier
import com.selfguide.di.initKoin
import com.selfguide.security.AndroidPreferences
import com.selfguide.shared.ComposeApp

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInLauncher: ActivityResultLauncher<android.content.Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidPreferences.init(this)
        initKoin()

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                AuthEventNotifier.notifyGoogleSignInResult(result.data)
            }
        }

        setContent {
            ComposeApp {
                val intent = GoogleSignInHelper.getSignInIntent(
                    this,
                    BuildConfig.GOOGLE_WEB_CLIENT_ID,
                )
                googleSignInLauncher.launch(intent)
            }
        }
    }
}
