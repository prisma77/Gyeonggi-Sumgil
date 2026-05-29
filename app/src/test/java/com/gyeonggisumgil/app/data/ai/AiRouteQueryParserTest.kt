package com.gyeonggisumgil.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiRouteQueryParserTest {
    @Test
    fun extractsRoadNameBeforeRoutingFallback() {
        assertEquals(
            "미사강변대로",
            AiRouteQueryParser.extractPlaceQuery("미사강변대로 3km 달리고 싶어")
        )
    }

    @Test
    fun extractsParkNameFromCourseRequest() {
        assertEquals(
            "광교 호수 공원",
            AiRouteQueryParser.extractPlaceQuery("광교 호수 공원 3km 코스 추천해줘")
        )
    }

    @Test
    fun removesCasualImperativeWordsFromPlaceQuery() {
        assertEquals(
            "두물머리",
            AiRouteQueryParser.extractPlaceQuery("두물머리에서 산책 코스 줘라")
        )
    }

    @Test
    fun removesLoopWordsFromPlaceQuery() {
        assertEquals(
            "광교호수",
            AiRouteQueryParser.extractPlaceQuery("광교호수를 한바퀴 도는 코스를 추천해줘")
        )
    }

    @Test
    fun ignoresDistanceOnlyRequests() {
        assertNull(AiRouteQueryParser.extractPlaceQuery("3km 달리고 싶어"))
    }

    @Test
    fun extractsLowercaseIcPlaceName() {
        assertEquals(
            "미사ic",
            AiRouteQueryParser.extractPlaceQuery("미사ic 근처 산책길 3km")
        )
    }

    @Test
    fun doesNotKeepAddingUnknownPlaceSuffixes() {
        assertNull(
            AiRouteQueryParser.extractPlaceQuery("분당저수지를 한바퀴 산책 코스 짜줘")
        )
    }
}
