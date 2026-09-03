package com.example.mondrain.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.mondrain.hydraulic.CulvertType

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectName: String,
    val projectNumber: String,
    val client: String,
    val designer: String,
    val location: String,
    val province: String,      // Аймаг
    val district: String,      // Сум
    val roadName: String,
    val roadSection: String,
    val dateCreated: String,
    val coordinateSystem: String = "WGS 84 / UTM Zone 48N",
    val demSourceInfo: String = "NASADEM 1 arc-second (~30 m)",
    val demMode: String = "ONLINE",
    val rainfallInfo: String = "БНбД 2.01.14-83 (H_p% = 65 мм, P = 2%)",
    val hydrologyMethod: String = "БНбД 2.01.14-83 & Рационал арга",
    val returnPeriodPercent: Double = 2.0, // 2% (50-year return period)
    val calculationVersion: String = "v1.2-MN",
    val fieldObservations: String = "Хээр талын хэвгий гадарга, хуурай сайр, уруйн үерийн аюултай бүс.",
    val attachedFilesCount: Int = 1,
    val photosCount: Int = 0,
    val roadLengthMeters: Double = 12540.0,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "drainage_crossings",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class DrainageCrossingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val stationMeters: Double,
    val stationLabel: String,       // e.g. "ПК 15+40"
    val latitude: Double,
    val longitude: Double,
    val utmEasting: Double,
    val utmNorthing: Double,
    val catchmentAreaKm2: Double,
    val streamLengthKm: Double,
    val slopePercent: Double,
    val runoffCoeff: Double,
    val designDischargeM3s: Double, // Q_req
    val culvertType: String = CulvertType.PIPE.name,
    val culvertSpanOrDiameterM: Double = 1.25,
    val culvertHeightM: Double = 1.25,
    val barrels: Int = 1,           // 1=Дан, 2=Хос, 3=Гурвалсан
    val capacityDischargeM3s: Double = 3.2,
    val headwaterM: Double = 1.1,
    val headwaterRatio: Double = 0.88,
    val flowVelocityMs: Double = 1.95,
    val flowControl: String = "Оролтын удирдлага",
    val isAdequate: Boolean = true,
    val scourProtectionRequired: Boolean = false,
    val notes: String = "Хэвийн ус зайлуулалттай"
)
