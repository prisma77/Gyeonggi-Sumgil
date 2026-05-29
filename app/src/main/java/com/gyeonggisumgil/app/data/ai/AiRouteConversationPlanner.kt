package com.gyeonggisumgil.app.data.ai

data class PendingAiRouteRequest(
    val originalRequest: String,
    val placeQuery: String?,
    val distanceMeters: Int?,
    val durationMinutes: Int?,
    val activity: AiRouteActivity,
    val clarificationType: AiRouteClarificationType
)

enum class AiRouteActivity {
    Walk,
    Run
}

enum class AiRouteClarificationType {
    NeedDistance,
    NeedStartPoint
}

sealed class AiRouteConversationAction {
    data object AdviceOnly : AiRouteConversationAction()

    data class AskClarifyingQuestion(
        val question: String,
        val pendingRequest: PendingAiRouteRequest
    ) : AiRouteConversationAction()

    data class ReadyToRoute(
        val requestText: String,
        val placeQuery: String?,
        val placeQueries: List<String> = listOfNotNull(placeQuery),
        val distanceMeters: Int?,
        val durationMinutes: Int?,
        val useCurrentLocationOnly: Boolean,
        val routeShapeHint: AiRouteShapeHint = AiRouteShapeHint.Unknown
    ) : AiRouteConversationAction()
}

object AiRouteConversationPlanner {
    fun plan(
        message: String,
        pendingRequest: PendingAiRouteRequest?,
        hasCurrentLocation: Boolean
    ): AiRouteConversationAction {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return AiRouteConversationAction.AdviceOnly

        if (pendingRequest != null) {
            return continuePendingRequest(
                answer = trimmed,
                pendingRequest = pendingRequest,
                hasCurrentLocation = hasCurrentLocation
            )
        }

        if (!AiRequestIntentClassifier.isRouteGenerationRequest(trimmed)) {
            return AiRouteConversationAction.AdviceOnly
        }

        val placeQuery = AiRouteQueryParser.extractPlaceQuery(trimmed)
        val distanceMeters = AiRouteQueryParser.extractRequestedDistanceMeters(trimmed)
        val durationMinutes = AiRouteQueryParser.extractRequestedDurationMinutes(trimmed)
        val activity = trimmed.detectActivity()
        val hasDefinedRouteShape = trimmed.hasDefinedRouteShape()

        if (!hasDefinedRouteShape && trimmed.isCandidateBrowseRequest() && (placeQuery != null || hasCurrentLocation)) {
            return AiRouteConversationAction.ReadyToRoute(
                requestText = trimmed,
                placeQuery = placeQuery,
                distanceMeters = distanceMeters
                    ?: if (durationMinutes == null) DEFAULT_RECOMMENDED_ROUTE_DISTANCE_METERS else null,
                durationMinutes = durationMinutes,
                useCurrentLocationOnly = placeQuery == null
            )
        }

        if (placeQuery.isBroadLinearPlace() && !trimmed.hasSpecificAddressNumber()) {
            return AiRouteConversationAction.AskClarifyingQuestion(
                question = "${placeQuery}는 범위가 넓어요. 현재 위치 기준으로 ${distanceMeters?.let { formatDistanceText(it) } ?: durationMinutes?.let { "${it}분" } ?: "짧은"} ${activity.label} 코스를 만들까요, 아니면 출발 지점을 더 구체적으로 입력할까요?",
                pendingRequest = PendingAiRouteRequest(
                    originalRequest = trimmed,
                    placeQuery = placeQuery,
                    distanceMeters = distanceMeters,
                    durationMinutes = durationMinutes,
                    activity = activity,
                    clarificationType = AiRouteClarificationType.NeedStartPoint
                )
            )
        }

        if (hasDefinedRouteShape) {
            if (placeQuery == null && (!hasCurrentLocation || !trimmed.allowsCurrentLocationFallbackForRoute())) {
                return AiRouteConversationAction.AskClarifyingQuestion(
                    question = "장소를 정확히 찾지 못했어요. 현재 위치 주변으로 찾을까요, 아니면 장소명을 더 구체적으로 입력할까요?",
                    pendingRequest = PendingAiRouteRequest(
                        originalRequest = trimmed,
                        placeQuery = null,
                        distanceMeters = distanceMeters,
                        durationMinutes = durationMinutes,
                        activity = activity,
                        clarificationType = AiRouteClarificationType.NeedStartPoint
                    )
                )
            }

            return AiRouteConversationAction.ReadyToRoute(
                requestText = trimmed,
                placeQuery = placeQuery,
                distanceMeters = distanceMeters,
                durationMinutes = durationMinutes,
                useCurrentLocationOnly = placeQuery == null
            )
        }

        if (distanceMeters == null && durationMinutes == null) {
            val placeText = placeQuery?.let { "$it 기준으로 " } ?: "현재 위치 기준으로 "
            return AiRouteConversationAction.AskClarifyingQuestion(
                question = "${placeText}어느 정도 길이로 만들까요? 예: 20분, 30분, 3km",
                pendingRequest = PendingAiRouteRequest(
                    originalRequest = trimmed,
                    placeQuery = placeQuery,
                    distanceMeters = null,
                    durationMinutes = null,
                    activity = activity,
                    clarificationType = AiRouteClarificationType.NeedDistance
                )
            )
        }

        if (placeQuery == null && (!hasCurrentLocation || !trimmed.allowsCurrentLocationFallbackForRoute())) {
            return AiRouteConversationAction.AskClarifyingQuestion(
                question = "장소를 정확히 찾지 못했어요. 현재 위치 주변으로 찾을까요, 아니면 장소명을 더 구체적으로 입력할까요?",
                pendingRequest = PendingAiRouteRequest(
                    originalRequest = trimmed,
                    placeQuery = null,
                    distanceMeters = distanceMeters,
                    durationMinutes = durationMinutes,
                    activity = activity,
                    clarificationType = AiRouteClarificationType.NeedStartPoint
                )
            )
        }

        return AiRouteConversationAction.ReadyToRoute(
            requestText = trimmed,
            placeQuery = placeQuery,
            distanceMeters = distanceMeters,
            durationMinutes = durationMinutes,
            useCurrentLocationOnly = placeQuery == null
        )
    }

