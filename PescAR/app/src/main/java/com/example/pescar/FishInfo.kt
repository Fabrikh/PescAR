package com.example.pescar

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.LinearLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pescar.ui.theme.RetroTestTheme
import com.google.gson.JsonObject

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
                        retroViewModel.getFishInfo(3)
                    }
                    ) {
                        Text("3")
                    }
                }

                HomeScreen(
                    uiState = retroViewModel.retroUiState,
                    fishCount = retroViewModel.fishCount,
                    backSide = true
                )


            }
        }
    }
}

@Composable
fun HomeScreen(
    uiState: RetroUiState, fishCount: Int, backSide: Boolean, modifier: Modifier = Modifier
) {

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is RetroUiState.Success -> FishInfoCard(uiState.fishInfo, fishCount, backSide)
            is RetroUiState.Error -> ErrorScreen()
            is RetroUiState.Loading -> LoadingScreen()
            else -> {}
        }
    }
}

@Composable
fun FishInfoScreen(
    fishInfo: JsonObject
) {

    Surface {
        Column(modifier = Modifier.background(color = Color.White)) {
            Text(
                fishInfo.get("name").toString() + "\n" +
                        fishInfo.get("description").toString(), color = Color.Black
            )

            DecodedImage(fishInfo.get("image").toString())

        }
    }

}

@Composable
fun DecodedImage(imageB64String: String) {
    val imageBytes = Base64.decode(imageB64String, Base64.DEFAULT)
    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)


    return Image(
        bitmap = decodedImage.asImageBitmap(),
        contentDescription = "fish",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .height(150.dp)
            .fillMaxWidth()
    )
}

@Composable
fun ErrorScreen() {
    Text("Error")
}

@Composable
fun LoadingScreen() {
    Text("Loading...")
}


val WRITING_BACKGROUND_COLOR = Color(0, 29, 245, 255)
val TEXT_COLOR = Color(210, 230, 243)
val BORDER_COLOR = Color(20, 70, 245, 10)
val CARD_BACKGROUND_COLOR_LIST = listOf(
    Color(53, 129, 245), Color(20, 100, 245)
)

@Composable
fun FishInfoCard(
    fishInfo: JsonObject,
    fishCount: Int,
    backSide: Boolean
) {
    val cardShape = RoundedCornerShape(8.dp)

    Surface(
        modifier = Modifier
            .padding(10.dp)
            .clip(cardShape)

    ) {
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Card(
                //shape = MaterialTheme.shapes.medium,
                shape = cardShape,
                // modifier = modifier.size(280.dp, 240.dp)
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp),
                border = BorderStroke(2.dp, Color.Black),
                //set card elevation of the card
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp,
                ),
            ) {
                if (!backSide) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    CARD_BACKGROUND_COLOR_LIST,
                                    endX = with(LocalDensity.current) {
                                        50.dp.toPx()
                                    },
                                    tileMode = TileMode.Mirror
                                )
                            )
                            .fillMaxHeight()
                    ) {


                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WRITING_BACKGROUND_COLOR),
                            horizontalArrangement = Arrangement.SpaceBetween,

                            ) {
                            Text(
                                text = fixJsonString(fishInfo.get("name").toString()),
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp),
                                color = TEXT_COLOR
                            )
                            Text(
                                text = fixJsonString(
                                    fishInfo.get("id").toString()
                                ) + "/" + fishCount.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp),
                                textAlign = TextAlign.End,
                                color = TEXT_COLOR
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))

                        Card(
                            shape = RoundedCornerShape(8.dp),
                            // modifier = modifier.size(280.dp, 240.dp)
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            border = BorderStroke(1.dp, BORDER_COLOR),
                            //set card elevation of the card
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 0.dp,
                            ),
                        ) {

                            Box(
                                Modifier
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.sea_background),
                                    contentDescription = null
                                )
                                DecodedImage(
                                    fishInfo.get("image").toString()
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Spacer(modifier = Modifier.height(5.dp))

                            Card(
                                shape = RoundedCornerShape(8.dp),
                                // modifier = modifier.size(280.dp, 240.dp)
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                border = BorderStroke(1.dp, BORDER_COLOR),
                                //set card elevation of the card
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 10.dp,
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = WRITING_BACKGROUND_COLOR,
                                ),
                            ) {
                                Text(
                                    modifier = Modifier.padding(8.dp),
                                    text = fixJsonString(fishInfo.get("description").toString()),
                                    //maxLines = 1,
                                    //overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TEXT_COLOR
                                )
                            }

                        }
                    }

                } else {
                    Column(
                        modifier = Modifier,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Card(
                            //shape = MaterialTheme.shapes.medium,
                            shape = cardShape,
                            // modifier = modifier.size(280.dp, 240.dp)
                            modifier = Modifier
                                .height(460.dp)
                                .fillMaxWidth(),
                            border = BorderStroke(2.dp, Color.Black),
                            //set card elevation of the card
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 0.dp,
                            ),


                            ) {

                            Card(
                                border = BorderStroke(10.dp, TEXT_COLOR),
                                colors = CardDefaults.cardColors(
                                    containerColor = WRITING_BACKGROUND_COLOR
                                )) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.logocard2),
                                        contentDescription = null
                                    )
                                }
                            }
                        }

                    }
                }
            }
        }

    }

}

