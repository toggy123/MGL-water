package com.example.mondrain.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mondrain.data.DrainageCrossingEntity
import com.example.mondrain.data.MonDrainDatabase
import com.example.mondrain.data.ProjectEntity
import com.example.mondrain.data.ProjectRepository
import com.example.mondrain.dem.*
import com.example.mondrain.gis.*
import com.example.mondrain.hydraulic.CulvertEngine
import com.example.mondrain.hydraulic.CulvertHydraulicAnalysis
import com.example.mondrain.hydraulic.CulvertType
import com.example.mondrain.hydrology.CatchmentBoundary
import com.example.mondrain.hydrology.FlowPath
import com.example.mondrain.hydrology.HydrologyEngine
import com.example.mondrain.util.AppLanguage
import com.example.mondrain.util.MonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max

class MonDrainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository

    // Language State
    private val _language = MutableStateFlow(AppLanguage.MONGOLIAN)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    // Projects State
    val allProjects: StateFlow<List<ProjectEntity>>
    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    private val _selectedProject = MutableStateFlow<ProjectEntity?>(null)
    val selectedProject: StateFlow<ProjectEntity?> = _selectedProject.asStateFlow()

    // Crossings State
    private val _crossings = MutableStateFlow<List<DrainageCrossingEntity>>(emptyList())
    val crossings: StateFlow<List<DrainageCrossingEntity>> = _crossings.asStateFlow()

    // Road Alignment State
    private val _roadAlignment = MutableStateFlow<ParsedAlignment?>(null)
    val roadAlignment: StateFlow<ParsedAlignment?> = _roadAlignment.asStateFlow()

    // DEM Engine State
    private val _demMode = MutableStateFlow(DemMode.ONLINE)
    val demMode: StateFlow<DemMode> = _demMode.asStateFlow()

    private val _demMetadata = MutableStateFlow<DemMetadata?>(null)
    val demMetadata: StateFlow<DemMetadata?> = _demMetadata.asStateFlow()

    private val _elevationGrid = MutableStateFlow<ElevationGrid?>(null)
    val elevationGrid: StateFlow<ElevationGrid?> = _elevationGrid.asStateFlow()

    private val _demStatusMessage = MutableStateFlow<String?>(null)
    val demStatusMessage: StateFlow<String?> = _demStatusMessage.asStateFlow()

    private val _isDemLoading = MutableStateFlow(false)
    val isDemLoading: StateFlow<Boolean> = _isDemLoading.asStateFlow()

    // Hydrology State
    private val _catchments = MutableStateFlow<List<CatchmentBoundary>>(emptyList())
    val catchments: StateFlow<List<CatchmentBoundary>> = _catchments.asStateFlow()

    private val _flowPaths = MutableStateFlow<List<FlowPath>>(emptyList())
    val flowPaths: StateFlow<List<FlowPath>> = _flowPaths.asStateFlow()

    // GIS Map Layer Toggles
    val showRoad = MutableStateFlow(true)
    val showDem = MutableStateFlow(true)
    val showContours = MutableStateFlow(true)
    val showCatchments = MutableStateFlow(true)
    val showFlowPaths = MutableStateFlow(true)
    val showCrossings = MutableStateFlow(true)
    val showGps = MutableStateFlow(false)

    private val _selectedCrossing = MutableStateFlow<DrainageCrossingEntity?>(null)
    val selectedCrossing: StateFlow<DrainageCrossingEntity?> = _selectedCrossing.asStateFlow()

    private val _mapCursorCoords = MutableStateFlow<GeoPoint?>(null)
    val mapCursorCoords: StateFlow<GeoPoint?> = _mapCursorCoords.asStateFlow()

    // Interactive Culvert Sizer Sandbox
    private val _calcDischarge = MutableStateFlow(3.5)
    val calcDischarge: StateFlow<Double> = _calcDischarge.asStateFlow()

    private val _calcSlope = MutableStateFlow(1.5)
    val calcSlope: StateFlow<Double> = _calcSlope.asStateFlow()

    private val _calcType = MutableStateFlow(CulvertType.PIPE)
    val calcType: StateFlow<CulvertType> = _calcType.asStateFlow()

    private val _calcSpan = MutableStateFlow(1.50)
    val calcSpan: StateFlow<Double> = _calcSpan.asStateFlow()

    private val _calcHeight = MutableStateFlow(1.50)
    val calcHeight: StateFlow<Double> = _calcHeight.asStateFlow()

    private val _calcBarrels = MutableStateFlow(1)
    val calcBarrels: StateFlow<Int> = _calcBarrels.asStateFlow()

    private val _calcAnalysis = MutableStateFlow<CulvertHydraulicAnalysis?>(null)
    val calcAnalysis: StateFlow<CulvertHydraulicAnalysis?> = _calcAnalysis.asStateFlow()

    init {
        val db = MonDrainDatabase.getDatabase(application)
        repository = ProjectRepository(db.projectDao())

        allProjects = repository.allProjects.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        viewModelScope.launch {
            repository.ensureSampleProjects()
            allProjects.collect { list ->
                if (list.isNotEmpty() && _selectedProjectId.value == null) {
                    selectProject(list.first().id)
                }
            }
        }

        // Initialize sandbox analysis
        recalculateSandbox()
    }

    fun toggleLanguage() {
        val newLang = if (_language.value == AppLanguage.MONGOLIAN) AppLanguage.ENGLISH else AppLanguage.MONGOLIAN
        _language.value = newLang
        MonStrings.currentLanguage = newLang
    }

    fun selectProject(projectId: Long) {
        _selectedProjectId.value = projectId
        viewModelScope.launch {
            val p = repository.getProjectDirect(projectId)
            _selectedProject.value = p
            if (p != null) {
                val crossingsList = repository.getCrossingsDirect(projectId)
                _crossings.value = crossingsList

                // Load initial alignment for sample project
                val sampleAlignment = GisEngine.createSampleMongolianAlignment()
                _roadAlignment.value = sampleAlignment

                // Prepare default DEM grid for the bounds
                val demSource = LocalDemSource()
                val grid = demSource.createDefaultSteppeTerrain(sampleAlignment.bounds.expandByBufferMeters(800.0))
                _elevationGrid.value = grid
                _demMetadata.value = DemMetadata(
                    sourceName = p.demSourceInfo,
                    mode = if (p.demMode == "ONLINE") DemMode.ONLINE else DemMode.OFFLINE,
                    resolution = "1 arc-second (~30 m)",
                    crs = p.coordinateSystem,
                    coverage = grid.bounds,
                    downloadStatus = "Бэлэн / Ready",
                    cacheStatus = "Хадгалагдсан / Cached",
                    minElevation = grid.minElevation,
                    maxElevation = grid.maxElevation
                )

                runDelineationAndHydrology()
            }
        }
    }

    fun createProject(
        name: String,
        number: String,
        client: String,
        designer: String,
        location: String,
        province: String,
        district: String,
        roadName: String,
        roadSection: String
    ) {
        viewModelScope.launch {
            val newProject = ProjectEntity(
                projectName = name.ifBlank { "Шинэ авто замын төсөл" },
                projectNumber = number.ifBlank { "PRJ-${System.currentTimeMillis() % 10000}" },
                client = client.ifBlank { "ЗТХЯ" },
                designer = designer.ifBlank { "Инженер" },
                location = location.ifBlank { "Монгол улс" },
                province = province.ifBlank { "Төв аймаг" },
                district = district.ifBlank { "Сум" },
                roadName = roadName.ifBlank { "Авто зам" },
                roadSection = roadSection.ifBlank { "ПК 0+00 - ПК 10+00" },
                dateCreated = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            )
            val newId = repository.insertProject(newProject)
            selectProject(newId)
        }
    }

    fun duplicateProject(id: Long) {
        viewModelScope.launch {
            val newId = repository.duplicateProject(id)
            if (newId > 0) {
                selectProject(newId)
            }
        }
    }

    fun renameProject(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renameProject(id, newName)
            _selectedProject.value = repository.getProjectDirect(id)
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProjectById(id)
            val remaining = repository.getProjectDirect(1)
            if (remaining != null) {
                selectProject(remaining.id)
            } else {
                _selectedProject.value = null
                _crossings.value = emptyList()
            }
        }
    }

    fun setDemMode(mode: DemMode) {
        _demMode.value = mode
        val currentMeta = _demMetadata.value
        if (currentMeta != null) {
            _demMetadata.value = currentMeta.copy(mode = mode)
        }
        val currentP = _selectedProject.value
        if (currentP != null) {
            viewModelScope.launch {
                val updated = currentP.copy(demMode = mode.name)
                repository.updateProject(updated)
                _selectedProject.value = updated
            }
        }
    }

    fun fetchOnlineDem(sourceType: String = "NASADEM") {
        viewModelScope.launch {
            val bounds = _roadAlignment.value?.bounds ?: GeoBounds(48.2, 48.35, 106.1, 106.2)
            _isDemLoading.value = true
            _demStatusMessage.value = "DEM хавтангуудыг тодорхойлж байна... / Determining DEM tiles..."

            val source: DemSource = if (sourceType == "SRTM") SrtmSource() else NasaDemSource()
            val result = source.fetchDem(getApplication(), bounds, bufferMeters = 600.0)

            when (result) {
                is DemResult.Success -> {
                    _elevationGrid.value = result.grid
                    _demMetadata.value = result.metadata
                    _demStatusMessage.value = "DEM амжилттай бэлтгэгдлээ (${result.grid.rows}x${result.grid.cols})"
                    runDelineationAndHydrology()
                }
                is DemResult.Error -> {
                    _demStatusMessage.value = result.message
                }
            }
            _isDemLoading.value = false
        }
    }

    fun importOfflineHgt(inputStream: InputStream, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isDemLoading.value = true
            _demStatusMessage.value = "HGT файл задалж байна... / Parsing HGT file..."
            val source = LocalDemSource()
            val result = source.parseHgtStream(inputStream, fileName, _roadAlignment.value?.bounds)
            withContext(Dispatchers.Main) {
                when (result) {
                    is DemResult.Success -> {
                        _elevationGrid.value = result.grid
                        _demMetadata.value = result.metadata
                        _demStatusMessage.value = "HGT DEM амжилттай уншигдлаа: $fileName"
                        setDemMode(DemMode.OFFLINE)
                        runDelineationAndHydrology()
                    }
                    is DemResult.Error -> {
                        _demStatusMessage.value = result.message
                    }
                }
                _isDemLoading.value = false
            }
        }
    }

    fun importOfflineGeoTiff(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            _isDemLoading.value = true
            _demStatusMessage.value = "GeoTIFF файл задалж байна... / Parsing GeoTIFF..."
            val source = LocalDemSource()
            val result = source.parseGeoTiffStream(inputStream, _roadAlignment.value?.bounds)
            withContext(Dispatchers.Main) {
                when (result) {
                    is DemResult.Success -> {
                        _elevationGrid.value = result.grid
                        _demMetadata.value = result.metadata
                        _demStatusMessage.value = "GeoTIFF DEM амжилттай уншигдлаа"
                        setDemMode(DemMode.OFFLINE)
                        runDelineationAndHydrology()
                    }
                    is DemResult.Error -> {
                        _demStatusMessage.value = result.message
                    }
                }
                _isDemLoading.value = false
            }
        }
    }

    fun loadRoadAlignmentFromStream(inputStream: InputStream, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsed = GisEngine.parseKmlOrKmz(inputStream, fileName)
                withContext(Dispatchers.Main) {
                    _roadAlignment.value = parsed
                    val p = _selectedProject.value
                    if (p != null) {
                        viewModelScope.launch {
                            val updated = p.copy(
                                roadLengthMeters = parsed.totalLengthMeters,
                                roadName = fileName.removeSuffix(".kml").removeSuffix(".kmz")
                            )
                            repository.updateProject(updated)
                            _selectedProject.value = updated
                        }
                    }
                    // Generate terrain for new bounds
                    val localSource = LocalDemSource()
                    val grid = localSource.createDefaultSteppeTerrain(parsed.bounds.expandByBufferMeters(800.0))
                    _elevationGrid.value = grid
                    runDelineationAndHydrology()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _demStatusMessage.value = "KML/KMZ файл уншихад алдаа гарлаа: ${e.message}"
                }
            }
        }
    }

    fun runDelineationAndHydrology() {
        val grid = _elevationGrid.value ?: return
        val alignment = _roadAlignment.value ?: return
        val currentProjectId = _selectedProjectId.value ?: return

        viewModelScope.launch(Dispatchers.Default) {
            val flowDir = HydrologyEngine.computeD8FlowDirection(grid)
            val accumulation = HydrologyEngine.computeFlowAccumulation(grid, flowDir)
            val paths = HydrologyEngine.extractFlowPaths(grid, flowDir, accumulation, thresholdCells = 12)

            withContext(Dispatchers.Main) {
                _flowPaths.value = paths
            }

            // Delineate catchments for existing crossings or sample alignment stations
            val currentCrossings = _crossings.value
            val catchmentsList = ArrayList<CatchmentBoundary>()
            val updatedCrossings = ArrayList<DrainageCrossingEntity>()

            for (c in currentCrossings) {
                val pourPoint = GeoPoint(c.latitude, c.longitude)
                val cat = HydrologyEngine.delineateCatchment(grid, flowDir, pourPoint, c.id.toString(), c.stationLabel)
                catchmentsList.add(cat)

                // Recalculate hydrology and culvert analysis
                val hydro = HydrologyEngine.calculateBnbdDischarge(
                    areaKm2 = max(0.4, cat.areaKm2),
                    streamLengthKm = cat.streamLengthKm,
                    avgSlopePercent = cat.avgSlopePercent
                )

                val analysis = CulvertEngine.analyzeCulvert(
                    type = if (c.culvertType.contains("BOX")) CulvertType.BOX else CulvertType.PIPE,
                    spanOrDiameterM = c.culvertSpanOrDiameterM,
                    heightM = c.culvertHeightM,
                    barrels = c.barrels,
                    culvertSlopePercent = max(0.5, c.slopePercent),
                    designDischargeM3s = hydro.designDischargeM3s
                )

                updatedCrossings.add(
                    c.copy(
                        catchmentAreaKm2 = cat.areaKm2,
                        streamLengthKm = cat.streamLengthKm,
                        slopePercent = cat.avgSlopePercent,
                        designDischargeM3s = hydro.designDischargeM3s,
                        capacityDischargeM3s = analysis.fullCapacityM3s,
                        headwaterM = analysis.headwaterM,
                        headwaterRatio = analysis.headwaterRatio,
                        flowVelocityMs = analysis.flowVelocityMs,
                        flowControl = analysis.flowControl,
                        isAdequate = analysis.isAdequate,
                        scourProtectionRequired = analysis.scourProtectionRequired
                    )
                )
            }

            withContext(Dispatchers.Main) {
                _catchments.value = catchmentsList
                _crossings.value = updatedCrossings
                repository.insertCrossings(updatedCrossings)
            }
        }
    }

    fun autoSizeAllCulverts() {
        val currentCrossings = _crossings.value
        val updated = currentCrossings.map { c ->
            val opt = CulvertEngine.recommendOptimalCulvert(
                designDischargeM3s = c.designDischargeM3s,
                culvertSlopePercent = c.slopePercent,
                preferredType = if (c.designDischargeM3s > 5.0) CulvertType.BOX else CulvertType.PIPE
            )
            c.copy(
                culvertType = opt.type.name,
                culvertSpanOrDiameterM = opt.spanOrDiameterM,
                culvertHeightM = opt.heightM,
                barrels = opt.barrels,
                capacityDischargeM3s = opt.fullCapacityM3s,
                headwaterM = opt.headwaterM,
                headwaterRatio = opt.headwaterRatio,
                flowVelocityMs = opt.flowVelocityMs,
                flowControl = opt.flowControl,
                isAdequate = opt.isAdequate,
                scourProtectionRequired = opt.scourProtectionRequired,
                notes = opt.recommendationMn
            )
        }
        _crossings.value = updated
        viewModelScope.launch {
            repository.insertCrossings(updated)
        }
    }

    fun updateCrossing(crossing: DrainageCrossingEntity) {
        viewModelScope.launch {
            repository.updateCrossing(crossing)
            _crossings.value = _crossings.value.map { if (it.id == crossing.id) crossing else it }
            if (_selectedCrossing.value?.id == crossing.id) {
                _selectedCrossing.value = crossing
            }
        }
    }

    fun selectCrossing(crossing: DrainageCrossingEntity?) {
        _selectedCrossing.value = crossing
    }

    fun setMapCursor(point: GeoPoint?) {
        _mapCursorCoords.value = point
    }

    // Sandbox culvert calculator updates
    fun updateSandboxParams(
        q: Double = _calcDischarge.value,
        s: Double = _calcSlope.value,
        type: CulvertType = _calcType.value,
        span: Double = _calcSpan.value,
        height: Double = _calcHeight.value,
        barrels: Int = _calcBarrels.value
    ) {
        _calcDischarge.value = q
        _calcSlope.value = s
        _calcType.value = type
        _calcSpan.value = span
        _calcHeight.value = height
        _calcBarrels.value = barrels
        recalculateSandbox()
    }

    private fun recalculateSandbox() {
        val analysis = CulvertEngine.analyzeCulvert(
            type = _calcType.value,
            spanOrDiameterM = _calcSpan.value,
            heightM = _calcHeight.value,
            barrels = _calcBarrels.value,
            culvertSlopePercent = _calcSlope.value,
            designDischargeM3s = _calcDischarge.value
        )
        _calcAnalysis.value = analysis
    }

    suspend fun exportProjectJson(): String {
        val id = _selectedProjectId.value ?: return "{}"
        return repository.exportProjectJson(id)
    }

    suspend fun importProjectJson(json: String): Long {
        val newId = repository.importProjectJson(json)
        if (newId > 0) {
            selectProject(newId)
        }
        return newId
    }
}
