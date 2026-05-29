package com.gyeonggisumgil.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRouteModelDecisionParserTest {
    @Test
    fun parsesRouteDecisionWithMultiplePlaceQueries() {
        val decision = AiRouteModelDecisionParser.parse(
            """
            {
              "intent": "route",
              "needs_clarification": false,
              "clarifying_question": null,
              "route_request_text": "미사IC 근처 한강변을 따라 걷는 3km 산책 코스",
              "place_queries": ["미사IC", "미사 한강공원", "미사강변대로"],
              "distance_meters": 3000,
              "duration_minutes": null,
              "activity": "walk",
              "route_shape": "river_out_and_back",
              "use_current_location": false
            }
            """.trimIndent()
        )

        requireNotNull(decision)
        assertEquals(AiRouteModelIntent.Route, decision.intent)
        assertFalse(decision.needsClarification)
        assertEquals(listOf("미사IC", "미사 한강공원", "미사강변대로"), decision.placeQueries)
        assertEquals(3_000, decision.distanceMeters)
        assertEquals(AiRouteShapeHint.RiverOutAndBack, decision.routeShape)
    }

    @Test
    fun convertsLoopDecisionWithoutAskingDistance() {
        val decision = AiRouteModelDecisionParser.parse(
            """
            ```json
            {"intent":"route","needs_clarification":false,"clarifying_question":null,"route_request_text":"광교호수공원 원천호수 한바퀴 산책 코스","place_queries":["광교호수공원","원천호수"],"distance_meters":null,"duration_minutes":null,"activity":"walk","route_shape":"lake_loop","use_current_location":false}
            ```
            """.trimIndent()
        )

        requireNotNull(decision)
        val action = decision.toConversationAction("광교호수를 한바퀴 도는 코스 추천해줘")

        assertTrue(action is AiRouteConversationAction.ReadyToRoute)
        val ready = action as AiRouteConversationAction.ReadyToRoute
        assertEquals(AiRouteShapeHint.LakeLoop, ready.routeShapeHint)
        assertEquals(listOf("광교호수공원", "원천호수"), ready.placeQueries)
        assertEquals(null, ready.distanceMeters)
    }
}
