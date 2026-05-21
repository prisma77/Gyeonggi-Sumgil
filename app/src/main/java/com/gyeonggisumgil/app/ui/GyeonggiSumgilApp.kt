package com.gyeonggisumgil.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gyeonggisumgil.app.BuildConfig
import com.gyeonggisumgil.app.data.RouteRepository
import com.gyeonggisumgil.app.data.SampleRouteRepository
import com.gyeonggisumgil.app.data.tmap.TmapPedestrianRouteApi
import com.gyeonggisumgil.app.data.tmap.TmapPedestrianRouteRepository
import com.gyeonggisumgil.app.domain.model.GeoPoint
import com.gyeonggisumgil.app.domain.model.RouteCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

@Composable
fun GyeonggiSumgilApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    val routeRepository = remember { SampleRouteRepository() }
    val routes = remember { routeRepository.getRecommendedRoutes(DEFAULT_START, DEFAULT_DESTINATION) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        AppHeader(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                AppTab.Home -> HomeScreen(
                    routes = routes,
                    onRouteClick = { selectedTab = AppTab.Route }
                )
                AppTab.Route -> RouteScreen(routeRepository = routeRepository)
                AppTab.Chat -> ChatScreen()
            }
        }
    }
}

@Composable
private fun AppHeader(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = "경기 숨길",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.Ink
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "경기도 공공데이터와 AI로 미세먼지를 피해 걷는 길",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.Muted
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTab.values().forEach { tab ->
                TabButton(
                    label = tab.label,
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    routes: List<RouteCandidate>,
    onRouteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AirQualitySummaryCard()
        MapPreviewCard(route = routes.first())

        SectionTitle("오늘의 추천")
        RouteCard(route = routes.first(), highlighted = true)

        Button(
            onClick = onRouteClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) {
            Text("경로 추천 시작")
        }
    }
}

@Composable
private fun RouteScreen(routeRepository: RouteRepository) {
    var start by remember { mutableStateOf("수원시청") }
    var destination by remember { mutableStateOf("광교호수공원") }
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    val sampleRoutes = remember(start, destination) {
        routeRepository.getRecommendedRoutes(start, destination)
    }
    var routes by remember(start, destination) { mutableStateOf(sampleRoutes) }
    var routeStatus by remember { mutableStateOf(routeApiStatusMessage()) }
    var isLoadingTmapRoute by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val tmapRouteRepository = remember {
        if (BuildConfig.TMAP_APP_KEY.isBlank()) {
            null
        } else {
            TmapPedestrianRouteRepository(TmapPedestrianRouteApi(BuildConfig.TMAP_APP_KEY))
        }
    }
    val selectedRoute = routes.firstOrNull { it.id == selectedRouteId } ?: routes.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle("경로 추천")
        MapPreviewCard(route = selectedRoute)

        CardSurface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("출발지") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("목적지") },
                    singleLine = true
                )
                Text(
                    text = routeStatus,
                    color = AppColors.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        val repository = tmapRouteRepository ?: return@Button
                        coroutineScope.launch {
                            isLoadingTmapRoute = true
                            routeStatus = "Tmap 보행자 경로를 불러오는 중입니다."
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    repository.getRecommendedRoutes(start, destination)
                                }
                            }.onSuccess { tmapRoutes ->
                                if (tmapRoutes.isEmpty()) {
                                    routeStatus = "등록된 장소 좌표가 없어 샘플 경로를 유지합니다. 현재는 수원시청과 광교호수공원을 지원합니다."
                                } else {
                                    routes = tmapRoutes
                                    selectedRouteId = tmapRoutes.first().id
                                    routeStatus = "Tmap 보행자 경로를 지도에 반영했습니다."
                                }
                            }.onFailure { throwable ->
                                routeStatus = "Tmap 경로 호출 실패: ${throwable.message ?: "오류 내용을 확인할 수 없습니다."}"
                            }
                            isLoadingTmapRoute = false
                        }
                    },
                    enabled = tmapRouteRepository != null && !isLoadingTmapRoute,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    Text(if (isLoadingTmapRoute) "불러오는 중" else "Tmap 도보 경로 불러오기")
                }
            }
        }

        routes.forEach { route ->
            RouteCard(
                route = route,
                highlighted = route.id == selectedRoute.id,
                onClick = { selectedRouteId = route.id }
            )
        }
    }
}

@Composable
private fun ChatScreen() {
    var message by remember { mutableStateOf("오늘 저녁에 산책해도 괜찮아?") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle("AI 대기질 상담")
        ChatBubble(
            title = "사용자",
            body = "수원시청에서 광교호수공원까지 걸어가도 괜찮을까?"
        )
        ChatBubble(
            title = "경기 숨길 AI",
            body = "현재 예시 데이터 기준 PM2.5는 보통 수준입니다. 민감군이 아니라면 짧은 산책은 가능하지만, 차량 통행량이 많은 대로변보다 하천·공원 인접 경로를 추천합니다."
        )
        CardSurface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("질문 입력") }
                )
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    Text("AI에게 물어보기")
                }
            }
        }
    }
}

