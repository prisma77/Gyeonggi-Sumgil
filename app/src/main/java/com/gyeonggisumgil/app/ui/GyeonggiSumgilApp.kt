package com.gyeonggisumgil.app.ui

import android.util.Log
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gyeonggisumgil.app.BuildConfig
import com.gyeonggisumgil.app.domain.model.RouteCandidate
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PathOverlay

@Composable
fun GyeonggiSumgilApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }

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
                AppTab.Home -> HomeScreen(onRouteClick = { selectedTab = AppTab.Route })
                AppTab.Route -> RouteScreen()
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
private fun HomeScreen(onRouteClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AirQualitySummaryCard()
        MapPreviewCard()

        SectionTitle("오늘의 추천")
        RouteCard(route = sampleRoutes.first(), highlighted = true)

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
private fun RouteScreen() {
    var start by remember { mutableStateOf("수원시청") }
    var destination by remember { mutableStateOf("광교호수공원") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle("경로 추천")
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
                    text = "카카오맵 승인 전까지는 더미 경로로 노출 점수 UI를 먼저 검증합니다.",
                    color = AppColors.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        sampleRoutes.forEachIndexed { index, route ->
            RouteCard(route = route, highlighted = index == 0)
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
private fun MapPreviewCard() {
    CardSurface(contentPadding = 0.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(22.dp))
        ) {
            if (BuildConfig.NAVER_CLIENT_ID.isNotBlank()) {
                NaverMapPreview(modifier = Modifier.fillMaxSize())
            } else {
                FallbackMapPreview(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun NaverMapPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var hasCreated by remember { mutableStateOf(false) }
    var mapStatus by remember { mutableStateOf("네이버 지도를 불러오는 중입니다.") }
    var isMapReady by remember { mutableStateOf(false) }

    DisposableEffect(mapView, lifecycleOwner) {
        if (!hasCreated) {
            hasCreated = true
            mapView.onCreate(null)
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    Box(modifier = modifier.background(Color(0xFF1B1F24))) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.getMapAsync { naverMap ->
                    isMapReady = true
                    mapStatus = ""

                    val routePoints = sampleRoutePoints
                    val bounds = routePoints.drop(1).fold(
                        LatLngBounds.Builder().include(routePoints.first())
                    ) { builder, point -> builder.include(point) }.build()
                    naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 72))
                    drawSampleRoute(naverMap, routePoints)
                    Log.d(TAG, "Naver map ready: $naverMap")
                }
                mapView
            }
        )

        if (!isMapReady && mapStatus.isNotBlank()) {
            MapStatusCard(
                message = mapStatus,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun drawSampleRoute(
    naverMap: com.naver.maps.map.NaverMap,
    routePoints: List<LatLng>
) {
    PathOverlay().apply {
        coords = routePoints
        color = 0xFF2E7D5B.toInt()
        outlineColor = 0xFFFFFFFF.toInt()
        width = 14
        map = naverMap
    }

    Marker().apply {
        position = routePoints.first()
        captionText = "출발"
        icon = OverlayImage.fromResource(com.naver.maps.map.R.drawable.navermap_default_marker_icon_green)
        map = naverMap
    }

    Marker().apply {
        position = routePoints.last()
        captionText = "도착"
        icon = OverlayImage.fromResource(com.naver.maps.map.R.drawable.navermap_default_marker_icon_blue)
        map = naverMap
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
    highlighted: Boolean
) {
    CardSurface(
        borderColor = if (highlighted) AppColors.Primary else AppColors.Border
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(route.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusBadge("${route.airScore}점")
            }
            Text("${route.distanceMeters / 1000.0} km · 약 ${route.durationMinutes}분", color = AppColors.Muted)
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
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
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

private val sampleRoutes = listOf(
    RouteCandidate(
        id = "clean",
        title = "깨끗한 경로",
        distanceMeters = 1800,
        durationMinutes = 24,
        airScore = 86,
        exposureSummary = "대로변을 줄이고 공원·하천 인접 구간을 우선 통과하는 추천 경로입니다."
    ),
    RouteCandidate(
        id = "balanced",
        title = "균형 경로",
        distanceMeters = 1600,
        durationMinutes = 21,
        airScore = 78,
        exposureSummary = "소요시간과 대기질 노출을 함께 고려한 일상 이동용 경로입니다."
    ),
    RouteCandidate(
        id = "fast",
        title = "빠른 경로",
        distanceMeters = 1400,
        durationMinutes = 18,
        airScore = 62,
        exposureSummary = "가장 빠르지만 차량 통행량이 많은 구간이 포함될 수 있습니다."
    )
)

private val sampleRoutePoints = listOf(
    LatLng(37.2636, 127.0286),
    LatLng(37.2665, 127.0301),
    LatLng(37.2704, 127.0332),
    LatLng(37.2752, 127.0398),
    LatLng(37.2794, 127.0471)
)

private const val TAG = "GyeonggiSumgil"
