package com.kelompok.anemom.data.remote.dto

// TODO(backend): sesuaikan nama field dengan kontrak API backend (cek di Postman).

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val hpht: String,
    val trimester: Int
)

data class AuthResponse(
    val token: String? = null,
    val message: String? = null
)