@Composable
private fun AirQualitySummaryCard() {
    CardSurface {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("현재 위치 대기질", color = AppColors.Muted)
                    Text("수원시 팔달구", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                StatusBadge("보통")
            }

            Divider(color = AppColors.Border)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricBlock(
                    label = "PM10",
                    value = "42",
                    unit = "ug/m3",
                    modifier = Modifier.weight(1f)
                )
                MetricBlock(
                    label = "PM2.5",
                    value = "21",
                    unit = "ug/m3",
                    modifier = Modifier.weight(1f)
                )
                MetricBlock(
                    label = "추천",
                    value = "86",
                    unit = "점",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "경기도 대기질 API 연결 전까지 예시 수치로 UI와 추천 흐름을 먼저 검증합니다.",
                color = AppColors.Muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MapPreviewCard(route: RouteCandidate) {
    CardSurface(contentPadding = 0.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(22.dp))
        ) {
            if (BuildConfig.TMAP_APP_KEY.isNotBlank()) {
                TmapStaticMapPreview(
                    route = route,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                FallbackMapPreview(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun TmapStaticMapPreview(
    route: RouteCandidate,
    modifier: Modifier = Modifier
) {
    val staticMapClient = remember { OkHttpClient() }
    val mapCenter = remember(route.coordinates) { route.mapCenter() }
    val mapImageResult by produceState<Result<androidx.compose.ui.graphics.ImageBitmap>?>(
        initialValue = null,
        route.id,
        route.coordinates
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                loadTmapStaticMap(
                    client = staticMapClient,
                    appKey = BuildConfig.TMAP_APP_KEY,
                    center = mapCenter
                ).asImageBitmap()
            }
        }
    }

    Box(modifier = modifier.background(Color(0xFF1B1F24))) {
        mapImageResult?.onSuccess { image ->
            Image(
                bitmap = image,
                contentDescription = "Tmap 지도 미리보기",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            TmapRouteOverlay(
                route = route,
                center = mapCenter,
                modifier = Modifier.fillMaxSize()
            )
        }

        val errorMessage = mapImageResult?.exceptionOrNull()?.message
        if (mapImageResult == null || errorMessage != null) {
            MapStatusCard(
                message = errorMessage?.let { "Tmap StaticMap 호출 실패: $it" }
                    ?: "Tmap 지도를 불러오는 중입니다.",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun TmapRouteOverlay(
    route: RouteCandidate,
    center: GeoPoint,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val routePoints = route.coordinates
        if (routePoints.size < 2) return@Canvas

        val projectedPoints = routePoints.map { point ->
            point.toStaticMapOffset(
                center = center,
                width = size.width,
                height = size.height
            )
        }

        for (index in 0 until projectedPoints.lastIndex) {
            drawLine(
                color = Color.White,
                start = projectedPoints[index],
                end = projectedPoints[index + 1],
                strokeWidth = 16f,
                cap = StrokeCap.Round
            )
        }
        for (index in 0 until projectedPoints.lastIndex) {
            drawLine(
                color = Color(route.routeColorArgb),
                start = projectedPoints[index],
                end = projectedPoints[index + 1],
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )
        }

        drawCircle(
            color = Color.White,
            radius = 18f,
            center = projectedPoints.first()
        )
        drawCircle(
            color = AppColors.Primary,
            radius = 12f,
            center = projectedPoints.first()
        )
        drawCircle(
            color = Color.White,
            radius = 18f,
            center = projectedPoints.last(),
            style = Stroke(width = 6f)
        )
        drawCircle(
            color = Color(route.routeColorArgb),
            radius = 11f,
            center = projectedPoints.last()
        )
    }
}

@Composable
private fun FallbackMapPreview(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(Color(0xFFEAF7F0), Color(0xFFDCECF7))
            )
        )
    ) {
        RouteLine(modifier = Modifier.align(Alignment.Center))
        MapPin(label = "출발", modifier = Modifier.align(Alignment.CenterStart).padding(start = 36.dp))
        MapPin(label = "도착", modifier = Modifier.align(Alignment.TopEnd).padding(top = 42.dp, end = 42.dp))
        Text(
            text = "지도 미리보기",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
            color = AppColors.Ink,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RouteLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.72f)
            .height(18.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(AppColors.Primary.copy(alpha = 0.72f))
    )
}

@Composable
private fun MapPin(label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(AppColors.Primary)
                .border(3.dp, Color.White, CircleShape)
        )
        Text(label, color = AppColors.Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MapStatusCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 18.dp)
            .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("지도 상태", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(message, color = AppColors.Muted)
    }
}

@Composable
private fun RouteCard(
    route: RouteCandidate,
    highlighted: Boolean,
    onClick: (() -> Unit)? = null
) {
    CardSurface(
        borderColor = if (highlighted) AppColors.Primary else AppColors.Border,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(route.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(route.highlightLabel)
                    StatusBadge("${route.airScore}점")
                }
            }
            Text("${formatDistance(route.distanceMeters)} · 약 ${route.durationMinutes}분", color = AppColors.Muted)
            Text(route.exposureSummary, color = AppColors.Ink)
        }
    }
}

@Composable
private fun ChatBubble(
    title: String,
    body: String
) {
    CardSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = AppColors.Primary, fontWeight = FontWeight.Bold)
            Text(body, color = AppColors.Ink)
        }
    }
}

@Composable
private fun MetricBlock(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(AppColors.Background, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = AppColors.Muted)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.Ink)
        Text(unit, color = AppColors.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = AppColors.Ink
    )
}