private fun fixJsonString(s: String): String {
    return s.replace("\"", "")
}

@Preview(showBackground = false)
@Composable
fun Preview() {
    Surface() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Card(
                //shape = MaterialTheme.shapes.medium,
                shape = RoundedCornerShape(8.dp),
                // modifier = modifier.size(280.dp, 240.dp)
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                border = BorderStroke(2.dp, Color.Black),
                //set card elevation of the card
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 10.dp,
                ),
            ) {
                Column(
                    modifier = Modifier.background(
                        Brush.horizontalGradient(
                            CARD_BACKGROUND_COLOR_LIST,
                            endX = with(LocalDensity.current) {
                                50.dp.toPx()
                            },
                            tileMode = TileMode.Mirror
                        )
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WRITING_BACKGROUND_COLOR),
                        horizontalArrangement = Arrangement.SpaceBetween,

                        ) {
                        Text(
                            text = "Orca",
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp),
                            color = TEXT_COLOR
                        )
                        Text(
                            text = "14/15",
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp),
                            textAlign = TextAlign.End,
                            color = TEXT_COLOR
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        // modifier = modifier.size(280.dp, 240.dp)
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        border = BorderStroke(1.dp, BORDER_COLOR),
                        //set card elevation of the card
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 10.dp,
                        ),
                    ) {

                        Box(
                            Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.sea_background),
                                contentDescription = null
                            )
                            DecodedImage(
                                "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"
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {


                        Spacer(modifier = Modifier.height(5.dp))


                        Card(
                            shape = RoundedCornerShape(8.dp),
                            // modifier = modifier.size(280.dp, 240.dp)
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            border = BorderStroke(1.dp, BORDER_COLOR),
                            //set card elevation of the card
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 10.dp,
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = WRITING_BACKGROUND_COLOR,
                            ),
                        ) {
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = "L'orca, o balena killer, è un predatore intelligente e sociale con una vasta gamma di vocalizzazioni.",
                                //maxLines = 1,
                                //overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                                color = TEXT_COLOR
                            )
                        }

                    }
                }
            }
        }

    }

}

@Preview(showBackground = false)
@Composable
fun Preview2() {
    val cardShape = RoundedCornerShape(8.dp)

    Surface(
        modifier = Modifier
            .padding(10.dp)
            .clip(cardShape)
    ) {
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Card(
                //shape = MaterialTheme.shapes.medium,
                shape = cardShape,
                // modifier = modifier.size(280.dp, 240.dp)
                modifier = Modifier
                    .height(460.dp)
                    .fillMaxWidth(),
                border = BorderStroke(2.dp, Color.Black),
                //set card elevation of the card
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp,
                ),


            ) {

                Card(
                    border = BorderStroke(10.dp, TEXT_COLOR),
                    colors = CardDefaults.cardColors(
                        containerColor = WRITING_BACKGROUND_COLOR
                    )) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logocard2),
                            contentDescription = null
                        )
                    }
                }
            }

        }
    }
}
