package com.github.radupana.featherweight.data.exercise

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WgerExerciseResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<WgerExercise>
)

@Serializable
data class WgerExercise(
    val id: Int,
    val uuid: String,
    val created: String,
    @SerialName("last_update")
    val lastUpdate: String,
    @SerialName("last_update_global")
    val lastUpdateGlobal: String,
    val category: WgerCategory,
    val muscles: List<WgerMuscle> = emptyList(),
    @SerialName("muscles_secondary")
    val musclesSecondary: List<WgerMuscle> = emptyList(),
    val equipment: List<WgerEquipment> = emptyList(),
    val license: WgerLicense,
    @SerialName("license_author")
    val licenseAuthor: String?,
    val images: List<WgerImage> = emptyList(),
    val translations: List<WgerTranslation> = emptyList(),
    val variations: List<Int>? = emptyList(),
    val videos: List<WgerVideo> = emptyList(),
    @SerialName("author_history")
    val authorHistory: List<String> = emptyList(),
    @SerialName("total_authors_history")
    val totalAuthorsHistory: List<String> = emptyList()
)

@Serializable
data class WgerCategory(
    val id: Int,
    val name: String
)

@Serializable
data class WgerMuscle(
    val id: Int,
    val name: String,
    @SerialName("name_en")
    val nameEn: String,
    @SerialName("is_front")
    val isFront: Boolean,
    @SerialName("image_url_main")
    val imageUrlMain: String?,
    @SerialName("image_url_secondary")
    val imageUrlSecondary: String?
)

@Serializable
data class WgerEquipment(
    val id: Int,
    val name: String
)

@Serializable
data class WgerLicense(
    val id: Int,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("short_name")
    val shortName: String,
    val url: String?
)

@Serializable
data class WgerLanguage(
    val id: Int,
    @SerialName("short_name")
    val shortName: String,
    @SerialName("full_name")
    val fullName: String
)

@Serializable
data class WgerTranslation(
    val id: Int,
    val uuid: String,
    val name: String,
    val exercise: Int,
    val description: String,
    val created: String,
    val language: Int,
    val aliases: List<WgerAlias> = emptyList(),
    val notes: List<String> = emptyList(),
    val license: Int,
    @SerialName("license_title")
    val licenseTitle: String,
    @SerialName("license_object_url")
    val licenseObjectUrl: String,
    @SerialName("license_author")
    val licenseAuthor: String,
    @SerialName("license_author_url")
    val licenseAuthorUrl: String,
    @SerialName("license_derivative_source_url")
    val licenseDerivativeSourceUrl: String,
    @SerialName("author_history")
    val authorHistory: List<String> = emptyList()
)

@Serializable
data class WgerAlias(
    val id: Int,
    val uuid: String,
    val alias: String
)

@Serializable
data class WgerImage(
    val id: Int,
    val uuid: String,
    val exercise: Int,
    @SerialName("exercise_uuid")
    val exerciseUuid: String,
    val image: String,
    @SerialName("is_main")
    val isMain: Boolean,
    val style: String,
    val license: Int,
    @SerialName("license_title")
    val licenseTitle: String,
    @SerialName("license_object_url")
    val licenseObjectUrl: String,
    @SerialName("license_author")
    val licenseAuthor: String?,
    @SerialName("license_author_url")
    val licenseAuthorUrl: String,
    @SerialName("license_derivative_source_url")
    val licenseDerivativeSourceUrl: String,
    @SerialName("author_history")
    val authorHistory: List<String> = emptyList()
)

@Serializable
data class WgerVideo(
    val id: Int,
    val uuid: String,
    val exercise: Int,
    @SerialName("exercise_uuid")
    val exerciseUuid: String,
    val video: String,
    @SerialName("is_main")
    val isMain: Boolean,
    val size: Int,
    val duration: String,
    val width: Int,
    val height: Int,
    val codec: String,
    @SerialName("codec_long")
    val codecLong: String,
    val license: Int,
    @SerialName("license_title")
    val licenseTitle: String,
    @SerialName("license_object_url")
    val licenseObjectUrl: String,
    @SerialName("license_author")
    val licenseAuthor: String?,
    @SerialName("license_author_url")
    val licenseAuthorUrl: String,
    @SerialName("license_derivative_source_url")
    val licenseDerivativeSourceUrl: String,
    @SerialName("author_history")
    val authorHistory: List<String> = emptyList()
)