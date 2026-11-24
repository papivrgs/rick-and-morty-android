package com.example.rickandmorty.data.remote.mapper

import com.example.rickandmorty.data.remote.dto.CharacterDto
import com.example.rickandmorty.domain.model.Character

fun CharacterDto.toDomain(): Character = Character(
    id = id,
    name = name,
    status = status,
    species = species,
    type = type,
    gender = gender,
    originName = origin.name,
    originUrl = origin.url,
    locationName = location.name,
    locationUrl = location.url,
    imageUrl = imageUrl,
    episodeUrls = episodeUrls,
    resourceUrl = resourceUrl,
    createdAt = createdAt,
)
