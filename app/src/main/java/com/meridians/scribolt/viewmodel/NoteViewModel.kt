package com.meridians.scribolt.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meridians.scribolt.data.Note
import com.meridians.scribolt.data.NoteDatabase
import com.meridians.scribolt.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    val allNotes: StateFlow<List<Note>>

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    init {
        val noteDao = NoteDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)

        val notesFlow = MutableStateFlow<List<Note>>(emptyList())
        allNotes = notesFlow.asStateFlow()

        viewModelScope.launch {
            repository.allNotes.collect { notes ->
                notesFlow.value = notes
            }
        }
    }

    fun loadNote(noteId: Int) {
        viewModelScope.launch {
            _currentNote.value = repository.getNoteById(noteId)
        }
    }

    fun saveNote(
        title: String,
        content: String,
        noteId: Int? = null,
        fontFamily: String = "default",
        fontSize: Int = 16,
        textColor: String = "#000000",
        backgroundColor: String = "#FFFFFF"
    ) {
        viewModelScope.launch {
            if (noteId != null && noteId > 0) {
                // Update existing note - preserve isFavorite status
                val existingNote = repository.getNoteById(noteId)
                val note = Note(
                    id = noteId,
                    title = title,
                    content = content,
                    isFavorite = existingNote?.isFavorite ?: false,
                    timestamp = System.currentTimeMillis(),
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                    textColor = textColor,
                    backgroundColor = backgroundColor
                )
                repository.updateNote(note)
                _currentNote.value = note
            } else {
                // Insert new note
                val note = Note(
                    title = title,
                    content = content,
                    isFavorite = false,
                    timestamp = System.currentTimeMillis(),
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                    textColor = textColor,
                    backgroundColor = backgroundColor
                )
                repository.insertNote(note)
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun clearCurrentNote() {
        _currentNote.value = null
    }

    // New function: Toggle favorite status
    fun toggleFavorite(noteId: Int?, isFavorite: Boolean) {
        viewModelScope.launch {
            if (noteId != null && noteId > 0) {
                val existingNote = repository.getNoteById(noteId)
                existingNote?.let { note ->
                    val updatedNote = note.copy(isFavorite = isFavorite)
                    repository.updateNote(updatedNote)
                    _currentNote.value = updatedNote
                }
            }
        }
    }

    // New function: Search notes
    fun searchNotes(query: String): StateFlow<List<Note>> {
        val searchResults = MutableStateFlow<List<Note>>(emptyList())
        viewModelScope.launch {
            repository.searchNotes(query).collect { notes ->
                searchResults.value = notes
            }
        }
        return searchResults.asStateFlow()
    }

    // New function: Get favorite notes
    fun getFavoriteNotes(): StateFlow<List<Note>> {
        val favoriteNotes = MutableStateFlow<List<Note>>(emptyList())
        viewModelScope.launch {
            repository.getFavoriteNotes().collect { notes ->
                favoriteNotes.value = notes
            }
        }
        return favoriteNotes.asStateFlow()
    }

    // New function: Duplicate a note
    fun duplicateNote(note: Note) {
        viewModelScope.launch {
            val duplicatedNote = Note(
                title = "${note.title} (Copy)",
                content = note.content,
                isFavorite = false,
                timestamp = System.currentTimeMillis()
            )
            repository.insertNote(duplicatedNote)
        }
    }

    // New function: Delete all notes
    fun deleteAllNotes() {
        viewModelScope.launch {
            repository.deleteAllNotes()
        }
    }
}