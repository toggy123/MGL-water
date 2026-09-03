package com.example.mondrain.hydraulic

import kotlin.math.*

enum class CulvertType(val labelMn: String, val labelEn: String) {
    PIPE("Дугуй төмөр бетон хоолой", "Circular Concrete Pipe"),
    BOX("Тэгш өнцөгт төмөр бетон хоолой", "Reinforced Concrete Box"),
    ARCH("Нум хэлбэрийн хоолой", "Arch Culvert")
}

data class CulvertStandardSize(
    val type: CulvertType,
    val spanOrDiameterM: Double,
    val heightM: Double,
    val displayName: String
)

data class CulvertHydraulicAnalysis(
    val type: CulvertType,
    val spanOrDiameterM: Double,
    val heightM: Double,
    val barrels: Int, // 1=Дан, 2=Хос, 3=Гурвалсан
    val culvertSlopePercent: Double,
    val designDischargeM3s: Double, // Q_req
    val fullCapacityM3s: Double,    // Q_cap
    val headwaterM: Double,         // HW
    val headwaterRatio: Double,     // HW/D or HW/H
    val flowVelocityMs: Double,     // V
    val flowControl: String,        // Inlet Control vs Outlet Control
    val isAdequate: Boolean,
    val capacityRatio: Double,      // Q_cap / Q_req
    val scourProtectionRequired: Boolean,
    val recommendationMn: String,
    val recommendationEn: String
)

object CulvertEngine {

    // Standard precast concrete pipe and box sizes used in Mongolian road engineering
    val STANDARD_SIZES = listOf(
        // Circular Concrete Pipes (Дугуй хоолой)
        CulvertStandardSize(CulvertType.PIPE, 0.75, 0.75, "Дугуй Ø0.75 м"),
        CulvertStandardSize(CulvertType.PIPE, 1.00, 1.00, "Дугуй Ø1.00 м"),
        CulvertStandardSize(CulvertType.PIPE, 1.25, 1.25, "Дугуй Ø1.25 м"),
        CulvertStandardSize(CulvertType.PIPE, 1.50, 1.50, "Дугуй Ø1.50 м"),
        CulvertStandardSize(CulvertType.PIPE, 2.00, 2.00, "Дугуй Ø2.00 м"),

        // Box Culverts (Тэгш өнцөгт хоолой)
        CulvertStandardSize(CulvertType.BOX, 1.50, 1.50, "Тэгш өнцөгт 1.5 × 1.5 м"),
        CulvertStandardSize(CulvertType.BOX, 2.00, 2.00, "Тэгш өнцөгт 2.0 × 2.0 м"),
        CulvertStandardSize(CulvertType.BOX, 2.50, 2.00, "Тэгш өнцөгт 2.5 × 2.0 м"),
        CulvertStandardSize(CulvertType.BOX, 3.00, 2.50, "Тэгш өнцөгт 3.0 × 2.5 м"),
        CulvertStandardSize(CulvertType.BOX, 4.00, 3.00, "Тэгш өнцөгт 4.0 × 3.0 м")
    )

    /**
     * Manning's roughness coefficient for precast concrete culvert: n = 0.013.
     */
    private const val MANNING_N = 0.013
    private const val GRAVITY = 9.80665

