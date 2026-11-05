package com.chronos.hsc.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chronos.hsc.data.local.DailyTaskSlice
import com.chronos.hsc.data.local.StudyTopicChain
import com.chronos.hsc.ui.theme.FoliageDensity
import com.chronos.hsc.ui.theme.LocalNatureCritterType
import com.chronos.hsc.ui.theme.NatureAutumnOrange
import com.chronos.hsc.ui.theme.NatureCritterType
import com.chronos.hsc.ui.theme.NatureDewdropCyan
import com.chronos.hsc.ui.theme.NatureFireflyGold
import com.chronos.hsc.ui.theme.NatureFlowerPink
import com.chronos.hsc.ui.theme.NatureLadybugRed
import com.chronos.hsc.ui.theme.NatureLeafGreen
import com.chronos.hsc.ui.theme.NaturePokaBody
import com.chronos.hsc.ui.theme.NaturePokaFace
import com.chronos.hsc.ui.theme.NatureSproutLime
import com.chronos.hsc.ui.theme.NatureVineBrown
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private data class FireflyParticle(
    val baseX: Float,
    val baseY: Float,
    val speedX: Float,
    val speedY: Float,
    val phase: Float,
    val radius: Float
)

private data class SporeParticle(
    val baseX: Float,
    val baseY: Float,
    val speedY: Float,
    val swayPhase: Float,
    val radius: Float
)

@Composable
fun NatureAmbientOverlayCanvas(
    modifier: Modifier = Modifier,
    density: FoliageDensity = FoliageDensity.LUSH
) {
    val infiniteTransition = rememberInfiniteTransition(label = "natureAmbientCycle")

    // Slow atmospheric sine wave cycles
    val timeTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timeTick"
    )

    val fernSway by infiniteTransition.animateFloat(
        initialValue = -0.07f,
        targetValue = 0.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fernSway"
    )

    val fireflyCount = when (density) {
        FoliageDensity.LOW -> 8
        FoliageDensity.MEDIUM -> 18
        FoliageDensity.LUSH -> 30
    }

    val fireflies = remember(density) {
        List(fireflyCount) { idx ->
            FireflyParticle(
                baseX = (idx * 0.19f + 0.04f) % 1f,
                baseY = (idx * 0.23f + 0.08f) % 1f,
                speedX = 0.7f + (idx % 3) * 0.35f,
                speedY = 0.9f + (idx % 4) * 0.25f,
                phase = idx * 1.35f,
                radius = 3.2f + (idx % 3) * 1.4f
            )
        }
    }

    val spores = remember(density) {
        if (density == FoliageDensity.LUSH) {
            List(12) { idx ->
                SporeParticle(
                    baseX = (idx * 0.13f + 0.07f) % 1f,
                    baseY = (idx * 0.17f) % 1f,
                    speedY = 0.25f + (idx % 3) * 0.15f,
                    swayPhase = idx * 1.8f,
                    radius = 1.6f + (idx % 2) * 0.8f
                )
            }
        } else {
            emptyList()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

        // 1. Draw glowing fireflies drifting along organic paths
        fireflies.forEach { ff ->
            val curX = (ff.baseX * canvasWidth + sin(timeTick * ff.speedX + ff.phase) * 65f).mod(canvasWidth)
            val curY = (ff.baseY * canvasHeight + cos(timeTick * ff.speedY + ff.phase) * 75f).mod(canvasHeight)
            val glowAlpha = (0.40f + 0.50f * sin(timeTick * 2.5f + ff.phase).coerceIn(0f, 1f)).coerceIn(0.15f, 0.95f)

            // Outer soft glow halo
            drawCircle(
                color = NatureFireflyGold.copy(alpha = glowAlpha * 0.38f),
                radius = ff.radius * 4.8f,
                center = Offset(curX, curY)
            )
            // Mid glow halo
            drawCircle(
                color = NatureSproutLime.copy(alpha = glowAlpha * 0.65f),
                radius = ff.radius * 2.4f,
                center = Offset(curX, curY)
            )
            // Bright luminous core
            drawCircle(
                color = Color.White.copy(alpha = glowAlpha),
                radius = ff.radius,
                center = Offset(curX, curY)
            )
        }

        // 2. Drifting glowing pollen spores (for Lush mode)
        spores.forEach { sp ->
            val sporeY = ((sp.baseY + timeTick * 0.05f * sp.speedY) % 1f) * canvasHeight
            val sporeX = (sp.baseX * canvasWidth + sin(timeTick * 1.5f + sp.swayPhase) * 25f).mod(canvasWidth)
            drawCircle(
                color = NatureSproutLime.copy(alpha = 0.35f),
                radius = sp.radius,
                center = Offset(sporeX, sporeY)
            )
        }

        // 3. Swaying fern silhouettes based on density
        val fernAlpha = when (density) {
            FoliageDensity.LOW -> 0.20f
            FoliageDensity.MEDIUM -> 0.35f
            FoliageDensity.LUSH -> 0.48f
        }

        // Top-left fern
        drawCornerFern(
            origin = Offset(0f, 0f),
            scale = 1.15f,
            sway = fernSway,
            isLeft = true,
            alpha = fernAlpha
        )
        // Top-right fern
        drawCornerFern(
            origin = Offset(canvasWidth, 0f),
            scale = 1.15f,
            sway = -fernSway,
            isLeft = false,
            alpha = fernAlpha
        )

        // For Lush density, also render subtle bottom corner canopy fronds
        if (density == FoliageDensity.LUSH) {
            drawCornerFern(
                origin = Offset(0f, canvasHeight),
                scale = 0.9f,
                sway = -fernSway * 0.8f,
                isLeft = true,
                alpha = fernAlpha * 0.75f,
                isBottom = true
            )
            drawCornerFern(
                origin = Offset(canvasWidth, canvasHeight),
                scale = 0.9f,
                sway = fernSway * 0.8f,
                isLeft = false,
                alpha = fernAlpha * 0.75f,
                isBottom = true
            )
        }
    }
}

