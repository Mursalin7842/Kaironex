package com.mursaline.kaironex.features.zones.vitality

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.core.encodeBase64
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object FridgeScanScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<VitalityViewModel>()
        val state by viewModel.uiState.collectAsState()
        
        // Local Screen State
        var scanState by remember { mutableStateOf<ScanState>(ScanState.Camera) }

        // Transition logic: If we find ingredients and we were analyzing, show results
        LaunchedEffect(state.fridgeIngredients) {
            if (state.fridgeIngredients.isNotEmpty() && scanState is ScanState.Analyzing) {
                scanState = ScanState.Results(state.fridgeIngredients)
            }
        }
        val scope = rememberCoroutineScope()

        val launcher = rememberFilePickerLauncher(
            type = PickerType.Image,
            mode = PickerMode.Single
        ) { file ->
            file?.let { platformFile ->
                scope.launch {
                    val bytes = platformFile.readBytes()
                    val b64 = encodeBase64(bytes)
                    scanState = ScanState.Analyzing
                    viewModel.onScanFridge(b64)
                    
                    // Wait for results in UI state
                    // We'll observe the state change instead of setting it here
                }
            }
        }

        Scaffold(
            containerColor = KaironexColors.CloudGray,
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            if(scanState is ScanState.Results) "Scan Results" else "Fridge Scan", 
                            fontWeight = FontWeight.Bold,
                            color = KaironexColors.Slate900
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = KaironexColors.Slate900)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = KaironexColors.CloudGray
                    ),
                    actions = {
                        if (scanState is ScanState.Results) {
                            IconButton(onClick = { scanState = ScanState.Camera }) {
                                Icon(Icons.Default.Refresh, "Rescan", tint = KaironexColors.GeminiBlurple)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (val current = scanState) {
                    is ScanState.Camera -> CameraView(
                        onCapture = {
                            // Launch the file picker (which handles camera/gallery)
                            launcher.launch()
                        },
                        onUpload = {
                            launcher.launch()
                        }
                    )
                    is ScanState.Analyzing -> AnalyzingView()
                    is ScanState.Results -> ResultsView(
                        ingredients = current.ingredients,
                        onSave = { navigator.pop() }
                    )
                }
            }
        }
    }

    @Composable
    private fun CameraView(onCapture: () -> Unit, onUpload: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Viewfinder Mock
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.Black,
                shadowElevation = 4.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // In a real app, this would be CameraX PreviewView
                    Text(
                        "📷 Point at Fridge",
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                    
                    // Grid Overlay
                    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                         // Minimal overlay UI
                         Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                             Surface(
                                 color = Color.Black.copy(alpha=0.5f), 
                                 shape = RoundedCornerShape(8.dp)
                             ) {
                                 Text("AI VISION READY", color = KaironexColors.NeonGreen, fontSize = 10.sp, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                             }
                         }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            
            // Controls
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                 IconButton(onClick = { 
                     // Launch Gallery Picker
                     onUpload()
                 }) { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Upload, null, tint = KaironexColors.Slate500)
                        Text("Upload", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate500)
                    }
                 }
                 
                 Button(
                     onClick = onCapture,
                     modifier = Modifier.size(80.dp),
                     shape = CircleShape,
                     colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.GeminiBlurple)
                 ) {
                     Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(32.dp))
                 }
                 
                 IconButton(onClick = {}) { Icon(Icons.Default.Refresh, null, tint = KaironexColors.Slate500) }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    @Composable
    private fun AnalyzingView() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = KaironexColors.GeminiBlurple,
                strokeWidth = 6.dp
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Analyzing Ingredients...",
                style = MaterialTheme.typography.titleMedium,
                color = KaironexColors.Slate900,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Brain is identifying food items",
                style = MaterialTheme.typography.bodyMedium,
                color = KaironexColors.Slate500
            )
        }
    }

    @Composable
    private fun ResultsView(ingredients: List<FridgeIngredient>, onSave: () -> Unit) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Stats
            Surface(
                color = KaironexColors.SuccessGreen.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "🎉 Found ${ingredients.size} Ingredients!",
                    modifier = Modifier.padding(16.dp),
                    color = KaironexColors.SuccessGreen,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(ingredients) { item ->
                    IngredientCard(item)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.padding(16.dp).fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.GeminiBlurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save Inventory", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    private fun IngredientCard(item: FridgeIngredient) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🥘", fontSize = 32.sp) // Default emoji for now
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
                    Text(item.quantity, style = MaterialTheme.typography.bodySmall, color = KaironexColors.Slate500)
                }
                Text("${item.servings} svg", color = KaironexColors.SuccessGreen, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Models
    sealed class ScanState {
        object Camera : ScanState()
        object Analyzing : ScanState()
        data class Results(val ingredients: List<FridgeIngredient>) : ScanState()
    }
}
