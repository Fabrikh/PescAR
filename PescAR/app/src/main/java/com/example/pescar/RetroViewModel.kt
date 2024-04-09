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
        public set

    var retroGridState: RetroUiState by mutableStateOf(RetroUiState.Loading)
        public set

    var fishCount: Int = 0

    init {
        getFishCount()
        getFishGrid()
    }

    fun getFishInfo(id :Int, difficulty :Int) {
        viewModelScope.launch {
            retroUiState = try {
                val jsonResult = RetroAPI.retrofitService.getFishInfo(id,difficulty)
                RetroUiState.Success(jsonResult)
            }catch (e: IOException){

                Log.println(Log.INFO,"ERR",e.message.toString())
                RetroUiState.Error
            }

        }
    }

    fun getFishCount() {
        viewModelScope.launch {
            fishCount = try {
                 RetroAPI.retrofitService.getFishCount().toInt()
            }catch (e: IOException){

                Log.println(Log.INFO,"ERR",e.message.toString())
                -1
            }

        }
    }

    fun getFishGrid() {
        viewModelScope.launch {
            retroGridState = try {
                val jsonResult = RetroAPI.retrofitService.getFishGrid()
                RetroUiState.Success(jsonResult)
            }catch (e: IOException){
                Log.println(Log.INFO,"ERR",e.message.toString())
                RetroUiState.Error
            }

        }
    }
}