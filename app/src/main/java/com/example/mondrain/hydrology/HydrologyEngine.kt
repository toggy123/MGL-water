package com.example.mondrain.hydrology

import com.example.mondrain.dem.ElevationGrid
import com.example.mondrain.gis.AlignmentStation
import com.example.mondrain.gis.GeoBounds
import com.example.mondrain.gis.GeoPoint
import com.example.mondrain.gis.GisEngine
import kotlin.math.*

data class CatchmentBoundary(
    val crossingId: String,
    val stationLabel: String,
    val areaKm2: Double,
    val areaHectares: Double,
    val streamLengthKm: Double,
    val avgSlopePercent: Double,
    val timeOfConcentrationMin: Double,
    val polygonPoints: List<GeoPoint>,
    val pourPoint: GeoPoint
)

data class FlowPath(
    val id: String,
    val points: List<GeoPoint>,
    val lengthMeters: Double,
    val maxAccumulation: Int
)

data class HydrologyResult(
    val areaKm2: Double,
    val runoffCoeff: Double,
    val rainfallIntensityMmHr: Double,
    val returnPeriodPercent: Double, // e.g. 1%, 2%, 3%, 5%
    val timeOfConcentrationMin: Double,
    val designDischargeM3s: Double, // Q_p
    val formulaUsed: String
)

/**
 * Hydrology Engine for Mongolian Road Drainage.
 * Implements D8 Flow Routing, Flow Accumulation, Catchment Delineation,
 * Stream-Road Crossing Detection, and Discharge Calculations (BNbD 2.01.14-83 & Rational Method).
 */
object HydrologyEngine {

    // D8 directions: E=1, SE=2, S=4, SW=8, W=16, NW=32, N=64, NE=128
    private val DR = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
    private val DC = intArrayOf(1, 1, 0, -1, -1, -1, 0, 1)
    private val D8_CODES = intArrayOf(1, 2, 4, 8, 16, 32, 64, 128)

    /**
     * Calculates D8 flow direction grid from elevation raster.
     * Each cell points to its steepest downslope neighbor.
     */
    fun computeD8FlowDirection(dem: ElevationGrid): Array<IntArray> {
        val flowDir = Array(dem.rows) { IntArray(dem.cols) { 0 } }
        val cellMetersY = dem.cellHeightDeg * 111139.0
        val cellMetersX = dem.cellWidthDeg * 111139.0 * cos(Math.toRadians(dem.bounds.centerLat))

        for (r in 0 until dem.rows) {
            for (c in 0 until dem.cols) {
                val z0 = dem.data[r][c]
                if (z0 == dem.noDataValue) continue

                var maxSlope = 0.0
                var bestDir = 0

                for (d in 0 until 8) {
                    val nr = r + DR[d]
                    val nc = c + DC[d]
                    if (nr in 0 until dem.rows && nc in 0 until dem.cols) {
                        val zNeighbor = dem.data[nr][nc]
                        if (zNeighbor != dem.noDataValue && zNeighbor < z0) {
                            val dist = if (DR[d] != 0 && DC[d] != 0) {
                                sqrt(cellMetersX * cellMetersX + cellMetersY * cellMetersY)
                            } else if (DR[d] != 0) {
                                cellMetersY
                            } else {
                                cellMetersX
                            }
                            val slope = (z0 - zNeighbor) / dist
                            if (slope > maxSlope) {
                                maxSlope = slope
                                bestDir = D8_CODES[d]
                            }
                        }
                    }
                }
                flowDir[r][c] = bestDir
            }
        }
        return flowDir
    }

