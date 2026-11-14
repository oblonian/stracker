package com.oblonian.stracker.data

import com.oblonian.stracker.data.db.PlaceDao
import com.oblonian.stracker.domain.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaceRepository(private val dao: PlaceDao) {

    val places: Flow<List<Place>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun placesOnce(): List<Place> = dao.all().map { it.toDomain() }

    suspend fun add(place: Place): Long = dao.insert(place.toEntity())

    suspend fun update(place: Place) = dao.update(place.toEntity())

    suspend fun delete(id: Long) = dao.delete(id)
}
