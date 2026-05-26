package com.google.mediapipe.examples.llminference.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "medical_relationships",
    foreignKeys = [
        ForeignKey(
            entity = MedicalEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MedicalEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
       Index(value = ["sourceId"]),
        Index(value = ["targetId"])
    ]
)

data class MedicalRelationship(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val targetId: Long,
    val relationType: String, // e.g., "TREATS", "CONTRAINDICATED_IN", "DAMAGE_RISK"
    val notes: String? = null // e.g., "Severe risk in chronic alcohol abuse"
)

