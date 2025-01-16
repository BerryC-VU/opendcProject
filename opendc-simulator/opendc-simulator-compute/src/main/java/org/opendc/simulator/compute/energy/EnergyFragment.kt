package org.opendc.simulator.compute.energy

import org.opendc.common.units.Power

public class EnergyFragment(
    public var startTime: Long = 0,
    public var endTime: Long = 0,
    energy: Double = 0.0
) {
    public var energy: Power = Power.ofWatts(energy)
}
