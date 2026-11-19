package com.randiman.maas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RandimanMaas()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandimanMaas() {
    var birim by remember { mutableStateOf("13.3550") }

    val mekikler = remember {
        mutableStateListOf(*Array(31) { "" })
    }

    val fiyat = birim.replace(",", ".").toDoubleOrNull() ?: 0.0
    val toplam = mekikler.sumOf { it.toIntOrNull() ?: 0 }
    val kazanc = toplam * fiyat

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("RANDIMAN MAAŞ") }
                )
            }
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                item {
                    Text("Mekik Birim Fiyatı (TL)")

                    OutlinedTextField(
                        value = birim,
                        onValueChange = { birim = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Text("Toplam Mekik: $toplam")

                    Text(
                        "Toplam Kazanç: %.2f TL".format(kazanc)
                    )

                    HorizontalDivider()

                    Spacer(Modifier.height(12.dp))
                }

                itemsIndexed(mekikler) { index, deger ->
                    OutlinedTextField(
                        value = deger,
                        onValueChange = {
                            mekikler[index] = it.filter { c -> c.isDigit() }
                        },
                        label = {
                            Text("${index + 1}. Gün Mekik")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
