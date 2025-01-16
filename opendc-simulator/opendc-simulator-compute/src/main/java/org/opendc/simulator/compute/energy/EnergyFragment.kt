package org.opendc.simulator.compute.energy

import org.opendc.common.units.Power

public class EnergyFragment(
    public var startTime: Long = 0,
    public var endTime: Long = 0,
    public var energy: Power = Power.ZERO
) {
    public constructor(
        startTime: Long,
        endTime: Long,
        energy: Double,
    ) : this() {
        this.startTime = startTime
        this.endTime = endTime
        this.energy = Power.ofWatts(energy)
    }
}
