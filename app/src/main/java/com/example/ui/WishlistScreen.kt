package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Product
import com.example.model.UserRole
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.LightBlueSurface
import com.example.ui.theme.RomelNavy
import com.example.ui.theme.RomelRed
import com.example.ui.theme.RomelText
import com.example.ui.theme.SlateMuted

@Composable
fun WishlistScreen(
    role: UserRole,
    wishlistProducts: List<Product>,
    onProductClick: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit,
    onOpenCalculator: (Product) -> Unit,
    onEditPrice: (Product) -> Unit,
    onAdjustStock: (Product) -> Unit,
    onBrowseCatalog: () -> Unit
) {
    var filterQuery by remember { mutableStateOf("") }

    val displayedProducts = remember(wishlistProducts, filterQuery) {
        val q = filterQuery.trim().lowercase()
        if (q.isEmpty()) wishlistProducts
        else wishlistProducts.filter { prod ->
            prod.name.lowercase().contains(q) ||
                prod.code.lowercase().contains(q) ||
                prod.category.lowercase().contains(q)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header summary & quick search within wishlist
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CleanWhite)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = RomelRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = RomelRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "My Saved Favorites",
                            style = MaterialTheme.typography.titleMedium,
                            color = RomelNavy,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${wishlistProducts.size} ${if (wishlistProducts.size == 1) "product" else "products"} synced to your account",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                    }
                }
                Surface(
                    color = LightBlueSurface,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "${wishlistProducts.size} saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = RomelNavy,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (wishlistProducts.isNotEmpty()) {
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = {
                        Text(
                            text = "Filter saved items by name, code, or category...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Filter wishlist", tint = RomelNavy)
                    },
                    trailingIcon = {
                        if (filterQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { filterQuery = "" },
                                modifier = Modifier.testTag("wishlist_search_clear")
                            ) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear filter", tint = SlateMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(999.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CleanWhite,
                        unfocusedContainerColor = CleanWhite,
                        focusedBorderColor = RomelNavy,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wishlist_search_input")
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (wishlistProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = CleanWhite),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = LightBlueSurface,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.FavoriteBorder,
                                    contentDescription = null,
                                    tint = RomelRed,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = "Your Wishlist is Empty",
                            style = MaterialTheme.typography.titleLarge,
                            color = RomelNavy,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap the heart icon on any ROMEL switch, socket, plate, or LED product to save it here for quick pricing and ordering.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onBrowseCatalog,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RomelRed,
                                contentColor = CleanWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("wishlist_browse_catalog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ElectricalServices,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Browse ROMEL Catalog", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (displayedProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = SlateMuted
                    )
                    Text(
                        text = "No saved items match \"$filterQuery\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = RomelText,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { filterQuery = "" },
                        colors = ButtonDefaults.buttonColors(containerColor = RomelNavy),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Show All Saved Items")
                    }
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val twoColumns = maxWidth >= 680.dp
                val rows = if (twoColumns) displayedProducts.chunked(2) else displayedProducts.map { listOf(it) }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("wishlist_list")
                ) {
                    items(rows, key = { chunk -> chunk.joinToString("_") { it.id } }) { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowItems.forEach { prod ->
                                ProductCard(
                                    product = prod,
                                    role = role,
                                    onClick = { onProductClick(prod) },
                                    onOpenCalculator = { onOpenCalculator(prod) },
                                    onEditPrice = { onEditPrice(prod) },
                                    onAdjustStock = { onAdjustStock(prod) },
                                    isWishlisted = true,
                                    onToggleWishlist = { onToggleWishlist(prod) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (twoColumns && rowItems.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
