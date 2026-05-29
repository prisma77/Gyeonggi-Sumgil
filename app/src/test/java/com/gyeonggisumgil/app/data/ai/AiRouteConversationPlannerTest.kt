package com.gyeonggisumgil.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRouteConversationPlannerTest {
    @Test
    fun broadRoadWithoutAddressNumberAsksForStartPoint() {
        val action = AiRouteConversationPlanner.plan(
            message = "미사강변대로 3km 달리고 싶어",
            pendingRequest = null,
            hasCurrentLocation = true
        )

        assertTrue(action is AiRouteConversationAction.AskClarifyingQuestion)
        val ask = action as AiRouteConversationAction.AskClarifyingQuestion
        assertEquals(AiRouteClarificationType.NeedStartPoint, ask.pendingRequest.clarificationType)
        assertEquals("미사강변대로", ask.pendingRequest.placeQuery)
    }

    @Test
    fun knownParkWithoutDistanceAsksForDistance() {
        val action = AiRouteConversationPlanner.plan(
            message = "광교호수 산책코스",
            pendingRequest = null,
            hasCurrentLocation = true
        )

        assertTrue(action is AiRouteConversationAction.AskClarifyingQuestion)
        val ask = action as AiRouteConversationAction.AskClarifyingQuestion
        assertEquals(AiRouteClarificationType.NeedDistance, ask.pendingRequest.clarificationType)
        assertEquals("광교호수", ask.pendingRequest.placeQuery)
    }

    @Test
    fun distanceAnswerCompletesPendingPlaceRequest() {
        val pending = PendingAiRouteRequest(
            originalRequest = "광교호수 산책코스",
            placeQuery = "광교호수",
            distanceMeters = null,
            durationMinutes = null,
            activity = AiRouteActivity.Walk,
            clarificationType = AiRouteClarificationType.NeedDistance
        )

        val action = AiRouteConversationPlanner.plan(
            message = "3km",
            pendingRequest = pending,
            hasCurrentLocation = true
        )

        assertTrue(action is AiRouteConversationAction.ReadyToRoute)
        val ready = action as AiRouteConversationAction.ReadyToRoute
        assertEquals("광교호수", ready.placeQuery)
        assertEquals(3_000, ready.distanceMeters)
    }

    @Test
    fun currentLocationConfirmationIgnoresBroadRoadPoiSearch() {
        val pending = PendingAiRouteRequest(
            originalRequest = "미사강변대로 3km 달리고 싶어",
            placeQuery = "미사강변대로",
            distanceMeters = 3_000,
            durationMinutes = null,
            activity = AiRouteActivity.Run,
            clarificationType = AiRouteClarificationType.NeedStartPoint
        )

        val action = AiRouteConversationPlanner.plan(
            message = "응 현재 위치 기준",
            pendingRequest = pending,
            hasCurrentLocation = true
        )

        assertTrue(action is AiRouteConversationAction.ReadyToRoute)
        val ready = action as AiRouteConversationAction.ReadyToRoute
        assertTrue(ready.useCurrentLocationOnly)
        assertNull(ready.placeQuery)
    }

    @Test
    fun fullLoopRequestDoesNotAskForDistance() {
        val action = AiRouteConversationPlanner.plan(
            message = "광교호수를 한바퀴 도는 코스를 추천해줘",
            pendingRequest = null,
            hasCurrentLocation = true
        )

        assertTrue(action is AiRouteConversationAction.ReadyToRoute)
        val ready = action as AiRouteConversationAction.ReadyToRoute
        assertEquals("광교호수", ready.placeQuery)
        assertNull(ready.distanceMeters)
        assertNull(ready.durationMinutes)
    }

    @Test
    fun lowercaseIcPlaceRequestBuildsRouteInsteadOfAdvice() {
        val action = AiRouteConversationPlanner.plan(
            message = "미사ic 근처 산책길 3km",
            pendingRequest = null,
            hasCurrentLocation = true
        )

        assertTrue(action is AiRouteConversationAction.ReadyToRoute)
        val ready = action as AiRouteConversationAction.ReadyToRoute
        assertEquals("미사ic", ready.placeQuery)
        assertEquals(3_000, ready.distanceMeters)
        assertFalse(ready.useCurrentLocationOnly)
    }

    @Test
    fun candidateBrowseRequestDoesNotAskForDistance() {
        val action = AiRouteConversationPlanner.plan(
            message = "미사 한강변 산책 후보 보여줘",
            pendingRequest = null,
            hasCurrentLocation = true
        )

        assertTrue(action is AiRouteConversationAction.ReadyToRoute)
        val ready = action as AiRouteConversationAction.ReadyToRoute
        assertEquals("미사 한강변", ready.placeQuery)
        assertEquals(2_500, ready.distanceMeters)
    }

    @Test
    fun genericWalkingRecommendationUsesCurrentLocation() {
        val action = AiRouteConversationPlanner.plan(
            message = "산책길 추천",
            pendingRequest = null,
            hasCurrentLocation = true
        )

        assertTrue(action is AiRouteConversationAction.ReadyToRoute)
        val ready = action as AiRouteConversationAction.ReadyToRoute
        assertTrue(ready.useCurrentLocationOnly)
        assertNull(ready.placeQuery)
        assertEquals(2_500, ready.distanceMeters)
    }

    @Test
    fun unknownPlaceLoopAsksAgainInsteadOfCurrentLocationFallback() {
        val action = AiRouteConversationPlanner.plan(
            message = "분당저수지를 한바퀴 산책 코스 짜줘",
            pendingRequest = null,
            hasCurrentLocation = true
        )

        assertTrue(action is AiRouteConversationAction.AskClarifyingQuestion)
        val ask = action as AiRouteConversationAction.AskClarifyingQuestion
        assertEquals(AiRouteClarificationType.NeedStartPoint, ask.pendingRequest.clarificationType)
        assertNull(ask.pendingRequest.placeQuery)
    }

    @Test
    fun explicitCurrentLocationRequestCanUseCurrentLocation() {
        val action = AiRouteConversationPlanner.plan(
            message = "현재 위치 2km 산책 코스 짜줘",
            pendingRequest = null,
            hasCurrentLocation = true
        )

        assertTrue(action is AiRouteConversationAction.ReadyToRoute)
        val ready = action as AiRouteConversationAction.ReadyToRoute
        assertTrue(ready.useCurrentLocationOnly)
        assertNull(ready.placeQuery)
    }
}
