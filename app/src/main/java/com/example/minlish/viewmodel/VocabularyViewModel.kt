package com.example.minlish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minlish.FirestoreManager
import com.example.minlish.VocabularyEntity
import com.example.minlish.repository.VocabularyRepository
import kotlinx.coroutines.launch

class VocabularyViewModel(
    private val repository: VocabularyRepository
) : ViewModel() {

    val allVocabulary = repository.getAllVocabulary()

    fun insertVocabulary(vocab: VocabularyEntity) {
        viewModelScope.launch {
            repository.insertVocabulary(vocab)
            FirestoreManager.syncVocabulary(vocab)
        }
    }

    fun updateVocabulary(vocab: VocabularyEntity) {
        viewModelScope.launch {
            repository.updateVocabulary(vocab)
            FirestoreManager.syncVocabulary(vocab)
        }
    }

    fun deleteVocabulary(vocab: VocabularyEntity) {
        viewModelScope.launch {
            repository.deleteVocabulary(vocab)
            FirestoreManager.deleteVocabulary(vocab)
        }
    }
}