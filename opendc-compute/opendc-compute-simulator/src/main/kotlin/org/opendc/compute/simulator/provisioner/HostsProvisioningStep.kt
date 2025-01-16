/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.compute.simulator.provisioner

import org.opendc.compute.carbon.getCarbonFragments
import org.opendc.compute.carbon.getEnergyFragments
import org.opendc.compute.simulator.host.SimHost
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.compute.topology.specs.EnergyJSONSpec.Companion.ENERGY_FUNCTION
import org.opendc.compute.topology.specs.EnergyJSONSpec.Companion.ENERGY_TRACE
import org.opendc.compute.topology.specs.HostSpec
import org.opendc.compute.topology.specs.PowerSourceSpec
import org.opendc.compute.topology.specs.PowerSourceSpec.Companion.ENERGY_MIX_MANAGER
import org.opendc.compute.topology.specs.PowerSourceSpec.Companion.ENERGY_SINGLE_MANAGER
import org.opendc.simulator.compute.energy.BatteryModel
import org.opendc.simulator.compute.energy.EnergyFunctionModel
import org.opendc.simulator.compute.energy.EnergyMixedSourceManager
import org.opendc.simulator.compute.energy.EnergySingleSourceManager
import org.opendc.simulator.compute.energy.EnergyTraceModel
import org.opendc.simulator.compute.energy.IEnergyManager
import org.opendc.simulator.compute.energy.IEnergySupplier.Companion.ENERGY_CLEAN
import org.opendc.simulator.compute.energy.IEnergySupplier.Companion.ENERGY_NON_CLEAN
import org.opendc.simulator.compute.energy.SinusoidalEnergySupplyModel
import org.opendc.simulator.compute.power.SimPowerSource
import org.opendc.simulator.engine.engine.FlowEngine
import org.opendc.simulator.engine.graph.FlowDistributor

/**
 * A [ProvisioningStep] that provisions a list of hosts for a [ComputeService].
 *
 * @param serviceDomain The domain name under which the compute service is registered.
 * @param specs A list of [HostSpec] objects describing the simulated hosts to provision.
 * @param optimize A flag to indicate that the CPU resources of the host should be merged into a single CPU resource.
 */
public class HostsProvisioningStep internal constructor(
    private val serviceDomain: String,
    private val clusterSpecs: List<ClusterSpec>,
    private val startTime: Long = 0L,
) : ProvisioningStep {
    override fun apply(ctx: ProvisioningContext): AutoCloseable {
        val service =
            requireNotNull(
                ctx.registry.resolve(serviceDomain, ComputeService::class.java),
            ) { "Compute service $serviceDomain does not exist" }
        val simHosts = mutableSetOf<SimHost>()
        val simPowerSources = mutableListOf<SimPowerSource>()

        val engine = FlowEngine.create(ctx.dispatcher)
        val graph = engine.newGraph()

        for (cluster in clusterSpecs) {
            // Create the Power Source to which hosts are connected

            val carbonFragments = getCarbonFragments(cluster.powerSource.carbonTracePath)
            val energyManager = genEnergyManager(cluster.powerSource)
            val simPowerSource = SimPowerSource(graph, cluster.powerSource.totalPower.toDouble(), carbonFragments,
                startTime, energyManager)

            service.addPowerSource(simPowerSource)
            simPowerSources.add(simPowerSource)

            val powerMux = FlowDistributor(graph)
            graph.addEdge(powerMux, simPowerSource)

            // Create hosts, they are connected to the powerMux when SimMachine is created
            for (hostSpec in cluster.hostSpecs) {
                val simHost =
                    SimHost(
                        hostSpec.uid,
                        hostSpec.name,
                        hostSpec.meta,
                        ctx.dispatcher.timeSource,
                        graph,
                        hostSpec.model,
                        hostSpec.cpuPowerModel,
                        powerMux,
                    )

                require(simHosts.add(simHost)) { "Host with uid ${hostSpec.uid} already exists" }
                service.addHost(simHost)
            }
        }

        return AutoCloseable {
            for (simHost in simHosts) {
                simHost.close()
            }

            for (simPowerSource in simPowerSources) {
                // TODO: add close function
                simPowerSource.close()
            }
        }
    }

    private fun genEnergyManager(powerSource: PowerSourceSpec): IEnergyManager? {
        val batterySpec = powerSource.battery
        val cleanEnergySpec = powerSource.cleanEnergy
        val nonCleanEnergySpec = powerSource.nonCleanEnergy
        if (batterySpec != null && cleanEnergySpec != null
            && nonCleanEnergySpec != null) {

            val batteryModel = BatteryModel(
                type = batterySpec.type,
                capacity = batterySpec.capacity,
                chargeEfficiency = batterySpec.chargeEfficiency,
                maxChargeRate = batterySpec.maxChargeRate
            )

            val cleanEnergySupplier = if (cleanEnergySpec.type == ENERGY_FUNCTION) {
                val supplySpec = cleanEnergySpec.supplyModel ?: return null
                val carbonFragments = getCarbonFragments(cleanEnergySpec.carbonTracePath)
                EnergyFunctionModel(
                    ENERGY_CLEAN,
                    SinusoidalEnergySupplyModel(
                        supplySpec.min,
                        supplySpec.max,
                        supplySpec.period,
                        supplySpec.phaseShift
                    ),
                    carbonFragments,
                    startTime
                )
            } else if (cleanEnergySpec.type == ENERGY_TRACE) {
                val traceSpec = cleanEnergySpec.energySupplyTracePath
                val energyFragments = getEnergyFragments(traceSpec) ?: return null
                val carbonFragments = getCarbonFragments(cleanEnergySpec.carbonTracePath)
                EnergyTraceModel(
                    ENERGY_CLEAN,
                    energyFragments,
                    carbonFragments,
                    startTime
                )
            } else {
                return null
            }

            val nonCleanEnergySupplier = if (nonCleanEnergySpec.type == ENERGY_FUNCTION) {
                val supplySpec = nonCleanEnergySpec.supplyModel ?: return null
                val carbonFragments = getCarbonFragments(nonCleanEnergySpec.carbonTracePath)
                EnergyFunctionModel(
                    ENERGY_NON_CLEAN,
                    SinusoidalEnergySupplyModel(
                        supplySpec.min,
                        supplySpec.max,
                        supplySpec.period,
                        supplySpec.phaseShift
                    ),
                    carbonFragments,
                    startTime
                )
            } else if (nonCleanEnergySpec.type == ENERGY_TRACE) {
                val traceSpec = nonCleanEnergySpec.energySupplyTracePath
                val energyFragments = getEnergyFragments(traceSpec) ?: return null
                val carbonFragments = getCarbonFragments(nonCleanEnergySpec.carbonTracePath)
                EnergyTraceModel(
                    ENERGY_NON_CLEAN,
                    energyFragments,
                    carbonFragments,
                    startTime
                )
            } else {
                return null
            }

            val energyManager = if (powerSource.energyManager == ENERGY_MIX_MANAGER) {
                EnergyMixedSourceManager(
                    cleanEnergySupplier,
                    nonCleanEnergySupplier,
                    batteryModel
                )
            } else if (powerSource.energyManager == ENERGY_SINGLE_MANAGER) {
                EnergySingleSourceManager(
                    cleanEnergySupplier,
                    nonCleanEnergySupplier,
                    batteryModel
                )
            } else {
                null
            }
            return energyManager
        }
        return null
    }
}
