package com.mursaline.kaironex.features.zones.vitality

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.ui.components.KxTextField
import kotlinx.coroutines.launch

object EmergencyFundScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<VitalityViewModel>()
        val state by viewModel.uiState.collectAsState()
        
        var transactionAmount by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = KaironexColors.CloudGray,
            topBar = {
                TopAppBar(
                    title = { Text("Emergency Fund", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = KaironexColors.CloudGray)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Balance Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = KaironexColors.Slate900),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "PROTECTED BALANCE",
                            style = MaterialTheme.typography.labelMedium,
                            color = KaironexColors.Slate300,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "$${state.emergencyFund}",
                            style = MaterialTheme.typography.displayLarge,
                            color = KaironexColors.SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Actions
                Text("MANAGE FUNDS", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate500)
                
                KxTextField(
                    value = transactionAmount,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) transactionAmount = it },
                    label = "Amount ($)",
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            val amount = transactionAmount.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                isSubmitting = true
                                viewModel.manageEmergencyFund("deposit", amount)
                                transactionAmount = ""
                                isSubmitting = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.SuccessGreen),
                        enabled = !isSubmitting && transactionAmount.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Deposit")
                    }

                    Button(
                        onClick = {
                            val amount = transactionAmount.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                isSubmitting = true
                                viewModel.manageEmergencyFund("withdraw", amount)
                                transactionAmount = ""
                                isSubmitting = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.Warning),
                        enabled = !isSubmitting && transactionAmount.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Remove, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Withdraw")
                    }
                }

                Spacer(Modifier.weight(1f))
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = KaironexColors.Slate100),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    PaddingValues(16.dp)
                    Text(
                        "⚠️ Withdrawal affects your overall financial stability score. Use only for genuine emergencies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KaironexColors.Slate500,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
