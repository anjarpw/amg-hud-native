package com.haskell.amghud



enum class GearMode(val stringAlias: String) {
    T("T"),
    P("P"),
    R("R"),
    D("D"),
    S("S"),
    S_PLUS("S+");

    companion object {
        fun fromString(alias: String): GearMode? {
            return values().find { it.stringAlias == alias }
        }
    }
}