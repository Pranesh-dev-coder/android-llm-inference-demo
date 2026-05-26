package com.google.mediapipe.examples.llminference.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

@Database(entities = [MedicalEntity::class, MedicalRelationship::class], version = 1, exportSchema = false)

abstract class MedicalDatabase : RoomDatabase() {
    abstract fun graphDao(): GraphDao

    companion object{
        @Volatile
        private var INSTANCE: MedicalDatabase? = null

        fun getDatabase(context: Context): MedicalDatabase{
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedicalDatabase::class.java,
                    "medical_graph_db"
                )
                    .addCallback(object : RoomDatabase.Callback(){
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Executors.newSingleThreadExecutor().execute {
                                seedDatabase(getDatabase(context).graphDao())
                            }
                        }
                    })
                    .build()
                     INSTANCE = instance
                     instance
            }
        }

        private fun seedDatabase(dao: GraphDao){
            // ============ TREATMENTS / MEDICATIONS ============
            val acetaminophen = dao.insertEntity(MedicalEntity(name = "Acetaminophen", domain = "Treatment", description = "Common analgesic and antipyretic"))
            val ibuprofen = dao.insertEntity(MedicalEntity(name = "Ibuprofen", domain = "Treatment", description = "NSAID for pain and inflammation"))
            val aspirin = dao.insertEntity(MedicalEntity(name = "Aspirin", domain = "Treatment", description = "NSAID and antiplatelet agent"))
            val metformin = dao.insertEntity(MedicalEntity(name = "Metformin", domain = "Treatment", description = "First-line oral medication for Type 2 Diabetes"))
            val insulin = dao.insertEntity(MedicalEntity(name = "Insulin", domain = "Treatment", description = "Hormone for blood glucose regulation"))
            val lisinopril = dao.insertEntity(MedicalEntity(name = "Lisinopril", domain = "Treatment", description = "ACE inhibitor for hypertension"))
            val amlodipine = dao.insertEntity(MedicalEntity(name = "Amlodipine", domain = "Treatment", description = "Calcium channel blocker for hypertension"))
            val atorvastatin = dao.insertEntity(MedicalEntity(name = "Atorvastatin", domain = "Treatment", description = "Statin for cholesterol management"))
            val albuterol = dao.insertEntity(MedicalEntity(name = "Albuterol", domain = "Treatment", description = "Bronchodilator for asthma/COPD"))
            val warfarin = dao.insertEntity(MedicalEntity(name = "Warfarin", domain = "Treatment", description = "Anticoagulant for blood clots"))
            val prednisone = dao.insertEntity(MedicalEntity(name = "Prednisone", domain = "Treatment", description = "Corticosteroid for inflammation/autoimmune"))
            val omeprazole = dao.insertEntity(MedicalEntity(name = "Omeprazole", domain = "Treatment", description = "PPI for GERD and ulcers"))
            val contrastDye = dao.insertEntity(MedicalEntity(name = "Contrast Dye", domain = "Diagnostic_Tool", description = "IV contrast for imaging studies"))


            // ============ BODY PARTS / ORGANS ============
            val liver = dao.insertEntity(MedicalEntity(name = "Liver", domain = "Body_Part", description = "Primary metabolic organ"))
            val kidney = dao.insertEntity(MedicalEntity(name = "Kidney", domain = "Body_Part", description = "Filters blood and produces urine"))
            val heart = dao.insertEntity(MedicalEntity(name = "Heart", domain = "Body_Part", description = "Pumps blood throughout body"))
            val lungs = dao.insertEntity(MedicalEntity(name = "Lungs", domain = "Body_Part", description = "Gas exchange organ for respiration"))
            val stomach = dao.insertEntity(MedicalEntity(name = "Stomach", domain = "Body_Part", description = "Digestive organ for food breakdown"))
            val pancreas = dao.insertEntity(MedicalEntity(name = "Pancreas", domain = "Body_Part", description = "Produces insulin and digestive enzymes"))
            val brain = dao.insertEntity(MedicalEntity(name = "Brain", domain = "Body_Part", description = "Central nervous system organ"))

            // ============ DISEASES / CONDITIONS ============
            val liverDisease = dao.insertEntity(MedicalEntity(name = "Liver Disease", domain = "Disease", description = "Hepatic impairment or cirrhosis"))
            val chronicKidneyDisease = dao.insertEntity(MedicalEntity(name = "CKD", domain = "Disease", description = "Chronic Kidney Disease"))
            val hypertension = dao.insertEntity(MedicalEntity(name = "Hypertension", domain = "Disease", description = "High blood pressure"))
            val type2Diabetes = dao.insertEntity(MedicalEntity(name = "Type 2 Diabetes", domain = "Disease", description = "Insulin resistance disorder"))
            val asthma = dao.insertEntity(MedicalEntity(name = "Asthma", domain = "Disease", description = "Chronic airway inflammation"))
            val heartFailure = dao.insertEntity(MedicalEntity(name = "Heart Failure", domain = "Disease", description = "Reduced cardiac pumping ability"))
            val gerd = dao.insertEntity(MedicalEntity(name = "GERD", domain = "Disease", description = "Gastroesophageal reflux disease"))
            val pepticUlcer = dao.insertEntity(MedicalEntity(name = "Peptic Ulcer", domain = "Disease", description = "Sores in stomach lining"))
            val atrialFibrillation = dao.insertEntity(MedicalEntity(name = "Atrial Fibrillation", domain = "Disease", description = "Irregular heartbeat"))
            val copd = dao.insertEntity(MedicalEntity(name = "COPD", domain = "Disease", description = "Chronic obstructive pulmonary disease"))
            val pneumonia = dao.insertEntity(MedicalEntity(name = "Pneumonia", domain = "Disease", description = "Lung infection"))

            // ============ RISK FACTORS ============
            val alcohol = dao.insertEntity(MedicalEntity(name = "Alcohol Abuse", domain = "Risk_Factor", description = "Chronic heavy drinking"))
            val smoking = dao.insertEntity(MedicalEntity(name = "Smoking", domain = "Risk_Factor", description = "Tobacco use"))
            val obesity = dao.insertEntity(MedicalEntity(name = "Obesity", domain = "Risk_Factor", description = "BMI > 30"))
            val sedentaryLifestyle = dao.insertEntity(MedicalEntity(name = "Sedentary Lifestyle", domain = "Risk_Factor", description = "Physical inactivity"))
            val highSaltDiet = dao.insertEntity(MedicalEntity(name = "High Salt Diet", domain = "Risk_Factor", description = "Excessive sodium intake"))
            val familyHistory = dao.insertEntity(MedicalEntity(name = "Family History", domain = "Risk_Factor", description = "Genetic predisposition"))
            val ageOver65 = dao.insertEntity(MedicalEntity(name = "Age > 65 years", domain = "Risk_Factor", description = "Elderly age group"))

            // ============ DIAGNOSTIC TOOLS / TESTS ============
            val gfr = dao.insertEntity(MedicalEntity(name = "GFR", domain = "Diagnostic_Tool", description = "Glomerular Filtration Rate measuring kidney function"))
            val bpMonitor = dao.insertEntity(MedicalEntity(name = "Blood Pressure Monitor", domain = "Diagnostic_Tool", description = "Measures systolic/diastolic pressure"))
            val hba1c = dao.insertEntity(MedicalEntity(name = "HbA1c", domain = "Diagnostic_Tool", description = "Average blood glucose over 3 months"))
            val ecg = dao.insertEntity(MedicalEntity(name = "ECG/EKG", domain = "Diagnostic_Tool", description = "Records heart electrical activity"))
            val chestXray = dao.insertEntity(MedicalEntity(name = "Chest X-Ray", domain = "Diagnostic_Tool", description = "Imaging of lungs and heart"))
            val spirometry = dao.insertEntity(MedicalEntity(name = "Spirometry", domain = "Diagnostic_Tool", description = "Lung function test"))
            val endoscopy = dao.insertEntity(MedicalEntity(name = "Endoscopy", domain = "Diagnostic_Tool", description = "Visualizes GI tract"))
            val lipidPanel = dao.insertEntity(MedicalEntity(name = "Lipid Panel", domain = "Diagnostic_Tool", description = "Measures cholesterol levels"))

            // ============ SYMPTOMS ============
            val chestPain = dao.insertEntity(MedicalEntity(name = "Chest Pain", domain = "Symptom", description = "Discomfort in chest area"))
            val dyspnea = dao.insertEntity(MedicalEntity(name = "Shortness of Breath", domain = "Symptom", description = "Difficulty breathing"))
            val edema = dao.insertEntity(MedicalEntity(name = "Edema", domain = "Symptom", description = "Swelling in extremities"))
            val fatigue = dao.insertEntity(MedicalEntity(name = "Fatigue", domain = "Symptom", description = "Excessive tiredness"))
            val polydipsia = dao.insertEntity(MedicalEntity(name = "Polydipsia", domain = "Symptom", description = "Excessive thirst"))
            val polyuria = dao.insertEntity(MedicalEntity(name = "Polyuria", domain = "Symptom", description = "Frequent urination"))

            // ============ RELATIONSHIPS (TREATMENT → DISEASE) ============
            dao.insertRelationship(MedicalRelationship(
                sourceId = metformin, targetId = type2Diabetes,
                relationType = "TREATS", notes = "First-line therapy for Type 2 Diabetes"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = insulin, targetId = type2Diabetes,
                relationType = "TREATS", notes = "Advanced or uncontrolled diabetes"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = lisinopril, targetId = hypertension,
                relationType = "TREATS", notes = "ACE inhibitor for blood pressure control"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = amlodipine, targetId = hypertension,
                relationType = "TREATS", notes = "Calcium channel blocker alternative"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = atorvastatin, targetId = heartFailure,
                relationType = "PREVENTS", notes = "Statin reduces cardiovascular events"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = albuterol, targetId = asthma,
                relationType = "TREATS", notes = "Rescue bronchodilator for acute attacks"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = omeprazole, targetId = gerd,
                relationType = "TREATS", notes = "PPI reduces stomach acid"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = omeprazole, targetId = pepticUlcer,
                relationType = "TREATS", notes = "Allows ulcer healing"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = warfarin, targetId = atrialFibrillation,
                relationType = "TREATS", notes = "Prevents stroke from clots"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = prednisone, targetId = asthma,
                relationType = "TREATS", notes = "Systemic corticosteroids for severe asthma"
            ))

            // ============ RELATIONSHIPS (DRUG → ORGAN) ============
            dao.insertRelationship(MedicalRelationship(
                sourceId = acetaminophen, targetId = liver,
                relationType = "METABOLIZED_BY", notes = "Primary hepatic pathway via CYP2E1"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = ibuprofen, targetId = kidney,
                relationType = "AFFECTS", notes = "NSAIDs can reduce renal blood flow"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = lisinopril, targetId = kidney,
                relationType = "PROTECTS", notes = "ACE inhibitors are reno-protective in diabetics"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = metformin, targetId = kidney,
                relationType = "EXCRETED_BY", notes = "90% renally excreted - contraindicated in advanced CKD"
            ))

            // ============ RELATIONSHIPS (RISK FACTOR → DISEASE) ============
            dao.insertRelationship(MedicalRelationship(
                sourceId = alcohol, targetId = liverDisease,
                relationType = "EXACERBATES", notes = "Synergistically accelerates hepatic cirrhosis"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = smoking, targetId = copd,
                relationType = "CAUSES", notes = "Primary cause of COPD development"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = smoking, targetId = lungs,
                relationType = "DAMAGES", notes = "Destroys lung tissue and cilia"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = obesity, targetId = type2Diabetes,
                relationType = "INCREASES_RISK", notes = "Insulin resistance from adiposity"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = obesity, targetId = hypertension,
                relationType = "INCREASES_RISK", notes = "Volume overload and RAAS activation"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = highSaltDiet, targetId = hypertension,
                relationType = "CAUSES", notes = "Sodium-induced fluid retention"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = sedentaryLifestyle, targetId = obesity,
                relationType = "LEADS_TO", notes = "Caloric imbalance and metabolic slowdown"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = familyHistory, targetId = type2Diabetes,
                relationType = "PREDISPOSES", notes = "Genetic susceptibility"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = ageOver65, targetId = hypertension,
                relationType = "INCREASES_RISK", notes = "Age-related vascular stiffening"
            ))

            // ============ RELATIONSHIPS (DIAGNOSTIC → DISEASE) ============
            dao.insertRelationship(MedicalRelationship(
                sourceId = gfr, targetId = chronicKidneyDisease,
                relationType = "DIAGNOSES", notes = "GFR below 60 for 3+ months indicates CKD"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = bpMonitor, targetId = hypertension,
                relationType = "DIAGNOSES", notes = "Sustained BP > 130/80 mmHg"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = hba1c, targetId = type2Diabetes,
                relationType = "DIAGNOSES", notes = "HbA1c ≥ 6.5% indicates diabetes"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = ecg, targetId = atrialFibrillation,
                relationType = "DIAGNOSES", notes = "Detects irregular rhythm"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = spirometry, targetId = copd,
                relationType = "DIAGNOSES", notes = "FEV1/FVC < 0.70 post-bronchodilator"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = chestXray, targetId = pneumonia,
                relationType = "DIAGNOSES", notes = "Shows infiltrates on imaging"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = lipidPanel, targetId = heartFailure,
                relationType = "RISK_ASSESSMENT", notes = "High LDL associated with cardiovascular disease"
            ))

            // ============ RELATIONSHIPS (SYMPTOM → DISEASE) ============
            dao.insertRelationship(MedicalRelationship(
                sourceId = chestPain, targetId = heartFailure,
                relationType = "INDICATES", notes = "Angina from reduced coronary flow"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = dyspnea, targetId = copd,
                relationType = "INDICATES", notes = "Air trapping and hyperinflation"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = dyspnea, targetId = heartFailure,
                relationType = "INDICATES", notes = "Pulmonary congestion"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = edema, targetId = heartFailure,
                relationType = "INDICATES", notes = "Right heart failure causes peripheral edema"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = edema, targetId = chronicKidneyDisease,
                relationType = "INDICATES", notes = "Fluid retention from renal dysfunction"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = polydipsia, targetId = type2Diabetes,
                relationType = "INDICATES", notes = "Osmotic diuresis causing thirst"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = polyuria, targetId = type2Diabetes,
                relationType = "INDICATES", notes = "Glucose-induced osmotic diuresis"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = fatigue, targetId = chronicKidneyDisease,
                relationType = "INDICATES", notes = "Anemia from reduced erythropoietin"
            ))

            // ============ DRUG CONTRAINDICATIONS ============
            dao.insertRelationship(MedicalRelationship(
                sourceId = acetaminophen, targetId = liverDisease,
                relationType = "CONTRAINDICATED_IN", notes = "Severe liver damage warning if dosage exceeds 2g daily"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = ibuprofen, targetId = pepticUlcer,
                relationType = "CONTRAINDICATED_IN", notes = "NSAIDs worsen GI bleeding risk"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = metformin, targetId = chronicKidneyDisease,
                relationType = "CONTRAINDICATED_IN", notes = "Lactic acidosis risk when GFR < 30 mL/min"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = warfarin, targetId = pepticUlcer,
                relationType = "CONTRAINDICATED_IN", notes = "Increased bleeding risk"
            ))

            // ============ DRUG INTERACTIONS ============
            dao.insertRelationship(MedicalRelationship(
                sourceId = warfarin, targetId = aspirin,
                relationType = "INTERACTS_WITH", notes = "Increased bleeding risk (synergistic anticoagulation)"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = lisinopril, targetId = ibuprofen,
                relationType = "INTERACTS_WITH", notes = "Reduced antihypertensive effect + kidney injury risk"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = metformin, targetId = contrastDye,  // You'd need to add contrastDye entity
                relationType = "INTERACTS_WITH", notes = "Increased lactic acidosis risk"
            ))

            // ============ DISEASE PROGRESSION ============
            dao.insertRelationship(MedicalRelationship(
                sourceId = hypertension, targetId = heartFailure,
                relationType = "LEADS_TO", notes = "Chronic pressure overload causes hypertrophy and failure"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = type2Diabetes, targetId = chronicKidneyDisease,
                relationType = "LEADS_TO", notes = "Diabetic nephropathy causes progressive renal decline"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = type2Diabetes, targetId = heartFailure,
                relationType = "LEADS_TO", notes = "Diabetic cardiomyopathy"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = obesity, targetId = type2Diabetes,
                relationType = "LEADS_TO", notes = "Insulin resistance pathway"
            ))
            dao.insertRelationship(MedicalRelationship(
                sourceId = gerd, targetId = pepticUlcer,
                relationType = "COMPLICATES", notes = "Chronic acid exposure"
            ))

        }
    }
}