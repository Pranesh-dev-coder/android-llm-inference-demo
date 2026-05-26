package com.google.mediapipe.examples.llminference.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "medical_entities",
    indices = [Index(value = ["name"], unique = true)] // Speeds up search by term
)
data class MedicalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,         // e.g., "Acetaminophen", "Liver Disease"
    val domain: String,       // e.g., "Treatment", "Disease", "Body_Part"
    val description: String? = null
)