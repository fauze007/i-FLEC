package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    private val _currentUserFlow = MutableStateFlow<FirebaseUser?>(null)
    val currentUserFlow: StateFlow<FirebaseUser?> = _currentUserFlow

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    // Safe accessor for FirebaseAuth
    fun getAuth(): FirebaseAuth? {
        if (!_isInitialized.value) return null
        return auth
    }

    // Safe accessor for Firestore
    fun getFirestore(): FirebaseFirestore? {
        if (!_isInitialized.value) return null
        return firestore
    }

    /**
     * Initializes Firebase programmatically. It first attempts default initialization
     * (which works if google-services.json is packaged), and falls back to manual config
     * using BuildConfig values from .env. If both are unconfigured, it can boot up in Demo/Safe Mode
     * with detailed configuration options in the UI.
     */
    fun initialize(context: Context) {
        synchronized(this) {
            if (_isInitialized.value) return
            
            try {
                // 1. Try default initialization first (google-services.json)
                val defaultApp = FirebaseApp.initializeApp(context)
                if (defaultApp == null) {
                    throw IllegalStateException("Default FirebaseApp returned null from initializeApp (no google-services.json details in package resources).")
                }
                Log.d(TAG, "Firebase initialized via default configuration.")
                setupInstances()
                _isInitialized.value = true
                return
            } catch (e: Exception) {
                Log.w(TAG, "Default Firebase initialization not available: ${e.message}")
            }

            // 2. Try programmatic initialization using .env configs
            try {
                val apiKey = BuildConfig.FIREBASE_API_KEY
                val projectId = BuildConfig.FIREBASE_PROJECT_ID
                val appId = BuildConfig.FIREBASE_APPLICATION_ID

                if (!apiKey.isNullOrBlank() && apiKey != "unconfigured" &&
                    !projectId.isNullOrBlank() && projectId != "unconfigured" &&
                    !appId.isNullOrBlank() && appId != "unconfigured") {
                    val options = FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setProjectId(projectId)
                        .setApplicationId(appId)
                        .setDatabaseUrl("https://$projectId.firebaseio.com")
                        .build()
                    
                    // manualApp cannot be null; if options are invalid it throws an exception which is caught below
                    FirebaseApp.initializeApp(context, options)
                    Log.i(TAG, "Firebase initialized via manual programmatic options.")
                    setupInstances()
                    _isInitialized.value = true
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Manual programmatic initialization failed: ${e.message}", e)
            }

            Log.w(TAG, "Firebase is currently unconfigured. Run in Simulated Cloud Demo module.")
        }
    }

    private fun setupInstances() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        _currentUserFlow.value = auth?.currentUser
        
        // Listen to auth changes
        auth?.addAuthStateListener { firebaseAuth ->
            _currentUserFlow.value = firebaseAuth.currentUser
            Log.d(TAG, "Auth state updated: user email is ${firebaseAuth.currentUser?.email}")
        }
    }

    /**
     * Manual configuration update at runtime from specific UI fields.
     */
    fun configureAtRuntime(context: Context, apiKey: String, projectId: String, appId: String): Boolean {
        return try {
            // Clear prior app instance if any
            try {
                FirebaseApp.getInstance().delete()
            } catch (e: Exception) {}

            val options = FirebaseOptions.Builder()
                .setApiKey(apiKey)
                .setProjectId(projectId)
                .setApplicationId(appId)
                .setDatabaseUrl("https://$projectId.firebaseio.com")
                .build()

            FirebaseApp.initializeApp(context, options)
            setupInstances()
            _isInitialized.value = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during runtime Firebase configuration: ${e.message}", e)
            false
        }
    }

    // --- High-level cloud syncing operations ---

    suspend fun syncLocalProgressToCloud(progress: UserProgressEntity): Boolean {
        val fbFirestore = firestore ?: return false
        val user = auth?.currentUser ?: return false

        return try {
            val data = hashMapOf(
                "streak" to progress.streak,
                "coins" to progress.coins,
                "xp" to progress.xp,
                "lastActiveTime" to progress.lastActiveTime,
                "syncedAt" to System.currentTimeMillis(),
                "email" to user.email
            )
            fbFirestore.collection("users")
                .document(user.uid)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Successfully backed up progress to Firestore for ${user.email}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun getCloudProgress(): UserProgressEntity? {
        val fbFirestore = firestore ?: return null
        val user = auth?.currentUser ?: return null

        return try {
            val snapshot = fbFirestore.collection("users")
                .document(user.uid)
                .get()
                .await()
            
            if (snapshot.exists()) {
                val streak = snapshot.getLong("streak")?.toInt() ?: 4
                val coins = snapshot.getLong("coins")?.toInt() ?: 450
                val xp = snapshot.getLong("xp")?.toInt() ?: 120
                val lastActiveTime = snapshot.getLong("lastActiveTime") ?: 0L
                UserProgressEntity(
                    streak = streak,
                    coins = coins,
                    xp = xp,
                    lastActiveTime = lastActiveTime
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cloud progress: ${e.message}", e)
            null
        }
    }

    suspend fun getLeaderboard(): List<CloudUserProgress> {
        val fbFirestore = firestore ?: return emptyList()
        return try {
            val snapshot = fbFirestore.collection("users")
                .orderBy("coins", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()

            snapshot.documents.mapIndexed { index, doc ->
                CloudUserProgress(
                    rank = index + 1,
                    email = doc.getString("email") ?: "Anonymous Scholar",
                    coins = doc.getLong("coins")?.toInt() ?: 0,
                    streak = doc.getLong("streak")?.toInt() ?: 1,
                    xp = doc.getLong("xp")?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching cloud leaderboard: ${e.message}", e)
            emptyList()
        }
    }
}

data class CloudUserProgress(
    val rank: Int,
    val email: String,
    val coins: Int,
    val streak: Int,
    val xp: Int
)
