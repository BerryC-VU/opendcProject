package org.opendc.compute.topology.specs

import org.opendc.common.units.Power


public data class SinusoidalEnergySupplySpec(
    public val min: Power,
    public val max: Power,
    public val period: Double,
    public val phaseShift: Double,
)
