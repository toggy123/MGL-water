package com.example.mondrain.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectByIdDirect(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT * FROM drainage_crossings WHERE projectId = :projectId ORDER BY stationMeters ASC")
    fun getCrossingsForProject(projectId: Long): Flow<List<DrainageCrossingEntity>>

    @Query("SELECT * FROM drainage_crossings WHERE projectId = :projectId ORDER BY stationMeters ASC")
    suspend fun getCrossingsForProjectDirect(projectId: Long): List<DrainageCrossingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossing(crossing: DrainageCrossingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossings(crossings: List<DrainageCrossingEntity>)

    @Update
    suspend fun updateCrossing(crossing: DrainageCrossingEntity)

    @Delete
    suspend fun deleteCrossing(crossing: DrainageCrossingEntity)

    @Query("DELETE FROM drainage_crossings WHERE projectId = :projectId")
    suspend fun deleteCrossingsForProject(projectId: Long)
}
