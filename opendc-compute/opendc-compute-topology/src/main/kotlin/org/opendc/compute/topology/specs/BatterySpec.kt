package org.opendc.compute.topology.specs

public data class BatterySpec(
    public val capacity: Double,
    public val chargeEfficiency: Double,
    public val maxChargeRate: Double,
    public val arch: String,
)
