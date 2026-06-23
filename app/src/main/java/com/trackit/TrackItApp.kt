package com.trackit

import android.app.Application
import com.trackit.data.repository.SupabaseLocator
import com.trackit.data.sync.SyncScheduler
import com.trackit.core.ui.theme.ThemeLocator
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets

class TrackItApp : Application() {

    lateinit var supabase: io.github.jan.supabase.SupabaseClient
        private set

    @OptIn(SupabaseInternal::class)
    override fun onCreate() {
        super.onCreate()

        supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            httpConfig {
                install(WebSockets)
            }
            httpEngine = OkHttp.create()
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Functions)
        }

        SupabaseLocator.init(this, supabase)
        ThemeLocator.init(this)

        // Trigger a sync whenever connectivity returns, and once on startup,
        // so Room (the single source of truth) catches up with Supabase.
        SupabaseLocator.networkObserver.register()
        SyncScheduler.enqueue(this)
    }
}
