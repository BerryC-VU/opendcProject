package org.opendc.simulator.compute.energy

import org.opendc.common.units.Power
import kotlin.math.PI
import kotlin.math.sin

public class SinusoidalEnergySupplyModel(
    private val min: Power,
    private val max: Power,
    private val period: Double,
    private val phaseShift: Double = -PI/2,
) {
    private val amplitude = (max.toWatts() - min.toWatts()) / 2
    private val offset = (max.toWatts() + min.toWatts()) / 2
    private var currentTime = 0L

    private fun getCurrentSupply(): Double {
        val angle = 2 * PI * (currentTime / period) + phaseShift
        return (amplitude * sin(angle) + offset).coerceAtLeast(0.0)
    }

    public fun supply(time: Long): Double {
        currentTime += time
        return getCurrentSupply()
    }
}