@Composable
private fun StatusBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFEAF7F0))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, color = AppColors.Primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) AppColors.Primary else Color.White
    val foreground = if (selected) Color.White else AppColors.Ink
    val borderColor = if (selected) AppColors.Primary else AppColors.Border

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(label, color = foreground, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CardSurface(
    modifier: Modifier = Modifier,
    borderColor: Color = AppColors.Border,
    contentPadding: androidx.compose.ui.unit.Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding)
    ) {
        content()
    }
}

private enum class AppTab(val label: String) {
    Home("홈"),
    Route("경로"),
    Chat("AI 상담")
}

private object AppColors {
    val Primary = Color(0xFF2E7D5B)
    val Ink = Color(0xFF16231D)
    val Muted = Color(0xFF53655C)
    val Border = Color(0xFFDCE8E1)
    val Background = Color(0xFFF3FAF6)
}

private const val TAG = "GyeonggiSumgil"
private const val DEFAULT_START = "수원시청"
private const val DEFAULT_DESTINATION = "광교호수공원"
private const val STATIC_MAP_IMAGE_SIZE = 512
private const val STATIC_MAP_ZOOM = 14

private fun formatDistance(distanceMeters: Int): String {
    return String.format(Locale.KOREA, "%.1f km", distanceMeters / 1000.0)
}

private fun routeApiStatusMessage(): String {
    return if (BuildConfig.TMAP_APP_KEY.isBlank()) {
        "현재는 샘플 경로를 사용합니다. Tmap appKey가 설정되면 보행자 경로 API 연동을 진행합니다."
    } else {
        "Tmap appKey가 설정되었습니다. Tmap 지도와 보행자 경로 API로 경로를 표시합니다."
    }
}

private fun loadTmapStaticMap(
    client: OkHttpClient,
    appKey: String,
    center: GeoPoint
): android.graphics.Bitmap {
    val url = "https://apis.openapi.sk.com/tmap/staticMap".toHttpUrl().newBuilder()
        .addQueryParameter("version", "1")
        .addQueryParameter("appKey", appKey)
        .addQueryParameter("coordType", "WGS84GEO")
        .addQueryParameter("width", STATIC_MAP_IMAGE_SIZE.toString())
        .addQueryParameter("height", STATIC_MAP_IMAGE_SIZE.toString())
        .addQueryParameter("zoom", STATIC_MAP_ZOOM.toString())
        .addQueryParameter("format", "PNG")
        .addQueryParameter("longitude", center.longitude.toString())
        .addQueryParameter("latitude", center.latitude.toString())
        .build()

    val request = Request.Builder()
        .url(url)
        .header("Accept", "image/png")
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            error("HTTP ${response.code}")
        }

        val bytes = response.body?.bytes() ?: error("빈 이미지 응답")
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("이미지 디코딩 실패")
    }
}

private fun RouteCandidate.mapCenter(): GeoPoint {
    if (coordinates.isEmpty()) {
        return GeoPoint(latitude = 37.2636, longitude = 127.0286)
    }

    return GeoPoint(
        latitude = coordinates.map { it.latitude }.average(),
        longitude = coordinates.map { it.longitude }.average()
    )
}

private fun GeoPoint.toStaticMapOffset(
    center: GeoPoint,
    width: Float,
    height: Float
): Offset {
    val centerPixel = center.toMercatorPixel()
    val pointPixel = toMercatorPixel()

    return Offset(
        x = width / 2f + ((pointPixel.x - centerPixel.x) * width / STATIC_MAP_IMAGE_SIZE).toFloat(),
        y = height / 2f + ((pointPixel.y - centerPixel.y) * height / STATIC_MAP_IMAGE_SIZE).toFloat()
    )
}

private fun GeoPoint.toMercatorPixel(): MercatorPixel {
    val worldSize = 256.0 * 2.0.pow(STATIC_MAP_ZOOM)
    val latitudeRadians = latitude * PI / 180.0
    val sinLatitude = sin(latitudeRadians)

    return MercatorPixel(
        x = (longitude + 180.0) / 360.0 * worldSize,
        y = (0.5 - ln((1.0 + sinLatitude) / (1.0 - sinLatitude)) / (4.0 * PI)) * worldSize
    )
}

private data class MercatorPixel(
    val x: Double,
    val y: Double
)
