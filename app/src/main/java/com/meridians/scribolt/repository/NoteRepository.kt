package com.meridians.scribolt.repository

import com.meridians.scribolt.data.Note
import com.meridians.scribolt.data.NoteDao
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun getNoteById(noteId: Int): Note? {
        return noteDao.getNoteById(noteId)
    }

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    // New repository functions for modern features
    fun getFavoriteNotes(): Flow<List<Note>> {
        return noteDao.getFavoriteNotes()
    }

    fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes(query)
    }

    suspend fun deleteAllNotes() {
        noteDao.deleteAllNotes()
    }
}