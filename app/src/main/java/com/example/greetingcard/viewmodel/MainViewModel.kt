package com.example.greetingcard.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greetingcard.data.FirebaseManager
import com.example.greetingcard.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val firebaseManager = FirebaseManager()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _needs = MutableStateFlow<List<Need>>(emptyList())
    val needs: StateFlow<List<Need>> = _needs

    private val _impactItems = MutableStateFlow<List<ImpactItem>>(emptyList())
    val impactItems: StateFlow<List<ImpactItem>> = _impactItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Observe Needs
            firebaseManager.getNeeds().collect {
                _needs.value = it
            }
        }

        viewModelScope.launch {
            // Observe Impact
            firebaseManager.getImpactItems().collect {
                _impactItems.value = it
            }
            _isLoading.value = false
        }

        // Load profile if logged in
        val uid = firebaseManager.getCurrentUserId()
        if (uid != null) {
            viewModelScope.launch {
                _userProfile.value = firebaseManager.getUserProfile(uid)
            }
        }
    }

    fun addNeed(title: String, desc: String, target: Int, imageUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true
            val schoolName = _userProfile.value?.uniqueField ?: "GHS Malgudi" // Fallback
            val newNeed = Need(
                school = schoolName,
                title = title,
                desc = desc,
                target = target,
                urgent = true,
                contact = "+91 9999999999"
            )
            firebaseManager.addNeed(newNeed, imageUri)
            _isLoading.value = false
        }
    }

    fun addImpact(title: String, desc: String, beforeUri: Uri?, afterUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true
            val schoolName = _userProfile.value?.uniqueField ?: "GHS Malgudi"
            val newItem = ImpactItem(
                title = title,
                school = schoolName,
                desc = desc
            )
            firebaseManager.addImpactItem(newItem, beforeUri, afterUri)
            _isLoading.value = false
        }
    }

    fun donate(needId: String, amount: Int) {
        viewModelScope.launch {
            firebaseManager.updateNeedDonation(needId, amount)
        }
    }

    fun logout() {
        firebaseManager.logout()
        _userProfile.value = null
    }
}
