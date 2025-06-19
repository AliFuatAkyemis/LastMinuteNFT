package com.example.lastminute.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.lastminute.R
import com.example.lastminute.model.NftItem
import com.example.lastminute.util.CartManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun MainScreen(navController: NavHostController) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableStateOf("Explore") }
    var showProfile by rememberSaveable { mutableStateOf(false) }

    // Get colors from MaterialTheme
    val backgroundColor = MaterialTheme.colorScheme.background
    val contentColor = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Kullanıcı ismi için state
    var userInitial by remember { mutableStateOf("A") }

    // Firestore'dan kullanıcı adı alıp ilk harfi göster
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    LaunchedEffect(uid) {
        if (uid != null) {
            val doc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
            val name = doc.getString("name") ?: "A"
            userInitial = name.firstOrNull()?.uppercase() ?: "?"
        }
    }

    // NFT listesi (örnek)
    var nftList by rememberSaveable { mutableStateOf<List<NftItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        nftList = fetchNfts()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerScreen(
                onItemClick = { label ->
                    scope.launch { drawerState.close() }
                    when (label.lowercase()) {
                        "home" -> navController.popBackStack("main", false)
                        "cart" -> navController.navigate("cart")
                        "apppreferences" -> navController.navigate("apppreferences")
                        "faq" -> navController.navigate("faq")
                        "support" -> navController.navigate("support")
                        "about" -> navController.navigate("about")
                    }
                },
                onLogoutClick = {
                    navController.popBackStack("login", false)
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(16.dp)
            ) {
                // Üst Menü: Menü, Başlık, Haber, Chat ve Profil İkonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "Menu",
                        tint = contentColor,
                        modifier = Modifier.clickable {
                            scope.launch { drawerState.open() }
                        }
                    )

                    Text("Home", fontSize = 20.sp, color = contentColor)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Haber İkonu
                        IconButton(
                            onClick = {
                                navController.navigate("news")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Article,
                                contentDescription = "News",
                                tint = contentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Chat İkonu
                        IconButton(
                            onClick = {
                                navController.navigate("user_selection")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Chat,
                                contentDescription = "Chat",
                                tint = contentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Profil Kutusu
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                                .clickable { showProfile = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(userInitial, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sekmeler (Tabs)
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Explore", "Favourites").forEach { tab ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = tab },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(tab, fontSize = 16.sp, color = contentColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .width(40.dp)
                                    .background(if (selectedTab == tab) contentColor else Color.Transparent)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gösterilecek NFT Listesi (Explore veya Favourites)
                val itemsToShow = if (selectedTab == "Explore") nftList else nftList.filter { it.isFavorite }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(itemsToShow) { nft ->
                        NFTCard(
                            nft = nft,
                            onFavoriteClick = {
                                nftList = nftList.map {
                                    if (it.id == nft.id) it.copy(isFavorite = !it.isFavorite) else it
                                }
                            },
                            onClick = {
                                val encodedUrl = URLEncoder.encode(nft.imageUrl, StandardCharsets.UTF_8.toString())
                                navController.navigate("detail/${encodedUrl}/${nft.owner}/${nft.price}/${nft.id}")
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Alt Butonlar (Create ve Cart)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { navController.navigate("upload") },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Create", fontSize = 18.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { navController.navigate("cart") },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.cart),
                                contentDescription = "Cart",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        if (CartManager.cartItems.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = (-4).dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    CartManager.cartItems.size.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Profil popup görünümü
            if (showProfile) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp, end = 16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    ProfilePopup(
                        onClose = { showProfile = false },
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun ImgurImage(url: String, contentDescription: String?, contentScale: ContentScale = ContentScale.Crop, modifier: Modifier = Modifier) {
    Image(
        painter = rememberAsyncImagePainter(model = url),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

suspend fun fetchNfts(): List<NftItem> {
    val db = FirebaseFirestore.getInstance()
    return try {
        val snapshot = db.collection("nfts").get().await()
        snapshot.documents.mapNotNull { it.toObject(NftItem::class.java) }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList<NftItem>()
    }
}