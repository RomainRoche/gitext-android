package io.gitrad.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.gitrad.sdk.sdk.Gitrad
import io.gitrad.sdk.sdk.rememberGitradString
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TranslationScreen(
                        onRefresh = { lifecycleScope.launch { Gitrad.refresh() } }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { Gitrad.refresh() }
    }
}

@Composable
fun TranslationScreen(onRefresh: () -> Unit) {
    val appName = rememberGitradString("app.name")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = appName, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRefresh) {
            Text("Refresh translations")
        }
    }
}
