package com.whattoeat.app

/** Canvas wedges start at twelve o'clock; land the chosen wedge's centre there. */
object WheelMath {
    fun landingAngle(index: Int, count: Int): Float {
        require(count > 0 && index in 0 until count)
        val step = 360f / count
        return ((-index * step - step / 2f) % 360f + 360f) % 360f
    }

    fun spinDestination(current: Float, index: Int, count: Int): Float {
        val normalized = (current % 360f + 360f) % 360f
        return current + 1800f + (landingAngle(index, count) - normalized + 360f) % 360f
    }
}
