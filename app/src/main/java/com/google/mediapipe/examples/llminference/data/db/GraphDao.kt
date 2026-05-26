package com.google.mediapipe.examples.llminference.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class GraphRelationResult(
    val sourceName: String,
    val sourceDomain: String,
    val relationType: String,
    val targetName: String,
    val targetDomain: String,
    val notes: String?
)

@Dao
interface GraphDao{
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertEntity(entity: MedicalEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertRelationship(relationship: MedicalRelationship)

    @Query("SELECT * FROM medical_entities WHERE name LIKE :term LIMIT 1")
    fun findEntityByName(term: String): MedicalEntity?

    /**
     * Looks up direct 1-hop relationships between any two matching concepts
     */
    @Query("""
        SELECT 
            e1.name AS sourceName, e1.domain AS sourceDomain, 
            r.relationType AS relationType, 
            e2.name AS targetName, e2.domain AS targetDomain,
            r.notes AS notes
        FROM medical_relationships r
        JOIN medical_entities e1 ON r.sourceId = e1.id
        JOIN medical_entities e2 ON r.targetId = e2.id
        WHERE (e1.name LIKE :entityA AND e2.name LIKE :entityB)
           OR (e1.name LIKE :entityB AND e2.name LIKE :entityA)
    """)
    fun getDirectRelationships(entityA: String, entityB: String): List<GraphRelationResult>

}
