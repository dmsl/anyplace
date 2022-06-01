package com.example.search.modules


data class Route(
    val message: String,
    val num_of_pois: Int,
    val pois: List<RoutePOI>,
    val status: String,
    val status_code: Int
)