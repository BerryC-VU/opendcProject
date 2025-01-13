package org.opendc.compute.topology.specs

public data class EnergySpec(
    public val type: String,

    public val supplyModel: SinusoidalEnergySupplySpec? = null,
    public val energySupplyTracePath: String? = null,

    public val carbonTracePath: String? = null,
)
