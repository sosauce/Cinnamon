package com.sosauce.cinnamon.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

fun NavBackStack<NavKey>.navigate(element: NavKey) {
    remove(element)
    add(element)
}

fun NavBackStack<NavKey>.navigateBack() {
    if (size == 1) {
        add(Screen.Conversations)
    } else {
        removeLastOrNull()
    }
}