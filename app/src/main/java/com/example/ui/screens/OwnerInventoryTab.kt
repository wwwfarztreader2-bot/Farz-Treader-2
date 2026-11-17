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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import com.example.ui.components.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.local.CategoryEntity
import com.example.data.local.ProductEntity
import com.example.ui.components.StockAlertBanner
import com.example.ui.theme.BrandRed
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.util.FormatUtils
import com.example.viewmodel.OwnerUiState
import com.example.viewmodel.OwnerViewModel

private enum class InventoryFilter(val label: String) {
    SEMUA("Semua"),
    HAMPIR_HABIS("Hampir Habis"),
    HABIS("Stok Kosong"),
    NONAKTIF("Nonaktif")
}

@Composable
fun OwnerInventoryTab(
    state: OwnerUiState,
    viewModel: OwnerViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(InventoryFilter.SEMUA) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var adjustingStockProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var deletingProduct by remember { mutableStateOf<ProductEntity?>(null) }

    val filteredProducts = remember(state.allProducts, searchQuery, activeFilter) {
        state.allProducts.filter { product ->
            val matchesQuery = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.categoryName.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (activeFilter) {
                InventoryFilter.SEMUA -> product.isActive
                InventoryFilter.HAMPIR_HABIS -> product.isActive && product.stock in 1..product.minStock
                InventoryFilter.HABIS -> product.isActive && product.stock <= 0
                InventoryFilter.NONAKTIF -> !product.isActive
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddProductDialog = true },
                containerColor = BrandRed,
                contentColor = Color.White,
                modifier = Modifier.testTag("inventory_add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Barang Baru")
            }
        }
    ) { innerPad ->
        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StockAlertBanner(
                    outOfStockCount = state.outOfStockProducts.size,
                    lowStockCount = state.lowStockProducts.size,
                    onViewInventory = { activeFilter = InventoryFilter.HABIS }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari nama barang atau kategori...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inventory_search_input")
                    )
                    OutlinedButton(
                        onClick = { showAddCategoryDialog = true },
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("inventory_add_category_button")
                    ) {
                        Icon(AppIcons.Category, contentDescription = "Tambah Kategori")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InventoryFilter.entries.forEach { filter ->
                        val count = when (filter) {
                            InventoryFilter.SEMUA -> state.allProducts.count { it.isActive }
                            InventoryFilter.HAMPIR_HABIS -> state.lowStockProducts.size
                            InventoryFilter.HABIS -> state.outOfStockProducts.size
                            InventoryFilter.NONAKTIF -> state.allProducts.count { !it.isActive }
                        }
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter },
                            label = { Text("${filter.label} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandRed,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("inventory_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = AppIcons.Inventory2Filled,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Belum ada barang pada kategori ini",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Ketuk tombol + di kanan bawah untuk menambahkan barang baru.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    InventoryProductCard(
                        product = product,
                        onEdit = { editingProduct = product },
                        onAdjustStock = { adjustingStockProduct = product },
                        onToggleActive = { viewModel.toggleProductActive(product) },
                        onDelete = { deletingProduct = product }
                    )
                }
            }
        }
    }

    if (showAddProductDialog) {
        ProductFormDialog(
            title = "Tambah Barang Baru",
            categories = state.categories,
            initialProduct = null,
            onDismiss = { showAddProductDialog = false },
            onSave = { name, category, cost, sell, stock, minStock ->
                viewModel.addProduct(name, category, cost, sell, stock, minStock)
                showAddProductDialog = false
            }
        )
    }

    editingProduct?.let { product ->
        ProductFormDialog(
            title = "Ubah Data Barang",
            categories = state.categories,
            initialProduct = product,
            onDismiss = { editingProduct = null },
            onSave = { name, category, cost, sell, _, minStock ->
                viewModel.updateProduct(product, name, category, cost, sell, minStock)
                editingProduct = null
            }
        )
    }

    adjustingStockProduct?.let { product ->
        StockAdjustDialog(
            product = product,
            onDismiss = { adjustingStockProduct = null },
            onConfirm = { newStock, reason ->
                viewModel.adjustStock(product, newStock, reason)
                adjustingStockProduct = null
            }
        )
    }

    deletingProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { deletingProduct = null },
            title = { Text("Hapus Barang?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Barang '${product.name}' akan dihapus permanen dari daftar. Jika hanya ingin disembunyikan dari kasir, gunakan Nonaktifkan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product)
                        deletingProduct = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProduct = null }) { Text("Batal") }
            }
        )
    }

    if (showAddCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Tambah Kategori Barang", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = catName,
                    onValueChange = { catName = it },
                    label = { Text("Nama Kategori") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            viewModel.addCategory(catName)
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun InventoryProductCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onAdjustStock: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val stockBg = when {
        !product.isActive -> MaterialTheme.colorScheme.surfaceVariant
        product.stock <= 0 -> DangerRedContainer
        product.stock <= product.minStock -> WarningAmberContainer
        else -> ProfitGreenContainer
    }
    val stockFg = when {
        !product.isActive -> MaterialTheme.colorScheme.onSurfaceVariant
        product.stock <= 0 -> DangerRed
        product.stock <= product.minStock -> WarningAmber
        else -> ProfitGreen
    }
    val stockLabel = when {
        !product.isActive -> "Nonaktif"
        product.stock <= 0 -> "Stok Kosong (0)"
        product.stock <= product.minStock -> "Hampir Habis (${product.stock})"
        else -> "Stok: ${product.stock}"
    }
    val margin = product.sellPrice - product.costPrice

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(color = stockBg, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = stockLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = stockFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Harga Modal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FormatUtils.formatRupiah(product.costPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Harga Jual", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        FormatUtils.formatRupiah(product.sellPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Untung / Unit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        FormatUtils.formatRupiah(margin),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (margin >= 0) ProfitGreen else DangerRed
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAdjustStock,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(AppIcons.Exposure, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stok", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ubah", style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = onToggleActive) {
                    Icon(
                        imageVector = AppIcons.PowerSettingsNew,
                        contentDescription = if (product.isActive) "Nonaktifkan" else "Aktifkan",
                        tint = if (product.isActive) WarningAmber else ProfitGreen
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Barang",
                        tint = DangerRed
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormDialog(
    title: String,
    categories: List<CategoryEntity>,
    initialProduct: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (String, CategoryEntity, Long, Long, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            categories.firstOrNull { it.id == initialProduct?.categoryId } ?: categories.firstOrNull()
        )
    }
    var costText by remember { mutableStateOf(initialProduct?.costPrice?.toString() ?: "") }
    var sellText by remember { mutableStateOf(initialProduct?.sellPrice?.toString() ?: "") }
    var stockText by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "10") }
    var minStockText by remember { mutableStateOf(initialProduct?.minStock?.toString() ?: "5") }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Barang") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Pilih Kategori",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Harga Modal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_cost_input")
                )
                OutlinedTextField(
                    value = sellText,
                    onValueChange = { sellText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Harga Jual (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_sell_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (initialProduct == null) {
                        OutlinedTextField(
                            value = stockText,
                            onValueChange = { stockText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Stok Awal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_stock_input")
                        )
                    }
                    OutlinedTextField(
                        value = minStockText,
                        onValueChange = { minStockText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Batas Hampir Habis") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cat = selectedCategory ?: return@Button
                    val cost = costText.toLongOrNull() ?: 0L
                    val sell = sellText.toLongOrNull() ?: 0L
                    val stock = stockText.toIntOrNull() ?: 0
                    val minStock = minStockText.toIntOrNull() ?: 5
                    if (name.isNotBlank()) {
                        onSave(name, cat, cost, sell, stock, minStock)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandRed,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("product_save_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun StockAdjustDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var stockValue by remember { mutableStateOf(product.stock.toString()) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Stok • ${product.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Stok saat ini: ${product.stock}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-5, -1, 1, 5, 10).forEach { delta ->
                        OutlinedButton(
                            onClick = {
                                val current = stockValue.toIntOrNull() ?: product.stock
                                stockValue = (current + delta).coerceAtLeast(0).toString()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (delta > 0) "+$delta" else "$delta", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                OutlinedTextField(
                    value = stockValue,
                    onValueChange = { stockValue = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Jumlah Stok Baru") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Keterangan (mis: Restok supplier / Koreksi)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newStock = stockValue.toIntOrNull() ?: product.stock
                    onConfirm(newStock, reason)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandRed,
                    contentColor = Color.White
                )
            ) {
                Text("Simpan Stok")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
