package com.example.mondrain

import com.example.mondrain.dem.ElevationGrid
import com.example.mondrain.gis.GeoBounds
import com.example.mondrain.gis.GeoPoint
import com.example.mondrain.gis.GisEngine
import com.example.mondrain.hydraulic.CulvertEngine
import com.example.mondrain.hydraulic.CulvertType
import com.example.mondrain.hydrology.HydrologyEngine
import org.junit.Assert.*
import org.junit.Test

class EngineeringEngineTest {

    @Test
    fun testGisStationFormatting() {
        val st0 = GisEngine.formatStation(0.0)
        assertEquals("ПК 0+0.0", st0)

        val st1540 = GisEngine.formatStation(1540.0)
        assertEquals("ПК 15+40.0", st1540)

        val st5230 = GisEngine.formatStation(5230.5)
        assertEquals("ПК 52+30.5", st5230)
    }

    @Test
    fun testGisHaversineDistance() {
        // Ulaanbaatar (47.9188, 106.9176) to nearby point ~1.1km north
        val p1 = GeoPoint(47.9188, 106.9176)
        val p2 = GeoPoint(47.9288, 106.9176)
        val dist = GisEngine.distanceMeters(p1, p2)
        assertTrue("Distance should be roughly 1111 meters", dist in 1100.0..1120.0)
    }

    @Test
    fun testUtmProjection() {
        // Central Mongolia ~106.9 E should be UTM Zone 48N
        val p = GeoPoint(47.9188, 106.9176)
        val utm = GisEngine.toUtm(p)
        assertEquals(48, utm.zone)
        assertEquals('N', utm.hemisphere)
        assertTrue("Easting should be valid UTM range", utm.easting in 100000.0..900000.0)
        assertTrue("Northing should be valid for Mongolia", utm.northing in 5000000.0..6000000.0)
    }

    @Test
    fun testHydrologyBnbdCalculation() {
        // 5 km2 basin, 3.5 km stream, 4.5% slope
        val result = HydrologyEngine.calculateBnbdDischarge(
            areaKm2 = 5.0,
            streamLengthKm = 3.5,
            avgSlopePercent = 4.5,
            returnPeriodPercent = 2.0
        )
        assertTrue("Discharge should be positive and realistic", result.designDischargeM3s > 0.5)
        assertTrue("Tc should be realistic for 3.5km stream", result.timeOfConcentrationMin > 10.0)
        assertEquals(2.0, result.returnPeriodPercent, 0.01)
    }

    @Test
    fun testCulvertHydraulicAnalysis() {
        // Pipe 1.25m with 3.5 m3/s
        val pipeAnalysis = CulvertEngine.analyzeCulvert(
            type = CulvertType.PIPE,
            spanOrDiameterM = 1.25,
            heightM = 1.25,
            barrels = 1,
            culvertSlopePercent = 1.5,
            designDischargeM3s = 3.5
        )

        assertNotNull(pipeAnalysis)
        assertTrue("Capacity must be positive", pipeAnalysis.fullCapacityM3s > 0.0)
        assertTrue("Velocity must be positive", pipeAnalysis.flowVelocityMs > 0.0)
        assertTrue("Headwater ratio must be calculated", pipeAnalysis.headwaterRatio > 0.0)

        // Box 2.0x2.0m with 12.0 m3/s (Twin barrel)
        val boxAnalysis = CulvertEngine.analyzeCulvert(
            type = CulvertType.BOX,
            spanOrDiameterM = 2.0,
            heightM = 2.0,
            barrels = 2,
            culvertSlopePercent = 1.0,
            designDischargeM3s = 12.0
        )

        assertTrue(boxAnalysis.fullCapacityM3s > 12.0)
        assertTrue(boxAnalysis.isAdequate)
    }

    @Test
    fun testD8FlowRouting() {
        // Small 3x3 synthetic grid with a depression in center-right
        val gridData = arrayOf(
            doubleArrayOf(100.0, 95.0, 90.0),
            doubleArrayOf(98.0, 90.0, 85.0),
            doubleArrayOf(96.0, 88.0, 80.0)
        )
        val grid = ElevationGrid(
            rows = 3,
            cols = 3,
            bounds = GeoBounds(47.0, 47.03, 106.0, 106.03),
            data = gridData
        )

        val flowDir = HydrologyEngine.computeD8FlowDirection(grid)
        assertNotNull(flowDir)
        assertEquals(3, flowDir.size)
        assertEquals(3, flowDir[0].size)

        val flowAcc = HydrologyEngine.computeFlowAccumulation(grid, flowDir)
        assertNotNull(flowAcc)
        // Bottom right cell (lowest elevation 80.0) should have high accumulation
        assertTrue("Lowest cell accumulates flow", flowAcc[2][2] >= 1)
    }
}