    private fun continuePendingRequest(
        answer: String,
        pendingRequest: PendingAiRouteRequest,
        hasCurrentLocation: Boolean
    ): AiRouteConversationAction {
        val answerDistance = AiRouteQueryParser.extractRequestedDistanceMeters(answer)
        val answerDuration = AiRouteQueryParser.extractRequestedDurationMinutes(answer)
        val answerPlace = AiRouteQueryParser.extractPlaceQuery(answer)

        return when (pendingRequest.clarificationType) {
            AiRouteClarificationType.NeedDistance -> {
                val distanceMeters = answerDistance ?: pendingRequest.distanceMeters
                val durationMinutes = answerDuration ?: pendingRequest.durationMinutes
                if (distanceMeters == null && durationMinutes == null) {
                    return AiRouteConversationAction.AskClarifyingQuestion(
                        question = "거리나 시간을 숫자로 알려주세요. 예: 30분, 3km",
                        pendingRequest = pendingRequest
                    )
                }
                AiRouteConversationAction.ReadyToRoute(
                    requestText = listOfNotNull(
                        answerPlace ?: pendingRequest.placeQuery,
                        distanceMeters?.let { formatDistanceText(it) },
                        durationMinutes?.let { "${it}분" },
                        pendingRequest.activity.label
                    ).joinToString(" "),
                    placeQuery = answerPlace ?: pendingRequest.placeQuery,
                    distanceMeters = distanceMeters,
                    durationMinutes = durationMinutes,
                    useCurrentLocationOnly = (answerPlace ?: pendingRequest.placeQuery) == null
                )
            }
            AiRouteClarificationType.NeedStartPoint -> {
                if (answer.isAffirmativeForCurrentLocation() && hasCurrentLocation) {
                    AiRouteConversationAction.ReadyToRoute(
                        requestText = listOfNotNull(
                            pendingRequest.distanceMeters?.let { formatDistanceText(it) },
                            pendingRequest.durationMinutes?.let { "${it}분" },
                            pendingRequest.activity.label
                        ).joinToString(" "),
                        placeQuery = null,
                        distanceMeters = pendingRequest.distanceMeters,
                        durationMinutes = pendingRequest.durationMinutes,
                        useCurrentLocationOnly = true
                    )
                } else {
                    val placeQuery = answerPlace
                    if (placeQuery == null) {
                        AiRouteConversationAction.AskClarifyingQuestion(
                            question = "출발 기준을 찾기 어렵습니다. 예: 미사역, 미사강변대로 90, 현재 위치",
                            pendingRequest = pendingRequest
                        )
                    } else {
                        AiRouteConversationAction.ReadyToRoute(
                            requestText = listOfNotNull(
                                placeQuery,
                                pendingRequest.distanceMeters?.let { formatDistanceText(it) },
                                pendingRequest.durationMinutes?.let { "${it}분" },
                                pendingRequest.activity.label
                            ).joinToString(" "),
                            placeQuery = placeQuery,
                            distanceMeters = pendingRequest.distanceMeters,
                            durationMinutes = pendingRequest.durationMinutes,
                            useCurrentLocationOnly = false
                        )
                    }
                }
            }
        }
    }