    /**
     * Calculates Flow Accumulation grid: count of upslope cells draining into each cell.
     */
    fun computeFlowAccumulation(dem: ElevationGrid, flowDir: Array<IntArray>): Array<IntArray> {
        val accumulation = Array(dem.rows) { IntArray(dem.cols) { 1 } }
        val inDegree = Array(dem.rows) { IntArray(dem.cols) { 0 } }

        // Compute in-degrees
        for (r in 0 until dem.rows) {
            for (c in 0 until dem.cols) {
                val dir = flowDir[r][c]
                val idx = D8_CODES.indexOf(dir)
                if (idx >= 0) {
                    val targetR = r + DR[idx]
                    val targetC = c + DC[idx]
                    if (targetR in 0 until dem.rows && targetC in 0 until dem.cols) {
                        inDegree[targetR][targetC]++
                    }
                }
            }
        }

        // Topological sort using queue of zero in-degree cells (headwaters/ridges)
        val queue = ArrayDeque<Pair<Int, Int>>()
        for (r in 0 until dem.rows) {
            for (c in 0 until dem.cols) {
                if (inDegree[r][c] == 0) {
                    queue.add(Pair(r, c))
                }
            }
        }

        while (queue.isNotEmpty()) {
            val (currR, currC) = queue.removeFirst()
            val dir = flowDir[currR][currC]
            val idx = D8_CODES.indexOf(dir)
            if (idx >= 0) {
                val targetR = currR + DR[idx]
                val targetC = currC + DC[idx]
                if (targetR in 0 until dem.rows && targetC in 0 until dem.cols) {
                    accumulation[targetR][targetC] += accumulation[currR][currC]
                    inDegree[targetR][targetC]--
                    if (inDegree[targetR][targetC] == 0) {
                        queue.add(Pair(targetR, targetC))
                    }
                }
            }
        }

        return accumulation
    }

    /**
     * Traces drainage network stream channels where accumulation exceeds threshold.
     */
    fun extractFlowPaths(
        dem: ElevationGrid,
        flowDir: Array<IntArray>,
        accumulation: Array<IntArray>,
        thresholdCells: Int = 15
    ): List<FlowPath> {
        val paths = ArrayList<FlowPath>()
        val visited = Array(dem.rows) { BooleanArray(dem.cols) { false } }

        var pathCounter = 1
        for (r in 0 until dem.rows) {
            for (c in 0 until dem.cols) {
                if (accumulation[r][c] >= thresholdCells && !visited[r][c]) {
                    val streamPoints = ArrayList<GeoPoint>()
                    var cr = r
                    var cc = c
                    var maxAcc = accumulation[r][c]

                    while (cr in 0 until dem.rows && cc in 0 until dem.cols && !visited[cr][cc]) {
                        visited[cr][cc] = true
                        val lat = dem.bounds.maxLat - cr * (dem.bounds.latSpan / (dem.rows - 1))
                        val lon = dem.bounds.minLon + cc * (dem.bounds.lonSpan / (dem.cols - 1))
                        val elev = dem.data[cr][cc]
                        streamPoints.add(GeoPoint(lat, lon, elev))
                        maxAcc = max(maxAcc, accumulation[cr][cc])

                        val dir = flowDir[cr][cc]
                        val idx = D8_CODES.indexOf(dir)
                        if (idx >= 0) {
                            cr += DR[idx]
                            cc += DC[idx]
                        } else break
                    }

                    if (streamPoints.size >= 3) {
                        var len = 0.0
                        for (i in 1 until streamPoints.size) {
                            len += GisEngine.distanceMeters(streamPoints[i - 1], streamPoints[i])
                        }
                        paths.add(
                            FlowPath(
                                id = "FP-$pathCounter",
                                points = streamPoints,
                                lengthMeters = len,
                                maxAccumulation = maxAcc
                            )
                        )
                        pathCounter++
                    }
                }
            }
        }
        return paths
    }

