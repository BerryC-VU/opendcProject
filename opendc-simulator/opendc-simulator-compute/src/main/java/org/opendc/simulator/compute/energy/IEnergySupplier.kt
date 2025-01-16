package org.opendc.simulator.compute.energy

public interface IEnergySupplier {

    public fun supply(now: Long): Double

    public fun energyType(): String

    public companion object {
        public val ENERGY_CLEAN: String = "clean"
        public val ENERGY_NON_CLEAN: String = "non_clean"
    }
}
