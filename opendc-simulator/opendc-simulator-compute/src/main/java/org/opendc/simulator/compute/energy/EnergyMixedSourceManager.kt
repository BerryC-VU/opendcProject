package org.opendc.simulator.compute.energy

import org.opendc.simulator.compute.energy.IEnergySupplier.Companion.ENERGY_CLEAN
import org.opendc.simulator.compute.energy.IEnergySupplier.Companion.ENERGY_NON_CLEAN

public class EnergyMixedSourceManager(
    private val cleanEnergySupplier: IEnergySupplier,
    private val nonCleanEnergySupplier: IEnergySupplier,
    private val battery: BatteryModel,
): IEnergyManager {

    private var cleanCarbonIntensity: Double = 0.0
    private var nonCleanCarbonIntensity: Double = 0.0


    public override fun supplyPower(time: Long, demand: Double): EnergyDetail {
        var remainingDemand = demand
        val cleanEnergy = cleanEnergySupplier.supply(time)
        val traditionalEnergy = nonCleanEnergySupplier.supply(time)
        var cleanEnergyUsed = 0.0
        var batteryDischarge = 0.0
        var nonCleanEnergyUsed = 0.0

        // Use green energy first
        if (cleanEnergy >= remainingDemand) {
            cleanEnergyUsed = remainingDemand
            remainingDemand = 0.0
        } else {
            cleanEnergyUsed = cleanEnergy
            remainingDemand -= cleanEnergy
        }

        // Use battery power
        if (remainingDemand > 0 && battery.chargeLevel.toWatts() > 0) {
            batteryDischarge = battery.discharge(remainingDemand)
            remainingDemand -= batteryDischarge
        }

        // Use traditional energy
        if (remainingDemand > 0) {
            nonCleanEnergyUsed = minOf(traditionalEnergy, remainingDemand)
            remainingDemand -= nonCleanEnergyUsed
        }

        val cleanEnergyRemain = cleanEnergy - cleanEnergyUsed
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
