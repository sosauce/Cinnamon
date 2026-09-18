@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.core.ui.components.menus

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.rememberInteractionSource
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile

@Composable
fun SortingDropdownMenu(
    isSortedAscending: Boolean,
    onChangeSorting: (Boolean) -> Unit,
    topContent: @Composable (() -> Unit)? = null,
    content: @Composable (ColumnScope.() -> Unit)
) {

    var expanded by remember { mutableStateOf(false) }
    val interactionSources = List(2) { rememberInteractionSource() }

    Box {
        IconButton(
            onClick = { expanded = !expanded },
            shapes = IconButtonDefaults.shapes()
        ) {
            AnimatedDrawable(
                drawable = AnimatedDrawableFile.SORT,
                atEnd = expanded
            )
        }
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(0, 2)
            ) { topContent?.invoke() }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(1, 2),
                content = content
            )
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                overflowIndicator = {}
            ) {

                customItem(
                    buttonGroupContent = {

                        val shape by animateDpAsState(
                            targetValue = if (isSortedAscending) 50.dp else 12.dp
                        )

                        FilledIconButton(
                            onClick = { onChangeSorting(true) },
                            interactionSource = interactionSources[0],
                            modifier = Modifier
                                .animateWidth(interactionSources[0])
                                .weight(1f)
                                .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isSortedAscending) MenuDefaults.groupVibrantContainerColor else MenuDefaults.groupStandardContainerColor
                            ),
                            shape = RoundedCornerShape(shape)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.up),
                                contentDescription = null,
                                modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                            )
                        }
                    },
                    menuContent = {}
                )

                customItem(
                    buttonGroupContent = {

                        val shape2 by animateDpAsState(
                            targetValue = if (!isSortedAscending) 50.dp else 12.dp
                        )

                        FilledIconButton(
                            onClick = { onChangeSorting(false) },
                            interactionSource = interactionSources[1],
                            modifier = Modifier
                                .animateWidth(interactionSources[1])
                                .weight(1f)
                                .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (!isSortedAscending) MenuDefaults.groupVibrantContainerColor else MenuDefaults.groupStandardContainerColor
                            ),
                            shape = RoundedCornerShape(shape2)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.down),
                                contentDescription = null,
                                modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                            )
                        }
                    },
                    menuContent = {}
                )
            }
        }
    }

}