private fun DrawScope.drawCornerFern(
    origin: Offset,
    scale: Float,
    sway: Float,
    isLeft: Boolean,
    alpha: Float,
    isBottom: Boolean = false
) {
    val dir = if (isLeft) 1f else -1f
    val verticalDir = if (isBottom) -1f else 1f
    val fernColor = NatureLeafGreen.copy(alpha = alpha)
    val fernPath = Path()

    val stemLength = 175f * scale
    val controlX = origin.x + dir * (75f + sway * 130f)
    val controlY = origin.y + verticalDir * 95f
    val endX = origin.x + dir * (140f + sway * 190f)
    val endY = origin.y + verticalDir * stemLength

    fernPath.moveTo(origin.x, origin.y)
    fernPath.quadraticBezierTo(controlX, controlY, endX, endY)

    drawPath(
        path = fernPath,
        color = fernColor,
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )

    // Frond leaflets
    for (i in 1..7) {
        val t = i / 8f
        val frondX = origin.x + dir * (t * 115f + sway * 85f * t)
        val frondY = origin.y + verticalDir * t * stemLength
        val leafletLen = (1f - t) * 38f * scale

        drawLine(
            color = fernColor,
            start = Offset(frondX, frondY),
            end = Offset(frondX + dir * leafletLen, frondY - verticalDir * 10f),
            strokeWidth = 2.4f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun NatureWormBossBar(
    chain: StudyTopicChain,
    coreSlices: List<DailyTaskSlice>,
    currentBossHp: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    barHeight: Dp = if (compact) 14.dp else 20.dp,
    critter: NatureCritterType = LocalNatureCritterType.current
) {
    val totalSegments = max(1, chain.totalDays)
    val sortedCore = remember(coreSlices) { coreSlices.sortedBy { it.sliceNumber } }

    val infiniteTransition = rememberInfiniteTransition(label = "natureCompanionAnimation")

    val crawlPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "crawlPhase"
    )

    val firstUncompletedIndex = sortedCore.indexOfFirst { !it.isCompleted }
    val targetSegmentFraction = if (firstUncompletedIndex == -1) {
        1.0f // All done!
    } else {
        (firstUncompletedIndex.toFloat() / totalSegments.toFloat()).coerceIn(0f, 1f)
    }

    val animatedProgress = remember { Animatable(targetSegmentFraction) }
    LaunchedEffect(targetSegmentFraction) {
        animatedProgress.animateTo(
            targetValue = targetSegmentFraction,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight + 14.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val stemY = height * 0.65f
            val startX = 14f
            val endX = width - 14f
            val usableWidth = endX - startX

            if (usableWidth <= 0f) return@Canvas

            // 1. Draw Vine / Botanical Stem Track
            val stemPath = Path().apply {
                moveTo(startX, stemY)
                val segments = 8
                for (s in 1..segments) {
                    val segEndX = startX + (usableWidth / segments) * s
                    val waveOffset = if (s % 2 == 0) 2.5f else -2.5f
                    quadraticBezierTo(
                        segEndX - (usableWidth / segments) / 2f,
                        stemY + waveOffset,
                        segEndX,
                        stemY
                    )
                }
            }

            // Outer stem bark
            drawPath(
                path = stemPath,
                color = NatureVineBrown,
                style = Stroke(width = if (compact) 4.5f else 6f, cap = StrokeCap.Round)
            )

            // Vine sap highlight
            drawPath(
                path = stemPath,
                color = NatureSproutLime.copy(alpha = 0.45f),
                style = Stroke(width = if (compact) 1.5f else 2f, cap = StrokeCap.Round)
            )

            // 2. Draw Slice Nodes (Leaves / Flowers / Dewdrops)
            val segmentWidth = usableWidth / totalSegments
            for (i in 0 until totalSegments) {
                val nodeCenterX = startX + segmentWidth * i + segmentWidth * 0.5f
                val slice = sortedCore.getOrNull(i)

                when {
                    slice != null && slice.isCompleted -> {
                        // Blooming Pink Flower on completed slice!
                        drawBloomingFlower(
                            center = Offset(nodeCenterX, stemY),
                            radius = if (compact) 6.5f else 8.5f
                        )
                    }
                    slice != null && slice.scheduledDateEpochDay < LocalDateEpoch() -> {
                        // Autumn withered leaf for overdue slice
                        drawLeafNode(
                            center = Offset(nodeCenterX, stemY),
                            radius = if (compact) 5.5f else 7f,
                            color = NatureAutumnOrange
                        )
                    }
                    else -> {
                        // Fresh green leaf node with dewdrop
                        drawLeafNode(
                            center = Offset(nodeCenterX, stemY),
                            radius = if (compact) 5.5f else 7f,
                            color = NatureLeafGreen
                        )
                        // Tiny sparkling dewdrop
                        drawCircle(
                            color = NatureDewdropCyan,
                            radius = 2.2f,
                            center = Offset(nodeCenterX, stemY - 5f)
                        )
                    }
                }
            }

            // 3. Draw Selected Critter Companion Leading the Milestone Progress
            val critterProgress = animatedProgress.value.coerceIn(0f, 1f)
            val critterHeadX = startX + usableWidth * critterProgress
            val isAllCompleted = firstUncompletedIndex == -1

            if (!isAllCompleted) {
                when (critter) {
                    NatureCritterType.POKA_INCHWORM -> {
                        drawCuteInchworm(
                            headX = critterHeadX,
                            groundY = stemY - 1f,
                            crawlPhase = crawlPhase,
                            compact = compact
                        )
                    }
                    NatureCritterType.FIREFLY -> {
                        drawCuteFirefly(
                            center = Offset(critterHeadX, stemY - 9f),
                            time = crawlPhase,
                            angleRad = 0f
                        )
                    }
                    NatureCritterType.LADYBUG -> {
                        drawLadybug(
                            center = Offset(critterHeadX, stemY - 5f),
                            angleRad = 0f
                        )
                    }
                    NatureCritterType.HONEYBEE -> {
                        drawCuteHoneybee(
                            center = Offset(critterHeadX, stemY - 9f),
                            time = crawlPhase,
                            angleRad = 0f
                        )
                    }
                }
            } else {
                // If all done: A cheerful butterfly flutter above the vine!
                drawButterfly(
                    center = Offset(endX - 10f, stemY - 12f),
                    time = crawlPhase
                )
            }
        }
    }
}

private fun LocalDateEpoch(): Long {
    return java.time.LocalDate.now().toEpochDay()
}

private fun DrawScope.drawLeafNode(center: Offset, radius: Float, color: Color) {
    val leafPath = Path().apply {
        moveTo(center.x - radius, center.y)
        quadraticBezierTo(center.x, center.y - radius * 1.5f, center.x + radius, center.y)
        quadraticBezierTo(center.x, center.y + radius * 0.6f, center.x - radius, center.y)
        close()
    }
    drawPath(path = leafPath, color = color)
    drawPath(
        path = leafPath,
        color = Color.White.copy(alpha = 0.3f),
        style = Stroke(width = 1f)
    )
}

private fun DrawScope.drawBloomingFlower(center: Offset, radius: Float) {
    val petalCount = 5
    val petalRadius = radius * 0.55f
    for (p in 0 until petalCount) {
        val angle = (p * (360f / petalCount)) * (PI.toFloat() / 180f)
        val petalCenterX = center.x + cos(angle) * petalRadius
        val petalCenterY = center.y + sin(angle) * petalRadius
        drawCircle(
            color = NatureFlowerPink,
            radius = petalRadius,
            center = Offset(petalCenterX, petalCenterY)
        )
    }
    // Golden center
    drawCircle(
        color = NatureFireflyGold,
        radius = radius * 0.4f,
        center = center
    )
}

private fun DrawScope.drawCuteInchworm(
    headX: Float,
    groundY: Float,
    crawlPhase: Float,
    compact: Boolean
) {
    val segmentCount = 5
    val baseSegRadius = if (compact) 3.8f else 4.8f
    val wormLength = baseSegRadius * 6f

    // Arching wave: segments arch upwards during contraction
    val archHeight = (sin(crawlPhase) * 6f).coerceAtLeast(0f)

    for (i in (segmentCount - 1) downTo 0) {
        val segProgress = i.toFloat() / (segmentCount - 1).toFloat()
        val segX = headX - (1f - segProgress) * wormLength
        // Center segments arch higher
        val archMultiplier = sin(segProgress * PI.toFloat())
        val segY = groundY - baseSegRadius - (archHeight * archMultiplier)

        // Alternate light & deep lime segments
        val segColor = if (i % 2 == 0) NaturePokaBody else NatureSproutLime

        drawCircle(
            color = segColor,
            radius = baseSegRadius,
            center = Offset(segX, segY)
        )
        drawCircle(
            color = NaturePokaFace.copy(alpha = 0.3f),
            radius = baseSegRadius,
            center = Offset(segX, segY),
            style = Stroke(width = 1f)
        )

        // Head features on front segment
        if (i == segmentCount - 1) {
            val headCenter = Offset(segX + 1f, segY)
            drawCircle(
                color = Color.Black,
                radius = 1.4f,
                center = Offset(headCenter.x + 1.5f, headCenter.y - 1f)
            )
            drawCircle(
                color = Color.White,
                radius = 0.6f,
                center = Offset(headCenter.x + 1.8f, headCenter.y - 1.4f)
            )
            // Tiny antenna
            drawLine(
                color = NaturePokaFace,
                start = Offset(headCenter.x + 1f, headCenter.y - baseSegRadius),
                end = Offset(headCenter.x + 3.5f, headCenter.y - baseSegRadius - 4f),
                strokeWidth = 1.2f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = NatureSproutLime,
                radius = 1f,
                center = Offset(headCenter.x + 3.5f, headCenter.y - baseSegRadius - 4f)
            )
        }
    }
}

private fun DrawScope.drawButterfly(center: Offset, time: Float) {
    val wingFlap = cos(time * 3f).coerceIn(-1f, 1f)
    val wingWidth = 7f * kotlin.math.abs(wingFlap).coerceAtLeast(0.2f)

    // Left wing
    drawOval(
        color = NatureDewdropCyan,
        topLeft = Offset(center.x - wingWidth - 1f, center.y - 6f),
        size = Size(wingWidth, 10f)
    )
    // Right wing
    drawOval(
        color = NatureDewdropCyan,
        topLeft = Offset(center.x + 1f, center.y - 6f),
        size = Size(wingWidth, 10f)
    )
    // Body
    drawCircle(color = Color.White, radius = 1.5f, center = center)
}

@Composable
fun NatureCritterBorder(
    critter: NatureCritterType = LocalNatureCritterType.current,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "critterBorderCycle")
    val walkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "walkProgress"
    )

    val timeTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timeTick"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val perimeter = 2f * (w + h)
        val distance = (walkProgress * perimeter) % perimeter

        val (pos, angle) = calculatePerimeterPosition(distance, w, h)

        when (critter) {
            NatureCritterType.POKA_INCHWORM -> {
                drawCuteBorderInchworm(center = pos, angleRad = angle, time = timeTick)
            }
            NatureCritterType.FIREFLY -> {
                drawCuteFirefly(center = pos, time = timeTick, angleRad = angle)
            }
            NatureCritterType.LADYBUG -> {
                drawLadybug(center = pos, angleRad = angle)
            }
            NatureCritterType.HONEYBEE -> {
                drawCuteHoneybee(center = pos, time = timeTick, angleRad = angle)
            }
        }
    }
}

