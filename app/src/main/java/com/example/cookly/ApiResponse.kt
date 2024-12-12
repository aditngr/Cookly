package com.example.cookly

data class ApiResponse(
    val status: String,
    val files: Map<String, String>
)
