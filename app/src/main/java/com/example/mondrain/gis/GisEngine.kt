package com.example.mondrain.gis

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipInputStream
import kotlin.math.*

data class GeoPoint(
    val lat: Double,
    val lon: Double,
    val elevation: Double = 0.0
)

data class GeoBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
) {
    val centerLat: Double get() = (minLat + maxLat) / 2.0
    val centerLon: Double get() = (minLon + maxLon) / 2.0
    val latSpan: Double get() = maxLat - minLat
    val lonSpan: Double get() = maxLon - minLon

    fun contains(lat: Double, lon: Double): Boolean {
        return lat in minLat..maxLat && lon in minLon..maxLon
    }

    fun expandByBufferMeters(bufferM: Double): GeoBounds {
        val latDelta = bufferM / 111139.0
        val lonDelta = bufferM / (111139.0 * max(0.1, cos(Math.toRadians(centerLat))))
        return GeoBounds(
            minLat = minLat - latDelta,
            maxLat = maxLat + latDelta,
            minLon = minLon - lonDelta,
            maxLon = maxLon + lonDelta
        )
    }
}

data class UtmCoord(
    val zone: Int,
    val hemisphere: Char = 'N',
    val easting: Double,
    val northing: Double
) {
    fun toFormattedString(): String = "UTM ${zone}${hemisphere} E: ${easting.roundToInt()} N: ${northing.roundToInt()}"
}

data class AlignmentStation(
    val stationMeters: Double,
    val stationLabel: String, // e.g. "ПК 15+50"
    val point: GeoPoint,
    val utmCoord: UtmCoord
)

data class ParsedAlignment(
    val fileName: String,
    val points: List<GeoPoint>,
    val totalLengthMeters: Double,
    val bounds: GeoBounds,
    val stations: List<AlignmentStation>,
    val crsAssumption: String = "WGS 84 (EPSG:4326)",
    val rawKmlSnippet: String = ""
)

object GisEngine {

    private const val EARTH_RADIUS_M = 6371000.0

