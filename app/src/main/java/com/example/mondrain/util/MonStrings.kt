package com.example.mondrain.util

/**
 * Technical engineering translations and string resources for MON-DRAIN Engineer.
 * Default language: Mongolian (Монгол хэл). Switchable to English.
 */
enum class AppLanguage {
    MONGOLIAN,
    ENGLISH
}

object MonStrings {
    var currentLanguage: AppLanguage = AppLanguage.MONGOLIAN

    fun get(mn: String, en: String): String {
        return if (currentLanguage == AppLanguage.MONGOLIAN) mn else en
    }

    // App Identity
    val appTitle get() = get("MON-DRAIN Инженер", "MON-DRAIN Engineer")
    val appSubtitle get() = get("Авто замын ус зайлуулах байгууламжийн тооцоо", "Road Drainage & Hydrology Engineering")

    // Navigation Tabs
    val tabProjects get() = get("Төсөл", "Projects")
    val tabRoadDem get() = get("Трасс ба DEM", "Road & DEM")
    val tabMap get() = get("Газрын зураг", "Engineering Map")
    val tabHydrology get() = get("Гидрологи", "Hydrology")
    val tabCulverts get() = get("Хоолой", "Culverts")
    val tabReport get() = get("Тайлан", "Report")

    // Core Engineering Terminology
    val catchmentArea get() = get("Ус хурах талбай", "Catchment Area")
    val flowPath get() = get("Усны урсгалын үндсэн зам", "Flow Path")
    val flowAccumulation get() = get("Урсац хуримтлал", "Flow Accumulation")
    val flowDirection get() = get("Урсгалын чиглэл", "Flow Direction")
    val drainageCrossing get() = get("Ус зайлуулах огтлол", "Drainage Crossing")
    val designDischarge get() = get("Тооцооны зарцуулалт", "Design Discharge")
    val culvert get() = get("Ус зайлуулах хоолой", "Culvert")
    val boxCulvert get() = get("Тэгш өнцөгт ус зайлуулах байгууламж", "Box Culvert")
    val pipeCulvert get() = get("Дугуй төмөр бетон хоолой", "Circular Pipe Culvert")
    val archCulvert get() = get("Нум хэлбэрийн хоолой", "Arch Culvert")

    val culvertSizing get() = get("Хоолойн хэмжээ сонголт", "Culvert Sizing")
    val hydraulicCapacity get() = get("Гидравлик нэвтрүүлэх чадвар", "Hydraulic Capacity")
    val peakFlow get() = get("Оргил урсац", "Peak Flow")
    val slope get() = get("Хэвгий / Налуу", "Slope")
    val roughness get() = get("Барзгаржилтын итгэлцүүр (n)", "Manning Roughness (n)")
    val velocity get() = get("Урсгалын хурд (V)", "Flow Velocity (V)")
    val headwater get() = get("Дээд бьеийн усны түвшин (HW)", "Headwater Depth (HW)")
    val freeboard get() = get("Аюулгүйн нөөц өндөр (FB)", "Freeboard Clearance (FB)")
    val station get() = get("Пикет (ПК)", "Station (Ch)")

    // DEM / Terrain
    val demMode get() = get("DEM ГОРИМ", "DEM MODE")
    val onlineMode get() = get("ONLINE", "ONLINE")
    val offlineMode get() = get("OFFLINE", "OFFLINE")
    val demSource get() = get("DEM Эх сурвалж", "DEM Source")
    val resolution get() = get("Нарийвчлал", "Resolution")
    val activeCrs get() = get("Координатын систем (CRS)", "Active CRS")
    val verticalDatum get() = get("Өндрийн систем", "Vertical Datum")
    val coverage get() = get("Хамрах хүрээ", "Coverage")
    val cacheStatus get() = get("Кэшийн төлөв", "Cache Status")
    val downloadStatus get() = get("Татаж авах төлөв", "Download Status")
    val cached get() = get("Хадгалагдсан", "Cached locally")
    val notCached get() = get("Кэшлэгдээгүй", "Not cached")
    val hillshade get() = get("Гадаргын сүүдэржилт (Hillshade)", "Terrain Hillshade")
    val contourLines get() = get("Хэвтээ шугам (Контур)", "Contour Lines")

    // Road alignment
    val importKmlKmz get() = get("KML / KMZ Трасс оруулах", "Import KML / KMZ Alignment")
    val roadLength get() = get("Трассын урт", "Road Length")
    val roadSection get() = get("Замын хэсэг", "Road Section")
    val totalCrossings get() = get("Нийт огтлол", "Total Crossings")
    val alignmentInfo get() = get("Замын трассын мэдээлэл", "Road Alignment Info")

    // Status & Validation
    val statusAdequate get() = get("ХАНГАСАН (Аюулгүй)", "ADEQUATE (Safe)")
    val statusInadequate get() = get("ХҮРЭЛЦЭХГҮЙ (Өргөтгөх шаардлагатай)", "INADEQUATE (Needs resizing)")
    val inletControl get() = get("Оролтын удирдлагатай", "Inlet Control")
    val outletControl get() = get("Гаралтын удирдлагатай", "Outlet Control")
    val scourProtectionReq get() = get("Чулуун бэхэлгээ шаардлагатай (V > 2.0 м/с)", "Rip-Rap Protection Required (V > 2.0 m/s)")
    val scourSafe get() = get("Угаагдах аюулгүй (V ≤ 2.0 м/с)", "Scour Safe (V ≤ 2.0 m/s)")

    // Errors
    val demParseError get() = get(
        "DEM унших боломжгүй. Файлын формат эсвэл координатын системийг шалгана уу.",
        "Unable to parse DEM. Please verify file format or coordinate reference system."
    )
    val noRoadLoaded get() = get("Замын трасс оруулаагүй байна", "No road alignment loaded")
    val selectDemFirst get() = get("Эхлээд DEM сонгоно уу", "Please select DEM source first")

    // Project Actions
    val newProject get() = get("Шинэ төсөл", "New Project")
    val openProject get() = get("Төсөл нээх", "Open Project")
    val renameProject get() = get("Төсөл нэр солих", "Rename Project")
    val duplicateProject get() = get("Төсөл хувилах", "Duplicate Project")
    val deleteProject get() = get("Төсөл устгах", "Delete Project")
    val exportProject get() = get("Төсөл экспортлох", "Export Project")
    val importProject get() = get("Төсөл импортлох", "Import Project")
    val backupProject get() = get("Нөөц хуулбар хийх", "Backup Project")
}
