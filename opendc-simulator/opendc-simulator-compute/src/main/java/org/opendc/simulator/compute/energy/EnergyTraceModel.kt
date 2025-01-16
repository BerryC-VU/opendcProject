package org.opendc.simulator.compute.energy

import org.opendc.simulator.compute.power.CarbonFragment

public class EnergyTraceModel(
    private val energyType: String,
    private val energyFragments: List<EnergyFragment>,
    private val carbonFragments: List<CarbonFragment>?,
    private val startTime: Long = 0L
): IEnergySupplier {

    private var currentEnergyFragment: EnergyFragment? = null
    private var energyFragmentIndex = 0
    private var currentCarbonFragment: CarbonFragment? = null
    private var carbonFragmentIndex = 0

    public var energyManager: IEnergyManager? = null

    /**
     * Convert the given relative time to the absolute time by adding the start of workload
     */
    private fun getAbsoluteTime(time: Long): Long {
        return time + startTime
    }

    /**
     * Convert the given absolute time to the relative time by subtracting the start of workload
     */
    private fun getRelativeTime(time: Long): Long {
        return time - startTime
    }

    /**
     * Traverse the fragments to find the fragment that matches the given absoluteTime
     */
    private fun findCorrectEnergyFragment(absoluteTime: Long) {
        // Traverse to the previous fragment, until you reach the correct fragment
        val fragment = currentEnergyFragment ?: return
        while (absoluteTime < fragment.startTime) {
            this.currentEnergyFragment = energyFragments[--this.energyFragmentIndex]
        }

        // Traverse to the next fragment, until you reach the correct fragment
        while (absoluteTime >= fragment.endTime) {
            this.currentEnergyFragment = energyFragments[++this.energyFragmentIndex]
        }
    }

    /**
     * Traverse the fragments to find the fragment that matches the given absoluteTime
     */
    private fun findCorrectCarbonFragment(absoluteTime: Long) {
        // Traverse to the previous fragment, until you reach the correct fragment
        carbonFragments ?: return
        val fragment = currentCarbonFragment ?: return
        while (absoluteTime < fragment.startTime) {
            this.currentCarbonFragment = carbonFragments[--this.carbonFragmentIndex]
        }

        // Traverse to the next fragment, until you reach the correct fragment
        while (absoluteTime >= fragment.endTime) {
            this.currentCarbonFragment = carbonFragments[++this.carbonFragmentIndex]
        }
    }

    override fun supply(now: Long): Double {
        val absolute_time = getAbsoluteTime(now)
        val fragment = currentEnergyFragment ?: return 0.0
        // Check if the current fragment is still the correct fragment,
        // Otherwise, find the correct fragment.
        val energySupply = if ((absolute_time < fragment.startTime) || (absolute_time >= fragment.endTime)) {
            this.findCorrectEnergyFragment(absolute_time)
            fragment.energy.toWatts()
        } else {
            return 0.0
        }

        val carbonFragment = currentCarbonFragment ?: return energySupply
        if ((absolute_time < carbonFragment.startTime) || (absolute_time >= carbonFragment.endTime)) {
            this.findCorrectCarbonFragment(absolute_time)

            energyManager?.updateCarbonIntensity(energyType(), carbonFragment.carbonIntensity)
        }

        return energySupply
    }

    override fun energyType(): String {
        return energyType
    }
}
