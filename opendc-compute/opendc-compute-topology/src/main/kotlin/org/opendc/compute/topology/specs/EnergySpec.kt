package org.opendc.compute.topology.specs

public data class EnergySpec(

    public val arch: String,

    public val sinusoidalSupply: SinusoidalEnergySupplySpec? = null,
    public val constantSupply: ConstantEnergySupplySpec? = null,
    public val energySupplyTracePath: String? = null,

    public val carbonTracePath: String? = null,
    )
