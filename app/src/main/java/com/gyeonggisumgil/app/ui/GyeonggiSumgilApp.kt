package com.gyeonggisumgil.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.gyeonggisumgil.app.BuildConfig
import com.gyeonggisumgil.app.data.RouteRepository
import com.gyeonggisumgil.app.data.SampleGyeonggiAirQualityData
import com.gyeonggisumgil.app.data.SampleRouteRepository
import com.gyeonggisumgil.app.data.tmap.TmapPedestrianRouteApi
import com.gyeonggisumgil.app.data.tmap.TmapPedestrianRouteRepository
import com.gyeonggisumgil.app.data.tmap.TmapPlace
import com.gyeonggisumgil.app.data.tmap.TmapPlaceResolver
import com.gyeonggisumgil.app.domain.model.AirQualityGrade
import com.gyeonggisumgil.app.domain.model.AirQualityReading
import com.gyeonggisumgil.app.domain.model.AirQualityStation
import com.gyeonggisumgil.app.domain.model.GeoPoint
import com.gyeonggisumgil.app.domain.model.RouteCandidate
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.PathOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun GyeonggiSumgilApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    val routeRepository = remember { SampleRouteRepository() }
    val routes = remember { routeRepository.getRecommendedRoutes(DEFAULT_START, DEFAULT_DESTINATION) }
    val context = LocalContext.current
    var currentAirQuality by remember { mutableStateOf(CurrentAirQualityState.empty()) }

    fun refreshCurrentAirQuality() {
        val currentLocation = context.findBestLastKnownLocation()
        if (currentLocation == null) {
            currentAirQuality = CurrentAirQualityState.empty("현재 위치를 확인할 수 없습니다.")
            return
        }

        val nearestStation = SampleGyeonggiAirQualityData.stations.nearestStationTo(currentLocation)
        val nearestReading = nearestStation?.let { station ->
            SampleGyeonggiAirQualityData.latestReadings.firstOrNull { reading ->
                reading.stationId == station.id
            }
        }

        currentAirQuality = if (nearestStation != null && nearestReading != null) {
            CurrentAirQualityState(
                station = nearestStation,
                reading = nearestReading,
                status = "현재 위치에서 가까운 ${nearestStation.city} ${nearestStation.name} 측정소 기준입니다."
            )
        } else {
            CurrentAirQualityState.empty("가까운 대기질 측정소를 찾지 못했습니다.")
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            refreshCurrentAirQuality()
        } else {
            currentAirQuality = CurrentAirQualityState.empty("위치 권한이 없어 현재 위치 대기질을 표시할 수 없습니다.")
        }
    }

    LaunchedEffect(Unit) {
        if (context.hasFineLocationPermission()) {
            refreshCurrentAirQuality()
        } else {
            currentAirQuality = CurrentAirQualityState.empty("위치 권한이 필요합니다.")
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

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
                    currentAirQuality = currentAirQuality,
                    onRefreshLocation = {
                        if (context.hasFineLocationPermission()) {
                            refreshCurrentAirQuality()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
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
    currentAirQuality: CurrentAirQualityState,
    onRefreshLocation: () -> Unit,
    onRouteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AirQualitySummaryCard(
            state = currentAirQuality,
            recommendationScore = routes.first().airScore,
            onRefreshLocation = onRefreshLocation
        )

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
    var start by remember { mutableStateOf("") }
    var waypoint by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    var routes by remember { mutableStateOf<List<RouteCandidate>>(emptyList()) }
    var routeStatus by remember { mutableStateOf("출발지와 목적지를 입력하거나 지도에서 선택하세요.") }
    var isLoadingTmapRoute by remember { mutableStateOf(false) }
    var routeRequestId by remember { mutableStateOf(0) }
    var routeJob by remember { mutableStateOf<Job?>(null) }
    var pickedStart by remember { mutableStateOf<GeoPoint?>(null) }
    var pickedWaypoint by remember { mutableStateOf<GeoPoint?>(null) }
    var pickedDestination by remember { mutableStateOf<GeoPoint?>(null) }
    var mapPickTarget by remember { mutableStateOf<MapPickTarget?>(null) }
    var mapCenter by remember { mutableStateOf(DEFAULT_MAP_CENTER.toGeoPoint()) }
    var mapCameraTarget by remember { mutableStateOf<GeoPoint?>(null) }
    var mapCameraRequestId by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val routeScrollState = rememberScrollState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val tmapRouteRepository = remember {
        if (BuildConfig.TMAP_APP_KEY.isBlank()) {
            null
        } else {
            TmapPedestrianRouteRepository(TmapPedestrianRouteApi(BuildConfig.TMAP_APP_KEY))
        }
    }
    val tmapPlaceResolver = remember {
        if (BuildConfig.TMAP_APP_KEY.isBlank()) {
            null
        } else {
            TmapPlaceResolver(BuildConfig.TMAP_APP_KEY)
        }
    }
    fun moveMapCamera(point: GeoPoint) {
        mapCameraTarget = point
        mapCameraRequestId += 1
    }
    fun centerOnCurrentLocation() {
        val currentLocation = context.findBestLastKnownLocation()
        if (currentLocation == null) {
            routeStatus = "현재 위치를 확인할 수 없습니다. 휴대폰 위치 설정을 확인하세요."
        } else {
            moveMapCamera(currentLocation)
            routeStatus = if (mapPickTarget == null) {
                "현재 위치로 지도를 이동했습니다."
            } else {
                "현재 위치로 이동했습니다. 지도 중심을 선택하세요."
            }
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            centerOnCurrentLocation()
        } else {
            routeStatus = "현재 위치 권한이 없어 지도를 이동할 수 없습니다."
        }
    }
    val selectedRoute = routes.firstOrNull { it.id == selectedRouteId } ?: routes.firstOrNull()

    fun clearRouteState(message: String) {
        routeJob?.cancel()
        routeRequestId += 1
        selectedRouteId = null
        routes = emptyList()
        isLoadingTmapRoute = false
        routeStatus = message
    }

    fun scrollToRouteCards() {
        coroutineScope.launch {
            delay(250)
            routeScrollState.animateScrollTo(routeScrollState.maxValue)
        }
    }

    fun loadTmapRoutes(requestedStart: String, requestedWaypoint: String?, requestedDestination: String) {
        val trimmedStart = requestedStart.trim()
        val trimmedWaypoint = requestedWaypoint?.trim().orEmpty()
        val trimmedDestination = requestedDestination.trim()
        routeJob?.cancel()
        routeRequestId += 1
        val activeRequestId = routeRequestId

        start = trimmedStart
        waypoint = trimmedWaypoint
        destination = trimmedDestination
        selectedRouteId = null
        routes = emptyList()

        if (trimmedStart.isBlank() || trimmedDestination.isBlank()) {
            isLoadingTmapRoute = false
            routeStatus = "출발지와 목적지를 모두 입력하세요."
            return
        }

        val repository = tmapRouteRepository
        if (repository == null) {
            routes = routeRepository.getRecommendedRoutes(trimmedStart, trimmedDestination)
            routeStatus = "Tmap appKey가 없어 샘플 경로를 표시합니다."
            return
        }

        routeJob = coroutineScope.launch {
            isLoadingTmapRoute = true
            routeStatus = "Tmap 보행자 경로를 불러오는 중입니다."
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.getRecommendedRoutes(
                        start = trimmedStart,
                        waypoint = trimmedWaypoint.takeIf { it.isNotBlank() },
                        destination = trimmedDestination
                    )
                }
            }.onSuccess { tmapRoutes ->
                if (activeRequestId != routeRequestId) return@onSuccess

                if (tmapRoutes.isEmpty()) {
                    routeStatus = "장소 좌표를 찾지 못했습니다. 장소명 또는 도로명 주소를 더 구체적으로 입력하세요."
                } else {
                    routes = tmapRoutes
                    selectedRouteId = tmapRoutes.first().id
                    val firstRoute = tmapRoutes.first()
                    routeStatus = "Tmap 도보 경로 반영: ${formatDistance(firstRoute.distanceMeters)} · 약 ${firstRoute.durationMinutes}분 · 좌표 ${firstRoute.coordinates.size}개"
                    scrollToRouteCards()
                }
            }.onFailure { throwable ->
                if (activeRequestId != routeRequestId) return@onFailure

                routeStatus = "Tmap 경로 호출 실패: ${throwable.message ?: "오류 내용을 확인할 수 없습니다."}"
            }
            if (activeRequestId == routeRequestId) {
                isLoadingTmapRoute = false
            }
        }
    }

    fun loadTmapRoutes(requestedStart: String, requestedDestination: String) {
        loadTmapRoutes(requestedStart, waypoint, requestedDestination)
    }

    fun loadTmapRoutesFromMap(startPoint: GeoPoint, waypointPoint: GeoPoint?, destinationPoint: GeoPoint) {
        routeJob?.cancel()
        routeRequestId += 1
        val activeRequestId = routeRequestId
        selectedRouteId = null
        routes = emptyList()

        val repository = tmapRouteRepository
        if (repository == null) {
            routeStatus = "Tmap appKey가 없어 지도 선택 경로를 계산할 수 없습니다."
            return
        }

        val startPlace = startPoint.toTmapPlace("지도 출발지")
        val waypointPlace = waypointPoint?.toTmapPlace("지도 경유지")
        val destinationPlace = destinationPoint.toTmapPlace("지도 목적지")
        routeJob = coroutineScope.launch {
            isLoadingTmapRoute = true
            routeStatus = if (waypointPoint == null) {
                "지도에서 선택한 두 지점의 도보 경로를 불러오는 중입니다."
            } else {
                "지도에서 선택한 경유 산책 경로를 불러오는 중입니다."
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.getRecommendedRoutes(startPlace, waypointPlace, destinationPlace)
                }
            }.onSuccess { tmapRoutes ->
                if (activeRequestId != routeRequestId) return@onSuccess

                if (tmapRoutes.isEmpty()) {
                    routeStatus = "선택한 지점 사이의 도보 경로를 찾지 못했습니다."
                } else {
                    routes = tmapRoutes
                    selectedRouteId = tmapRoutes.first().id
                    val firstRoute = tmapRoutes.first()
                    routeStatus = "지도 선택 경로 반영: ${formatDistance(firstRoute.distanceMeters)} · 약 ${firstRoute.durationMinutes}분 · 좌표 ${firstRoute.coordinates.size}개"
                    scrollToRouteCards()
                }
            }.onFailure { throwable ->
                if (activeRequestId != routeRequestId) return@onFailure

                routeStatus = "Tmap 경로 호출 실패: ${throwable.message ?: "오류 내용을 확인할 수 없습니다."}"
            }
            if (activeRequestId == routeRequestId) {
                isLoadingTmapRoute = false
            }
        }
    }

    fun clearRouteForManualInput() {
        pickedStart = null
        pickedWaypoint = null
        pickedDestination = null
        mapPickTarget = null
        clearRouteState("출발지와 목적지를 입력한 뒤 경로 찾기를 누르세요.")
    }

    fun resolvePickedAddress(target: MapPickTarget, point: GeoPoint) {
        val resolver = tmapPlaceResolver ?: return

        coroutineScope.launch {
            val address = runCatching {
                withContext(Dispatchers.IO) {
                    resolver.reverseGeocode(point)
                }
            }.getOrNull() ?: "지도에서 선택한 위치"

            when (target) {
                MapPickTarget.Start -> {
                    if (pickedStart == point) start = address
                }
                MapPickTarget.Waypoint -> {
                    if (pickedWaypoint == point) waypoint = address
                }
                MapPickTarget.Destination -> {
                    if (pickedDestination == point) destination = address
                }
            }
        }
    }

    fun selectMapCenter() {
        val point = mapCenter
        when (mapPickTarget) {
            MapPickTarget.Start -> {
                pickedStart = point
                start = "주소 확인 중..."
                selectedRouteId = null
                routes = emptyList()
                routeStatus = "경유지 또는 도착지를 지도 중심에 맞춘 뒤 선택하세요."
                resolvePickedAddress(MapPickTarget.Start, point)
            }
            MapPickTarget.Waypoint -> {
                pickedWaypoint = point
                waypoint = "주소 확인 중..."
                selectedRouteId = null
                routes = emptyList()
                mapPickTarget = MapPickTarget.Destination
                routeStatus = "도착지를 지도 중심에 맞춘 뒤 선택하세요."
                resolvePickedAddress(MapPickTarget.Waypoint, point)
            }
            MapPickTarget.Destination -> {
                pickedDestination = point
                destination = "주소 확인 중..."
                selectedRouteId = null
                routes = emptyList()
                mapPickTarget = null
                val selectedStart = pickedStart
                if (selectedStart == null && start.isBlank()) {
                    routeStatus = "출발지를 먼저 지도에서 선택하세요."
                    mapPickTarget = MapPickTarget.Start
                } else if (selectedStart != null) {
                    resolvePickedAddress(MapPickTarget.Destination, point)
                    loadTmapRoutesFromMap(selectedStart, pickedWaypoint, point)
                } else {
                    resolvePickedAddress(MapPickTarget.Destination, point)
                    routeStatus = "출발지를 확인한 뒤 경로 찾기를 누르세요."
                }
            }
            null -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle("경로 추천")
        MapPreviewCard(
            route = selectedRoute,
            interactive = true,
            isLoading = isLoadingTmapRoute,
            pickedStart = pickedStart,
            pickedWaypoint = pickedWaypoint,
            pickedDestination = pickedDestination,
            pickTarget = mapPickTarget,
            cameraTarget = mapCameraTarget,
            cameraRequestId = mapCameraRequestId,
            onMapCenterChanged = { center -> mapCenter = center }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(routeScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CardSurface {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TabButton(
                            label = "출발지 입력",
                            selected = mapPickTarget == MapPickTarget.Start,
                            onClick = {
                                focusManager.clearFocus()
                                clearRouteState("출발지를 지도에서 선택하세요.")
                                pickedStart = null
                                pickedWaypoint = null
                                pickedDestination = null
                                start = ""
                                waypoint = ""
                                destination = ""
                                mapPickTarget = MapPickTarget.Start
                            }
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        TabButton(
                            label = "도착지 입력",
                            selected = mapPickTarget == MapPickTarget.Destination,
                            onClick = {
                                focusManager.clearFocus()
                                selectedRouteId = null
                                routes = emptyList()
                                if (pickedStart == null && start.isBlank()) {
                                    mapPickTarget = MapPickTarget.Start
                                    "출발지를 먼저 선택하거나 출발 선택을 누르세요."
                                } else {
                                    mapPickTarget = MapPickTarget.Destination
                                    "도착지를 지도에서 선택하세요."
                                }.also { routeStatus = it }
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (mapPickTarget != null) {
                            ActionPillButton(
                                label = "선택",
                                background = AppColors.MapAction,
                                onClick = {
                                    focusManager.clearFocus()
                                    selectMapCenter()
                                }
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        TabButton(
                            label = "경유",
                            selected = mapPickTarget == MapPickTarget.Waypoint,
                            onClick = {
                                focusManager.clearFocus()
                                if (pickedStart == null && start.isBlank()) {
                                    mapPickTarget = MapPickTarget.Start
                                    routeStatus = "출발지를 먼저 입력하거나 지도에서 선택하세요."
                                } else {
                                    selectedRouteId = null
                                    routes = emptyList()
                                    mapPickTarget = MapPickTarget.Waypoint
                                    routeStatus = "경유지 또는 반환점을 지도 중심에 맞춘 뒤 선택하세요."
                                }
                            }
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        TabButton(
                            label = "현 위치",
                            selected = false,
                            onClick = {
                                focusManager.clearFocus()
                                if (context.hasFineLocationPermission()) {
                                    centerOnCurrentLocation()
                                } else {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            }
                        )
                    }
                    OutlinedTextField(
                        value = start,
                        onValueChange = {
                            start = it
                            clearRouteForManualInput()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("출발지") },
                        placeholder = { Text("장소명 또는 도로명 주소") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = waypoint,
                        onValueChange = {
                            waypoint = it
                            pickedWaypoint = null
                            clearRouteState("경유지 또는 반환점을 입력한 뒤 경로 찾기를 누르세요.")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("경유지/반환점") },
                        placeholder = { Text("선택 입력") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = destination,
                        onValueChange = {
                            destination = it
                            clearRouteForManualInput()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("목적지") },
                        placeholder = { Text("장소명 또는 도로명 주소") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                loadTmapRoutes(start, destination)
                            }
                        )
                    )
                    TabButton(
                        label = "도착=출발",
                        selected = destination.isNotBlank() && destination == start,
                        onClick = {
                            destination = start
                            pickedDestination = pickedStart
                            clearRouteState("반환점 또는 경유지를 입력한 뒤 경로 찾기를 누르세요.")
                        }
                    )
                    Text(
                        text = routeStatus,
                        color = AppColors.Muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val mapStart = pickedStart
                            val mapWaypoint = pickedWaypoint
                            val mapDestination = pickedDestination
                            if (mapStart != null && mapDestination != null) {
                                loadTmapRoutesFromMap(mapStart, mapWaypoint, mapDestination)
                            } else {
                                loadTmapRoutes(start, waypoint, destination)
                            }
                        },
                        enabled = tmapRouteRepository != null && !isLoadingTmapRoute,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(if (isLoadingTmapRoute) "불러오는 중" else "입력한 경로 찾기")
                    }
                }
            }

            routes.forEach { route ->
                RouteCard(
                    route = route,
                    highlighted = route.id == selectedRoute?.id,
                    onClick = { selectedRouteId = route.id }
                )
            }
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
            body = "현재 대기질 데이터 기준 PM2.5는 보통 수준입니다. 민감군이 아니라면 짧은 산책은 가능하지만, 차량 통행량이 많은 대로변보다 하천·공원 인접 경로를 추천합니다."
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
private fun AirQualitySummaryCard(
    state: CurrentAirQualityState,
    recommendationScore: Int,
    onRefreshLocation: () -> Unit
) {
    val station = state.station
    val reading = state.reading

    CardSurface {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("현재 위치 대기질", color = AppColors.Muted)
                    Text(
                        text = station?.let { "${it.city} ${it.name}" } ?: "--",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusBadge(reading?.grade?.displayName() ?: "--")
            }

            Divider(color = AppColors.Border)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricBlock(
                    label = "PM10",
                    value = reading?.pm10?.toString() ?: "-",
                    unit = "ug/m3",
                    modifier = Modifier.weight(1f)
                )
                MetricBlock(
                    label = "PM2.5",
                    value = reading?.pm25?.toString() ?: "-",
                    unit = "ug/m3",
                    modifier = Modifier.weight(1f)
                )
                MetricBlock(
                    label = "추천",
                    value = recommendationScore.toString(),
                    unit = "점",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = reading?.let { "${it.measuredAt} 기준 ${state.status}" } ?: state.status,
                color = AppColors.Muted,
                style = MaterialTheme.typography.bodyMedium
            )
            TabButton(
                label = "현재 위치 대기질 갱신",
                selected = false,
                onClick = onRefreshLocation
            )
        }
    }
}

@Composable
private fun MapPreviewCard(
    route: RouteCandidate?,
    interactive: Boolean,
    isLoading: Boolean,
    pickedStart: GeoPoint?,
    pickedWaypoint: GeoPoint?,
    pickedDestination: GeoPoint?,
    pickTarget: MapPickTarget?,
    cameraTarget: GeoPoint?,
    cameraRequestId: Int,
    onMapCenterChanged: (GeoPoint) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(Color.White, RoundedCornerShape(22.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(22.dp))
            .padding(1.dp)
    ) {
        when {
            isLoading -> {
                MapStatusCard(
                    message = "Tmap 보행자 경로를 불러오는 중입니다.",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            interactive && BuildConfig.NAVER_CLIENT_ID.isNotBlank() -> {
                NaverRouteMapPreview(
                    route = route,
                    pickedStart = pickedStart,
                    pickedWaypoint = pickedWaypoint,
                    pickedDestination = pickedDestination,
                    pickTarget = pickTarget,
                    cameraTarget = cameraTarget,
                    cameraRequestId = cameraRequestId,
                    onMapCenterChanged = onMapCenterChanged,
                    modifier = Modifier.fillMaxSize()
                )
                if (pickTarget != null) {
                    CenterPickMarker(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            else -> {
                MapStatusCard(
                    message = "네이버 지도 키가 설정되면 경로 지도를 표시합니다.",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun NaverRouteMapPreview(
    route: RouteCandidate?,
    pickedStart: GeoPoint?,
    pickedWaypoint: GeoPoint?,
    pickedDestination: GeoPoint?,
    pickTarget: MapPickTarget?,
    cameraTarget: GeoPoint?,
    cameraRequestId: Int,
    onMapCenterChanged: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onPause()
            mapView?.onStop()
            mapView?.onDestroy()
        }
    }

    Box(modifier = modifier.background(AppColors.Background)) {
        AndroidView(
            factory = { context ->
                MapView(context).apply {
                    mapView = this
                    onCreate(null)
                    onStart()
                    onResume()
                    renderMapState(route, pickedStart, pickedWaypoint, pickedDestination, pickTarget, cameraTarget, cameraRequestId, onMapCenterChanged)
                }
            },
            update = { view ->
                view.renderMapState(route, pickedStart, pickedWaypoint, pickedDestination, pickTarget, cameraTarget, cameraRequestId, onMapCenterChanged)
            },
            modifier = Modifier.fillMaxSize()
        )
        if (route != null && route.coordinates.size < 2) {
            MapStatusCard(
                message = "표시할 경로 좌표가 없습니다.",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun MapView.renderMapState(
    route: RouteCandidate?,
    pickedStart: GeoPoint?,
    pickedWaypoint: GeoPoint?,
    pickedDestination: GeoPoint?,
    pickTarget: MapPickTarget?,
    cameraTarget: GeoPoint?,
    cameraRequestId: Int,
    onMapCenterChanged: (GeoPoint) -> Unit
) {
    val routeKey = buildMapRenderKey(route, pickedStart, pickedWaypoint, pickedDestination, pickTarget, cameraTarget, cameraRequestId)
    val currentState = tag as? NaverRouteOverlayState
    if (currentState?.routeKey == routeKey) return

    tag = NaverRouteOverlayState(
        routeKey = routeKey,
        overlays = currentState?.overlays.orEmpty()
    )

    getMapAsync { naverMap ->
        val latestState = tag as? NaverRouteOverlayState
        if (latestState?.routeKey != routeKey) {
            return@getMapAsync
        }

        naverMap.setOnMapClickListener(null)
        naverMap.addOnCameraIdleListener {
            val target = naverMap.cameraPosition.target
            onMapCenterChanged(GeoPoint(target.latitude, target.longitude))
        }
        latestState.overlays.forEach { overlay -> overlay.map = null }
        val overlays = naverMap.drawMapState(route, pickedStart, pickedWaypoint, pickedDestination)
        if (cameraTarget != null) {
            naverMap.moveCamera(CameraUpdate.scrollTo(cameraTarget.toLatLng()))
        }
        tag = NaverRouteOverlayState(routeKey, overlays)
    }
}

private fun NaverMap.drawMapState(
    route: RouteCandidate?,
    pickedStart: GeoPoint?,
    pickedWaypoint: GeoPoint?,
    pickedDestination: GeoPoint?
): List<Overlay> {
    uiSettings.isZoomControlEnabled = true
    uiSettings.isCompassEnabled = true
    uiSettings.isScaleBarEnabled = true

    val points = route?.coordinates.orEmpty().map { point ->
        LatLng(point.latitude, point.longitude)
    }
    val overlays = mutableListOf<Overlay>()
    if (points.size >= 2) {
        overlays += PathOverlay().apply {
            coords = points
            color = route?.routeColorArgb?.toInt() ?: 0xFF2E7D5B.toInt()
            outlineColor = android.graphics.Color.WHITE
            width = 12
            outlineWidth = 4
            map = this@drawMapState
        }
    }

    val startPosition = pickedStart?.toLatLng() ?: points.firstOrNull()
    val destinationPosition = pickedDestination?.toLatLng() ?: points.lastOrNull()

    if (startPosition != null) {
        overlays += Marker().apply {
            position = startPosition
            captionText = "출발"
            map = this@drawMapState
        }
    }
    if (pickedWaypoint != null) {
        overlays += Marker().apply {
            position = pickedWaypoint.toLatLng()
            captionText = "경유"
            map = this@drawMapState
        }
    }
    if (destinationPosition != null) {
        overlays += Marker().apply {
            position = destinationPosition
            captionText = "도착"
            map = this@drawMapState
        }
    }

    if (points.size >= 2) {
        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { point -> boundsBuilder.include(point) }
        moveCamera(CameraUpdate.fitBounds(boundsBuilder.build(), 64))
    } else if (startPosition != null && destinationPosition != null) {
        val boundsBuilder = LatLngBounds.Builder()
        boundsBuilder.include(startPosition)
        boundsBuilder.include(destinationPosition)
        moveCamera(CameraUpdate.fitBounds(boundsBuilder.build(), 64))
    } else if (startPosition != null) {
        moveCamera(CameraUpdate.scrollTo(startPosition))
    } else {
        moveCamera(CameraUpdate.scrollTo(DEFAULT_MAP_CENTER))
    }

    return overlays
}

private data class NaverRouteOverlayState(
    val routeKey: String,
    val overlays: List<Overlay>
)

@Composable
private fun CenterPickMarker(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.offset(y = (-16).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(AppColors.Primary)
                .border(3.dp, Color.White, RoundedCornerShape(999.dp))
        )
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 18.dp)
                .background(AppColors.Primary)
        )
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
private fun ActionPillButton(
    label: String,
    background: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, background, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
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

private enum class MapPickTarget {
    Start,
    Waypoint,
    Destination
}

private data class CurrentAirQualityState(
    val station: AirQualityStation?,
    val reading: AirQualityReading?,
    val status: String
) {
    companion object {
        fun empty(status: String = "위치 권한이 없으면 현재 위치 대기질은 표시되지 않습니다."): CurrentAirQualityState {
            return CurrentAirQualityState(
                station = null,
                reading = null,
                status = status
            )
        }
    }
}

private object AppColors {
    val Primary = Color(0xFF2E7D5B)
    val MapAction = Color(0xFF2F6FED)
    val Ink = Color(0xFF16231D)
    val Muted = Color(0xFF53655C)
    val Border = Color(0xFFDCE8E1)
    val Background = Color(0xFFF3FAF6)
}

private const val DEFAULT_START = "수원시청"
private const val DEFAULT_DESTINATION = "광교호수공원"
private val DEFAULT_MAP_CENTER = LatLng(37.2636, 127.0286)

private fun formatDistance(distanceMeters: Int): String {
    return String.format(Locale.KOREA, "%.1f km", distanceMeters / 1000.0)
}

private fun AirQualityGrade.displayName(): String {
    return when (this) {
        AirQualityGrade.GOOD -> "좋음"
        AirQualityGrade.NORMAL -> "보통"
        AirQualityGrade.BAD -> "나쁨"
        AirQualityGrade.VERY_BAD -> "매우 나쁨"
        AirQualityGrade.UNKNOWN -> "확인 중"
    }
}

private fun RouteCandidate.renderKey(): String {
    return buildString {
        append(id)
        append(':')
        append(coordinates.size)
        coordinates.forEach { point ->
            append(':')
            append(point.latitude)
            append(',')
            append(point.longitude)
        }
    }
}

private fun buildMapRenderKey(
    route: RouteCandidate?,
    pickedStart: GeoPoint?,
    pickedWaypoint: GeoPoint?,
    pickedDestination: GeoPoint?,
    pickTarget: MapPickTarget?,
    cameraTarget: GeoPoint?,
    cameraRequestId: Int
): String {
    return buildString {
        append(route?.renderKey() ?: "empty")
        append(':')
        append(pickedStart?.latitude)
        append(',')
        append(pickedStart?.longitude)
        append(':')
        append(pickedWaypoint?.latitude)
        append(',')
        append(pickedWaypoint?.longitude)
        append(':')
        append(pickedDestination?.latitude)
        append(',')
        append(pickedDestination?.longitude)
        append(':')
        append(pickTarget?.name)
        append(':')
        append(cameraTarget?.latitude)
        append(',')
        append(cameraTarget?.longitude)
        append(':')
        append(cameraRequestId)
    }
}

private fun GeoPoint.toTmapPlace(name: String): TmapPlace {
    return TmapPlace(
        name = name,
        longitude = longitude,
        latitude = latitude
    )
}

private fun GeoPoint.toLatLng(): LatLng {
    return LatLng(latitude, longitude)
}

private fun LatLng.toGeoPoint(): GeoPoint {
    return GeoPoint(latitude, longitude)
}

private fun Context.hasFineLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun Context.findBestLastKnownLocation(): GeoPoint? {
    if (!hasFineLocationPermission()) return null

    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )
    return providers
        .mapNotNull { provider ->
            runCatching {
                locationManager.getLastKnownLocation(provider)
            }.getOrNull()
        }
        .maxByOrNull(Location::getTime)
        ?.let { location -> GeoPoint(location.latitude, location.longitude) }
}

private fun List<AirQualityStation>.nearestStationTo(point: GeoPoint): AirQualityStation? {
    return minByOrNull { station ->
        point.approximateDistanceTo(GeoPoint(station.latitude, station.longitude))
    }
}

private fun GeoPoint.approximateDistanceTo(other: GeoPoint): Double {
    val latitudeScale = 111_000.0
    val longitudeScale = latitudeScale * kotlin.math.cos(Math.toRadians((latitude + other.latitude) / 2.0))
    val latitudeDistance = (latitude - other.latitude) * latitudeScale
    val longitudeDistance = (longitude - other.longitude) * longitudeScale

    return kotlin.math.sqrt(latitudeDistance * latitudeDistance + longitudeDistance * longitudeDistance)
}
