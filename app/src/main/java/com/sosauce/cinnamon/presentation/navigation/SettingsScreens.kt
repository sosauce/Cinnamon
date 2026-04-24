package com.sosauce.cinnamon.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class SettingsScreens: NavKey {

    @Serializable
    data object Settings : SettingsScreens()

    @Serializable
    data object LookAndFeel : SettingsScreens()

    @Serializable
    data object Behavior : SettingsScreens()

    @Serializable
    data object Migration : SettingsScreens()


}