// Backward-compatible alias
@Composable
fun NatureLadybugBorder(
    modifier: Modifier = Modifier
) {
    NatureCritterBorder(
        critter = LocalNatureCritterType.current,
        modifier = modifier
    )
}

private fun calculatePerimeterPosition(dist: Float, w: Float, h: Float): Pair<Offset, Float> {
    return when {
        dist < w -> Pair(Offset(dist, 0f), 0f) // Top edge, moving right
        dist < w + h -> Pair(Offset(w, dist - w), (PI / 2).toFloat()) // Right edge, moving down
        dist < 2f * w + h -> Pair(Offset(w - (dist - (w + h)), h), PI.toFloat()) // Bottom edge, moving left
        else -> Pair(Offset(0f, h - (dist - (2f * w + h))), (-PI / 2).toFloat()) // Left edge, moving up
    }
}

private fun DrawScope.drawLadybug(center: Offset, angleRad: Float) {
    val bodyRadius = 5.2f

    // Red Shell
    drawCircle(
        color = NatureLadybugRed,
        radius = bodyRadius,
        center = center
    )

    // Black Head
    val headOffset = Offset(
        center.x + cos(angleRad) * (bodyRadius + 1.2f),
        center.y + sin(angleRad) * (bodyRadius + 1.2f)
    )
    drawCircle(
        color = Color(0xFF1E1E1E),
        radius = 2.4f,
        center = headOffset
    )

    // Shell central wing divide line
    drawLine(
        color = Color(0xFF1E1E1E),
        start = Offset(center.x - cos(angleRad) * bodyRadius, center.y - sin(angleRad) * bodyRadius),
        end = Offset(center.x + cos(angleRad) * bodyRadius, center.y + sin(angleRad) * bodyRadius),
        strokeWidth = 1.2f
    )

    // 4 Crisp Black Spots
    val perpAngle = angleRad + (PI / 2).toFloat()
    val spotDist = bodyRadius * 0.45f
    val offsets = listOf(
        Offset(center.x + cos(perpAngle) * spotDist, center.y + sin(perpAngle) * spotDist),
        Offset(center.x - cos(perpAngle) * spotDist, center.y - sin(perpAngle) * spotDist),
        Offset(center.x + cos(perpAngle) * spotDist - cos(angleRad) * 2.2f, center.y + sin(perpAngle) * spotDist - sin(angleRad) * 2.2f),
        Offset(center.x - cos(perpAngle) * spotDist - cos(angleRad) * 2.2f, center.y - sin(perpAngle) * spotDist - sin(angleRad) * 2.2f)
    )
    offsets.forEach { pt ->
        drawCircle(color = Color.Black, radius = 0.95f, center = pt)
    }
}

