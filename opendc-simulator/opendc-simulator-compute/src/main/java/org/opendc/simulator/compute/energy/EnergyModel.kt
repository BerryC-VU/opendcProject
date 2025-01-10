package org.opendc.simulator.compute.energy

public class EnergyModel(
    public val greenEnergyProfile: ArrayList<Double>,   // Green energy supply over time (Ws)
    simulationSteps: Int
) {
    private val traditionalEnergyProfile: List<Double> = List(simulationSteps) { 9999.0 }
    public var currentTime: Int = 0
        private set

    public fun getGreenEnergy(): Double {
        return greenEnergyProfile[currentTime % greenEnergyProfile.size]
    }

    public fun getTraditionalEnergy(): Double {
        return traditionalEnergyProfile[currentTime % traditionalEnergyProfile.size]
    }

    public fun advanceTime() {
        currentTime += 1
    }
}
