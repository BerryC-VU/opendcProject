package org.opendc.compute.topology.specs

public data class SinusoidalEnergySupplySpec(
    public val amplitude: Long,
    public val period: Long,
    public val phaseShift: Long,
    public val offset: Long,
)
