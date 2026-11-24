package com.kelompok.anemom.data.remote.dto

import com.google.gson.annotations.SerializedName

// Kontrak: docs/API.md (backend). Nama field mengikuti JSON server (snake_case).

data class LoginRequest(
    val email: String,
    val password: String,
    @SerializedName("device_name") val deviceName: String? = null
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    /** yyyy-MM-dd */
    @SerializedName("birth_date") val birthDate: String,
    /** yyyy-MM-dd */
    val hpht: String,
    @SerializedName("device_name") val deviceName: String? = null
)

/**
 * Isi PUT /me. Semua field dikirim lengkap setiap kali simpan.
 * [healthFacility] kosong ("") = hapus fasilitas kesehatan (server menyimpannya sebagai NULL).
 */
data class UpdateProfileRequest(
    val name: String,
    val email: String,
    /** yyyy-MM-dd */
    @SerializedName("birth_date") val birthDate: String,
    /** yyyy-MM-dd */
    val hpht: String,
    @SerializedName("health_facility") val healthFacility: String
)

/** Isi `data` dari register/login. */
data class AuthData(
    val token: String = "",
    @SerializedName("expires_at") val expiresAt: String? = null,
    val user: UserDto = UserDto()
)

data class UserDto(
    val id: Long = 0,
    val name: String = "",
    val email: String = "",
    /** false = Profil Kesehatan Awal (langkah 2 pendaftaran) belum diisi. Server lama tanpa field ini = true. */
    @SerializedName("profile_completed") val profileCompleted: Boolean = true,
    val profile: ProfileDto = ProfileDto(),
    val pregnancy: PregnancyDto = PregnancyDto()
)

data class ProfileDto(
    val hpht: String? = null,
    @SerializedName("birth_date") val birthDate: String? = null,
    @SerializedName("health_facility") val healthFacility: String? = null
)

/** Dihitung server dari HPHT. */
data class PregnancyDto(
    val week: Int = 0,
    val trimester: Int = 1,
    @SerializedName("due_date") val dueDate: String? = null
)
