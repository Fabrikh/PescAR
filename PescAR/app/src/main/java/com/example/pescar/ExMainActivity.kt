package com.example.pescar

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pescar.ui.theme.RetroTestTheme
import com.google.gson.JsonObject


class EXMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RetroTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RetroApp()
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun RetroApp(
    retroViewModel: RetroViewModel = viewModel()
) {

    RetroTestTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        ) {

            Column {
                Row {
                    Button(onClick = {
                        retroViewModel.getFishInfo(1)
                    }) {
                        Text("1")
                    }
                    Button(onClick = {
                        retroViewModel.getFishInfo(2)
                    }) {
                        Text("2")
                    }
                    Button(onClick = {
                        retroViewModel.getFishInfo(3)}
                    ) {
                        Text("3")
                    }
                }

                HomeScreen(
                    uiState = retroViewModel.retroUiState
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    uiState: RetroUiState, modifier: Modifier = Modifier
) {
    when(uiState){
        is RetroUiState.Success -> FishInfoScreen(uiState.fishInfo)
        is RetroUiState.Error -> ErrorScreen()
        is RetroUiState.Loading -> LoadingScreen()
        else -> {}
    }
}

@Composable
fun FishInfoScreen(
    fishInfo: JsonObject
){

    Column {
        Text(fishInfo.get("name").toString())
        Text(fishInfo.get("description").toString())

        DecodedImage(fishInfo.get("image").toString())


    }
}

@Composable
fun DecodedImage(imageB64String: String){
    val imageBytes = Base64.decode(imageB64String, Base64.DEFAULT)
    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

    return Image(bitmap = decodedImage.asImageBitmap(), contentDescription = "fish")
}

@Composable
fun ErrorScreen(){
    Text("Error")
}

@Composable
fun LoadingScreen(){
    Text("Loading...")
}