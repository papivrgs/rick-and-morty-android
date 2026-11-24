package com.example.rickandmorty.data.remote.mapper

import com.example.rickandmorty.data.remote.dto.EpisodeDto
import com.example.rickandmorty.domain.model.Episode

fun EpisodeDto.toDomain(): Episode = Episode(
    id = id,
    name = name,
    code = code,
)
