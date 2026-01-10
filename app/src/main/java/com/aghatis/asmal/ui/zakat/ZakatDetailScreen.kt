package com.aghatis.asmal.ui.zakat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aghatis.asmal.data.model.CalculatorType
import com.aghatis.asmal.data.model.ZakatContent
import com.aghatis.asmal.data.model.ZakatType
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatDetailScreen(
    zakatId: String,
    onNavigateBack: () -> Unit
) {
    val zakat = remember(zakatId) { ZakatContent.getByType(zakatId) }
    
    if (zakat == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Zakat Type Not Found")
            Button(onClick = onNavigateBack) { Text("Go Back") }
        }
        return
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Informasi", "Kalkulator")
    // Hide calculator tab if not applicable
    val showCalculator = zakat.calculatorType != CalculatorType.NONE
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(zakat.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showCalculator) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (!showCalculator || selectedTab == 0) {
                    ZakatInfoContent(zakat)
                } else {
                    ZakatCalculatorContent(zakat)
                }
            }
        }
    }
}

@Composable
fun ZakatInfoContent(zakat: ZakatType) {
    InfoItem(label = "Definisi", value = zakat.rules.definition)
    Spacer(modifier = Modifier.height(16.dp))
    
    InfoItem(label = "Hukum", value = zakat.rules.hukm)
    Spacer(modifier = Modifier.height(16.dp))

    Text("Syarat Wajib", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    zakat.rules.conditions.forEach { condition ->
        Row(modifier = Modifier.padding(top = 4.dp)) {
            Text("• ", fontWeight = FontWeight.Bold)
            Text(condition, style = MaterialTheme.typography.bodyMedium)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    
    InfoItem(label = "Nisab (Batas Minimal)", value = zakat.rules.nisab)
    Spacer(modifier = Modifier.height(16.dp))
    
    InfoItem(label = "Haul (Masa Kepemilikan)", value = zakat.rules.haul)
    Spacer(modifier = Modifier.height(16.dp))
    
    InfoItem(label = "Kadar Zakat", value = zakat.rules.rate)
    Spacer(modifier = Modifier.height(24.dp))
    
    if (zakat.rules.dalil.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Dalil", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"${zakat.rules.dalil}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ZakatCalculatorContent(zakat: ZakatType) {
    var amountText by remember { mutableStateOf("") }
    var goldPriceText by remember { mutableStateOf("1300000") } // Default estimate IDR
    var result by remember { mutableStateOf<Double?>(null) }
    
    val formatCurrency = { value: Double -> 
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value) 
    }
    
    val goldPrice = goldPriceText.toDoubleOrNull() ?: 0.0
    val amount = amountText.toDoubleOrNull() ?: 0.0
    
    // Simplistic Calculation Logic per type
    fun calculate() {
        result = when (zakat.calculatorType) {
            CalculatorType.GOLD -> {
                if (amount >= 85) amount * 0.025 else 0.0
            }
            CalculatorType.SILVER -> {
                 if (amount >= 595) amount * 0.025 else 0.0
            }
            CalculatorType.MONEY, CalculatorType.TRADE -> {
                val nisabValue = 85 * goldPrice
                if (amount >= nisabValue) amount * 0.025 else 0.0
            }
            CalculatorType.AGRICULTURE -> {
                // Assuming 5% rate for simplicity default
                if (amount >= 653) amount * 0.05 else 0.0
            }
            else -> 0.0
        }
    }

    Text("Kalkulator ${zakat.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))

    // Dynamic Fields based on Type
    when (zakat.calculatorType) {
        CalculatorType.GOLD -> {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Jumlah Emas (gram)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
        CalculatorType.MONEY, CalculatorType.TRADE -> {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Total Nilai Harta (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
             OutlinedTextField(
                value = goldPriceText,
                onValueChange = { goldPriceText = it },
                label = { Text("Harga Emas per Gram (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Text("Digunakan untuk menghitung Nisab (85g emas)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        else -> {
             OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Jumlah (Kg/Gram)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    
    Button(
        onClick = { calculate() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Hitung Zakat")
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    if (result != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Zakat yang Harus Dikeluarkan", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val displayResult = if (zakat.calculatorType == CalculatorType.GOLD || zakat.calculatorType == CalculatorType.SILVER || zakat.calculatorType == CalculatorType.AGRICULTURE) 
                    "${String.format("%.2f", result)} ${if(zakat.calculatorType == CalculatorType.AGRICULTURE) "Kg" else "gram"}"
                else
                    formatCurrency(result!!)
                
                Text(
                    text = displayResult,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (result!! == 0.0) {
                     Spacer(modifier = Modifier.height(8.dp))
                     Text("Belum mencapai Nisab", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
