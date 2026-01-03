package com.aghatis.asmal.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aghatis.asmal.data.model.AyahResponse
import com.aghatis.asmal.data.model.PrayerData
import com.aghatis.asmal.data.model.User
import com.aghatis.asmal.data.repository.Mosque
import com.aghatis.asmal.data.repository.MosqueRepository
import com.aghatis.asmal.data.repository.PrayerRepository
import com.aghatis.asmal.data.repository.PrefsRepository
import com.aghatis.asmal.data.repository.QuranRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeViewModel(
    private val prefsRepository: PrefsRepository,
    private val prayerRepository: PrayerRepository,
    private val quranRepository: QuranRepository,
    private val mosqueRepository: MosqueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState.asStateFlow()
    
    private val _ayahState = MutableStateFlow<AyahUiState>(AyahUiState.Loading)
    val ayahState: StateFlow<AyahUiState> = _ayahState.asStateFlow()

    private val _mosqueState = MutableStateFlow<MosqueUiState>(MosqueUiState.Loading)
    val mosqueState: StateFlow<MosqueUiState> = _mosqueState.asStateFlow()

    init {
        loadUser()
        fetchRandomAyah()
    }
    
    private fun fetchRandomAyah() {
        viewModelScope.launch {
            _ayahState.value = AyahUiState.Loading
            val result = quranRepository.getRandomAyah()
            result.onSuccess { ayah ->
                _ayahState.value = AyahUiState.Success(ayah)
            }.onFailure {
                _ayahState.value = AyahUiState.Error("Failed to load Verse")
            }
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            prefsRepository.userData.collectLatest { user ->
                _userState.value = user
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchPrayerTimesWithLocation(context: Context) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            _mosqueState.value = MosqueUiState.Loading
            try {
                val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
                
                // Use getCurrentLocation for fresher result than getLastLocation
                val locationResult = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token
                ).await()

                if (locationResult != null) {
                    val address = getAddress(context, locationResult.latitude, locationResult.longitude)
                    fetchPrayerTimes(locationResult.latitude, locationResult.longitude, address)
                    fetchNearestMosques(locationResult.latitude, locationResult.longitude)
                } else {
                    // Fallback to last location if current fails or is null
                     val lastLocation = fusedLocationClient.lastLocation.await()
                     if (lastLocation != null) {
                         val address = getAddress(context, lastLocation.latitude, lastLocation.longitude)
                         fetchPrayerTimes(lastLocation.latitude, lastLocation.longitude, address)
                         fetchNearestMosques(lastLocation.latitude, lastLocation.longitude)
                     } else {
                         _uiState.value = HomeUiState.Error("Unable to get location")
                         _mosqueState.value = MosqueUiState.Error("Unable to get location")
                     }
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Location error: ${e.message}")
                _mosqueState.value = MosqueUiState.Error("Location error: ${e.message}")
            }
        }
    }

    private suspend fun getAddress(context: Context, lat: Double, lon: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    addresses[0].locality ?: addresses[0].subAdminArea ?: "Unknown Location"
                } else {
                    "Unknown Location"
                }
            } catch (e: Exception) {
                "Unknown Location"
            }
        }
    }

    private suspend fun fetchPrayerTimes(lat: Double, long: Double, address: String) {
        val result = prayerRepository.getPrayerTimes(lat, long)
        result.onSuccess { prayerData ->
            _uiState.value = HomeUiState.Success(prayerData, address)
        }.onFailure { e ->
            _uiState.value = HomeUiState.Error(e.message ?: "Failed to fetch prayer times")
        }
    }

    private suspend fun fetchNearestMosques(lat: Double, long: Double) {
        val result = mosqueRepository.getNearestMosques(lat, long)
        result.onSuccess { mosques ->
            _mosqueState.value = MosqueUiState.Success(mosques)
        }.onFailure { e ->
            _mosqueState.value = MosqueUiState.Error(e.message ?: "Failed to fetch mosques")
        }
    }

    companion object {
        fun Factory(
            prefsRepository: PrefsRepository,
            prayerRepository: PrayerRepository,
            quranRepository: QuranRepository,
            mosqueRepository: MosqueRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    prefsRepository, 
                    prayerRepository, 
                    quranRepository,
                    mosqueRepository
                )
            }
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val prayerData: PrayerData, val locationName: String) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

sealed class AyahUiState {
    object Loading : AyahUiState()
    data class Success(val ayah: AyahResponse) : AyahUiState()
    data class Error(val message: String) : AyahUiState()
}

sealed class MosqueUiState {
    object Loading : MosqueUiState()
    data class Success(val mosques: List<Mosque>) : MosqueUiState()
    data class Error(val message: String) : MosqueUiState()
}
