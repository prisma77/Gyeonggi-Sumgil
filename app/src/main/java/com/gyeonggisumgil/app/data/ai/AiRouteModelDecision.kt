package com.gyeonggisumgil.app.data.ai

import com.google.gson.JsonObject
import com.google.gson.JsonParser

enum class AiRouteModelIntent {
    Advice,
    Route
}

enum class AiRouteShapeHint {
    Unknown,
    PointToPoint,
    Loop,
    LakeLoop,
    RiverOutAndBack
}

data class AiRouteModelDecision(
    val intent: AiRouteModelIntent,
    val needsClarification: Boolean,
    val clarifyingQuestion: String?,
    val routeRequestText: String,
    val placeQueries: List<String>,
    val distanceMeters: Int?,
    val durationMinutes: Int?,
    val activity: AiRouteActivity,
    val routeShape: AiRouteShapeHint,
    val useCurrentLocation: Boolean
) {
    fun toConversationAction(originalMessage: String): AiRouteConversationAction {
        if (intent == AiRouteModelIntent.Advice) {
            return AiRouteConversationAction.AdviceOnly
        }

        val primaryPlaceQuery = placeQueries.firstOrNull()
        if (needsClarification) {
            return AiRouteConversationAction.AskClarifyingQuestion(
                question = clarifyingQuestion?.takeIf { it.isNotBlank() }
                    ?: "출발 기준이나 원하는 길이를 조금 더 구체적으로 알려주세요.",
                pendingRequest = PendingAiRouteRequest(
                    originalRequest = originalMessage,
                    placeQuery = primaryPlaceQuery,
                    distanceMeters = distanceMeters,
                    durationMinutes = durationMinutes,
                    activity = activity,
                    clarificationType = if (distanceMeters == null && durationMinutes == null) {
                        AiRouteClarificationType.NeedDistance
                    } else {
                        AiRouteClarificationType.NeedStartPoint
                    }
                )
            )
        }

        return AiRouteConversationAction.ReadyToRoute(
            requestText = routeRequestText.ifBlank { originalMessage },
            placeQuery = primaryPlaceQuery,
            placeQueries = placeQueries,
            distanceMeters = distanceMeters,
            durationMinutes = durationMinutes,
            useCurrentLocationOnly = useCurrentLocation && placeQueries.isEmpty(),
            routeShapeHint = routeShape
        )
    }
}

object AiRouteModelDecisionParser {
    fun parse(rawText: String): AiRouteModelDecision? {
        val jsonText = rawText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
            .substringAfterFirstJsonObject()

        val root = runCatching { JsonParser.parseString(jsonText).asJsonObject }.getOrNull()
            ?: return null

        return AiRouteModelDecision(
            intent = root.stringOrNull("intent").toIntent(),
            needsClarification = root.booleanOrFalse("needs_clarification"),
            clarifyingQuestion = root.stringOrNull("clarifying_question"),
            routeRequestText = root.stringOrNull("route_request_text").orEmpty(),
            placeQueries = root.stringList("place_queries"),
            distanceMeters = root.intOrNull("distance_meters"),
            durationMinutes = root.intOrNull("duration_minutes"),
            activity = root.stringOrNull("activity").toActivity(),
            routeShape = root.stringOrNull("route_shape").toRouteShape(),
            useCurrentLocation = root.booleanOrFalse("use_current_location")
        )
    }

    private fun String.substringAfterFirstJsonObject(): String {
        val start = indexOf('{')
        val end = lastIndexOf('}')
        return if (start >= 0 && end >= start) substring(start, end + 1) else this
    }

    private fun JsonObject.stringOrNull(name: String): String? {
        val element = get(name) ?: return null
        return if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
            element.asString.takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

    private fun JsonObject.booleanOrFalse(name: String): Boolean {
        val element = get(name) ?: return false
        return if (element.isJsonPrimitive && element.asJsonPrimitive.isBoolean) {
            element.asBoolean
        } else {
            false
        }
    }

    private fun JsonObject.intOrNull(name: String): Int? {
        val element = get(name) ?: return null
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isNumber) return null
        return element.asInt.takeIf { it > 0 }
    }

    private fun JsonObject.stringList(name: String): List<String> {
        val element = get(name) ?: return emptyList()
        if (!element.isJsonArray) return emptyList()
        return element.asJsonArray
            .mapNotNull { item ->
                if (item.isJsonPrimitive && item.asJsonPrimitive.isString) {
                    item.asString.trim().takeIf { it.isNotBlank() }
                } else {
                    null
                }
            }
            .distinct()
    }

    private fun String?.toIntent(): AiRouteModelIntent {
        return when (this?.lowercase()) {
            "route", "route_request", "route_generation" -> AiRouteModelIntent.Route
            else -> AiRouteModelIntent.Advice
        }
    }

    private fun String?.toActivity(): AiRouteActivity {
        return when (this?.lowercase()) {
            "run", "running", "jogging", "러닝", "달리기", "조깅" -> AiRouteActivity.Run
            else -> AiRouteActivity.Walk
        }
    }

    private fun String?.toRouteShape(): AiRouteShapeHint {
        return when (this?.lowercase()) {
            "point_to_point", "pointtopoint", "one_way" -> AiRouteShapeHint.PointToPoint
            "loop", "round_trip", "return_loop" -> AiRouteShapeHint.Loop
            "lake_loop" -> AiRouteShapeHint.LakeLoop
            "river_out_and_back", "out_and_back", "river" -> AiRouteShapeHint.RiverOutAndBack
            else -> AiRouteShapeHint.Unknown
        }
    }
}
