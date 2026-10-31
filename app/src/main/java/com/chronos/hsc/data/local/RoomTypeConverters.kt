package com.chronos.hsc.data.local

import androidx.room.TypeConverter

class RoomTypeConverters {

    @TypeConverter
    fun fromHscSubject(subject: HscSubject): String = subject.name

    @TypeConverter
    fun toHscSubject(value: String): HscSubject =
        runCatching { HscSubject.valueOf(value) }.getOrDefault(HscSubject.PHYSICS)

    @TypeConverter
    fun fromHscPaper(paper: HscPaper): String = paper.name

    @TypeConverter
    fun toHscPaper(value: String): HscPaper =
        runCatching { HscPaper.valueOf(value) }.getOrDefault(HscPaper.FIRST_PAPER)

    @TypeConverter
    fun fromDistributionCurve(curve: DistributionCurve): String = curve.name

    @TypeConverter
    fun toDistributionCurve(value: String): DistributionCurve =
        runCatching { DistributionCurve.valueOf(value) }.getOrDefault(DistributionCurve.CONCEPT_TO_PROBLEM)

    @TypeConverter
    fun fromEnergyLevel(level: EnergyLevel?): String? = level?.name

    @TypeConverter
    fun toEnergyLevel(value: String?): EnergyLevel? =
        value?.let { runCatching { EnergyLevel.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromNeuroDrillType(drillType: NeuroDrillType): String = drillType.name

    @TypeConverter
    fun toNeuroDrillType(value: String): NeuroDrillType =
        runCatching { NeuroDrillType.valueOf(value) }.getOrDefault(NeuroDrillType.FOVEAL_LOCK)
}
