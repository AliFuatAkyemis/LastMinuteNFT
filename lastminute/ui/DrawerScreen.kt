package com.example.lastminute.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lastminute.R

@Composable
fun DrawerScreen(
    onItemClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            "Prefs",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        DrawerItem("Home", R.drawable.home) { onItemClick("home") }
        DrawerItem("Cart", R.drawable.cart) { onItemClick("cart") }
        DrawerItem("App Preferences", R.drawable.settings) { onItemClick("apppreferences") }
        DrawerItem("Frequently Asked Questions", R.drawable.question) { onItemClick("faq") }
        DrawerItem("Live Support", R.drawable.help) { onItemClick("support") }
        DrawerItem("About Us", R.drawable.information) { onItemClick("about") }

        Spacer(modifier = Modifier.weight(1f))

        DrawerItem(
            label = "Log out",
            icon = R.drawable.logout,
            color = MaterialTheme.colorScheme.error,
            onClick = onLogoutClick
        )
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: Int,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = label,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = color, fontSize = 16.sp)
    }
}
