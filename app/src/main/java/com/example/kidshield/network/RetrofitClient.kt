package com.example.kidshield.network

import android.os.Build
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val EMULATOR_URL = "http://10.0.2.2:8000/api/"
    // Default IP for development (updated to match user's current server IP)
    private const val DEFAULT_IP = "10.51.102.155"

    private var BASE_URL: String = "http://$DEFAULT_IP:8000/api/"
    private var _instance: ApiService? = null

    @get:JvmStatic
    val BASE_URL_DIAGNOSTIC: String get() = BASE_URL

    private val okHttpClient: OkHttpClient by lazy {
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS) 
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @get:JvmStatic
    @get:Synchronized
    val instance: ApiService
        get() {
            if (_instance == null) {
                _instance = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService::class.java)
            }
            return _instance!!
        }

    /**
     * Initializes the client from saved preferences.
     * We PRIORITIZE the saved IP over the emulator default because some devices
     * give a false-positive emulator detection.
     */
    @JvmStatic
    @Synchronized
    fun initFromPrefs(context: android.content.Context, savedIp: String?) {
        val finalIp = "10.51.102.155"
        BASE_URL = if (finalIp.startsWith("http")) {
            if (finalIp.endsWith("/")) finalIp else "$finalIp/"
        } else {
            "http://$finalIp:8000/api/"
        }
        
        android.util.Log.i("KidShieldNetwork", "Server URL initialized to: $BASE_URL")
    _instance = null
    }

    /**
     * Manually override the base URL. 
     * IMPORTANT: We allow this even on emulated devices to ensure manual configuration 
     * always works if automatic detection fails.
     */
    @JvmStatic
    @Synchronized
    fun updateBaseUrl(newIp: String) {
        val cleanIp = newIp.trim()
        if (cleanIp.isEmpty()) return
        
        BASE_URL = if (cleanIp.startsWith("http")) {
            if (cleanIp.endsWith("/")) cleanIp else "$cleanIp/"
        } else {
            "http://$cleanIp:8000/api/"
        }
        _instance = null 
    }

    /**
     * Detect if running inside an Android emulator.
     */
    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }
}
