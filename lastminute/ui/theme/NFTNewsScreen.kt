@file:OptIn(ExperimentalMaterial3Api::class)import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import retrofit2.HttpException
import java.io.IOException

@Composable
fun NFTNewsScreen(onBackClick: () -> Unit, apiKey: String) {
    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = NewsApiClient.apiService.getNFTNews(apiKey = apiKey)
            articles = response.articles
        } catch (e: IOException) {
            error = "Network Error: ${e.localizedMessage}"
        } catch (e: HttpException) {
            error = "HTTP Error: ${e.localizedMessage}"
        } catch (e: Exception) {
            error = "Unknown Error: ${e.localizedMessage}"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("NFT News") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> Text(error ?: "Error", modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(articles) { article ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                article.urlToImage?.let { imageUrl ->
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = article.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = article.title, style = MaterialTheme.typography.titleMedium)
                                article.description?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
