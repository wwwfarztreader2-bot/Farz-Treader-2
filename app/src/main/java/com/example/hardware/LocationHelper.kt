package com.example.hardware

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

sealed interface LocationFetchResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val address: String
    ) : LocationFetchResult

    data class Failure(val message: String) : LocationFetchResult
}

object LocationHelper {

    fun isLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): LocationFetchResult {
        if (!isLocationEnabled(context)) {
            return LocationFetchResult.Failure(
                "GPS atau Layanan Lokasi nonaktif. Aktifkan GPS perangkat terlebih dahulu untuk melakukan absensi."
            )
        }

        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        val freshLocation: Location? = withTimeoutOrNull(12_000L) {
            try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).awaitTask()
            } catch (_: Exception) {
                null
            }
        }

        val resolvedLocation: Location? = freshLocation ?: withTimeoutOrNull(4_000L) {
            try {
                client.lastLocation.awaitTask()
            } catch (_: Exception) {
                null
            }
        }

        if (resolvedLocation == null) {
            return LocationFetchResult.Failure(
                "Gagal mendapatkan titik koordinat GPS. Pastikan Anda berada di area dengan sinyal GPS yang jelas lalu coba lagi."
            )
        }

        val addressText = reverseGeocode(
            context = context,
            latitude = resolvedLocation.latitude,
            longitude = resolvedLocation.longitude
        )

        return LocationFetchResult.Success(
            latitude = resolvedLocation.latitude,
            longitude = resolvedLocation.longitude,
            address = addressText
        )
    }

    private suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = withContext(Dispatchers.IO) {
        val fallback = "Koordinat %.5f, %.5f".format(Locale.US, latitude, longitude)
        if (!Geocoder.isPresent()) return@withContext fallback

        return@withContext try {
            val geocoder = Geocoder(context, Locale("id", "ID"))
            val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(5_000L) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(results: MutableList<Address>) {
                                if (cont.isActive) cont.resume(results)
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)
            }

            val first = addresses?.firstOrNull()
            if (first != null) {
                val lines = (0..first.maxAddressLineIndex).mapNotNull { idx ->
                    first.getAddressLine(idx)?.takeIf { it.isNotBlank() }
                }
                lines.joinToString(", ").ifBlank { fallback }
            } else {
                fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener {
            if (cont.isActive) cont.resume(null)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.resume(null)
        }
    }
}