    /**
     * Compute Haversine geodetic distance in meters between two coordinates.
     */
    fun distanceMeters(p1: GeoPoint, p2: GeoPoint): Double {
        val dLat = Math.toRadians(p2.lat - p1.lat)
        val dLon = Math.toRadians(p2.lon - p1.lon)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(p1.lat)) * cos(Math.toRadians(p2.lat)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    fun wgs84ToUtm(lat: Double, lon: Double, forcedZone: Int? = null): UtmCoord {
        return toUtm(GeoPoint(lat, lon), forcedZone)
    }

    /**
     * Compute initial bearing in degrees (0..360) from p1 to p2.
     */
    fun bearingDegrees(p1: GeoPoint, p2: GeoPoint): Double {
        val y = sin(Math.toRadians(p2.lon - p1.lon)) * cos(Math.toRadians(p2.lat))
        val x = cos(Math.toRadians(p1.lat)) * sin(Math.toRadians(p2.lat)) -
                sin(Math.toRadians(p1.lat)) * cos(Math.toRadians(p2.lat)) * cos(Math.toRadians(p2.lon - p1.lon))
        val brng = Math.toDegrees(atan2(y, x))
        return (brng + 360.0) % 360.0
    }

    /**
     * Standard Mongolian / Russian road stationing format:
     * Distance in meters to "ПК <hundreds>+<remainder>"
     * Example: 1540 meters -> "ПК 15+40.00"
     */
    fun formatStation(meters: Double): String {
        val pks = (meters / 100.0).toInt()
        val rem = meters - (pks * 100.0)
        return "ПК $pks+${String.format("%.1f", rem)}"
    }

    /**
     * Project WGS84 (Lat, Lon) to Universal Transverse Mercator (UTM).
     * Automatically calculates standard UTM zone for Mongolia (Zones 46N to 50N, central Mongolia 48N).
     */
    fun toUtm(point: GeoPoint, forcedZone: Int? = null): UtmCoord {
        val latRad = Math.toRadians(point.lat)
        val lonRad = Math.toRadians(point.lon)

        val zone = forcedZone ?: ((floor((point.lon + 180.0) / 6.0)).toInt() + 1)
        val lon0 = Math.toRadians(((zone - 1) * 6 - 180 + 3).toDouble())

        // WGS84 Ellipsoid constants
        val a = 6378137.0
        val f = 1.0 / 298.257223563
        val b = a * (1.0 - f)
        val e2 = (a * a - b * b) / (a * a)
        val ePrime2 = (a * a - b * b) / (b * b)
        val k0 = 0.9996

        val n = a / sqrt(1.0 - e2 * sin(latRad).pow(2))
        val t = tan(latRad).pow(2)
        val c = ePrime2 * cos(latRad).pow(2)
        val bigA = cos(latRad) * (lonRad - lon0)

        val m = a * ((1.0 - e2 / 4.0 - 3.0 * e2 * e2 / 64.0 - 5.0 * e2.pow(3) / 256.0) * latRad -
                (3.0 * e2 / 8.0 + 3.0 * e2 * e2 / 32.0 + 45.0 * e2.pow(3) / 1024.0) * sin(2.0 * latRad) +
                (15.0 * e2 * e2 / 256.0 + 45.0 * e2.pow(3) / 1024.0) * sin(4.0 * latRad) -
                (35.0 * e2.pow(3) / 3072.0) * sin(6.0 * latRad))

        val easting = k0 * n * (bigA + (1.0 - t + c) * bigA.pow(3) / 6.0 +
                (5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * ePrime2) * bigA.pow(5) / 120.0) + 500000.0

        var northing = k0 * (m + n * tan(latRad) * (bigA.pow(2) / 2.0 +
                (5.0 - t + 9.0 * c + 4.0 * c * c) * bigA.pow(4) / 24.0 +
                (61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * ePrime2) * bigA.pow(6) / 720.0))

        if (point.lat < 0.0) {
            northing += 10000000.0
        }

        return UtmCoord(
            zone = zone,
            hemisphere = if (point.lat >= 0) 'N' else 'S',
            easting = easting,
            northing = northing
        )
    }

    /**
     * Computes cumulative stations along alignment points.
     */
    fun buildAlignment(fileName: String, points: List<GeoPoint>, rawKml: String = ""): ParsedAlignment {
        if (points.isEmpty()) {
            return ParsedAlignment(
                fileName = fileName,
                points = emptyList(),
                totalLengthMeters = 0.0,
                bounds = GeoBounds(0.0, 0.0, 0.0, 0.0),
                stations = emptyList()
            )
        }

        var minLat = points[0].lat
        var maxLat = points[0].lat
        var minLon = points[0].lon
        var maxLon = points[0].lon

        var cumDist = 0.0
        val stations = ArrayList<AlignmentStation>()
        stations.add(
            AlignmentStation(
                stationMeters = 0.0,
                stationLabel = formatStation(0.0),
                point = points[0],
                utmCoord = toUtm(points[0])
            )
        )

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val dist = distanceMeters(prev, curr)
            cumDist += dist

            minLat = min(minLat, curr.lat)
            maxLat = max(maxLat, curr.lat)
            minLon = min(minLon, curr.lon)
            maxLon = max(maxLon, curr.lon)

            stations.add(
                AlignmentStation(
                    stationMeters = cumDist,
                    stationLabel = formatStation(cumDist),
                    point = curr,
                    utmCoord = toUtm(curr)
                )
            )
        }

        return ParsedAlignment(
            fileName = fileName,
            points = points,
            totalLengthMeters = cumDist,
            bounds = GeoBounds(minLat, maxLat, minLon, maxLon),
            stations = stations,
            crsAssumption = "WGS 84 (EPSG:4326)",
            rawKmlSnippet = rawKml.take(500)
        )
    }

