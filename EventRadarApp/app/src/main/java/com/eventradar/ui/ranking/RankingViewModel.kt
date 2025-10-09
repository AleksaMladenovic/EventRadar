package com.eventradar.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RankingState())
    val state: StateFlow<RankingState> = _state.asStateFlow()

    init {
        getRankingList()
    }

    private fun getRankingList() {
        userRepository.getAllUsersSortedByPoints()
            .onEach { result ->
                result.onSuccess { users ->
                    // Ako je uspešno, ažuriramo stanje sa listom korisnika
                    _state.update { it.copy(
                        topThreeUsers = users.take(3),
                        otherUsers = users.drop(3),
                        isLoading = false,
                        error = null
                    ) }
                }.onFailure { exception ->
                    // Ako je neuspešno, ažuriramo stanje sa greškom
                    _state.update { it.copy(isLoading = false, error = exception.message) }
                }
            }
            .launchIn(viewModelScope)
    }
}
