package org.opendc.simulator.compute.energy

public interface IEnergyManager {
    public fun supplyPower(time: Long, demand: Double): EnergyDetail

    public fun updateCarbonIntensity(type: String, carbonIntensity: Double)
}

public data class EnergyDetail(
    public val cleanEnergyUsage: Double,
    public val nonCleanEnergyUsage: Double,
    public val batteryDischarge: Double,
    public val cleanEnergyCarbonIntensity: Double,
    public val nonCleanEnergyCarbonIntensity: Double,
)

