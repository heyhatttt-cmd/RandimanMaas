
package com.randiman.maas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import java.util.Calendar
import java.util.Locale

private val Turkuaz = Color(0xFF16A6A1)
private val Yesil = Color(0xFF167A45)
private val PazarZemin = Color(0xFFFFE0E0)
private val PazarYazi = Color(0xFFC62828)

data class GunKaydi(
    val mekikler: List<String> = listOf("", "", ""),
    val duruslar: List<String> = listOf("", "", "")
)

private fun sayi(deger: String): Double =
    deger.replace(",", ".").toDoubleOrNull() ?: 0.0

private fun tl(deger: Double): String =
    String.format(Locale("tr", "TR"), "%.2f", deger)

private fun fiyat(deger: Double): String =
    String.format(Locale("tr", "TR"), "%.4f", deger)

private fun ayAnahtari(ay: Calendar): String =
    String.format(
        Locale.US,
        "%04d_%02d",
        ay.get(Calendar.YEAR),
        ay.get(Calendar.MONTH) + 1
    )

private fun ayAdi(ay: Calendar): String {
    val ad = ay.getDisplayName(
        Calendar.MONTH,
        Calendar.LONG,
        Locale("tr", "TR")
    ) ?: ""
    return "$ad ${ay.get(Calendar.YEAR)}"
}

private fun ayGunSayisi(ay: Calendar): Int =
    ay.getActualMaximum(Calendar.DAY_OF_MONTH)

private fun haftaninGunu(ay: Calendar, gun: Int): String {
    val takvim = ay.clone() as Calendar
    takvim.set(Calendar.DAY_OF_MONTH, gun)

    return when (takvim.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "Pazar"
        Calendar.MONDAY -> "Pazartesi"
        Calendar.TUESDAY -> "Salı"
        Calendar.WEDNESDAY -> "Çarşamba"
        Calendar.THURSDAY -> "Perşembe"
        Calendar.FRIDAY -> "Cuma"
        Calendar.SATURDAY -> "Cumartesi"
        else -> ""
    }
}

private fun pazarMi(ay: Calendar, gun: Int): Boolean {
    val takvim = ay.clone() as Calendar
    takvim.set(Calendar.DAY_OF_MONTH, gun)
    return takvim.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
}

private fun ayHazirla(
    prefs: android.content.SharedPreferences,
    ay: Calendar
) {
    val anahtar = ayAnahtari(ay)

    if (prefs.getBoolean("ay_hazir_$anahtar", false)) return

    val editor = prefs.edit()
    val bugun = Calendar.getInstance()

    val buAyMi =
        ay.get(Calendar.YEAR) == bugun.get(Calendar.YEAR) &&
        ay.get(Calendar.MONTH) == bugun.get(Calendar.MONTH)

    for (tezgah in 0..2) {
        val eskiBirim = if (buAyMi) {
            prefs.getString("birim_$tezgah", "13.3550")
                ?: "13.3550"
        } else {
            "13.3550"
        }

        editor.putString("birim_${anahtar}_$tezgah", eskiBirim)
    }

    if (buAyMi) {
        for (gun in 0..30) {
            for (tezgah in 0..2) {
                editor.putString(
                    "mekik_${anahtar}_${gun}_$tezgah",
                    prefs.getString("mekik_${gun}_$tezgah", "") ?: ""
                )

                editor.putString(
                    "durus_${anahtar}_${gun}_$tezgah",
                    prefs.getString("durus_${gun}_$tezgah", "") ?: ""
                )
            }
        }
    }

    editor.putBoolean("ay_hazir_$anahtar", true)
    editor.apply()
}

private fun ayKayitlariniOku(
    prefs: android.content.SharedPreferences,
    ay: Calendar
): List<GunKaydi> {
    val anahtar = ayAnahtari(ay)

    return List(ayGunSayisi(ay)) { gun ->
        GunKaydi(
            mekikler = List(3) { tezgah ->
                prefs.getString(
                    "mekik_${anahtar}_${gun}_$tezgah",
                    ""
                ) ?: ""
            },
            duruslar = List(3) { tezgah ->
                prefs.getString(
                    "durus_${anahtar}_${gun}_$tezgah",
                    ""
                ) ?: ""
            }
        )
    }
}