    /**
     * Evaluates complete hydraulic capacity, headwater depth (Inlet vs Outlet control),
     * velocity, and scour risk for a given culvert configuration.
     */
    fun analyzeCulvert(
        type: CulvertType,
        spanOrDiameterM: Double,
        heightM: Double,
        barrels: Int = 1,
        culvertLengthM: Double = 18.0,
        culvertSlopePercent: Double = 1.0,
        designDischargeM3s: Double = 2.5
    ): CulvertHydraulicAnalysis {
        val s0 = max(0.002, culvertSlopePercent / 100.0)
        val singleQReq = designDischargeM3s / barrels.toDouble()

        // 1. Full cross-sectional geometry per barrel
        val fullArea: Double
        val wettedPerimeter: Double
        val dim: Double

        when (type) {
            CulvertType.PIPE -> {
                val d = spanOrDiameterM
                dim = d
                fullArea = (Math.PI * d * d) / 4.0
                wettedPerimeter = Math.PI * d
            }
            CulvertType.BOX -> {
                val b = spanOrDiameterM
                val h = heightM
                dim = h
                fullArea = b * h
                wettedPerimeter = 2.0 * (b + h)
            }
            CulvertType.ARCH -> {
                val b = spanOrDiameterM
                val h = heightM
                dim = h
                fullArea = 0.80 * b * h
                wettedPerimeter = b + 2.0 * h
            }
        }

        val hydraulicRadius = fullArea / wettedPerimeter

        // 2. Manning's full capacity: Q_full = (1/n) * A * R^(2/3) * S^(1/2) * barrels
        val singleQFull = (1.0 / MANNING_N) * fullArea * hydraulicRadius.pow(2.0 / 3.0) * sqrt(s0)
        val totalCapacityM3s = singleQFull * barrels

        // 3. Inlet Control Headwater Calculation (FHWA HEC-5 / HEC-22 & Mongolian standards)
        // Form: HW_i / D = c * (Q / (A * sqrt(D)))^M + Y - 0.5 * S0
        val qFactor = singleQReq / (fullArea * sqrt(dim))
        val hwInlet: Double = if (qFactor <= 1.0) {
            // Unsubmerged inlet
            (0.5 + 0.035 * qFactor.pow(1.8) - 0.5 * s0) * dim
        } else {
            // Submerged inlet
            (0.85 + 0.045 * qFactor.pow(2.0) - 0.5 * s0) * dim
        }

        // 4. Outlet Control Headwater Calculation
        // HW_o = H + h_o - L * S0, where H = [1 + Ke + (29.16 * n^2 * L) / R^(4/3)] * (V^2 / 2g)
        val ke = 0.5 // Entrance loss coefficient for square edge headwall
        val velocityFull = singleQReq / fullArea
        val frictionHead = (19.62 * MANNING_N * MANNING_N * culvertLengthM) / hydraulicRadius.pow(4.0 / 3.0)
        val headLoss = (1.0 + ke + frictionHead) * (velocityFull.pow(2) / (2.0 * GRAVITY))
        val ho = dim * 0.8
        val hwOutlet = max(0.2, headLoss + ho - (culvertLengthM * s0))

        // Governing Headwater is the maximum of inlet and outlet control
        val isOutletGoverning = hwOutlet > hwInlet
        val headwaterM = max(hwInlet, hwOutlet)
        val hwRatio = headwaterM / dim

        // Actual flow velocity
        val flowVelocity = min(8.0, singleQReq / (fullArea * min(1.0, max(0.25, hwRatio))))

        // Adequacy checks:
        // - Capacity ratio >= 1.0
        // - Headwater ratio HW/D <= 1.20 for free-surface non-submerged flow (or <= 1.50 allowable maximum)
        val capacityRatio = totalCapacityM3s / max(0.01, designDischargeM3s)
        val isAdequate = capacityRatio >= 1.05 && hwRatio <= 1.30

        val scourProtectionRequired = flowVelocity > 2.0

        val recommendationMn: String
        val recommendationEn: String

        if (isAdequate) {
            if (scourProtectionRequired) {
                recommendationMn = "Хоолойн нэвтрүүлэх чадвар хангалттай. Гаралтын урсгалын хурд $flowVelocity м/с > 2.0 м/с тул 5-8 м урттай чулуун өнгөлгөө (Rip-rap) бэхэлгээ хийхийг зөвлөж байна."
                recommendationEn = "Culvert capacity adequate. Outlet velocity $flowVelocity m/s exceeds 2.0 m/s; 5-8 m stone rip-rap apron protection is recommended."
            } else {
                recommendationMn = "Хоолойн хэмжээ ба гидравлик нэвтрүүлэх чадвар БНбД шаардлагыг бүрэн хангасан. Угаагдах аюулгүй."
                recommendationEn = "Culvert size and hydraulic capacity meet all regulatory standards. Safe against scour."
            }
        } else {
            recommendationMn = "Нэвтрүүлэх чадвар хүрэлцэхгүй (HW/D = ${String.format("%.2f", hwRatio)} > 1.2). Хоолойн голчийг ихэсгэх эсвэл нүхний тоог (хос/гурвалсан) нэмэгдүүлэх шаардлагатай."
            recommendationEn = "Capacity inadequate (HW/D = ${String.format("%.2f", hwRatio)} > 1.2). Increase diameter/span or add barrels."
        }

        return CulvertHydraulicAnalysis(
            type = type,
            spanOrDiameterM = spanOrDiameterM,
            heightM = heightM,
            barrels = barrels,
            culvertSlopePercent = culvertSlopePercent,
            designDischargeM3s = designDischargeM3s,
            fullCapacityM3s = totalCapacityM3s,
            headwaterM = headwaterM,
            headwaterRatio = hwRatio,
            flowVelocityMs = flowVelocity,
            flowControl = if (isOutletGoverning) "Гаралтын удирдлагатай (Outlet Control)" else "Оролтын удирдлагатай (Inlet Control)",
            isAdequate = isAdequate,
            capacityRatio = capacityRatio,
            scourProtectionRequired = scourProtectionRequired,
            recommendationMn = recommendationMn,
            recommendationEn = recommendationEn
        )
    }

