package com.movietorr.v2

data class SiteConfig(
    val id: String,
    val name: String,
    val url: String,
    val isEnabled: Boolean = true
) 