package com.aghatis.asmal.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class ZakatCategory {
    FITRAH, MAL
}

data class ZakatType(
    val id: String,
    val title: String,
    val arabicTitle: String,
    val description: String,
    val category: ZakatCategory,
    val icon: ImageVector,
    val rules: ZakatRules,
    val calculatorType: CalculatorType = CalculatorType.NONE
)

data class ZakatRules(
    val definition: String,
    val hukm: String = "Wajib (Fardhu Ain)", // Law
    val conditions: List<String>, // Syarat Wajib
    val nisab: String, // Threshold
    val haul: String, // Time period
    val rate: String, // Kadar Zakat
    val recipients: List<String> = listOf(
        "Fakir (Destitute)", "Miskin (Poor)", "Amil (Collectors)", 
        "Muallaf (New converts)", "Riqab (Slaves)", "Gharimin (Debtors)",
        "Fisabilillah (In path of Allah)", "Ibn Sabil (Wayfarer)"
    ),
    val dalil: String = "" // Quran/Hadith source
)

enum class CalculatorType {
    NONE, GOLD, SILVER, MONEY, TRADE, AGRICULTURE, LIVESTOCK
}

object ZakatContent {
    val allZakatTypes = listOf(
        ZakatType(
            id = "fitrah",
            title = "Zakat Fitrah",
            arabicTitle = "زكاة الفطرة",
            description = "Zakat diri yang wajib dikeluarkan pada bulan Ramadhan sebelum shalat Idul Fitri.",
            category = ZakatCategory.FITRAH,
            icon = Icons.Default.RiceBowl,
            rules = ZakatRules(
                definition = "Zakat yang wajib dikeluarkan berupaka makanan pokok oleh setiap muslim yang menemui sebagian bulan Ramadhan dan sebagian bulan Syawal.",
                conditions = listOf(
                    "Beragama Islam/Muslim",
                    "Hidup pada saat terbenamnya matahari di akhir Ramadhan",
                    "Mempunyai kelebihan makanan pokok untuk diri dan tanggungannya pada malam dan hari raya"
                ),
                nisab = "Tidak ada nisab (asal mencukupi syarat)",
                haul = "Satu kali setahun (Ramadhan)",
                rate = "1 Sha' (sekitar 2.5 kg - 3.0 kg) makanan pokok (beras)",
                dalil = "Rasulullah SAW mewajibkan zakat fitrah sebanyak satu sha' kurma atau satu sha' gandum..."
            )
        ),
        ZakatType(
            id = "gold",
            title = "Zakat Emas",
            arabicTitle = "زكاة الذهب",
            description = "Zakat atas kepemilikan emas yang telah mencapai nisab dan haul.",
            category = ZakatCategory.MAL,
            icon = Icons.Default.MonetizationOn,
            rules = ZakatRules(
                definition = "Zakat yang wajib dikeluarkan atas kepemilikan emas murni maupun perhiasan (jika melampaui batas kewajaran) yang disimpan.",
                conditions = listOf(
                    "Milik sendiri secara sempurna",
                    "Sampai Nisab",
                    "Telah berlalu satu tahun (Haul)"
                ),
                nisab = "85 gram emas murni",
                haul = "1 Tahun Qamariyah",
                rate = "2.5%",
                dalil = "Dan orang-orang yang menyimpan emas dan perak dan tidak menafkahkannya pada jalan Allah..."
            ),
            calculatorType = CalculatorType.GOLD
        ),
        ZakatType(
            id = "silver",
            title = "Zakat Perak",
            arabicTitle = "زكاة الفضة",
            description = "Zakat atas kepemilikan perak yang telah mencapai nisab dan haul.",
            category = ZakatCategory.MAL,
            icon = Icons.Default.LensBlur, // Placeholder for Silver shape
            rules = ZakatRules(
                definition = "Zakat yang wajib dikeluarkan atas kepemilikan perak.",
                conditions = listOf(
                    "Milik sendiri secara sempurna",
                    "Sampai Nisab",
                    "Telah berlalu satu tahun (Haul)"
                ),
                nisab = "595 gram perak",
                haul = "1 Tahun Qamariyah",
                rate = "2.5%",
            ),
            calculatorType = CalculatorType.SILVER
        ),
        ZakatType(
            id = "money",
            title = "Zakat Maal (Uang)",
            arabicTitle = "زكاة المال",
            description = "Zakat simpanan uang, tabungan, deposito, atau surat berharga.",
            category = ZakatCategory.MAL,
            icon = Icons.Default.AttachMoney,
            rules = ZakatRules(
                definition = "Zakat atas uang atau surat berharga yang disimpan. Diqiyaskan (dianalogikan) dengan emas.",
                conditions = listOf(
                    "Milik penuh",
                    "Sampai Nisab (setara 85 gr emas)",
                    "Bebas dari hutang yang jatuh tempo",
                    "Mencapai Haul 1 tahun"
                ),
                nisab = "Setara 85 gram emas",
                haul = "1 Tahun",
                rate = "2.5%",
            ),
            calculatorType = CalculatorType.MONEY
        ),
        ZakatType(
            id = "trade",
            title = "Zakat Perdagangan",
            arabicTitle = "زكاة التجارة",
            description = "Zakat atas aset perniagaan yang diputar untuk mencari keuntungan.",
            category = ZakatCategory.MAL,
            icon = Icons.Default.Store,
            rules = ZakatRules(
                definition = "Zakat yang dikeluarkan dari harta niaga perniagaan. Harta niaga adalah apa yang dipersiapkan untuk diperjualbelikan demi keuntungan.",
                conditions = listOf(
                    "Niat untuk diperdagangkan",
                    "Nilai aset lancar (modal + laba) mencapai nisab",
                    "Telah berlalu satu tahun (Haul)"
                ),
                nisab = "Setara 85 gram emas",
                haul = "1 Tahun",
                rate = "2.5% dari (Modal Diputar + Keuntungan + Piutang Lancar - Hutang Jatuh Tempo)",
            ),
            calculatorType = CalculatorType.TRADE
        ),
        ZakatType(
            id = "agriculture",
            title = "Zakat Pertanian",
            arabicTitle = "زكاة الزراعة",
            description = "Zakat hasil bumi berupa makanan pokok yang tahan lama.",
            category = ZakatCategory.MAL,
            icon = Icons.Default.Agriculture,
            rules = ZakatRules(
                definition = "Zakat yang dikeluarkan saat panen hasil bumi yang menjadi makanan pokok dan tahan disimpan (seperti padi, gandum, jagung, kurma).",
                conditions = listOf(
                    "Tanaman sendiri",
                    "Merupakan makanan pokok dan tahan lama",
                    "Mencapai nisab"
                ),
                nisab = "5 Wasaq (sekitar 653 kg gabah/biji-bijian bersih)",
                haul = "Setiap kali panen (tidak menunggu setahun)",
                rate = "10% (jika air hujan/alami) atau 5% (jika ada biaya irigasi)",
                dalil = "Makanlah dari buahnya (yang bermacam-macam itu) bila dia berbuah, dan tunaikanlah haknya di hari memetik hasilnya..."
            ),
            calculatorType = CalculatorType.AGRICULTURE
        ),
        ZakatType(
            id = "livestock",
            title = "Zakat Peternakan",
            arabicTitle = "زكاة An'am",
            description = "Zakat hewan ternak (Unta, Sapi/Kerbau, Kambing/Domba).",
            category = ZakatCategory.MAL,
            icon = Icons.Default.Pets,
            rules = ZakatRules(
                definition = "Zakat wajib bagi pemilik hewan ternak yang digembalakan (saaimah) dan tidak dipekerjakan.",
                conditions = listOf(
                    "Hewan Saaimah (digembalakan di padang rumput umum)",
                    "Tidak dipakai bekerja (membajak, dll)",
                    "Mencapai Nisab",
                    "Haul 1 tahun"
                ),
                nisab = "Unta (5 ekor), Sapi (30 ekor), Kambing (40 ekor)",
                haul = "1 Tahun",
                rate = "Bervariasi (Lihat tabel detail)", // Simplified for now
            ),
            calculatorType = CalculatorType.LIVESTOCK
        ),
        ZakatType(
            id = "rikaz",
            title = "Zakat Rikaz (Temuan)",
            arabicTitle = "زكاة الركاز",
            description = "Zakat atas harta karun temuan dari zaman jahiliyah.",
            category = ZakatCategory.MAL,
            icon = Icons.Default.Diamond, // Placeholder
            rules = ZakatRules(
                definition = "Harta peninggalan masa lalu yang ditemukan (harta karun) yang tidak ada pemiliknya.",
                conditions = listOf(
                    "Menemukan harta terpendam"
                ),
                nisab = "Tidak ada nisab (sedikit atau banyak)",
                haul = "Langsung saat ditemukan",
                rate = "20% (Seperlima)",
            )
        )
    )

    fun getByType(id: String): ZakatType? = allZakatTypes.find { it.id == id }
}
