package com.example.lastminute.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lastminute.ui.theme.ThemeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPreferencesScreen(onBackClick: () -> Unit) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoUpdateEnabled by remember { mutableStateOf(true) }
    
    // Use the shared ThemeState
    val isDarkMode = ThemeState.isDarkMode

    val backgroundColor = if (isDarkMode) Color.Black else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Preferences", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Preferences
            PreferenceItem(
                title = "Enable Notifications",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it },
                textColor = textColor
            )
            
            PreferenceItem(
                title = "Dark Mode",
                checked = isDarkMode,
                onCheckedChange = { newValue ->
                    ThemeState.isDarkMode = newValue
                },
                textColor = textColor
            )
            
            PreferenceItem(
                title = "Auto Updates",
                checked = autoUpdateEnabled,
                onCheckedChange = { autoUpdateEnabled = it },
                textColor = textColor
            )
        }
    }
}

@Composable
fun PreferenceItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        )
    }
}