private fun DrawScope.drawCuteFirefly(center: Offset, time: Float, angleRad: Float) {
    val bodyRadius = 4.8f
    val pulse = sin(time * 3.5f)
    val glowAlpha = (0.55f + 0.35f * pulse).coerceIn(0.2f, 0.95f)

    // 1. Radiant Outer Glow Halo
    drawCircle(
        color = NatureFireflyGold.copy(alpha = glowAlpha * 0.45f),
        radius = bodyRadius * 3.2f,
        center = center
    )
    // 2. Lime Sprout Mid Halo
    drawCircle(
        color = NatureSproutLime.copy(alpha = glowAlpha * 0.65f),
        radius = bodyRadius * 1.8f,
        center = center
    )
    // 3. Golden Core Body
    drawCircle(
        color = NatureFireflyGold,
        radius = bodyRadius,
        center = center
    )
    // 4. White Brilliant Star Center
    drawCircle(
        color = Color.White,
        radius = 2.2f,
        center = center
    )

    // 5. Fluttering Cyan/White Wings
    val wingFlap = cos(time * 14f)
    val wingLen = 6.5f
    val wingWidth = 3.5f * kotlin.math.abs(wingFlap).coerceAtLeast(0.25f)
    val perp = angleRad + (PI / 2).toFloat()

    drawOval(
        color = NatureDewdropCyan.copy(alpha = 0.75f),
        topLeft = Offset(center.x + cos(perp) * 2f - wingWidth / 2f, center.y + sin(perp) * 2f - wingLen / 2f),
        size = Size(wingWidth, wingLen)
    )
    drawOval(
        color = NatureDewdropCyan.copy(alpha = 0.75f),
        topLeft = Offset(center.x - cos(perp) * 2f - wingWidth / 2f, center.y - sin(perp) * 2f - wingLen / 2f),
        size = Size(wingWidth, wingLen)
    )

    // 6. Trailing Star Sparkles behind movement
    val trailDistances = listOf(7f, 14f, 21f)
    trailDistances.forEachIndexed { index, d ->
        val trailX = center.x - cos(angleRad) * d
        val trailY = center.y - sin(angleRad) * d
        val alpha = (0.6f - index * 0.18f).coerceIn(0.1f, 0.7f)
        drawCircle(
            color = NatureFireflyGold.copy(alpha = alpha),
            radius = (2.0f - index * 0.5f).coerceAtLeast(0.7f),
            center = Offset(trailX, trailY)
        )
    }
}

