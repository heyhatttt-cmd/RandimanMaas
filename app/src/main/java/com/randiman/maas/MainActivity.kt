
package com.randiman.maas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

private val Turkuaz = Color(0xFF16A6A1)
private val Yesil = Color(0xFF167A45)
private val AcikTurkuaz = Color(0xFFE0F5F3)

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

private fun sayi(deger: String): Double {
    return deger.replace(",", ".").toDoubleOrNull() ?: 0.0
}

private fun tl(deger: Double): String {
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

    val acikGunler = remember {
        mutableStateListOf(*Array(31) { false })
    }

    var ayarlarAcik by remember { mutableStateOf(false) }

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
        val durus = sayi(gun.duruslar[tezgah])
        val ekMekik = durus * 7.0
        return (mekik + ekMekik) * sayi(birimler[tezgah])
    }

    val toplamMekik = gunler.sumOf { gun ->
        gun.mekikler.sumOf { sayi(it) }
    }

    val toplamDurus = gunler.sumOf { gun ->
        gun.duruslar.sumOf { sayi(it) }
    }

    val toplamKazanc = gunler.sumOf { gun ->
        (0..2).sumOf { tezgah ->
            tezgahKazanci(gun, tezgah)
        }
    }

    val ikinciTezgahGunleri = gunler.filter { gun ->
        gun.mekikler[1].isNotBlank() ||
            gun.duruslar[1].isNotBlank()
    }

    val ikinciTezgahToplam = gunler.sumOf { gun ->
        tezgahKazanci(gun, 1)
    }

    val ikinciTezgahOrtalama =
        if (ikinciTezgahGunleri.isNotEmpty()) {
            ikinciTezgahToplam / ikinciTezgahGunleri.size
        } else {
            0.0
        }

    val renkler = lightColorScheme(
        primary = Turkuaz,
        secondary = Yesil,
        background = Turkuaz,
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White
    )

    MaterialTheme(colorScheme = renkler) {
        Scaffold(
            containerColor = Turkuaz,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "RANDIMAN MAAŞ",
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Yesil,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Yesil
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "TOPLAM MAAŞ",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                "${tl(toplamKazanc)} TL",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge
                            )

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.5f)
                            )

                            Text(
                                "2. TEZGÂH ORTALAMASI",
                                color = Color.White
                            )

                            Text(
                                "${tl(ikinciTezgahOrtalama)} TL",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(
                                "Toplam mekik: ${tl(toplamMekik)}",
                                color = Color.White
                            )

                            Text(
                                "Toplam duruş: ${tl(toplamDurus)} saat",
                                color = Color.White
                            )

                            Text(
                                "Duruş ek mekiği: ${tl(toplamDurus * 7)}",
                                color = Color.White
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    ayarlarAcik = !ayarlarAcik
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "⚙ AYARLAR",
                                        color = Yesil,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        if (ayarlarAcik) "Kapat ▲" else "Aç ▼",
                                        color = Turkuaz
                                    )
                                }
                            }

                            if (ayarlarAcik) {
                                Text(
                                    "TEZGÂH BİRİM FİYATLARI",
                                    style = MaterialTheme.typography.titleSmall
                                )

                                Spacer(Modifier.height(8.dp))

                                for (tezgah in 0..2) {
                                    OutlinedTextField(
                                        value = birimler[tezgah],
                                        onValueChange = {
                                            guncelleBirim(tezgah, it)
                                        },
                                        label = {
                                            Text(
                                                "${tezgah + 1}. Tezgâh Birim Fiyatı (TL)"
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "GÜNLÜK KAYITLAR",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                itemsIndexed(gunler) { index, gun ->

                    val gunKazanci = (0..2).sumOf { tezgah ->
                        tezgahKazanci(gun, tezgah)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    acikGunler[index] = !acikGunler[index]
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${index + 1}. GÜN",
                                        color = Yesil,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        if (acikGunler[index]) "Kapat ▲" else "Aç ▼",
                                        color = Turkuaz
                                    )
                                }
                            }

                            Text(
                                "Günlük kazanç: ${tl(gunKazanci)} TL",
                                color = Turkuaz
                            )

                            if (acikGunler[index]) {
                                HorizontalDivider()
                                Spacer(Modifier.height(8.dp))

                                for (tezgah in 0..2) {
                                    Text(
                                        "${tezgah + 1}. TEZGÂH",
                                        color = Yesil,
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    OutlinedTextField(
                                        value = gun.mekikler[tezgah],
                                        onValueChange = {
                                            guncelleMekik(index, tezgah, it)
                                        },
                                        label = {
                                            Text("Mekik sayısı")
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = gun.duruslar[tezgah],
                                        onValueChange = {
                                            guncelleDurus(index, tezgah, it)
                                        },
                                        label = {
                                            Text("Duruş saati")
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Text(
                                        "Tezgâh kazancı: ${
                                            tl(tezgahKazanci(gun, tezgah))
                                        } TL",
                                        color = Turkuaz
                                    )

                                    Spacer(Modifier.height(12.dp))
                                }

                                HorizontalDivider()

                                Text(
                                    "GÜNLÜK TOPLAM: ${tl(gunKazanci)} TL",
                                    color = Yesil,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
c
