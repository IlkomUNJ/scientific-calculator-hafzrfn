package com.example.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Updated button layout with RAD/DEG button
val scientificButtonList = listOf(
    "sin", "cos", "tan", "(", ")",
    "sin⁻¹", "cos⁻¹", "tan⁻¹", "log", "ln",
    "√", "x^y", "x!", "1/x", "π",
    "7", "8", "9", "DEL", "AC",
    "4", "5", "6", "×", "÷",
    "1", "2", "3", "+", "-",
    "0", ".", "e", "RAD/DEG", "="
)

@Composable
fun Calculator(modifier: Modifier = Modifier, viewModel: CalculatorViewModel) {

    val equationText = viewModel.equationText.observeAsState()
    val resultText = viewModel.resultText.observeAsState()
    val angleMode = viewModel.angleModeText.observeAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Header with angle mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SCIENTIFIC CALCULATOR",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = angleMode.value ?: "RAD",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Equation display with better scrolling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = equationText.value ?: "",
                    style = TextStyle(
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        textAlign = TextAlign.End
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Result display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = resultText.value ?: "0",
                    style = TextStyle(
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Calculator buttons grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(scientificButtonList) { btn ->
                    ScientificCalculatorButton(btn = btn, onClick = {
                        viewModel.onButtonClick(btn)
                    })
                }
            }
        }
    }
}

@Composable
fun ScientificCalculatorButton(btn: String, onClick: () -> Unit) {
    val buttonColor = getScientificColor(btn)
    val textColor = if (btn in listOf("AC", "DEL", "=", "RAD/DEG")) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .height(60.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = buttonColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = btn,
                style = TextStyle(
                    fontSize = when {
                        btn == "RAD/DEG" -> 10.sp
                        btn.length <= 2 -> 16.sp
                        btn == "sin⁻¹" || btn == "cos⁻¹" || btn == "tan⁻¹" -> 10.sp
                        else -> 14.sp
                    },
                    color = textColor,
                    fontWeight = if (btn in listOf("=", "AC", "DEL", "RAD/DEG")) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
fun getScientificColor(btn: String): Color {
    return when {
        // Clear buttons - Error color
        btn == "AC" -> MaterialTheme.colorScheme.error
        btn == "DEL" -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)

        // Equals button and RAD/DEG - Primary color
        btn == "=" || btn == "RAD/DEG" -> MaterialTheme.colorScheme.primary

        // Basic operators - Secondary
        btn == "+" || btn == "-" || btn == "×" || btn == "÷" ->
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)

        // Scientific functions - Tertiary
        btn == "sin" || btn == "cos" || btn == "tan" ||
                btn == "sin⁻¹" || btn == "cos⁻¹" || btn == "tan⁻¹" ||
                btn == "log" || btn == "ln" || btn == "√" || btn == "x!" ||
                btn == "1/x" || btn == "x^y" || btn == "π" || btn == "e" ->
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)

        // Parentheses - Surface variant
        btn == "(" || btn == ")" ->
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)

        // Numbers and ANS - Surface
        else -> MaterialTheme.colorScheme.surface
    }
}