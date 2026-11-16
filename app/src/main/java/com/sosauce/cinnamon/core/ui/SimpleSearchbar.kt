package com.sosauce.cinnamon.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R

@Composable
fun SimpleSearchbar(
    modifier: Modifier = Modifier,
    textFieldState: TextFieldState = rememberTextFieldState(),
    navigationButton: @Composable (() -> Unit)? = null,
    fab: @Composable (() -> Unit)? = null,
    @StringRes placeholder: Int = R.string.search_here
) {

    val isSearching = textFieldState.text.isNotEmpty()


    Column(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth(0.85f)
            .imePadding()

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            navigationButton?.invoke()
            Spacer(Modifier.weight(1f))
            fab?.invoke()
        }

    }

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth(0.85f)
            .imePadding()

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            navigationButton?.invoke()
            Spacer(Modifier.weight(1f))
            fab?.invoke()
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            TextField(
                state = textFieldState,
                modifier = Modifier
                    .padding(6.dp)
                    .fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = {
                    Text(
                        text = stringResource(placeholder),
                        maxLines = 1
                    )
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                shape = FloatingToolbarDefaults.ContainerShape
            )
        }
    }
}
