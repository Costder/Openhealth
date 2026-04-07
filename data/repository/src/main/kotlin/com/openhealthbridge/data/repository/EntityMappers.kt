package com.openhealthbridge.data.repository

import com.openhealthbridge.core.models.ActivityEntry
import com.openhealthbridge.core.models.CycleEntry
import com.openhealthbridge.core.models.DailyHealthSnapshot
import com.openhealthbridge.core.models.DataSource
import com.openhealthbridge.core.models.EntrySourceType
import com.openhealthbridge.core.models.ExerciseSetEntry
import com.openhealthbridge.core.models.HealthFlag
import com.openhealthbridge.core.models.NutritionEntry
import com.openhealthbridge.core.models.PRRecord
import com.openhealthbridge.core.models.PrType
import com.openhealthbridge.core.models.RecoveryEntry
import com.openhealthbridge.core.models.WorkoutEntry
import com.openhealthbridge.data.db.ActivityEntryEntity
import com.openhealthbridge.data.db.CycleEntryEntity
import com.openhealthbridge.data.db.DailySnapshotEntity
import com.openhealthbridge.data.db.ExerciseSetEntryEntity
import com.openhealthbridge.data.db.NutritionEntryEntity
import com.openhealthbridge.data.db.PrRecordEntity
import com.openhealthbridge.data.db.RecoveryEntryEntity
import com.openhealthbridge.data.db.WorkoutEntryEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

internal fun DailySnapshotEntity.toDomain(): DailyHealthSnapshot = DailyHealthSnapshot(
    date = LocalDate.parse(date),
    weightKg = weightKg,
    heightM = heightM,
    stepCount = stepCount,
    sleepHours = sleepHours,
    caloriesConsumed = caloriesConsumed,
    activeCaloriesBurned = activeCaloriesBurned,
    totalCaloriesBurned = totalCaloriesBurned,
    workoutCount = workoutCount,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    recoveryScore = recoveryScore,
    flags = emptyList(),
    lastSyncedAt = Instant.parse(lastSyncedAt),
    dataAvailability = parseDataAvailability(dataAvailabilityJson)
)

internal fun DailyHealthSnapshot.toEntity(): DailySnapshotEntity = DailySnapshotEntity(
    date = date.toString(),
    weightKg = weightKg,
    heightM = heightM,
    stepCount = stepCount,
    sleepHours = sleepHours,
    caloriesConsumed = caloriesConsumed,
    activeCaloriesBurned = activeCaloriesBurned,
    totalCaloriesBurned = totalCaloriesBurned,
    workoutCount = workoutCount,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    recoveryScore = recoveryScore,
    lastSyncedAt = lastSyncedAt.toString(),
    dataAvailabilityJson = encodeDataAvailability(dataAvailability)
)

internal fun WorkoutEntryEntity.toDomain(exerciseSets: List<ExerciseSetEntry>): WorkoutEntry = WorkoutEntry(
    id = id,
    startTime = OffsetDateTime.parse(startTime),
    endTime = OffsetDateTime.parse(endTime),
    sourceType = sourceType.toEntrySourceType(),
    workoutType = workoutType,
    completed = completed,
    notes = notes,
    perceivedDifficulty = perceivedDifficulty,
    painFlag = painFlag,
    exerciseSets = exerciseSets
)

internal fun WorkoutEntry.toEntity(): WorkoutEntryEntity = WorkoutEntryEntity(
    id = id,
    startTime = startTime.toString(),
    endTime = endTime.toString(),
    sourceType = sourceType.name,
    workoutType = workoutType,
    completed = completed,
    notes = notes,
    perceivedDifficulty = perceivedDifficulty,
    painFlag = painFlag
)

internal fun ExerciseSetEntryEntity.toDomain(): ExerciseSetEntry = ExerciseSetEntry(
    id = id,
    workoutId = workoutId,
    exerciseName = exerciseName,
    setIndex = setIndex,
    reps = reps,
    weightKg = weightKg,
    durationSeconds = durationSeconds,
    distanceMeters = distanceMeters,
    isBodyweight = isBodyweight,
    notes = notes
)

internal fun ExerciseSetEntry.toEntity(): ExerciseSetEntryEntity = ExerciseSetEntryEntity(
    id = id,
    workoutId = workoutId,
    exerciseName = exerciseName,
    setIndex = setIndex,
    reps = reps,
    weightKg = weightKg,
    durationSeconds = durationSeconds,
    distanceMeters = distanceMeters,
    isBodyweight = isBodyweight,
    notes = notes
)