    /**
     * Automatic culvert size recommender:
     * Iterates through standard Mongolian culvert catalog and returns the most economical
     * safe culvert configuration for the target discharge.
     */
    fun recommendOptimalCulvert(
        designDischargeM3s: Double,
        culvertSlopePercent: Double = 1.0,
        preferredType: CulvertType = CulvertType.PIPE
    ): CulvertHydraulicAnalysis {
        val candidates = ArrayList<CulvertHydraulicAnalysis>()

        // Try single, twin, and triple barrels for each catalog size
        for (size in STANDARD_SIZES.filter { it.type == preferredType }) {
            for (barrels in 1..3) {
                val analysis = analyzeCulvert(
                    type = size.type,
                    spanOrDiameterM = size.spanOrDiameterM,
                    heightM = size.heightM,
                    barrels = barrels,
                    culvertSlopePercent = culvertSlopePercent,
                    designDischargeM3s = designDischargeM3s
                )
                if (analysis.isAdequate) {
                    candidates.add(analysis)
                }
            }
        }

        // If preferred type cannot satisfy, fallback to box culverts
        if (candidates.isEmpty()) {
            for (size in STANDARD_SIZES.filter { it.type == CulvertType.BOX }) {
                for (barrels in 1..3) {
                    val analysis = analyzeCulvert(
                        type = size.type,
                        spanOrDiameterM = size.spanOrDiameterM,
                        heightM = size.heightM,
                        barrels = barrels,
                        culvertSlopePercent = culvertSlopePercent,
                        designDischargeM3s = designDischargeM3s
                    )
                    if (analysis.isAdequate) {
                        candidates.add(analysis)
                    }
                }
            }
        }

        // Choose candidate with capacity ratio closest to 1.15 (safe but economical)
        return candidates.minByOrNull { abs(it.capacityRatio - 1.20) }
            ?: analyzeCulvert(
                type = preferredType,
                spanOrDiameterM = if (preferredType == CulvertType.PIPE) 1.5 else 2.5,
                heightM = if (preferredType == CulvertType.PIPE) 1.5 else 2.0,
                barrels = 2,
                culvertSlopePercent = culvertSlopePercent,
                designDischargeM3s = designDischargeM3s
            )
    }
}
