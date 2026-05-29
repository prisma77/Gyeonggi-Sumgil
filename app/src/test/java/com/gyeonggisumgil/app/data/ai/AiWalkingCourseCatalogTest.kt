package com.gyeonggisumgil.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiWalkingCourseCatalogTest {
    @Test
    fun byeollaeWangsukcheonRequestSelectsByeollaeCourse() {
        val selection = AiWalkingCourseCatalog().selectCourse(
            query = "별내역에서 별내별가람 사이의 왕숙천 왕복 산책 코스",
            currentLocation = null,
            nearbyRecommendations = emptyList()
        )

        assertEquals("byeollae-wangsukcheon-river", selection?.course?.id)
    }

    @Test
    fun misaHanRiverRequestSelectsRiverCourse() {
        val selection = AiWalkingCourseCatalog().selectCourse(
            query = "미사IC 근처 한강따라 걷는 산책길 3km 미사강변대로",
            currentLocation = null,
            nearbyRecommendations = emptyList()
        )

        assertEquals("misa-han-river-outback", selection?.course?.id)
    }
}
