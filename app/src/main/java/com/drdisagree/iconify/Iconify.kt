package com.drdisagree.iconify

import android.app.Application
import android.content.Context
import com.drdisagree.iconify.utils.helper.LocaleHelper
import com.google.android.material.color.DynamicColors
import java.lang.ref.WeakReference

class Iconify : Application() {

    companion object {
        private var instance: Iconify? = null
        private var contextReference: WeakReference<Context>? = null

        val appContext: Context
            get() {
                if (contextReference == null || contextReference?.get() == null) {
                    val app = instance?.applicationContext
                        ?: throw IllegalStateException("Iconify application is not created yet")
                    contextReference = WeakReference(app)
                }
                return contextReference!!.get()
                    ?: throw IllegalStateException("Iconify application context was garbage collected")
            }

        val appContextLocale: Context
            get() {
                return LocaleHelper.setLocale(appContext)
            }

        private fun getInstance(): Iconify {
            return instance
                ?: throw IllegalStateException("Iconify application is not created yet")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        contextReference = WeakReference(applicationContext)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}