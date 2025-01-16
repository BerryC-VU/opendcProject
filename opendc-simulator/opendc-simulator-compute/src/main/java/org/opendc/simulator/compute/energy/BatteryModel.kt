package org.opendc.simulator.compute.energy

import org.opendc.common.units.Power

public class BatteryModel(
    public val type: String = BATTERY_TYPE_SMOOTH,
    public val capacity: Power,        // Maximum battery capacity in Ws
    private val chargeEfficiency: Double = 0.9,
    private val maxChargeRate: Power = Power.ofKWatts(1)
) {
    public companion object {
        public val BATTERY_TYPE_SMOOTH: String = "Smooth"
        public val BATTERY_TYPE_HARD: String = "Hard"
        public val STATE_IDLE: String = "Idle"
        public val STATE_CHARGING: String = "Charging"
        public val STATE_DISCHARGING: String = "Discharging"
    }
    public var chargeLevel: Power = Power.ZERO        // Current charge level in Ws
    public var state: String = "idle"           // State: charging, discharging, idle

    public fun charge(power: Double): Double {
        if (type == BATTERY_TYPE_HARD && state == STATE_DISCHARGING) {
            return 0.0
        }

        state = STATE_CHARGING
        val chargePower = minOf(power, maxChargeRate.toWatts())
        val chargeAmount = chargePower * chargeEfficiency
        chargeLevel = Power.ofWatts(minOf(capacity.toWatts(), chargeLevel.toWatts() + chargeAmount))

        if (chargeLevel == capacity) {
            idle()
        }
        return chargePower
    }

    public fun discharge(demand: Double): Double {
        state = STATE_DISCHARGING
        val dischargeAmount = minOf(chargeLevel.toWatts(), demand)
        val remain = chargeLevel.toWatts() - dischargeAmount
        chargeLevel = Power.ofWatts(remain)
        return dischargeAmount
    }

    public fun idle() {
        state = STATE_IDLE
    }
}
