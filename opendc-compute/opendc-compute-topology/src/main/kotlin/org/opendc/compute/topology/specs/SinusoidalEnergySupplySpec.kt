package org.opendc.compute.topology.specs

public data class SinusoidalEnergySupplySpec(
    public val amplitude: Double, // 最大能源供给 (Ws)
    public val period: Double, // 周期长度 (时间步)
    public val phaseShift: Double = 0.0, // 相位偏移 (时间步)
    public val offset: Double = 0.0, // 上下偏移量 (基准供给值)
    public val interval: Int = 1 // 时间间隔 (秒)
)
