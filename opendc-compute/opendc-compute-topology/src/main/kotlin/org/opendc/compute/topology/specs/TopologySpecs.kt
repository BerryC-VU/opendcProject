/*
 * Copyright (c) 2024 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.compute.topology.specs

import kotlinx.serialization.Serializable
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.Power
import org.opendc.compute.topology.specs.BatteryJSONSpec.Companion.BATTERY_DEFAULT
import org.opendc.compute.topology.specs.EnergyJSONSpec.Companion.ENERGY_CLEAN_DEFAULT
import org.opendc.compute.topology.specs.EnergyJSONSpec.Companion.ENERGY_NON_CLEAN_DEFAULT
import org.opendc.compute.topology.specs.SinusoidalEnergySupplyJSONSpec.Companion.ENERGY_SUPPLY_DEFAULT
import org.opendc.compute.topology.specs.SinusoidalEnergySupplyJSONSpec.Companion.ENERGY_SUPPLY_MAXIMUM

/**
 * Definition of a Topology modeled in the simulation.
 *
 * @param clusters List of the clusters in this topology
 */
@Serializable
public data class TopologySpec(
    val clusters: List<ClusterJSONSpec>,
    val schemaVersion: Int = 1,
)

/**
 * Definition of a compute cluster modeled in the simulation.
 *
 * @param name The name of the cluster.
 * @param hosts List of the different hosts (nodes) available in this cluster
 * @param location Location of the cluster. This can impact the carbon intensity
 */
@Serializable
public data class ClusterJSONSpec(
    val name: String = "Cluster",
    val count: Int = 1,
    val hosts: List<HostJSONSpec>,
    val powerSource: PowerSourceJSONSpec = PowerSourceJSONSpec.DFLT,
    val location: String = "NL",
)

/**
 * Definition of a compute host modeled in the simulation.
 *
 * @param name The name of the host.
 * @param cpu The CPU available in this cluster
 * @param memory The amount of RAM memory available in Byte
 * @param powerModel The power model used to determine the power draw of a host
 * @param count The power model used to determine the power draw of a host
 */
@Serializable
public data class HostJSONSpec(
    val name: String? = null,
    val cpu: CPUJSONSpec,
    val memory: MemoryJSONSpec,
    val powerModel: PowerModelSpec = PowerModelSpec.DFLT,
    val count: Int = 1,
)

/**
 * Definition of a compute CPU modeled in the simulation.
 *
 * @param vendor The vendor of the storage device.
 * @param modelName The model name of the device.
 * @param arch The micro-architecture of the processor node.
 * @param coreCount The number of cores in the CPU
 * @param coreSpeed The speed of the cores
 */
@Serializable
public data class CPUJSONSpec(
    val vendor: String = "unknown",
    val modelName: String = "unknown",
    val arch: String = "unknown",
    val coreCount: Int,
    val coreSpeed: Frequency,
    val count: Int = 1,
)

/**
 * Definition of a compute Memory modeled in the simulation.
 *
 * @param vendor The vendor of the storage device.
 * @param modelName The model name of the device.
 * @param arch The micro-architecture of the processor node.
 * @param memorySpeed The speed of the cores
 * @param memorySize The size of the memory Unit
 */
@Serializable
public data class MemoryJSONSpec(
    val vendor: String = "unknown",
    val modelName: String = "unknown",
    val arch: String = "unknown",
    val memorySpeed: Frequency = Frequency.ofMHz(-1),
    val memorySize: DataSize,
)

@Serializable
public data class PowerModelSpec(
    val modelType: String,
    val power: Power = Power.ofWatts(400),
    val maxPower: Power,
    val idlePower: Power,
    val carbonTracePaths: String? = null,
) {
    init {
        require(maxPower >= idlePower) { "The max power of a power model can not be less than the idle power" }
    }

    public companion object {
        public val DFLT: PowerModelSpec =
            PowerModelSpec(
                modelType = "linear",
                power = Power.ofWatts(350),
                maxPower = Power.ofWatts(400.0),
                idlePower = Power.ofWatts(200.0),
            )
    }
}

/**
 * Definition of a power source used for JSON input.
 *
 * @property vendor
 * @property modelName
 * @property arch
 * @property totalPower
 */
@Serializable
public data class PowerSourceJSONSpec(
    val vendor: String = "unknown",
    val modelName: String = "unknown",
    val arch: String = "unknown",
    val totalPower: Long = Long.MAX_VALUE,
    val carbonTracePath: String? = null,
    val battery: BatteryJSONSpec? = BATTERY_DEFAULT,
    val cleanEnergy: EnergyJSONSpec? = ENERGY_CLEAN_DEFAULT,
    val nonCleanEnergy: EnergyJSONSpec? = ENERGY_NON_CLEAN_DEFAULT,
) {
    public companion object {
        public val DFLT: PowerSourceJSONSpec =
            PowerSourceJSONSpec(
                totalPower = Long.MAX_VALUE,
            )
    }
}

@Serializable
public data class BatteryJSONSpec(
    public val type: String = BATTERY_TYPE_SMOOTH,
    public val capacity: Power,
    public val chargeEfficiency: Double,
    public val maxChargeRate: Power,
    public val initialLevel: Power,
) {
    public companion object {

        public val BATTERY_TYPE_SMOOTH: String = "Smooth"
        public val BATTERY_TYPE_HARD: String = "Hard"

        public val BATTERY_DEFAULT: BatteryJSONSpec =
            BatteryJSONSpec(
                type = BATTERY_TYPE_SMOOTH,
                capacity = Power.ofKWatts(50*3600),
                chargeEfficiency = 0.9,
                maxChargeRate = Power.ofKWatts(10*3600),
                initialLevel = Power.ofKWatts(50*3600)
            )
    }
}

@Serializable
public data class EnergyJSONSpec(
    public val type: String = ENERGY_CONSTANT,

    public val supplyModel: SinusoidalEnergySupplyJSONSpec? = ENERGY_SUPPLY_MAXIMUM,
    public val energySupplyTracePath: String? = null,

    public val carbonTracePath: String? = null,
) {
    public companion object {
        public val ENERGY_CONSTANT: String = "Constant"
        public val ENERGY_SINUSOIDAL: String = "Sinusoidal"
        public val ENERGY_TRACE: String = "Traces"

        public val ENERGY_CLEAN_DEFAULT: EnergyJSONSpec =
            EnergyJSONSpec(
                type = ENERGY_SINUSOIDAL,
                supplyModel = ENERGY_SUPPLY_DEFAULT

            )

        public val ENERGY_NON_CLEAN_DEFAULT: EnergyJSONSpec =
            EnergyJSONSpec(
                type = ENERGY_CONSTANT,
                supplyModel = ENERGY_SUPPLY_MAXIMUM
            )
    }
}

@Serializable
public data class SinusoidalEnergySupplyJSONSpec(
    public val amplitude: Long,
    public val period: Long,
    public val phaseShift: Long,
    public val offset: Long,
) {
    public companion object {
        public val ENERGY_SUPPLY_MAXIMUM: SinusoidalEnergySupplyJSONSpec =
            SinusoidalEnergySupplyJSONSpec(
                amplitude = 0,
                period = 24,
                phaseShift = 0,
                offset = Long.MAX_VALUE
            )

        public val ENERGY_SUPPLY_DEFAULT: SinusoidalEnergySupplyJSONSpec =
            SinusoidalEnergySupplyJSONSpec(
                amplitude = 2000,
                period = 24,
                phaseShift = 0,
                offset = 2000
            )
    }
}
