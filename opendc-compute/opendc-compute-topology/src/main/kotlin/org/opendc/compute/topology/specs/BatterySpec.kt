package org.opendc.compute.topology.specs

import org.opendc.common.units.Power
import org.opendc.compute.topology.specs.BatteryJSONSpec.Companion.BATTERY_TYPE_SMOOTH

public data class BatterySpec(
    public val type: String = BATTERY_TYPE_SMOOTH,
    public val capacity: Power,
    public val chargeEfficiency: Double,
    public val maxChargeRate: Power,
    public val initialLevel: Power,
)
