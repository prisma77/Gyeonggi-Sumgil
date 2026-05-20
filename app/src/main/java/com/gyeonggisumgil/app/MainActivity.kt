package com.gyeonggisumgil.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gyeonggisumgil.app.ui.theme.GyeonggiSumgilTheme
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GyeonggiSumgilTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    GyeonggiSumgilApp()
                }
            }
        }
    }
}

@Composable
private fun GyeonggiSumgilApp() {
    Box(modifier = Modifier.fillMaxSize()) {
        if (MapRuntime.isKakaoMapSupported()) {
            KakaoMapScreen(modifier = Modifier.fillMaxSize())
        } else {
            EmulatorMapFallback(modifier = Modifier.fillMaxSize())
        }
        HomeOverlay(modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun KakaoMapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var hasStarted by remember { mutableStateOf(false) }

    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.finish()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            if (!hasStarted) {
                hasStarted = true
                mapView.start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() {
                            Log.d(TAG, "Kakao map destroyed")
                        }

                        override fun onMapError(error: Exception) {
                            Log.e(TAG, "Kakao map error", error)
                        }
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(kakaoMap: KakaoMap) {
                            Log.d(TAG, "Kakao map ready: $kakaoMap")
                        }

                        override fun getPosition(): LatLng = LatLng.from(SUWON_CITY_HALL_LAT, SUWON_CITY_HALL_LNG)
                        override fun getZoomLevel(): Int = 15
                    }
                )
            }
            mapView
        }
    )
}

@Composable
private fun EmulatorMapFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFFEAF7F0))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.88f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("지도 개발 미리보기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("현재 실행 환경은 x86_64 에뮬레이터라 카카오맵 네이티브 SDK를 초기화하지 않았습니다.")
            Text("실제 카카오 지도는 ARM64 Android 기기에서 확인하세요.", color = Color(0xFF2E7D5B), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HomeOverlay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.93f))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text("경기 숨길", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("경기도 공공데이터와 AI로 미세먼지를 피해 걷는 길", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3F4A45))
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            AssistChip(onClick = {}, label = { Text("깨끗한 경로") })
            Spacer(modifier = Modifier.width(8.dp))
            AssistChip(onClick = {}, label = { Text("AI 상담") })
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {}) { Text("경로 찾기") }
        }
    }
}

private const val TAG = "GyeonggiSumgil"
private const val SUWON_CITY_HALL_LAT = 37.2636
private const val SUWON_CITY_HALL_LNG = 127.0286
