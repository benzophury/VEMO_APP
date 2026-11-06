package dev.zlt.companion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zlt.companion.data.UssdResult
import dev.zlt.companion.ui.theme.DarkSurface
import dev.zlt.companion.ui.theme.DarkSurfaceBorder
import dev.zlt.companion.ui.theme.DarkSurfaceElevated
import dev.zlt.companion.ui.theme.NeonCyan
import dev.zlt.companion.ui.theme.NeonGreen
import dev.zlt.companion.ui.theme.NeonRed
import dev.zlt.companion.ui.theme.TextMuted
import dev.zlt.companion.ui.theme.TextPrimary
import dev.zlt.companion.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UssdScreen(
    ussdResult: UssdResult?,
    isLoading: Boolean,
    onSendUssd: (String, Boolean) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var ussdCode by remember { mutableStateOf("*121#") }
    var replyText by remember { mutableStateOf("") }

    val quickCodes = listOf("*121#", "*121*1#", "*5000#", "*123#", "*566#")

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Default.Dialpad,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(28.dp),
                )
                Column {
                    Text(
                        text = "USSD TERMINAL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Dial carrier balance and service codes directly",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }

            // Dial input card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "ENTER USSD CODE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                    )

                    OutlinedTextField(
                        value = ussdCode,
                        onValueChange = { ussdCode = it },
                        placeholder = { Text("*121#") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated,
                        ),
                        trailingIcon = {
                            if (ussdCode.isNotEmpty()) {
                                IconButton(onClick = { ussdCode = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Quick code chips
                    Text(
                        text = "QUICK SHORTCODES",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        quickCodes.forEach { code ->
                            FilterChip(
                                selected = ussdCode == code,
                                onClick = { ussdCode = code },
                                label = { Text(code, fontFamily = FontFamily.Monospace) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCyan,
                                    containerColor = DarkSurfaceElevated,
                                    labelColor = TextSecondary,
                                ),
                            )
                        }
                    }

                    Button(
                        onClick = { onSendUssd(ussdCode.trim(), false) },
                        enabled = ussdCode.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black,
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                "  DIAL USSD CODE",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }

            // Output / Result Terminal Box
            if (ussdResult != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (ussdResult.success) NeonGreen.copy(alpha = 0.5f) else NeonRed.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (ussdResult.success) "CARRIER RESPONSE" else "USSD ERROR",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (ussdResult.success) NeonGreen else NeonRed,
                                letterSpacing = 1.sp,
                            )
                            IconButton(onClick = onClear) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear result", tint = TextMuted)
                            }
                        }

                        // Terminal output container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black, shape = MaterialTheme.shapes.small)
                                .border(1.dp, DarkSurfaceBorder, shape = MaterialTheme.shapes.small)
                                .padding(14.dp),
                        ) {
                            Text(
                                text = ussdResult.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (ussdResult.success) NeonGreen else NeonRed,
                                lineHeight = 20.sp,
                            )
                        }

                        // If USSD session is open, allow sending replies!
                        if (ussdResult.isSessionOpen) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "SESSION ACTIVE — REPLY TO CARRIER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    OutlinedTextField(
                                        value = replyText,
                                        onValueChange = { replyText = it },
                                        placeholder = { Text("e.g. 1 or 2") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonCyan,
                                            unfocusedBorderColor = DarkSurfaceBorder,
                                            focusedContainerColor = DarkSurfaceElevated,
                                            unfocusedContainerColor = DarkSurfaceElevated,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Button(
                                        onClick = {
                                            onSendUssd(replyText.trim(), true)
                                            replyText = ""
                                        },
                                        enabled = replyText.isNotBlank() && !isLoading,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonCyan,
                                            contentColor = Color.Black,
                                        ),
                                    ) {
                                        Text("Reply")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
