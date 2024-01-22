package com.example.pescar

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface RetroUiState {
    data class Success(val fishInfo: JsonObject) : RetroUiState
    object Error : RetroUiState
    object Loading : RetroUiState
}

class RetroViewModel : ViewModel() {

    var retroUiState: RetroUiState by mutableStateOf(RetroUiState.Loading)
        private set

    fun getFishInfo(id :Int) {
        viewModelScope.launch {
            retroUiState = try {
                val jsonResult = RetroAPI.retrofitService.getFishInfo(id)
                RetroUiState.Success(jsonResult)
            }catch (e: IOException){

                Log.println(Log.INFO,"ERR",e.message.toString())
                RetroUiState.Error
            }

        }
    }
}