    /**
     * Delineates catchment basin upslope from a pour point (culvert road crossing).
     */
    fun delineateCatchment(
        dem: ElevationGrid,
        flowDir: Array<IntArray>,
        pourPoint: GeoPoint,
        crossingId: String,
        stationLabel: String
    ): CatchmentBoundary {
        val colF = ((pourPoint.lon - dem.bounds.minLon) / dem.bounds.lonSpan * (dem.cols - 1)).toInt()
        val rowF = ((dem.bounds.maxLat - pourPoint.lat) / dem.bounds.latSpan * (dem.rows - 1)).toInt()

        val startC = colF.coerceIn(0, dem.cols - 1)
        val startR = rowF.coerceIn(0, dem.rows - 1)

        val inCatchment = Array(dem.rows) { BooleanArray(dem.cols) { false } }
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(Pair(startR, startC))
        inCatchment[startR][startC] = true

        var cellCount = 0
        var minElev = Double.MAX_VALUE
        var maxElev = -Double.MAX_VALUE

        // Backward BFS: find all upstream cells that flow into the current basin
        while (queue.isNotEmpty()) {
            val (cr, cc) = queue.removeFirst()
            cellCount++
            val elev = dem.data[cr][cc]
            if (elev != dem.noDataValue) {
                if (elev < minElev) minElev = elev
                if (elev > maxElev) maxElev = elev
            }

            for (d in 0 until 8) {
                val nr = cr + DR[d]
                val nc = cc + DC[d]
                if (nr in 0 until dem.rows && nc in 0 until dem.cols && !inCatchment[nr][nc]) {
                    // Check if neighbor (nr, nc) flows into (cr, cc)
                    val nDir = flowDir[nr][nc]
                    val nIdx = D8_CODES.indexOf(nDir)
                    if (nIdx >= 0 && (nr + DR[nIdx] == cr) && (nc + DC[nIdx] == cc)) {
                        inCatchment[nr][nc] = true
                        queue.add(Pair(nr, nc))
                    }
                }
            }
        }

        // Calculate approximate physical dimensions
        val cellAreaM2 = (dem.cellHeightDeg * 111139.0) * (dem.cellWidthDeg * 111139.0 * cos(Math.toRadians(dem.bounds.centerLat)))
        val totalAreaM2 = max(cellAreaM2 * 8.0, cellCount * cellAreaM2)
        val areaKm2 = totalAreaM2 / 1_000_000.0
        val areaHa = totalAreaM2 / 10_000.0

        // Approximate main stream length L from basin area: L ≈ 1.4 * A^0.6 (Hack's Law)
        val streamLengthKm = max(0.4, 1.4 * areaKm2.pow(0.6))

        // Average slope
        val elevDiff = max(5.0, if (maxElev > minElev) maxElev - minElev else 25.0)
        val avgSlopePercent = max(0.8, (elevDiff / (streamLengthKm * 1000.0)) * 100.0)

        // Time of concentration using Kirpich formula:
        // t_c = 0.0195 * L^0.77 * S^(-0.385), where L in meters, S in m/m
        val sMeterMeter = max(0.005, avgSlopePercent / 100.0)
        val lMeters = streamLengthKm * 1000.0
        val tcMinutes = (0.0195 * lMeters.pow(0.77) * sMeterMeter.pow(-0.385)).coerceIn(5.0, 180.0)

        // Generate polygon boundary points encircling the basin cells
        val boundaryPoints = ArrayList<GeoPoint>()
        val angles = 16
        for (i in 0 until angles) {
            val angle = i * (2.0 * Math.PI / angles)
            val radiusM = sqrt(totalAreaM2 / Math.PI) * (0.8 + 0.4 * sin(angle * 2.0).pow(2))
            val dLat = (radiusM * cos(angle)) / 111139.0
            val dLon = (radiusM * sin(angle)) / (111139.0 * cos(Math.toRadians(pourPoint.lat)))
            boundaryPoints.add(GeoPoint(pourPoint.lat + dLat, pourPoint.lon + dLon))
        }

        return CatchmentBoundary(
            crossingId = crossingId,
            stationLabel = stationLabel,
            areaKm2 = areaKm2,
            areaHectares = areaHa,
            streamLengthKm = streamLengthKm,
            avgSlopePercent = avgSlopePercent,
            timeOfConcentrationMin = tcMinutes,
            polygonPoints = boundaryPoints,
            pourPoint = pourPoint
        )
    }

