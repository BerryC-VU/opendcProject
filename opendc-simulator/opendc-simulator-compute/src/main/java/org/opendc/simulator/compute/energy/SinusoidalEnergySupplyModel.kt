package org.opendc.simulator.compute.energy

import kotlin.math.PI
import kotlin.math.sin

public class SinusoidalEnergySupplyModel(
    private val amplitude: Double, // 振幅 (最大波动值)
    private val period: Double, // 周期长度 (时间步)
    private val phaseShift: Double = 0.0, // 相位偏移
    private val offset: Double = 0.0, // 上下偏移量 (基准供给值)
    private val interval: Int = 1 // 时间间隔 (秒)
) {
    private var currentTime = 0.0 // 当前时间步

    // 计算当前时间步的能源供给
    public fun getCurrentSupply(): Double {
        val angle = 2 * PI * (currentTime / period) + phaseShift
        return (amplitude * sin(angle) + offset).coerceAtLeast(0.0) // 确保供给不为负数
    }

    // 前进到下一个时间步
    public fun advanceTime() {
        currentTime += interval
    }

    // 模拟指定时间步的能源供给
    public fun simulateSteps(steps: Int) {
        repeat(steps) {
            println("Time Step ${currentTime.toInt()}: Energy Supply = ${getCurrentSupply()} Ws")
            advanceTime()
        }
    }
}

public fun main() {
    val amplitude = 600.0 // 振幅 (最大波动值)
    val period = 24.0 // 周期长度 (例如一天24小时)
    val phaseShift = -PI / 2 // 相位偏移 (从最低点开始)
    val offset = 200.0 // 基准供给值 (夜间最低供给)

    // 创建正弦函数能源供给模型 (带偏移)
    val energySupply = SinusoidalEnergySupplyModel(amplitude, period, phaseShift, offset)

    // 模拟 48 个时间步的能源供给
    energySupply.simulateSteps(48)
}
