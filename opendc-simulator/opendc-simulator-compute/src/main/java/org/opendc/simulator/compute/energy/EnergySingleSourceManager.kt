package org.opendc.simulator.compute.energy

import org.opendc.simulator.compute.energy.IEnergySupplier.Companion.ENERGY_CLEAN
import org.opendc.simulator.compute.energy.IEnergySupplier.Companion.ENERGY_NON_CLEAN

public class EnergySingleSourceManager(
    private val cleanEnergySupplier: IEnergySupplier,
    private val nonCleanEnergySupplier: IEnergySupplier,
    private val battery: BatteryModel
): IEnergyManager {

    private var cleanCarbonIntensity: Double = 0.0
    private var nonCleanCarbonIntensity: Double = 0.0

    public override fun supplyPower(time: Long, demand: Double): EnergyDetail {
        val cleanEnergy = cleanEnergySupplier.supply(time)
        val nonCleanEnergy = nonCleanEnergySupplier.supply(time)
        battery.idle()

        var cleanEnergyUsed = 0.0
        var batteryDischarge = 0.0
        var nonCleanEnergyUsed = 0.0

        if (cleanEnergy >= demand) {
            cleanEnergyUsed = demand
        } else if (battery.chargeLevel.toWatts() >= demand) {
            batteryDischarge = battery.discharge(demand)
        } else {
            nonCleanEnergyUsed = demand.coerceAtMost(nonCleanEnergy)
        }
        val cleanEnergyRemain = cleanEnergy - nonCleanEnergyUsed;
        if (cleanEnergyRemain > 0 && battery.chargeLevel < battery.capacity) {
            battery.charge(cleanEnergyRemain)
        }
        return EnergyDetail(
            cleanEnergyUsed,
            nonCleanEnergyUsed,
            batteryDischarge,
            cleanCarbonIntensity,
            nonCleanCarbonIntensity,
        )
    }

    override fun updateCarbonIntensity(type: String, carbonIntensity: Double) {
        if (type == ENERGY_CLEAN) {
            cleanCarbonIntensity = carbonIntensity
        } else if (type == ENERGY_NON_CLEAN) {
            nonCleanCarbonIntensity = carbonIntensity
        }
    }
}
