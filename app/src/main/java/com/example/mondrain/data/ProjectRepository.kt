package com.example.mondrain.data

import com.example.mondrain.gis.GisEngine
import com.example.mondrain.hydraulic.CulvertType
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class ProjectRepository(private val dao: ProjectDao) {

    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjects()

    fun getProject(id: Long): Flow<ProjectEntity?> = dao.getProjectById(id)

    suspend fun getProjectDirect(id: Long): ProjectEntity? = dao.getProjectByIdDirect(id)

    fun getCrossings(projectId: Long): Flow<List<DrainageCrossingEntity>> =
        dao.getCrossingsForProject(projectId)

    suspend fun getCrossingsDirect(projectId: Long): List<DrainageCrossingEntity> =
        dao.getCrossingsForProjectDirect(projectId)

    suspend fun insertProject(project: ProjectEntity): Long = dao.insertProject(project)

    suspend fun updateProject(project: ProjectEntity) = dao.updateProject(project)

    suspend fun deleteProject(project: ProjectEntity) = dao.deleteProject(project)

    suspend fun deleteProjectById(id: Long) = dao.deleteProjectById(id)

    suspend fun insertCrossing(crossing: DrainageCrossingEntity): Long = dao.insertCrossing(crossing)

    suspend fun insertCrossings(crossings: List<DrainageCrossingEntity>) = dao.insertCrossings(crossings)

    suspend fun updateCrossing(crossing: DrainageCrossingEntity) = dao.updateCrossing(crossing)

    suspend fun deleteCrossing(crossing: DrainageCrossingEntity) = dao.deleteCrossing(crossing)

    suspend fun deleteCrossingsForProject(projectId: Long) = dao.deleteCrossingsForProject(projectId)

    suspend fun renameProject(id: Long, newName: String) {
        val current = dao.getProjectByIdDirect(id) ?: return
        dao.updateProject(current.copy(projectName = newName, lastModified = System.currentTimeMillis()))
    }

    suspend fun duplicateProject(projectId: Long): Long {
        val original = dao.getProjectByIdDirect(projectId) ?: return -1L
        val crossings = dao.getCrossingsForProjectDirect(projectId)

        val copyProject = original.copy(
            id = 0,
            projectName = "${original.projectName} (Хуулбар)",
            projectNumber = "${original.projectNumber}-COPY",
            dateCreated = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date()),
            lastModified = System.currentTimeMillis()
        )
        val newId = dao.insertProject(copyProject)

        val copiedCrossings = crossings.map {
            it.copy(id = 0, projectId = newId)
        }
        dao.insertCrossings(copiedCrossings)
        return newId
    }

    suspend fun exportProjectJson(projectId: Long): String {
        val project = dao.getProjectByIdDirect(projectId) ?: return "{}"
        val crossings = dao.getCrossingsForProjectDirect(projectId)

        val root = JSONObject()
        root.put("version", "1.0")
        root.put("appName", "MON-DRAIN Engineer")
        root.put("exportDate", System.currentTimeMillis())

        val pObj = JSONObject()
        pObj.put("projectName", project.projectName)
        pObj.put("projectNumber", project.projectNumber)
        pObj.put("client", project.client)
        pObj.put("designer", project.designer)
        pObj.put("location", project.location)
        pObj.put("province", project.province)
        pObj.put("district", project.district)
        pObj.put("roadName", project.roadName)
        pObj.put("roadSection", project.roadSection)
        pObj.put("coordinateSystem", project.coordinateSystem)
        pObj.put("demSourceInfo", project.demSourceInfo)
        pObj.put("demMode", project.demMode)
        pObj.put("rainfallInfo", project.rainfallInfo)
        pObj.put("hydrologyMethod", project.hydrologyMethod)
        pObj.put("returnPeriodPercent", project.returnPeriodPercent)
        pObj.put("roadLengthMeters", project.roadLengthMeters)
        pObj.put("fieldObservations", project.fieldObservations)
        root.put("project", pObj)

        val cArray = JSONArray()
        for (c in crossings) {
            val cObj = JSONObject()
            cObj.put("stationMeters", c.stationMeters)
            cObj.put("stationLabel", c.stationLabel)
            cObj.put("latitude", c.latitude)
            cObj.put("longitude", c.longitude)
            cObj.put("utmEasting", c.utmEasting)
            cObj.put("utmNorthing", c.utmNorthing)
            cObj.put("catchmentAreaKm2", c.catchmentAreaKm2)
            cObj.put("streamLengthKm", c.streamLengthKm)
            cObj.put("slopePercent", c.slopePercent)
            cObj.put("runoffCoeff", c.runoffCoeff)
            cObj.put("designDischargeM3s", c.designDischargeM3s)
            cObj.put("culvertType", c.culvertType)
            cObj.put("culvertSpanOrDiameterM", c.culvertSpanOrDiameterM)
            cObj.put("culvertHeightM", c.culvertHeightM)
            cObj.put("barrels", c.barrels)
            cObj.put("capacityDischargeM3s", c.capacityDischargeM3s)
            cObj.put("headwaterM", c.headwaterM)
            cObj.put("headwaterRatio", c.headwaterRatio)
            cObj.put("flowVelocityMs", c.flowVelocityMs)
            cObj.put("flowControl", c.flowControl)
            cObj.put("isAdequate", c.isAdequate)
            cObj.put("scourProtectionRequired", c.scourProtectionRequired)
            cObj.put("notes", c.notes)
            cArray.put(cObj)
        }
        root.put("crossings", cArray)

        return root.toString(2)
    }

    suspend fun importProjectJson(jsonStr: String): Long {
        val root = JSONObject(jsonStr)
        val pObj = root.getJSONObject("project")

        val newProject = ProjectEntity(
            projectName = pObj.optString("projectName", "Импортолсон Төсөл"),
            projectNumber = pObj.optString("projectNumber", "IMP-001"),
            client = pObj.optString("client", "Зам, тээврийн хөгжлийн яам"),
            designer = pObj.optString("designer", "Авто замын инженер"),
            location = pObj.optString("location", "Монгол улс"),
            province = pObj.optString("province", "Төв аймаг"),
            district = pObj.optString("district", "Борнуур сум"),
            roadName = pObj.optString("roadName", "Улсын чанартай авто зам"),
            roadSection = pObj.optString("roadSection", "ПК 0+00 - ПК 12+50"),
            dateCreated = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
            coordinateSystem = pObj.optString("coordinateSystem", "WGS 84 / UTM Zone 48N"),
            demSourceInfo = pObj.optString("demSourceInfo", "NASADEM 1 arc-second (~30 m)"),
            demMode = pObj.optString("demMode", "ONLINE"),
            rainfallInfo = pObj.optString("rainfallInfo", "БНбД 2.01.14-83"),
            hydrologyMethod = pObj.optString("hydrologyMethod", "БНбД 2.01.14-83"),
            returnPeriodPercent = pObj.optDouble("returnPeriodPercent", 2.0),
            fieldObservations = pObj.optString("fieldObservations", "Хээрийн судалгааны өгөгдөл"),
            roadLengthMeters = pObj.optDouble("roadLengthMeters", 12500.0)
        )

        val newId = dao.insertProject(newProject)

        if (root.has("crossings")) {
            val cArray = root.getJSONArray("crossings")
            val list = ArrayList<DrainageCrossingEntity>()
            for (i in 0 until cArray.length()) {
                val c = cArray.getJSONObject(i)
                list.add(
                    DrainageCrossingEntity(
                        projectId = newId,
                        stationMeters = c.optDouble("stationMeters", i * 1000.0),
                        stationLabel = c.optString("stationLabel", "ПК $i+00"),
                        latitude = c.optDouble("latitude", 48.2 + i * 0.01),
                        longitude = c.optDouble("longitude", 106.1 + i * 0.01),
                        utmEasting = c.optDouble("utmEasting", 435000.0 + i * 800),
                        utmNorthing = c.optDouble("utmNorthing", 5340000.0 + i * 900),
                        catchmentAreaKm2 = c.optDouble("catchmentAreaKm2", 1.5),
                        streamLengthKm = c.optDouble("streamLengthKm", 1.8),
                        slopePercent = c.optDouble("slopePercent", 2.5),
                        runoffCoeff = c.optDouble("runoffCoeff", 0.45),
                        designDischargeM3s = c.optDouble("designDischargeM3s", 2.8),
                        culvertType = c.optString("culvertType", CulvertType.PIPE.name),
                        culvertSpanOrDiameterM = c.optDouble("culvertSpanOrDiameterM", 1.25),
                        culvertHeightM = c.optDouble("culvertHeightM", 1.25),
                        barrels = c.optInt("barrels", 1),
                        capacityDischargeM3s = c.optDouble("capacityDischargeM3s", 3.4),
                        headwaterM = c.optDouble("headwaterM", 1.15),
                        headwaterRatio = c.optDouble("headwaterRatio", 0.92),
                        flowVelocityMs = c.optDouble("flowVelocityMs", 2.1),
                        flowControl = c.optString("flowControl", "Оролтын удирдлага"),
                        isAdequate = c.optBoolean("isAdequate", true),
                        scourProtectionRequired = c.optBoolean("scourProtectionRequired", false),
                        notes = c.optString("notes", "")
                    )
                )
            }
            dao.insertCrossings(list)
        }
        return newId
    }

    suspend fun ensureSampleProjects() {
        val projects = dao.getProjectByIdDirect(1)
        if (projects != null) return

        // 1. Primary Mongolian highway project: Ulaanbaatar - Darkhan Highway Section
        val p1 = ProjectEntity(
            projectName = "Улаанбаатар - Дархан чиглэлийн авто зам (ПК 45+00 - ПК 57+50)",
            projectNumber = "UB-DAR-2026-04",
            client = "Зам, тээврийн хөгжлийн яам (ЗТХЯ)",
            designer = "Д. Бат-Эрдэнэ (Тэргүүлэх инженер)",
            location = "Төв аймаг, Баянчандмань - Борнуур сумдын зааг",
            province = "Төв аймаг",
            district = "Борнуур сум",
            roadName = "А0101 Улсын чанартай авто зам",
            roadSection = "ПК 45+00 - ПК 57+50 (Урт: 12.5 км)",
            dateCreated = "2026-08-15",
            coordinateSystem = "WGS 84 / UTM Zone 48N",
            demSourceInfo = "NASADEM 1 arc-second (~30 m)",
            demMode = "ONLINE",
            rainfallInfo = "БНбД 2.01.14-83 (H_1% = 78 мм, H_2% = 65 мм)",
            hydrologyMethod = "БНбД 2.01.14-83 & Рационал арга (Rational Method)",
            returnPeriodPercent = 2.0,
            roadLengthMeters = 12540.0,
            fieldObservations = "Хөндийн хэвгий гадарга, голын сав газрын хуурай сайр огтлолцолууд. Уруйн үерийн аюултай."
        )
        val id1 = dao.insertProject(p1)

        val crossings1 = listOf(
            DrainageCrossingEntity(
                projectId = id1,
                stationMeters = 1450.0,
                stationLabel = "ПК 14+50",
                latitude = 48.2240,
                longitude = 106.1300,
                utmEasting = 435420.0,
                utmNorthing = 5341200.0,
                catchmentAreaKm2 = 1.85,
                streamLengthKm = 2.10,
                slopePercent = 2.80,
                runoffCoeff = 0.45,
                designDischargeM3s = 3.12,
                culvertType = CulvertType.PIPE.name,
                culvertSpanOrDiameterM = 1.25,
                culvertHeightM = 1.25,
                barrels = 1,
                capacityDischargeM3s = 3.65,
                headwaterM = 1.18,
                headwaterRatio = 0.94,
                flowVelocityMs = 2.35,
                flowControl = "Оролтын удирдлага",
                isAdequate = true,
                scourProtectionRequired = true,
                notes = "Гаралтад 6 м чулуун өнгөлгөө шаардлагатай"
            ),
            DrainageCrossingEntity(
                projectId = id1,
                stationMeters = 3820.0,
                stationLabel = "ПК 38+20",
                latitude = 48.2410,
                longitude = 106.1410,
                utmEasting = 436210.0,
                utmNorthing = 5343050.0,
                catchmentAreaKm2 = 0.65,
                streamLengthKm = 1.15,
                slopePercent = 3.40,
                runoffCoeff = 0.42,
                designDischargeM3s = 1.24,
                culvertType = CulvertType.PIPE.name,
                culvertSpanOrDiameterM = 1.00,
                culvertHeightM = 1.00,
                barrels = 1,
                capacityDischargeM3s = 1.95,
                headwaterM = 0.85,
                headwaterRatio = 0.85,
                flowVelocityMs = 1.80,
                flowControl = "Оролтын удирдлага",
                isAdequate = true,
                scourProtectionRequired = false,
                notes = "Хэвийн нэвтрүүлэлттэй, аюулгүй"
            ),
            DrainageCrossingEntity(
                projectId = id1,
                stationMeters = 6540.0,
                stationLabel = "ПК 65+40",
                latitude = 48.2610,
                longitude = 106.1510,
                utmEasting = 436980.0,
                utmNorthing = 5345220.0,
                catchmentAreaKm2 = 4.80,
                streamLengthKm = 3.60,
                slopePercent = 2.10,
                runoffCoeff = 0.48,
                designDischargeM3s = 8.45,
                culvertType = CulvertType.BOX.name,
                culvertSpanOrDiameterM = 2.50,
                culvertHeightM = 2.00,
                barrels = 2,
                capacityDischargeM3s = 11.20,
                headwaterM = 1.62,
                headwaterRatio = 0.81,
                flowVelocityMs = 2.40,
                flowControl = "Гаралтын удирдлага",
                isAdequate = true,
                scourProtectionRequired = true,
                notes = "Хос тэгш өнцөгт хоолой 2х(2.5x2.0м), угаагдлаас хамгаалах өнгөлгөө хийх"
            ),
            DrainageCrossingEntity(
                projectId = id1,
                stationMeters = 9180.0,
                stationLabel = "ПК 91+80",
                latitude = 48.2810,
                longitude = 106.1600,
                utmEasting = 437640.0,
                utmNorthing = 5347450.0,
                catchmentAreaKm2 = 2.40,
                streamLengthKm = 2.45,
                slopePercent = 1.90,
                runoffCoeff = 0.44,
                designDischargeM3s = 3.85,
                culvertType = CulvertType.PIPE.name,
                culvertSpanOrDiameterM = 1.50,
                culvertHeightM = 1.50,
                barrels = 1,
                capacityDischargeM3s = 5.20,
                headwaterM = 1.32,
                headwaterRatio = 0.88,
                flowVelocityMs = 2.15,
                flowControl = "Оролтын удирдлага",
                isAdequate = true,
                scourProtectionRequired = true,
                notes = "Дугуй Ø1.50 м хоолой хангасан"
            ),
            DrainageCrossingEntity(
                projectId = id1,
                stationMeters = 11850.0,
                stationLabel = "ПК 118+50",
                latitude = 48.3050,
                longitude = 106.1720,
                utmEasting = 438510.0,
                utmNorthing = 5350100.0,
                catchmentAreaKm2 = 1.10,
                streamLengthKm = 1.50,
                slopePercent = 3.10,
                runoffCoeff = 0.40,
                designDischargeM3s = 1.95,
                culvertType = CulvertType.PIPE.name,
                culvertSpanOrDiameterM = 1.25,
                culvertHeightM = 1.25,
                barrels = 1,
                capacityDischargeM3s = 3.60,
                headwaterM = 0.95,
                headwaterRatio = 0.76,
                flowVelocityMs = 1.90,
                flowControl = "Оролтын удирдлага",
                isAdequate = true,
                scourProtectionRequired = false,
                notes = "Хангасан"
            )
        )
        dao.insertCrossings(crossings1)
    }
}
