package dev.coupleapp.lovecounter.data.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import dev.coupleapp.lovecounter.domain.CounterCredential
import dev.coupleapp.lovecounter.domain.CounterIdentity
import kotlinx.coroutines.tasks.await

/** The caller must pass an existing project's public client configuration. */
fun firebaseCounterIdentity(context: Context, projectId: String, applicationId: String, apiKey: String, emulator: Boolean = false): CounterIdentity {
    require(projectId.isNotBlank() && applicationId.isNotBlank() && apiKey.isNotBlank())
    require(!emulator || projectId.startsWith("demo-"))
    val name = "couple-$projectId-${if (emulator) "emulator" else "cloud"}"
    val app = FirebaseApp.getApps(context).firstOrNull { it.name == name }
        ?: FirebaseApp.initializeApp(context, FirebaseOptions.Builder().setProjectId(projectId).setApplicationId(applicationId).setApiKey(apiKey).build(), name)
    val auth = FirebaseAuth.getInstance(app)
    if (emulator) auth.useEmulator("10.0.2.2", 19099)
    return object : CounterIdentity {
        override fun currentUid() = auth.currentUser?.uid
        override suspend fun credential(forceRefresh: Boolean): CounterCredential? {
            val user = auth.currentUser ?: auth.signInAnonymously().await().user ?: return null
            val token = user.getIdToken(forceRefresh).await().token ?: return null
            // Do not return a stale user's token if the native auth state changed.
            if (auth.currentUser?.uid != user.uid) return null
            return CounterCredential(user.uid, token)
        }
    }
}
