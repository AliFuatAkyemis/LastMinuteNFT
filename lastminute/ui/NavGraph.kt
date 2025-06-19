package com.example.lastminute.ui

import NFTNewsScreen
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.google.firebase.auth.FirebaseAuth


@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "start") {

        composable("start") {
            StartScreen(onStartClick = {
                navController.navigate("signup")
            })
        }

        composable("signup") {
            SignUpScreen(navController)
        }

        composable("login") {
            LoginScreen(navController)
        }

        composable("forgot") {
            ForgotPasswordScreen(
                onBackClick = { navController.popBackStack() },
                navController = navController
            )
        }

        composable("main") {
            MainScreen(navController = navController)
        }

        composable("detail/{imageUrl}/{owner}/{price}/{identity}") { backStackEntry ->

            val encodedImageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
            val imageUrl = java.net.URLDecoder.decode(encodedImageUrl, "UTF-8")

            val owner = backStackEntry.arguments?.getString("owner") ?: "Unknown"
            val price = backStackEntry.arguments?.getString("price") ?: "-"
            val id = backStackEntry.arguments?.getString("identity") ?: "0"

            NFTDetailScreen(
                id = id,
                imageUrl = imageUrl,
                ownerName = owner,
                price = price,
                onBackClick = { navController.popBackStack("main", false) },
                navController
            )
        }

        composable("upload?imageUrl={imageUrl}&nftId={nftId}") { backStackEntry ->
            val imageUrl = backStackEntry.arguments?.getString("imageUrl")
            val nftId = backStackEntry.arguments?.getString("nftId")
            UploadNFT(
                navController = navController,
                imageUrl = imageUrl?.let { java.net.URLDecoder.decode(it, "UTF-8") },
                nftId = nftId
            )
        }

        composable("cart") {
            CartScreen(navController)
        }

        composable("purchase") {
            PurchaseScreen(navController)
        }

        composable("loading") {
            LoadingScreen(onLoadingFinished = { navController.navigate("purchasefinal") })
        }

        composable("purchasefinal") {
            PurchaseFinalScreen(onBackClick = { navController.popBackStack("main", false) })
        }

        composable("payment-methods") {
            PaymentMethodsScreen(navController = navController)
        }

        composable("add-card") {
            AddCardScreen(onBackClick = { navController.popBackStack() })
        }

        composable("edit-profile") {
            EditProfileScreen(
                onBackClick = { navController.popBackStack("main", false) },
                navController = navController
            )
        }

        composable("add_balance") {
            AddBalanceScreen(navController = navController)
        }

        composable("apppreferences") {
            AppPreferencesScreen(onBackClick = { navController.popBackStack("main", false) })
        }

        composable("faq") {
            FAQScreen(onBackClick = { navController.popBackStack("main", false) })
        }

        composable("support") {
            LiveSupportScreen(onBackClick = { navController.popBackStack("main", false) })
        }

        composable("about") {
            AboutUsScreen(onBackClick = { navController.popBackStack("main", false) })
        }

        composable("purchase-history") {
            PurchaseHistoryScreen(onBackClick = { navController.popBackStack("main", false) })
        }

        composable("news") {
            NFTNewsScreen(
                onBackClick = { navController.popBackStack() },
                apiKey = "845c9334ca6c495c886813541a91a3ad"
            )
        }

        // Kullanıcı seçme ve sohbet ekranları için ekle (isteğe bağlı, eğer eklemek istersen)
        composable("user_selection") {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            UserSelectionScreen(
                currentUserId = currentUserId,
                navController
            )
        }

        composable(
            "chat/{chatId}/{otherUserName}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("otherUserName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val otherUserName = Uri.decode(backStackEntry.arguments?.getString("otherUserName") ?: "")
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            ChatScreen(
                chatId = chatId,
                currentUserId = currentUserId,
                otherUserName = otherUserName,
                onBackClick = {
                    navController.popBackStack("userProfile/${chatId.split("_").find { it != currentUserId } ?: ""}", false)
                }
            )
        }

        composable("inventory") {
            InventoryScreen(
                navController = navController,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "userProfile/{userId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@composable
            UserProfileScreen(
                userId = userId,
                currentUserId = currentUserId,
                navController = navController
            )
        }
    }
}
