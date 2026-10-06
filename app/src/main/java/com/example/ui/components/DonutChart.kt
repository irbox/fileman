package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DiskCategoryUsage

@Composable
fun DonutChart(
    categories: List<DiskCategoryUsage>,
    totalUsedFormatted: String,
    usedPercentage: Int,
    modifier: Modifier = Modifier,
    chartSize: Dp = 220.dp,
    strokeWidth: Dp = 28.dp,
    isLowPowerMode: Boolean = false,
    onCategoryClick: ((DiskCategoryUsage) -> Unit)? = null
) {
    var selectedCategory by remember { mutableStateOf<DiskCategoryUsage?>(null) }
    val animationProgress = remember { Animatable(if (isLowPowerMode) 1f else 0f) }

    LaunchedEffect(categories, isLowPowerMode) {
        if (isLowPowerMode) {
            animationProgress.snapTo(1f)
        } else {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("storage_donut_chart_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Chart Canvas with Center Text Overlay
        Box(
            modifier = Modifier.size(chartSize),
            contentAlignment = Alignment.Center
        ) {
            val emptyColor = MaterialTheme.colorScheme.surfaceVariant

            Canvas(modifier = Modifier.fillMaxSize().testTag("donut_chart_canvas")) {
                val strokePx = strokeWidth.toPx()
                val diameter = size.minDimension - strokePx
                val topLeft = Offset(strokePx / 2f, strokePx / 2f)
                val arcSize = Size(diameter, diameter)

                // Background track
                drawArc(
                    color = emptyColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx)
                )

                // Colored slices
                var currentAngle = -90f // Start from 12 o'clock
                val progress = animationProgress.value

                for (cat in categories) {
                    val sweepAngle = cat.percentageOfUsed * 360f * progress
                    if (sweepAngle > 0.5f) {
                        val isSelected = selectedCategory?.categoryName == cat.categoryName
                        val actualStroke = if (isSelected) strokePx * 1.15f else strokePx

                        drawArc(
                            color = cat.composeColor,
                            startAngle = currentAngle,
                            sweepAngle = sweepAngle - 1.5f, // slight gap between slices
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = actualStroke, cap = StrokeCap.Round)
                        )
                    }
                    currentAngle += cat.percentageOfUsed * 360f
                }
            }

            // Center Text inside the donut hole
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                if (selectedCategory != null) {
                    Text(
                        text = selectedCategory!!.categoryName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = selectedCategory!!.composeColor
                    )
                    Text(
                        text = selectedCategory!!.formattedSize,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(selectedCategory!!.percentageOfUsed * 100).toInt()}% of used",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Total Used",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalUsedFormatted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$usedPercentage% Capacity",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Donut Chart Legend
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory?.categoryName == cat.categoryName
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) cat.composeColor.copy(alpha = 0.15f) else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            selectedCategory = if (isSelected) null else cat
                            onCategoryClick?.invoke(cat)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(cat.composeColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = cat.categoryName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${cat.fileCount} files)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = cat.formattedSize,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${(cat.percentageOfUsed * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = cat.composeColor
                            )
                        }
                    }
                }
            }
        }
    }
}