private fun birimleriOku(
    prefs: android.content.SharedPreferences,
    ay: Calendar
): List<String> {
    val anahtar = ayAnahtari(ay)

    return List(3) { tezgah ->
        prefs.getString(
            "birim_${anahtar}_$tezgah",
            "13.3550"
        ) ?: "13.3550"
    }
}

private fun tezgahNumaralariniOku(
    prefs: android.content.SharedPreferences
): List<String> {
    return List(3) { index ->
        prefs.getString("tezgah_no_$index", "${index + 1}")
            ?: "${index + 1}"
    }
}

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

    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences("randiman_maas", 0)
    }

    var seciliAy by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }
        )
    }

    val ilkAy = remember {
        seciliAy.clone() as Calendar
    }

    val gunler = remember {
        ayHazirla(prefs, ilkAy)

        mutableStateListOf<GunKaydi>().apply {
            addAll(ayKayitlariniOku(prefs, ilkAy))
        }
    }

    val birimler = remember {
        mutableStateListOf<String>().apply {
            addAll(birimleriOku(prefs, ilkAy))
        }
    }

    val tezgahNumaralari = remember {
        mutableStateListOf<String>().apply {
            addAll(tezgahNumaralariniOku(prefs))
        }
    }

    val acikGunler = remember {
        mutableStateListOf<Boolean>().apply {
            repeat(ayGunSayisi(ilkAy)) {
                add(false)
            }
        }
    }

    var ayarlarAcik by remember {
        mutableStateOf(false)
    }

    fun kaydet() {
        val anahtar = ayAnahtari(seciliAy)
        val editor = prefs.edit()

        birimler.forEachIndexed { index, deger ->
            editor.putString("birim_${anahtar}_$index", deger)
        }

        tezgahNumaralari.forEachIndexed { index, numara ->
            editor.putString("tezgah_no_$index", numara)
        }

        gunler.forEachIndexed { gun, kayit ->
            for (tezgah in 0..2) {
                editor.putString(
                    "mekik_${anahtar}_${gun}_$tezgah",
                    kayit.mekikler[tezgah]
                )

                editor.putString(
                    "durus_${anahtar}_${gun}_$tezgah",
                    kayit.duruslar[tezgah]
                )
            }
        }

        editor.apply()
    }

    fun ayYukle(yeniAy: Calendar) {
        kaydet()

        val yeniTakvim = yeniAy.clone() as Calendar
        yeniTakvim.set(Calendar.DAY_OF_MONTH, 1)

        ayHazirla(prefs, yeniTakvim)

        seciliAy = yeniTakvim

        gunler.clear()
        gunler.addAll(ayKayitlariniOku(prefs, yeniTakvim))

        birimler.clear()
        birimler.addAll(birimleriOku(prefs, yeniTakvim))

        acikGunler.clear()
        repeat(ayGunSayisi(yeniTakvim)) {
            acikGunler.add(false)
        }
    }

    fun ayDegistir(fark: Int) {
        val yeniAy = seciliAy.clone() as Calendar
        yeniAy.add(Calendar.MONTH, fark)
        ayYukle(yeniAy)
    }

    fun guncelleMekik(
        gun: Int,
        tezgah: Int,
        deger: String
    ) {
        val yeni = gunler[gun].mekikler.toMutableList()
        yeni[tezgah] = deger.filter { it.isDigit() }

        gunler[gun] = gunler[gun].copy(mekikler = yeni)
        kaydet()
    }

    fun guncelleDurus(
        gun: Int,
        tezgah: Int,
        deger: String
    ) {
        val yeni = gunler[gun].duruslar.toMutableList()

        yeni[tezgah] = deger.filter {
            it.isDigit() || it == ',' || it == '.'
        }

        gunler[gun] = gunler[gun].copy(duruslar = yeni)
        kaydet()
    }

    fun guncelleBirim(
        tezgah: Int,
        deger: String
    ) {
        birimler[tezgah] = deger.filter {
            it.isDigit() || it == ',' || it == '.'
        }

        kaydet()
    }

    fun guncelleTezgahNumarasi(
        tezgah: Int,
        deger: String
    ) {
        tezgahNumaralari[tezgah] = deger.filter {
            it.isDigit()
        }

        kaydet()
    }

    fun tezgahKazanci(
        gun: GunKaydi,
        tezgah: Int
    ): Double {
        val mekik = sayi(gun.mekikler[tezgah])
        val durus = sayi(gun.duruslar[tezgah])

        return (mekik + durus * 7.0) *
            sayi(birimler[tezgah])
    }

    // TEZGÂH BAZINDA TOPLAM MEKİKLER

    val tezgahToplamMekikler = (0..2).map { tezgah ->
        gunler.sumOf { gun ->
            sayi(gun.mekikler[tezgah])
        }
    }

    // ÜÇ TEZGÂHIN GENEL MEKİK TOPLAMI

    val toplamMekik = tezgahToplamMekikler.sum()

    // TÜM DURUŞ SAATLERİ

    val toplamDurus = gunler.sumOf { gun ->
        gun.duruslar.sumOf { sayi(it) }
    }

    // DURUŞTAN GELEN EK MEKİK

    val durusEkMekik = toplamDurus * 7.0

    // TOPLAM BİRİM FİYATI

    val toplamBirimFiyati = birimler.sumOf {
        sayi(it)
    }

    // TOPLAM MAAŞ

    val toplamKazanc = gunler.sumOf { gun ->
        (0..2).sumOf { tezgah ->
            tezgahKazanci(gun, tezgah)
        }
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
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),

                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {

                // AY SEÇİMİ

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                ),

                            horizontalArrangement =
                                Arrangement.SpaceBetween,

                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically
                        ) {

                            TextButton(
                                onClick = {
                                    ayDegistir(-1)
                                }
                            ) {
                                Text("‹ ÖNCEKİ", color = Yesil)
                            }

                            Text(
                                ayAdi(seciliAy).uppercase(
                                    Locale("tr", "TR")
                                ),
                                color = Yesil,
                                style = MaterialTheme.typography.titleMedium
                            )

                            TextButton(
                                onClick = {
                                    ayDegistir(1)
                                }
                            ) {
                                Text("SONRAKİ ›", color = Yesil)
                            }
                        }
                    }
                }

                // TOPLAM MAAŞ VE MEKİK BİLGİLERİ

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
                            verticalArrangement =
                                Arrangement.spacedBy(10.dp)
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

                            // HER TEZGÂHIN MEKİK TOPLAMI

                            for (tezgah in 0..2) {

                                Text(
                                    "${tezgahNumaralari[tezgah]}. TEZGÂH TOPLAM MEKİK",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall
                                )

                                Text(
                                    "${tl(tezgahToplamMekikler[tezgah])} mekik",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.5f)
                            )

                            Text(
                                "TEZGÂH TOPLAMI",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                "${tl(toplamMekik)} mekik",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall
                            )

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.5f)
                            )

                            Text(
                                "DURUŞLAR TOPLAM SAATİ",
                                color = Color.White
                            )

                            Text(
                                "${tl(toplamDurus)} saat",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(
                                "Duruş ek mekiği: ${tl(durusEkMekik)}",
                                color = Color.White
                            )

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.5f)
                            )

                            Text(
                                "TOPLAM BİRİM FİYATI",
                                color = Color.White
                            )

                            Text(
                                "${fiyat(toplamBirimFiyati)} TL",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }

                // AYARLAR

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
                                    horizontalArrangement =
                                        Arrangement.SpaceBetween
                                ) {

                                    Text(
                                        "⚙ AYARLAR",
                                        color = Yesil,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        if (ayarlarAcik) "Kapat ▲"
                                        else "Aç ▼",
                                        color = Turkuaz
                                    )
                                }
                            }

                            if (ayarlarAcik) {

                                HorizontalDivider()

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    "TEZGÂH NUMARALARI",
                                    color = Yesil,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    "Numarayı değiştirdiğinizde bütün geçmiş kayıtlar ve listeler de yeni numarayı kullanır.",
                                    color = Color.DarkGray,
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Spacer(Modifier.height(10.dp))

                                for (tezgah in 0..2) {

                                    OutlinedTextField(
                                        value = tezgahNumaralari[tezgah],

                                        onValueChange = {
                                            guncelleTezgahNumarasi(
                                                tezgah,
                                                it
                                            )
                                        },

                                        label = {
                                            Text(
                                                "${tezgah + 1}. Tezgâhın yeni numarası"
                                            )
                                        },

                
