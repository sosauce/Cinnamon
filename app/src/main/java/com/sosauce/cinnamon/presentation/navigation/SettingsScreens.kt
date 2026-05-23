package com.sosauce.cinnamon.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class SettingsScreens : NavKey {

    @Serializable
    data object Settings : SettingsScreens()

    @Serializable
    data object LookAndFeel : SettingsScreens()

    @Serializable
    data object Messages : SettingsScreens()

    @Serializable
    data object Contacts : SettingsScreens()

    @Serializable
    data object Phone : SettingsScreens()

    @Serializable
    data object Behavior : SettingsScreens()

    @Serializable
    data object Migration : SettingsScreens()


}