private fun DrawScope.drawCuteHoneybee(center: Offset, time: Float, angleRad: Float) {
    val bodyLength = 11.5f
    val bodyWidth = 7.5f

    // Rotated bee oval body
    // Yellow Base Body
    drawOval(
        color = Color(0xFFFACC15),
        topLeft = Offset(center.x - bodyLength / 2f, center.y - bodyWidth / 2f),
        size = Size(bodyLength, bodyWidth)
    )

    // 2 Dark Slate Stripes
    val stripeWidth = 2.4f
    drawRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(center.x - 2.5f, center.y - bodyWidth / 2f),
        size = Size(stripeWidth, bodyWidth)
    )
    drawRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(center.x + 1.5f, center.y - bodyWidth / 2f),
        size = Size(stripeWidth, bodyWidth)
    )

    // Black Head
    val headX = center.x + cos(angleRad) * (bodyLength / 2f + 1f)
    val headY = center.y + sin(angleRad) * (bodyLength / 2f + 1f)
    drawCircle(
        color = Color(0xFF0F172A),
        radius = 2.6f,
        center = Offset(headX, headY)
    )

    // Rapid fluttering wings
    val wingW = 4f
    val wingH = 7f * kotlin.math.abs(cos(time * 22f)).coerceAtLeast(0.3f)
    drawOval(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(center.x - wingW / 2f, center.y - bodyWidth / 2f - wingH),
        size = Size(wingW, wingH)
    )
}

