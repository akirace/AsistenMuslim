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
import com.aghatis.asmal.data.repository.PrayerLogRepository
import com.aghatis.asmal.data.model.PrayerLog
import java.text.SimpleDateFormat
import java.util.Date
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
    private val mosqueRepository: MosqueRepository,
    private val prayerLogRepository: PrayerLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState.asStateFlow()
    
    private val _ayahState = MutableStateFlow<AyahUiState>(AyahUiState.Loading)
    val ayahState: StateFlow<AyahUiState> = _ayahState.asStateFlow()

    private val _mosqueState = MutableStateFlow<MosqueUiState>(MosqueUiState.Loading)
    val mosqueState: StateFlow<MosqueUiState> = _mosqueState.asStateFlow()

    private val _selectedDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _prayerLogState = MutableStateFlow<PrayerLog>(PrayerLog())
    val prayerLogState: StateFlow<PrayerLog> = _prayerLogState.asStateFlow()

    private val _prayerProgress = MutableStateFlow(0f)
    val prayerProgress: StateFlow<Float> = _prayerProgress.asStateFlow()
    
    // Cache location for date changes
    private var lastLat: Double? = null
    private var lastLong: Double? = null

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

    private fun observePrayerLog(userId: String, date: String) {
        viewModelScope.launch {
            prayerLogRepository.getPrayerLog(userId, date).collectLatest { log ->
                _prayerLogState.value = log
                calculateProgress(log)
            }
        }
    }
    
    private fun calculateProgress(log: PrayerLog) {
        val total = 5f
        var completed = 0f
        if (log.fajr) completed++
        if (log.dhuhr) completed++
        if (log.asr) completed++
        if (log.maghrib) completed++
        if (log.isha) completed++
        _prayerProgress.value = completed / total
    }

    fun togglePrayerStatus(prayer: String, isChecked: Boolean) {
        val user = _userState.value ?: return
        val date = _selectedDate.value
        viewModelScope.launch {
            try {
                prayerLogRepository.updatePrayerStatus(user.id, date, prayer, isChecked)
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
    
    fun changeDate(daysToAdd: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
             val current = sdf.parse(_selectedDate.value) ?: Date()
             val calendar = java.util.Calendar.getInstance()
             calendar.time = current
             calendar.add(java.util.Calendar.DAY_OF_YEAR, daysToAdd)
             val newDate = sdf.format(calendar.time)
             _selectedDate.value = newDate
             
             // Refresh data
             val user = _userState.value
             if (user != null) {
                 observePrayerLog(user.id, newDate)
             }
             
             if (lastLat != null && lastLong != null) {
                 viewModelScope.launch {
                     fetchPrayerTimes(lastLat!!, lastLong!!, newDate)
                 }
             }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            prefsRepository.userData.collectLatest { user ->
                _userState.value = user
                user?.let { observePrayerLog(it.id, _selectedDate.value) }
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
                    lastLat = locationResult.latitude
                    lastLong = locationResult.longitude
                    fetchPrayerTimes(locationResult.latitude, locationResult.longitude, _selectedDate.value, address)
                    fetchNearestMosques(locationResult.latitude, locationResult.longitude)
                } else {
                    // Fallback to last location if current fails or is null
                     val lastLocation = fusedLocationClient.lastLocation.await()
                     if (lastLocation != null) {
                         val address = getAddress(context, lastLocation.latitude, lastLocation.longitude)
                         lastLat = lastLocation.latitude
                         lastLong = lastLocation.longitude
                         fetchPrayerTimes(lastLocation.latitude, lastLocation.longitude, _selectedDate.value, address)
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    @SuppressLint("MissingPermission")
    fun refresh(context: Context) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Force refresh flow
                val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
                val locationResult = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token
                ).await()
                
                if (locationResult != null) {
                    val address = getAddress(context, locationResult.latitude, locationResult.longitude)
                    lastLat = locationResult.latitude
                    lastLong = locationResult.longitude
                    fetchPrayerTimes(locationResult.latitude, locationResult.longitude, _selectedDate.value, address, forceRefresh = true)
                    fetchNearestMosques(locationResult.latitude, locationResult.longitude, forceRefresh = true)
                } else {
                     // Try last location if current fails
                     val lastLocation = fusedLocationClient.lastLocation.await()
                     if (lastLocation != null) {
                         val address = getAddress(context, lastLocation.latitude, lastLocation.longitude)
                         fetchPrayerTimes(lastLocation.latitude, lastLocation.longitude, _selectedDate.value, address, forceRefresh = true)
                         fetchNearestMosques(lastLocation.latitude, lastLocation.longitude, forceRefresh = true)
                     }
                }
            } catch (e: Exception) {
                 _uiState.value = HomeUiState.Error("Refresh failed: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun fetchPrayerTimes(lat: Double, long: Double, date: String, address: String? = null, forceRefresh: Boolean = false) {
        // If not refreshing, set loading state only if no data
        if (!forceRefresh && _uiState.value !is HomeUiState.Success) {
             _uiState.value = HomeUiState.Loading
        }
        
        val result = prayerRepository.getPrayerTimes(lat, long, date, address, forceRefresh)
        result.onSuccess { prayerData ->
            val currentAddress = (uiState.value as? HomeUiState.Success)?.locationName ?: address ?: "Unknown Location"
            _uiState.value = HomeUiState.Success(prayerData, currentAddress)
        }.onFailure { e ->
            _uiState.value = HomeUiState.Error(e.message ?: "Failed to fetch prayer times")
        }
    }

    private suspend fun fetchNearestMosques(lat: Double, long: Double, forceRefresh: Boolean = false) {
        if (!forceRefresh && _mosqueState.value !is MosqueUiState.Success) {
            _mosqueState.value = MosqueUiState.Loading
        }
        val result = mosqueRepository.getNearestMosques(lat, long, forceRefresh = forceRefresh)
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
            mosqueRepository: MosqueRepository,
            prayerLogRepository: PrayerLogRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    prefsRepository, 
                    prayerRepository, 
                    quranRepository,
                    mosqueRepository,
                    PrayerLogRepository()
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
