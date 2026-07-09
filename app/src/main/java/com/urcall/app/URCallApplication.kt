package com.urcall.app

import android.app.Application
import com.urcall.app.webrtc.PresenceManager

class URCallApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Sinisimulan dito yung "naka-on ang data" detection.
        // Habang bukas ang app (or naka-background pa rin sa v1),
        // itinatatak natin sa Firebase na "online" tayo.
        PresenceManager.startPresenceTracking(this)
    }
}
