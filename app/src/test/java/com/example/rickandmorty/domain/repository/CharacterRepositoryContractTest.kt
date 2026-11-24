package com.example.rickandmorty.domain.repository

import org.junit.Assert.assertFalse
import org.junit.Test

class CharacterRepositoryContractTest {

    @Test
    fun `repository contract does not expose Retrofit or DTO types`() {
        val contractTypes = CharacterRepository::class.java.methods
            .flatMap { method ->
                buildList {
                    add(method.genericReturnType.typeName)
                    method.genericParameterTypes.forEach { add(it.typeName) }
                }
            }

        assertFalse(contractTypes.any { it.contains("retrofit2") })
        assertFalse(contractTypes.any { it.contains("data.remote.dto") })
    }
}
