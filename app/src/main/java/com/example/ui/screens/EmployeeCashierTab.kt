package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PaymentMethod
import com.example.data.local.ProductEntity
import com.example.ui.components.AppIcons
import com.example.ui.components.ReceiptDialog
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeCashierTab(
    state: EmployeeWorkspaceUiState,
    viewModel: EmployeeViewModel,
    isWide: Boolean,
    modifier: Modifier = Modifier
) {
    var showMobileCartSheet by remember { mutableStateOf(false) }

    val filteredProducts = remember(state.activeProducts, state.searchQuery, state.selectedCategoryId) {
        state.activeProducts.filter { prod ->
            val matchSearch = state.searchQuery.isBlank() ||
                prod.name.contains(state.searchQuery, ignoreCase = true) ||
                prod.categoryName.contains(state.searchQuery, ignoreCase = true)
            val matchCat = state.selectedCategoryId == null || prod.categoryId == state.selectedCategoryId
            matchSearch && matchCat
        }
    }

    if (isWide) {
        Row(
            modifier = modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1.25f).fillMaxHeight()) {
                EmployeeSelectorCard(
                    employees = state.activeEmployees,
                    selectedEmployee = state.selectedEmployee,
                    onSelectEmployee = viewModel::selectEmployee
                )
                Spacer(Modifier.height(10.dp))
                ProductCatalogSection(
                    state = state,
                    filteredProducts = filteredProducts,
                    onSearchChange = viewModel::setSearchQuery,
                    onSelectCategory = viewModel::selectCategory,
                    onAddToCart = viewModel::addToCart,
                    modifier = Modifier.weight(1f)
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
            ) {
                CartCheckoutPane(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                EmployeeSelectorCard(
                    employees = state.activeEmployees,
                    selectedEmployee = state.selectedEmployee,
                    onSelectEmployee = viewModel::selectEmployee
                )
                Spacer(Modifier.height(10.dp))
                ProductCatalogSection(
                    state = state,
                    filteredProducts = filteredProducts,
                    onSearchChange = viewModel::setSearchQuery,
                    onSelectCategory = viewModel::selectCategory,
                    onAddToCart = viewModel::addToCart,
                    bottomExtraPadding = if (state.cartItems.isNotEmpty()) 84.dp else 16.dp,
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.cartItems.isNotEmpty()) {
                val totalQty = state.cartItems.sumOf { it.quantity }
                Surface(
                    color = BrandRed,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp,
                    onClick = { showMobileCartSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("cashier_open_cart_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "$totalQty Barang di Keranjang",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Ketuk untuk bayar & proses",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Surface(color = Color.White, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = FormatUtils.formatRupiah(state.cartTotal),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandRedDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showMobileCartSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showMobileCartSheet = false },
                sheetState = sheetState
            ) {
                CartCheckoutPane(
                    state = state,
                    viewModel = viewModel,
                    onTransactionSuccessClose = { showMobileCartSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
        }
    }

    state.lastCompletedReceipt?.let { receipt ->
        ReceiptDialog(
            receipt = receipt,
            onDismiss = viewModel::dismissReceiptDialog
        )
    }
}

@Composable
private fun ProductCatalogSection(
    state: EmployeeWorkspaceUiState,
    filteredProducts: List<ProductEntity>,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
    bottomExtraPadding: androidx.compose.ui.unit.Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari produk di kasir...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cashier_search_input")
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedCategoryId == null,
                onClick = { onSelectCategory(null) },
                label = { Text("Semua") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandRed,
                    selectedLabelColor = Color.White
                )
            )
            state.categories.forEach { cat ->
                FilterChip(
                    selected = state.selectedCategoryId == cat.id,
                    onClick = { onSelectCategory(cat.id) },
                    label = { Text(cat.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (filteredProducts.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Belum ada barang tersedia. Owner dapat menambahkan barang di menu Manajemen Barang.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = bottomExtraPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts, key = { it.id }) { prod ->
                    val inCartQty = state.cartItems.firstOrNull { it.product.id == prod.id }?.quantity ?: 0
                    val isOut = prod.stock <= 0
                    val stockBg = when {
                        isOut -> DangerRedContainer
                        prod.stock <= prod.minStock -> WarningAmberContainer
                        else -> ProfitGreenContainer
                    }
                    val stockFg = when {
                        isOut -> DangerRed
                        prod.stock <= prod.minStock -> WarningAmber
                        else -> ProfitGreen
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prod.categoryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = prod.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = FormatUtils.formatRupiah(prod.sellPrice),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Surface(color = stockBg, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = if (isOut) "Habis" else "Stok ${prod.stock}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = stockFg,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onAddToCart(prod) },
                                enabled = !isOut && inCartQty < prod.stock,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandRed,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("cashier_add_product_${prod.id}")
                            ) {
                                Icon(AppIcons.AddShoppingCart, contentDescription = "Tambah ke keranjang", modifier = Modifier.size(18.dp))
                                if (inCartQty > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Text("($inCartQty)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartCheckoutPane(
    state: EmployeeWorkspaceUiState,
    viewModel: EmployeeViewModel,
    onTransactionSuccessClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Keranjang Belanja",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (state.cartItems.isNotEmpty()) {
                TextButton(onClick = viewModel::clearCart) {
                    Icon(AppIcons.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Kosongkan", color = DangerRed)
                }
            }
        }
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        if (state.cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada barang di keranjang.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.cartItems, key = { it.product.id }) { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(line.product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${FormatUtils.formatRupiah(line.product.sellPrice)} • Subtotal ${FormatUtils.formatRupiah(line.subtotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.updateCartQty(line.product.id, line.quantity - 1) }
                            ) {
                                Icon(AppIcons.Remove, contentDescription = "Kurangi")
                            }
                            Text(
                                text = "${line.quantity}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    if (line.quantity < line.product.stock) {
                                        viewModel.updateCartQty(line.product.id, line.quantity + 1)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tambah")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Pembayaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = FormatUtils.formatRupiah(state.cartTotal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(Modifier.height(10.dp))

            // Payment Method Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = state.paymentMethod == method,
                        onClick = { viewModel.setPaymentMethod(method) },
                        label = { Text(method.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (state.paymentMethod == PaymentMethod.TUNAI) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.cashReceivedInput,
                    onValueChange = viewModel::setCashReceivedInput,
                    label = { Text("Uang Diterima (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cashier_cash_input")
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.setCashReceivedInput(state.cartTotal.toString()) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Uang Pas", style = MaterialTheme.typography.labelSmall)
                    }
                    listOf(10_000L, 20_000L, 50_000L, 100_000L).forEach { nominal ->
                        OutlinedButton(
                            onClick = { viewModel.setCashReceivedInput(nominal.toString()) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(FormatUtils.formatRupiah(nominal), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Kembalian", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = FormatUtils.formatRupiah(state.changeAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    viewModel.processCashierTransaction()
                    onTransactionSuccessClose?.invoke()
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
                    .testTag("cashier_process_button")
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                } else {
                    Text(
                        text = "PROSES TRANSAKSI",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
