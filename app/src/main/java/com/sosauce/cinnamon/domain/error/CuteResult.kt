package com.sosauce.cinnamon.domain.error

interface CuteError

sealed interface CuteResult<out D, out E : CuteError> {
    data class Success<out D, out E : CuteError>(val data: D) : CuteResult<D, E>
    data class Error<out D, out E : CuteError>(val error: E) : CuteResult<D, E>
    data object InProgress : CuteResult<Nothing, Nothing>
}
