package me.okmanideep.landmine.compass.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * To represent data part of a destination
 *
 * @param type A unique string. eg. login, home etc.
 * @param args arguments you want to pass.
 *
 * See [StackDestination][me.okmanideep.landmine.compass.stack.StackDestination] for UI part
 */
@Parcelize
data class Page(
    val type: String,
    val args: Parcelable? = null
) : Parcelable