internal fun NutritionEntryEntity.toDomain(): NutritionEntry = NutritionEntry(
    id = id,
    timestamp = OffsetDateTime.parse(timestamp),
    mealName = mealName,
    sourceType = sourceType.toEntrySourceType(),
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    notes = notes
)

internal fun NutritionEntry.toEntity(): NutritionEntryEntity = NutritionEntryEntity(
    id = id,
    timestamp = timestamp.toString(),
    mealName = mealName,
    sourceType = sourceType.name,
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    notes = notes
)

internal fun RecoveryEntryEntity.toDomain(): RecoveryEntry = RecoveryEntry(
    id = id,
    timestamp = OffsetDateTime.parse(timestamp),
    sleepHours = sleepHours,
    sleepQuality = sleepQuality,
    sorenessLevel = sorenessLevel,
    fatigueLevel = fatigueLevel,
    painLevel = painLevel,
    stressLevel = stressLevel,
    notes = notes
)

internal fun RecoveryEntry.toEntity(): RecoveryEntryEntity = RecoveryEntryEntity(
    id = id,
    timestamp = timestamp.toString(),
    sleepHours = sleepHours,
    sleepQuality = sleepQuality,
    sorenessLevel = sorenessLevel,
    fatigueLevel = fatigueLevel,
    painLevel = painLevel,
    stressLevel = stressLevel,
    notes = notes
)

internal fun CycleEntryEntity.toDomain(): CycleEntry = CycleEntry(
    id = id,
    timestamp = OffsetDateTime.parse(timestamp),
    sourceType = sourceType.toEntrySourceType(),
    flow = flow,
    isStartOfCycle = isStartOfCycle,
    bbtCelsius = bbtCelsius,
    ovulationTestResult = ovulationTestResult,
    cervicalMucusAppearance = cervicalMucusAppearance,
    intermenstrualBleeding = intermenstrualBleeding,
    notes = notes
)

internal fun CycleEntry.toEntity(): CycleEntryEntity = CycleEntryEntity(
    id = id,
    timestamp = timestamp.toString(),
    sourceType = sourceType.name,
    flow = flow,
    isStartOfCycle = isStartOfCycle,
    bbtCelsius = bbtCelsius,
    ovulationTestResult = ovulationTestResult,
    cervicalMucusAppearance = cervicalMucusAppearance,
    intermenstrualBleeding = intermenstrualBleeding,
    notes = notes
)

internal fun ActivityEntryEntity.toDomain(): ActivityEntry = ActivityEntry(
    id = id,
    timestamp = OffsetDateTime.parse(timestamp),
    sourceType = sourceType.toEntrySourceType(),
    wasProtected = wasProtected,
    notes = notes
)

internal fun ActivityEntry.toEntity(): ActivityEntryEntity = ActivityEntryEntity(
    id = id,
    timestamp = timestamp.toString(),
    sourceType = sourceType.name,
    wasProtected = wasProtected,
    notes = notes
)

internal fun PrRecordEntity.toDomain(): PRRecord = PRRecord(
    id = id,
    exerciseName = exerciseName,
    prType = PrType.valueOf(prType),
    value = value,
    unit = unit,
    previousValue = previousValue,
    achievedAt = OffsetDateTime.parse(achievedAt),
    sourceWorkoutId = sourceWorkoutId,
    deduplicationKey = deduplicationKey
)

internal fun PRRecord.toEntity(): PrRecordEntity = PrRecordEntity(
    id = id,
    exerciseName = exerciseName,
    prType = prType.name,
    value = value,
    unit = unit,
    previousValue = previousValue,
    achievedAt = achievedAt.toString(),
    sourceWorkoutId = sourceWorkoutId,
    deduplicationKey = deduplicationKey
)

private fun String.toEntrySourceType(): EntrySourceType = runCatching { EntrySourceType.valueOf(this) }
    .getOrDefault(EntrySourceType.MANUAL)

private fun parseDataAvailability(raw: String): Map<String, DataSource> {
    val json = JSONObject(raw)
    return buildMap {
        json.keys().forEach { key ->
            val value = runCatching { DataSource.valueOf(json.optString(key, DataSource.UNAVAILABLE.name)) }
                .getOrDefault(DataSource.UNAVAILABLE)
            put(key, value)
        }
    }
}

private fun encodeDataAvailability(input: Map<String, DataSource>): String {
    val json = JSONObject()
    input.forEach { (key, value) -> json.put(key, value.name) }
    return json.toString()
}

internal fun encodeFlags(flags: List<HealthFlag>): String = JSONArray(flags.map { it.name }).toString()
