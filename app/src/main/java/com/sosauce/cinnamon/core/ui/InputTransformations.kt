package com.sosauce.cinnamon.core.ui

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.util.fastAny

object ValidFileNameTransformation : InputTransformation {
    private val forbiddenChars = listOf('/', '\\')
    override fun TextFieldBuffer.transformInput() {
        if (forbiddenChars.fastAny { it in asCharSequence() }) revertAllChanges()
    }
}