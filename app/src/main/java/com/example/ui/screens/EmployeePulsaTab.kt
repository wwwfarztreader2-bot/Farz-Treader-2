package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.PaymentMethod
import com.example.data.local.PulsaStatus
import com.example.data.local.PulsaTransactionEntity
import com.example.ui.components.AppIcons
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedDark
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.util.FormatUtils
import com.example.viewmodel.EmployeeViewModel
import com.example.viewmodel.EmployeeWorkspaceUiState

private val PRESET_SERVICES = listOf(
    "Pulsa Telkomsel",
    "Pulsa Indosat",
    "Pulsa XL",
    "Pulsa Tri",
    "Pulsa Smartfren",
    "Paket Data",
    "Token PLN",
    "Top Up E-Wallet"
)

@Composable
fun EmployeePulsaTab(
    state: EmployeeWorkspaceUiState,
    viewModel: EmployeeViewModel,
    modifier: Modifier = Modifier
) {
    var serviceName by remember { mutableStateOf("Pulsa Telkomsel") }
    var phoneNumber by remember { mutableStateOf("") }
    var nominalText by remember { mutableStateOf("") }
    var costPriceText by remember { mutableStateOf("") }
    var adminFeeText by remember { mutableStateOf("2000") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }
    var status by remember { mutableStateOf(PulsaStatus.BERHASIL) }

    val nominal = nominalText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val costPrice = costPriceText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val adminFee = adminFeeText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val totalPayment = nominal + adminFee

    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EmployeeSelectorCard(
                employees = state.activeEmployees,
                selectedEmployee = state.selectedEmployee,
                onSelectEmployee = viewModel::selectEmployee
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.PhoneAndroidFilled, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Catat Transaksi Pulsa & Layanan Digital",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pencatatan manual tanpa saldo otomatis. Biaya admin tercatat sebagai pendapatan admin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PRESET_SERVICES.forEach { preset ->
                            SuggestionChip(
                                onClick = { serviceName = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serviceName,
                        onValueChange = { serviceName = it },
                        label = { Text("Nama Layanan / Operator") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pulsa_service_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Nomor HP / ID Pelanggan") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pulsa_phone_input")
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nominalText,
                            onValueChange = { nominalText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Nominal (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pulsa_nominal_input")
                        )
                        OutlinedTextField(
                            value = adminFeeText,
                            onValueChange = { adminFeeText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Biaya Admin (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pulsa_admin_input")
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5_000L, 10_000L, 20_000L, 25_000L, 50_000L, 100_000L).forEach { amt ->
                            OutlinedButton(
                                onClick = { nominalText = amt.toString() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(FormatUtils.formatRupiah(amt), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Harga Modal (Opsional, Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))
                    Text("Metode Pembayaran", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text("Status Transaksi", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PulsaStatus.entries.forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = BrandRedDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Pembayaran Pelanggan", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                                Text("Nominal + Biaya Admin", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
                            }
                            Text(
                                text = FormatUtils.formatRupiah(totalPayment),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            viewModel.submitPulsaTransaction(
                                serviceName = serviceName,
                                phoneNumber = phoneNumber,
                                nominal = nominal,
                                costPrice = costPrice,
                                adminFee = adminFee,
                                paymentMethod = paymentMethod,
                                status = status,
                                onSuccessClear = {
                                    phoneNumber = ""
                                    nominalText = ""
                                    costPriceText = ""
                                }
                            )
                        },
                        enabled = !state.isSubmitting && state.selectedEmployee != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("pulsa_submit_button")
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("SIMPAN TRANSAKSI PULSA", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (state.todayPulsaHistory.isNotEmpty()) {
            item {
                Text(
                    text = "Transaksi Pulsa Hari Ini (${state.todayPulsaHistory.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(state.todayPulsaHistory, key = { it.id }) { item ->
                EmployeePulsaHistoryCard(
                    tx = item,
                    onMarkSuccess = { viewModel.updatePendingPulsaStatus(item.id, PulsaStatus.BERHASIL) },
                    onMarkCanceled = { viewModel.updatePendingPulsaStatus(item.id, PulsaStatus.DIBATALKAN) }
                )
            }
        }
    }
}

@Composable
private fun EmployeePulsaHistoryCard(
    tx: PulsaTransactionEntity,
    onMarkSuccess: () -> Unit,
    onMarkCanceled: () -> Unit
) {
    val (bg, fg) = when (tx.status) {
        PulsaStatus.BERHASIL.label -> ProfitGreenContainer to ProfitGreen
        PulsaStatus.PENDING.label -> WarningAmberContainer to WarningAmber
        else -> DangerRedContainer to DangerRed
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tx.serviceName} • ${tx.phoneNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = tx.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = fg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${tx.transactionNumber} • ${FormatUtils.formatTimeWib(tx.createdAtMillis)} • ${tx.paymentMethod}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Nominal: ${FormatUtils.formatRupiah(tx.nominal)} • Admin: ${FormatUtils.formatRupiah(tx.adminFee)} • Total: ${FormatUtils.formatRupiah(tx.totalPayment)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )

            if (tx.status == PulsaStatus.PENDING.label) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onMarkSuccess,
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tandai Berhasil", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onMarkCanceled,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batalkan", color = DangerRed, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