    private fun String.detectActivity(): AiRouteActivity {
        val normalized = replace(" ", "")
        return if (listOf("달리", "뛰", "러닝", "조깅").any { normalized.contains(it) }) {
            AiRouteActivity.Run
        } else {
            AiRouteActivity.Walk
        }
    }

    private fun String?.isBroadLinearPlace(): Boolean {
        if (this == null) return false
        val compact = replace(" ", "")
        return broadLinearSuffixes.any { compact.endsWith(it) }
    }

    private fun String.hasSpecificAddressNumber(): Boolean {
        return Regex(
            """(?:대로|로|길)\s*\d+(?!\s*(?:km|킬로|키로|킬로미터|m|미터))""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(this)
    }

    private fun String.isAffirmativeForCurrentLocation(): Boolean {
        val normalized = replace(" ", "")
        return listOf("응", "ㅇㅇ", "그래", "좋아", "현재위치", "현위치", "내위치", "지금위치", "맞아").any {
            normalized.contains(it)
        }
    }

    private fun String.hasDefinedRouteShape(): Boolean {
        val normalized = replace(" ", "")
        return listOf("한바퀴", "일주", "둘레", "도는", "돌아오는", "왕복").any {
            normalized.contains(it)
        }
    }

    private fun String.isCandidateBrowseRequest(): Boolean {
        val normalized = replace(" ", "")
        return listOf("후보", "보여줘", "보여", "추천").any { normalized.contains(it) }
    }

    private fun String.allowsCurrentLocationFallbackForRoute(): Boolean {
        val normalized = replace(" ", "")
        return listOf(
            "현재위치",
            "현위치",
            "내위치",
            "지금위치",
            "근처",
            "주변",
            "가까운",
            "인근",
            "아무",
            "근방"
        ).any { normalized.contains(it) }
    }

    private fun formatDistanceText(distanceMeters: Int): String {
        return if (distanceMeters % 1_000 == 0) {
            "${distanceMeters / 1_000}km"
        } else {
            String.format(java.util.Locale.KOREA, "%.1fkm", distanceMeters / 1_000.0)
        }
    }

    private val AiRouteActivity.label: String
        get() = when (this) {
            AiRouteActivity.Walk -> "산책"
            AiRouteActivity.Run -> "러닝"
        }

    private val broadLinearSuffixes = listOf("대로", "로", "길", "강변")
    private const val DEFAULT_RECOMMENDED_ROUTE_DISTANCE_METERS = 2_500
}
