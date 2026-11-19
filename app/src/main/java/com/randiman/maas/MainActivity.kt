
package com.randiman.maas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

data class GunKaydi(
    val mekikler: List<String> = listOf("", "", ""),
    val duruslar: List<String> = listOf("", "", "")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RandimanMaas()
        }
    }
}

fun sayi(deger: String): Double {
    return deger.replace(",", ".").toDoubleOrNull() ?: 0.0
}

fun tl(deger: Double): String {
    return String.format(Locale("tr", "TR"), "%.2f", deger)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandimanMaas() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("randiman_maas", 0)
    }

    val birimler = remember {
        mutableStateListOf(
            prefs.getString("birim_0", "13.3550") ?: "13.3550",
            prefs.getString("birim_1", "13.3550") ?: "13.3550",
            prefs.getString("birim_2", "13.3550") ?: "13.3550"
        )
    }

    val gunler = remember {
        mutableStateListOf<GunKaydi>().apply {
            repeat(31) { gun ->
                add(
                    GunKaydi(
                        mekikler = List(3) { tezgah ->
                            prefs.getString("mekik_${gun}_$tezgah", "") ?: ""
                        },
                        duruslar = List(3) { tezgah ->
                            prefs.getString("durus_${gun}_$tezgah", "") ?: ""
                        }
                    )
                )
            }
        }
    }

    fun kaydet() {
        val editor = prefs.edit()

        birimler.forEachIndexed { index, deger ->
            editor.putString("birim_$index", deger)
        }

        gunler.forEachIndexed { gun, kayit ->
            for (tezgah in 0..2) {
                editor.putString(
                    "mekik_${gun}_$tezgah",
                    kayit.mekikler[tezgah]
                )
                editor.putString(
                    "durus_${gun}_$tezgah",
                    kayit.duruslar[tezgah]
                )
            }
        }

        editor.apply()
    }

    fun guncelleMekik(gun: Int, tezgah: Int, deger: String) {
        val yeni = gunler[gun].mekikler.toMutableList()
        yeni[tezgah] = deger.filter { it.isDigit() }
        gunler[gun] = gunler[gun].copy(mekikler = yeni)
        kaydet()
    }

    fun guncelleDurus(gun: Int, tezgah: Int, deger: String) {
        val yeni = gunler[gun].duruslar.toMutableList()
        yeni[tezgah] = deger.filter {
            it.isDigit() || it == ',' || it == '.'
        }
        gunler[gun] = gunler[gun].copy(duruslar = yeni)
        kaydet()
    }

    fun guncelleBirim(tezgah: Int, deger: String) {
        birimler[tezgah] = deger.filter {
            it.isDigit() || it == ',' || it == '.'
        }
        kaydet()
    }

    fun tezgahKazanci(gun: GunKaydi, tezgah: Int): Double {
        val mekik = sayi(gun.mekikler[tezgah])
        val durusSaati = sayi(gun.duruslar[tezgah])
        val ekMekik = durusSaati * 7.0
        return (mekik + ekMekik) * sayi(birimler[tezgah])
    }

    val toplamMekik = gunler.sumOf { gun ->
        gun.mekikler.sumOf { sayi(it) }
    }

    val toplamDurus = gunler.sumOf { gun ->
        gun.duruslar.sumOf { sayi(it) }
    }

    val ekMekik = toplamDurus * 7.0

    val toplamKazanc = gunler.sumOf { gun ->
        (0..2).sumOf { tezgah ->
            tezgahKazanci(gun, tezgah)
        }
    }

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
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "TEZGÂH BİRİM FİYATLARI",
                        style = MaterialTheme.typography.titleMedium
                    )

                    for (tezgah in 0..2) {
                        OutlinedTextField(
                            value = birimler[tezgah],
                            onValueChange = {
                                guncelleBirim(tezgah, it)
                            },
                            label = {
                                Text("${tezgah + 1}. Tezgâh Birim Fiyatı (TL)")
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "AYLIK TOPLAM",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text("Toplam Mekik: ${tl(toplamMekik)}")
                            Text("Toplam Duruş: ${tl(toplamDurus)} saat")
                            Text("Duruştan Gelen Ek Mekik: ${tl(ekMekik)}")
                            HorizontalDivider()
                            Text(
                                "TOPLAM KAZANÇ: ${tl(toplamKazanc)} TL",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        "Duruşun her saati 7 ek mekik olarak hesaplanır.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                itemsIndexed(gunler) { index, gun ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "${index + 1}. GÜN",
                                style = MaterialTheme.typography.titleMedium
                            )

                            for (tezgah in 0..2) {
                                Text(
                                    "${tezgah + 1}. TEZGÂH",
                                    style = MaterialTheme.typography.titleSmall
                                )

                                OutlinedTextField(
                                    value = gun.mekikler[tezgah],
                                    onValueChange = {
                                        guncelleMekik(index, tezgah, it)
                                    },
                                    label = { Text("Mekik sayısı") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = gun.duruslar[tezgah],
                                    onValueChange = {
                                        guncelleDurus(index, tezgah, it)
                                    },
                                    label = { Text("Duruş saati") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    "Bu tezgâh kazancı: ${
                                        tl(tezgahKazanci(gun, tezgah))
                                    } TL"
                                )
                            }

                            HorizontalDivider()

                            val gunKazanci = (0..2).sumOf {
                                tezgahKazanci(gun, it)
                            }

                            Text(
                                "Günlük toplam: ${tl(gunKazanci)} TL",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