private fun DrawScope.drawCuteBorderInchworm(center: Offset, angleRad: Float, time: Float) {
    val segmentCount = 4
    val segRadius = 3.6f
    val arch = (sin(time * 4.5f) * 4.5f).coerceAtLeast(0f)

    for (s in (segmentCount - 1) downTo 0) {
        val t = s.toFloat() / (segmentCount - 1).toFloat()
        val distBack = (1f - t) * segRadius * 4.5f
        val perp = angleRad + (PI / 2).toFloat()
        val archLift = sin(t * PI.toFloat()) * arch

        val segX = center.x - cos(angleRad) * distBack + cos(perp) * archLift
        val segY = center.y - sin(angleRad) * distBack + sin(perp) * archLift
        val color = if (s % 2 == 0) NaturePokaBody else NatureSproutLime

        drawCircle(
            color = color,
            radius = segRadius,
            center = Offset(segX, segY)
        )

        // Head eyes on front segment
        if (s == segmentCount - 1) {
            drawCircle(
                color = Color.Black,
                radius = 1.1f,
                center = Offset(segX + cos(angleRad) * 1.5f, segY + sin(angleRad) * 1.5f)
            )
        }
    }
}

@Composable
fun NatureVineSwipeOverlay(
    progress: Float,
    modifier: Modifier = Modifier
) {
    if (progress <= 0f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val vinePath = Path().apply {
            moveTo(0f, h * 0.8f)
            quadraticBezierTo(
                w * 0.4f * progress,
                h * (0.8f - 0.5f * progress),
                w * progress,
                h * 0.2f
            )
        }

        drawPath(
            path = vinePath,
            color = NatureLeafGreen.copy(alpha = (1f - progress * 0.5f).coerceIn(0f, 1f)),
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )

        // Blooming flower bursts along vine
        val flowerPoints = listOf(0.3f, 0.6f, 0.9f)
        flowerPoints.forEach { pt ->
            if (progress >= pt) {
                val burstRadius = ((progress - pt) * 45f).coerceIn(2f, 16f)
                val flowerAlpha = (1f - (progress - pt) * 1.5f).coerceIn(0f, 1f)
                drawCircle(
                    color = NatureFlowerPink.copy(alpha = flowerAlpha),
                    radius = burstRadius,
                    center = Offset(w * pt, h * (0.8f - pt * 0.6f))
                )
            }
        }
    }
}