    /**
     * Parses KML/KMZ input stream. Handles KMZ by inspecting ZIP entries for .kml files.
     */
    fun parseKmlOrKmz(inputStream: InputStream, originalFileName: String): ParsedAlignment {
        val bytes = inputStream.readBytes()
        val isKmz = originalFileName.endsWith(".kmz", ignoreCase = true) || isZipHeader(bytes)

        val kmlContent: String = if (isKmz) {
            extractKmlFromKmz(ByteArrayInputStream(bytes))
                ?: throw IllegalArgumentException("KMZ container does not contain a valid .kml file")
        } else {
            String(bytes, Charsets.UTF_8)
        }

        val points = parseKmlCoordinates(kmlContent)
        if (points.isEmpty()) {
            throw IllegalArgumentException("No LineString or geometry coordinates found in KML")
        }

        return buildAlignment(originalFileName, points, kmlContent)
    }

    private fun isZipHeader(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() &&
                bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()
    }

    private fun extractKmlFromKmz(inputStream: InputStream): String? {
        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && entry.name.endsWith(".kml", ignoreCase = true)) {
                return String(zip.readBytes(), Charsets.UTF_8)
            }
            entry = zip.nextEntry
        }
        return null
    }

    /**
     * Parses <coordinates> blocks from KML XML.
     */
    fun parseKmlCoordinates(kmlString: String): List<GeoPoint> {
        val points = ArrayList<GeoPoint>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(kmlString))

        var eventType = parser.eventType
        var inCoordinates = false
        val coordsBuffer = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals("coordinates", ignoreCase = true)) {
                        inCoordinates = true
                        coordsBuffer.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inCoordinates) {
                        coordsBuffer.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("coordinates", ignoreCase = true)) {
                        inCoordinates = false
                        parseCoordinatesText(coordsBuffer.toString(), points)
                    }
                }
            }
            eventType = parser.next()
        }

        // Fallback: regex search if XML parser had structural issue
        if (points.isEmpty()) {
            val regex = Regex("<coordinates>([\\s\\S]*?)</coordinates>", RegexOption.IGNORE_CASE)
            for (match in regex.findAll(kmlString)) {
                parseCoordinatesText(match.groupValues[1], points)
            }
        }

        return points
    }

    private fun parseCoordinatesText(text: String, outList: MutableList<GeoPoint>) {
        val tokens = text.trim().split(Regex("[\\s\n\r]+"))
        for (token in tokens) {
            val parts = token.split(",")
            if (parts.size >= 2) {
                val lon = parts[0].toDoubleOrNull()
                val lat = parts[1].toDoubleOrNull()
                val alt = if (parts.size > 2) parts[2].toDoubleOrNull() ?: 0.0 else 0.0
                if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    outList.add(GeoPoint(lat = lat, lon = lon, elevation = alt))
                }
            }
        }
    }

    /**
     * Generates a sample road alignment in Mongolia (Ulaanbaatar - Darkhan Highway corridor)
     * Useful for immediate testing and initial project setup.
     */
    fun createSampleMongolianAlignment(): ParsedAlignment {
        // Highway section near Bornuur / Darkhan corridor, central Mongolia
        val samplePoints = listOf(
            GeoPoint(48.2150, 106.1200, 1050.0),
            GeoPoint(48.2220, 106.1280, 1058.0),
            GeoPoint(48.2310, 106.1350, 1065.0),
            GeoPoint(48.2430, 106.1420, 1072.0),
            GeoPoint(48.2560, 106.1480, 1081.0),
            GeoPoint(48.2700, 106.1550, 1092.0),
            GeoPoint(48.2830, 106.1610, 1085.0),
            GeoPoint(48.2970, 106.1680, 1076.0),
            GeoPoint(48.3100, 106.1740, 1068.0),
            GeoPoint(48.3250, 106.1820, 1055.0)
        )
        return buildAlignment("Sample_Road_UB_Darkhan_KM12.kml", samplePoints)
    }
}
