package com.haskell.amghud

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserPreferencesViewModel(context: Context) : ViewModel() {
    private val preferencesRepository = DataStorePreferencesRepository(context)
    private val nameKey = "userName" // Define a key for your preference

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    init {
        loadSavedName()
    }

    private fun loadSavedName() {
        viewModelScope.launch {
            preferencesRepository.get(nameKey, "").collectLatest { savedName ->
                _name.value = savedName
            }
        }
    }

    fun saveName(name: String) {
        viewModelScope.launch {
            preferencesRepository.set(nameKey, name)
        }
    }
}