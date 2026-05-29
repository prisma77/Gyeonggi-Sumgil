package com.gyeonggisumgil.app.data.ai

enum class AiRequestIntent {
    Advice,
    RouteGeneration
}

object AiRequestIntentClassifier {
    fun classify(question: String): AiRequestIntent {
        val normalized = question.normalizedForIntent()
        if (normalized.isBlank()) return AiRequestIntent.Advice

        val hasRouteKeyword = routeKeywords.any(normalized::contains)
        val hasAdviceKeyword = adviceKeywords.any(normalized::contains)
        val hasWeatherOrAirKeyword = weatherOrAirKeywords.any(normalized::contains)
        val hasWalkingKeyword = walkingKeywords.any(normalized::contains)
        val hasGenerationKeyword = generationKeywords.any(normalized::contains)
        val hasDesireKeyword = desireKeywords.any(normalized::contains)
        val hasPlaceKeyword = placeKeywords.any(normalized::contains)
        val hasNearbyKeyword = nearbyKeywords.any(normalized::contains)
        val hasDistance = normalized.hasRequestedDistance()
        val hasDuration = normalized.hasRequestedDuration()
        val asksForRouteShape = routeShapeKeywords.any(normalized::contains)

        if (hasRouteKeyword || asksForRouteShape) {
            return AiRequestIntent.RouteGeneration
        }

        if (hasAdviceKeyword && !hasGenerationKeyword) {
            return AiRequestIntent.Advice
        }

        if (hasWeatherOrAirKeyword && !hasGenerationKeyword && !hasDistance && !hasDuration) {
            return AiRequestIntent.Advice
        }

        if ((hasDistance || hasDuration) && hasGenerationKeyword && (hasWalkingKeyword || hasPlaceKeyword || hasNearbyKeyword)) {
            return AiRequestIntent.RouteGeneration
        }

        if ((hasDistance || hasDuration) && hasDesireKeyword && (hasWalkingKeyword || hasPlaceKeyword || hasNearbyKeyword)) {
            return AiRequestIntent.RouteGeneration
        }

        if (hasDistance && hasWalkingKeyword && (hasPlaceKeyword || hasNearbyKeyword)) {
            return AiRequestIntent.RouteGeneration
        }

        if (hasPlaceKeyword && hasWalkingKeyword && hasGenerationKeyword) {
            return AiRequestIntent.RouteGeneration
        }

        if (hasNearbyKeyword && hasWalkingKeyword && hasGenerationKeyword) {
            return AiRequestIntent.RouteGeneration
        }

        return AiRequestIntent.Advice
    }

    fun isRouteGenerationRequest(question: String): Boolean {
        return classify(question) == AiRequestIntent.RouteGeneration
    }

    private val routeKeywords = listOf(
        "코스",
        "경로",
        "산책길",
        "루트",
        "길찾",
        "길추천",
        "도보경로",
        "동선",
        "후보",
        "출발",
        "도착",
        "경유",
        "반환",
        "왕복"
    )

    private val routeShapeKeywords = listOf(
        "한바퀴",
        "일주",
        "둘레",
        "돌아오는",
        "돌아오기",
        "돌고싶",
        "돌수",
        "돌아줘"
    )

    private val adviceKeywords = listOf(
        "어때",
        "괜찮",
        "가능",
        "될까",
        "되나",
        "돼",
        "되겠",
        "좋을까",
        "나쁠까",
        "위험",
        "주의",
        "나가도"
    )

    private val weatherOrAirKeywords = listOf(
        "날씨",
        "대기질",
        "미세먼지",
        "초미세먼지",
        "공기",
        "비",
        "눈",
        "바람",
        "기온",
        "춥",
        "덥",
        "습도"
    )

    private val walkingKeywords = listOf(
        "산책",
        "걷",
        "걸",
        "달리",
        "뛰",
        "조깅",
        "도보",
        "러닝",
        "운동"
    )

    private val generationKeywords = listOf(
        "추천",
        "만들",
        "짜",
        "찾",
        "생성",
        "그려",
        "잡아",
        "보여",
        "보여줘",
        "설계",
        "부탁"
    )

    private val desireKeywords = listOf(
        "싶",
        "하려고",
        "하고싶",
        "가고싶",
        "걷고싶",
        "달리고싶"
    )

    private val placeKeywords = listOf(
        "광교",
        "미사",
        "호수",
        "공원",
        "하천",
        "강변",
        "중랑천",
        "왕숙천",
        "탄천",
        "안양천",
        "역",
        "집"
    )

    private val nearbyKeywords = listOf(
        "근처",
        "주변",
        "가까운",
        "인근",
        "현재위치",
        "현위치"
    )
}

private fun String.normalizedForIntent(): String {
    return lowercase()
        .replace(" ", "")
        .replace("-", "")
        .replace("_", "")
        .replace(",", "")
}

private fun String.hasRequestedDistance(): Boolean {
    return Regex("""\d+(?:\.\d+)?(?:km|킬로|키로|킬로미터|m|미터)""", RegexOption.IGNORE_CASE)
        .containsMatchIn(this)
}

private fun String.hasRequestedDuration(): Boolean {
    return Regex("""\d+(?:\.\d+)?(?:분|시간)""")
        .containsMatchIn(this)
}
