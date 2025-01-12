package org.opendc.simulator.compute.energy

public class PowerManagerSingleSupplier(
    private val energyModel: EnergyModel,
    private val battery: BatteryModel
) {
    public fun supplyPower(demand: Double): Triple<Double, Double, Double> {
        val greenEnergy = energyModel.getGreenEnergy()
        val traditionalEnergy = energyModel.getTraditionalEnergy()
        battery.idle()

        var cleanEnergyUsed = 0.0
        var batteryDischarge = 0.0
        var nonCleanEnergyUsed = 0.0

        if (greenEnergy >= demand) {
            cleanEnergyUsed = demand
            energyModel.greenEnergyProfile[energyModel.currentTime % energyModel.greenEnergyProfile.size] -= cleanEnergyUsed
        } else if (battery.chargeLevel >= demand) {
            batteryDischarge = battery.discharge(demand)
        } else {
            nonCleanEnergyUsed = demand.coerceAtMost(traditionalEnergy)
        }

        return Triple(cleanEnergyUsed / 1000, nonCleanEnergyUsed / 1000, batteryDischarge / 1000) // Convert to kW
    }

    public fun manageBattery() {
        if (battery.state == "discharging") {
            energyModel.advanceTime()
            return
        }

        val greenEnergy = energyModel.getGreenEnergy()
        if (greenEnergy > 0 && battery.chargeLevel < battery.capacity) {
            val usedPower = battery.charge(greenEnergy)
            energyModel.greenEnergyProfile[energyModel.currentTime % energyModel.greenEnergyProfile.size] -= usedPower
        }

        if (battery.chargeLevel == battery.capacity) {
            battery.idle()
        }

        energyModel.advanceTime()
    }
}
