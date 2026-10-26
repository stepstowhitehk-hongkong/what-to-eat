package com.whattoeat.app

import org.junit.Assert.*
import org.junit.Test

class WheelMathTest {
    @Test fun everyWedgeLandsAtTwelveOClockForAllSupportedMenuSizes() {
        for (count in 1..12) for (index in 0 until count) {
            val angle = WheelMath.landingAngle(index, count)
            val centre = (angle + (index + .5f) * 360f / count) % 360f
            assertTrue("count=$count index=$index centre=$centre", centre < .001f || centre > 359.999f)
        }
    }

    @Test fun repeatedSpinsAlwaysMoveForwardAtLeastFiveRotationsAndLandCorrectly() {
        var angle = 0f
        repeat(24) { index ->
            val target = index % 9
            val next = WheelMath.spinDestination(angle, target, 9)
            assertTrue(next - angle >= 1800f)
            assertEquals(WheelMath.landingAngle(target, 9), next % 360f, .01f)
            angle = next
        }
    }
}