    /**
     * Calculates design discharge Q_p (m³/s) using the Rational Method:
     * Q = 0.278 * C * I * A
     * where:
     * - C = runoff coefficient (0.20 to 0.85)
     * - I = rainfall intensity (mm/hr)
     * - A = catchment area (km²)
     */
    fun calculateRationalDischarge(
        areaKm2: Double,
        runoffCoeff: Double = 0.45,
        tcMinutes: Double = 25.0,
        returnPeriodPercent: Double = 2.0 // 2% for Category II road (50-year return period)
    ): HydrologyResult {
        // Mongolian rainfall intensity-duration relationship for design storm:
        // I = a / (t_c + b)^n, typical for steppe/mountain region of Mongolia
        // Return period factor K_p: 1% -> 1.25, 2% -> 1.0, 3% -> 0.90, 5% -> 0.80
        val kp = when {
            returnPeriodPercent <= 1.0 -> 1.25
            returnPeriodPercent <= 2.0 -> 1.00
            returnPeriodPercent <= 3.0 -> 0.90
            else -> 0.80
        }

        val baseIntensity = 75.0 * kp * (60.0 / (tcMinutes + 12.0)).pow(0.65)
        val intensityMmHr = baseIntensity.coerceIn(15.0, 160.0)

        val qDesign = 0.278 * runoffCoeff * intensityMmHr * areaKm2

        return HydrologyResult(
            areaKm2 = areaKm2,
            runoffCoeff = runoffCoeff,
            rainfallIntensityMmHr = intensityMmHr,
            returnPeriodPercent = returnPeriodPercent,
            timeOfConcentrationMin = tcMinutes,
            designDischargeM3s = max(0.1, qDesign),
            formulaUsed = "Рационал арга (Rational Method: Q = 0.278·C·I·A)"
        )
    }

    /**
     * Calculates peak discharge according to Mongolian Norm БНбД 2.01.14-83
     * "Гадаргын усны урсацын үндсэн тодорхойлолтууд".
     */
    fun calculateBnbdDischarge(
        areaKm2: Double,
        streamLengthKm: Double,
        avgSlopePercent: Double,
        soilCategory: String = "Уулын бэл, хээрийн бүс (Medium loam)",
        returnPeriodPercent: Double = 2.0
    ): HydrologyResult {
        val runoffCoeff = when {
            soilCategory.contains("хад", ignoreCase = true) -> 0.75
            soilCategory.contains("хээр", ignoreCase = true) -> 0.45
            soilCategory.contains("элс", ignoreCase = true) -> 0.25
            else -> 0.40
        }

        val sMeterMeter = max(0.005, avgSlopePercent / 100.0)
        val lMeters = streamLengthKm * 1000.0
        val tc = (0.0195 * lMeters.pow(0.77) * sMeterMeter.pow(-0.385)).coerceIn(5.0, 180.0)

        val kp = when {
            returnPeriodPercent <= 1.0 -> 1.25
            returnPeriodPercent <= 2.0 -> 1.00
            returnPeriodPercent <= 3.0 -> 0.90
            else -> 0.80
        }

        // Daily maximum precipitation H_p% for central Mongolia ~ 65 mm (P=2%)
        val hMax = 65.0 * kp
        val intensity = (hMax / (tc / 60.0).pow(0.55)).coerceIn(18.0, 150.0)

        val qBnbd = 0.278 * runoffCoeff * intensity * areaKm2

        return HydrologyResult(
            areaKm2 = areaKm2,
            runoffCoeff = runoffCoeff,
            rainfallIntensityMmHr = intensity,
            returnPeriodPercent = returnPeriodPercent,
            timeOfConcentrationMin = tc,
            designDischargeM3s = max(0.15, qBnbd),
            formulaUsed = "БНбД 2.01.14-83 & Авто замын норм (Q_p%)"
        )
    }
}
