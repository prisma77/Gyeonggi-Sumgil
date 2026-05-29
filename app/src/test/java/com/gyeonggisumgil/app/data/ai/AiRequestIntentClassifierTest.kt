package com.gyeonggisumgil.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiRequestIntentClassifierTest {
    @Test
    fun weatherAndAirQuestionsStayAsAdvice() {
        val questions = listOf(
            "지금 밖에 나가도 돼?",
            "오늘 저녁에 산책해도 괜찮아?",
            "지금 날씨 어때?",
            "미세먼지 괜찮아?",
            "비 오는데 산책 가능할까?",
            "3km 걸어도 괜찮아?",
            "광교호수 지금 어때?",
            "오늘 공기 좋으면 얼마나 걸어도 돼?"
        )

        questions.forEach { question ->
            assertEquals(
                "question=$question",
                AiRequestIntent.Advice,
                AiRequestIntentClassifier.classify(question)
            )
        }
    }

    @Test
    fun concreteCourseRequestsGenerateRoutes() {
        val questions = listOf(
            "광교 산책 3km 거리 추천해줘",
            "광교호수공원 3km 코스 만들어줘",
            "미사호수공원 30분 코스 짜줘",
            "근처 2km 산책 코스 추천",
            "한바퀴 도는 산책길 만들어줘",
            "집에서 출발해서 돌아오는 경로 만들어줘",
            "중랑천 왕복 5km 산책길 추천해줘",
            "출발지 미사역 도착지 미사강변중앙로 90 경로 찾아줘",
            "미사강변대로 3km 달리고 싶어",
            "광교호수 주변 30분 뛰고 싶어",
            "근처에서 2km 조깅하고 싶어"
        )

        questions.forEach { question ->
            assertEquals(
                "question=$question",
                AiRequestIntent.RouteGeneration,
                AiRequestIntentClassifier.classify(question)
            )
        }
    }

    @Test
    fun vagueRecommendationsDoNotDrawAMapRoute() {
        val questions = listOf(
            "산책 추천해줘",
            "오늘 운동 추천해줘",
            "날씨에 맞게 산책 조언해줘",
            "광교호수 산책 괜찮아?",
            "근처에서 걷기 좋을까?"
        )

        questions.forEach { question ->
            assertEquals(
                "question=$question",
                AiRequestIntent.Advice,
                AiRequestIntentClassifier.classify(question)
            )
        }
    }
}
