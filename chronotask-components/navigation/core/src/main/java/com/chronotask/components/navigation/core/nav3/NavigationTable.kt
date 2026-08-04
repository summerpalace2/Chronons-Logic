package com.chronotask.components.navigation.core.nav3

object NavigationTable {
    const val NAV_TABS = "tabs"
    const val NAV_HOME = "home"
    const val NAV_CREATE = "create/{taskId}"
    const val NAV_STATS = "stats"
    const val NAV_SETTINGS = "settings"
    const val NAV_TASK_DETAIL = "taskDetail/{taskId}"
    const val NAV_NOTES = "notes"
    const val NAV_NOTES_READ = "notes/read/{noteId}"
    const val NAV_NOTES_EDIT = "notes/edit/{noteId}"
    const val NAV_NOTES_CREATE = "notes/create"
    const val NAV_THEME = "settings/theme"
    const val NAV_FONT = "settings/font"
    const val NAV_LANGUAGE = "settings/language"
    const val NAV_ABOUT_APP = "settings/aboutApp"
    const val NAV_ABOUT_AUTHOR = "settings/aboutAuthor"
    const val NAV_QUICK_IMPORT = "settings/quickImport"
    const val NAV_WORKDAY = "settings/workday"

    object ParamKey {
        const val TASK_ID = "taskId"
    }
}
