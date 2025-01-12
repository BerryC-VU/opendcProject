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

package org.opendc.simulator.compute.power;

import java.util.ArrayList;
import java.util.List;

import kotlin.Triple;
import org.opendc.simulator.compute.cpu.SimCpu;
import org.opendc.simulator.compute.energy.BatteryModel;
import org.opendc.simulator.compute.energy.EnergyModel;
import org.opendc.simulator.compute.energy.EnergyUtils;
import org.opendc.simulator.compute.energy.PowerManager;
import org.opendc.simulator.compute.energy.PowerManagerSingleSupplier;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowGraph;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;

/**
 * A {@link SimPsu} implementation that estimates the power consumption based on CPU usage.
 */
public final class SimPowerSource extends FlowNode implements FlowSupplier {
    private long lastUpdate;

    private double powerDemand = 0.0f;
    private double powerSupplied = 0.0f;
    private double totalEnergyUsage = 0.0f;
    private double totalCleanEnergyUsage = 0.0f;
    private double totalNonCleanEnergyUsage = 0.0f;
    private double totalBatteryEnergyUsage = 0.0f;

    private double carbonIntensity = 0.0f;
    private double totalCarbonEmission = 0.0f;

    private CarbonModel carbonModel = null;
    private FlowEdge muxEdge;
    private final BatteryModel battery;
    private final EnergyModel energyModel;
    private final PowerManager powerManager;
    private final PowerManagerSingleSupplier powerManagerSingleSupplier;

    private int powerPolicy = 0;

    private double capacity = Long.MAX_VALUE;


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Determine whether the InPort is connected to a {@link SimCpu}.
     *
     * @return <code>true</code> if the InPort is connected to an OutPort, <code>false</code> otherwise.
     */
    public boolean isConnected() {
        return muxEdge != null;
    }

    /**
     * Return the power demand of the machine (in W) measured in the PSU.
     * <p>
     * This method provides access to the power consumption of the machine before PSU losses are applied.
     */
    public double getPowerDemand() {
        return this.powerDemand;
    }

    /**
     * Return the instantaneous power usage of the machine (in W) measured at the InPort of the power supply.
     */
    public double getPowerDraw() {
        return this.powerSupplied;
    }

    public double getCarbonIntensity() {
        return this.carbonIntensity;
    }

    /**
     * Return the cumulated energy usage of the machine (in J) measured at the InPort of the powers supply.
     */
    public double getEnergyUsage() {
        return totalEnergyUsage;
    }

    public double getTotalBatteryEnergyUsage() {
        return totalBatteryEnergyUsage;
    }

    public double getTotalCleanEnergyUsage() {
        return totalCleanEnergyUsage;
    }

    public double getTotalNonCleanEnergyUsage() {
        return totalNonCleanEnergyUsage;
    }

    public double getCarbonEmission() {
        return this.totalCarbonEmission;
    }

    @Override
    public double getCapacity() {
        return this.capacity;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public SimPowerSource(FlowGraph graph, double max_capacity, List<CarbonFragment> carbonFragments, long startTime) {
        super(graph);

        this.capacity = max_capacity;

        if (carbonFragments != null) {
            this.carbonModel = new CarbonModel(graph, this, carbonFragments, startTime);
        }
        lastUpdate = this.clock.millis();

        ArrayList<Double> greenEnergyProfile = EnergyUtils.generateGreenEnergyProfile(1000, 5000.0);

        this.battery = new BatteryModel(50000.0,0.9, 10000.0);
        this.energyModel = new EnergyModel(greenEnergyProfile, 1000);
        this.powerManager = new PowerManager(energyModel, battery);
        this.powerManagerSingleSupplier = new PowerManagerSingleSupplier(energyModel, battery);
    }

    public void close() {
        if (this.carbonModel != null) {
            this.carbonModel.close();
            this.carbonModel = null;
        }

        this.closeNode();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowNode related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public long onUpdate(long now) {

        return Long.MAX_VALUE;
    }

    public void updateCounters() {
        updateCounters(clock.millis());
    }

    private Triple<Double, Double, Double> energySupply(double energyUsage) {
        Triple<Double, Double, Double> result;
        if (powerPolicy == -1) {
            result = new Triple<>(0., energyUsage, 0.);
        } else if (powerPolicy == 0) {
            result = powerManager.supplyPower(energyUsage);
        } else {
            result = powerManagerSingleSupplier.supplyPower(energyUsage);
        }
        return result;
    }

    /**
     * Calculate the energy usage up until <code>now</code>.
     */
    public void updateCounters(long now) {
        long lastUpdate = this.lastUpdate;
        this.lastUpdate = now;

        long duration = now - lastUpdate;
        if (duration > 0) {
            double energyUsage = (this.powerSupplied * duration * 0.001);
            Triple<Double, Double, Double> batterySupply = energySupply(energyUsage);
            double cleanEnergy = batterySupply.getFirst();
            double nonCleanEnergyUsage = batterySupply.getSecond();
            double batteryEnergyUsage = batterySupply.getThird();
            // Compute the energy usage of the machine
            this.totalEnergyUsage += energyUsage;
            this.totalCleanEnergyUsage += cleanEnergy;
            this.totalNonCleanEnergyUsage += nonCleanEnergyUsage;
            this.totalBatteryEnergyUsage += batteryEnergyUsage;
            this.totalCarbonEmission += this.carbonIntensity * (nonCleanEnergyUsage / 3600000.0);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowGraph Related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void handleDemand(FlowEdge consumerEdge, double newPowerDemand) {
        this.powerDemand = newPowerDemand;

        double powerSupply = this.powerDemand;

        if (powerSupply != this.powerSupplied) {
            this.pushSupply(this.muxEdge, powerSupply);
        }
    }

    @Override
    public void pushSupply(FlowEdge consumerEdge, double newSupply) {
        this.powerSupplied = newSupply;
        consumerEdge.pushSupply(newSupply);
    }

    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        this.muxEdge = consumerEdge;
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        this.muxEdge = null;
    }

    // Update the carbon intensity of the power source
    public void updateCarbonIntensity(double carbonIntensity) {
        this.updateCounters();
        this.carbonIntensity = carbonIntensity;
    }
}
