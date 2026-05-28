package com.trackit.data.repository

import io.github.jan.supabase.SupabaseClient

object SupabaseLocator {
    lateinit var client: SupabaseClient
        internal set
}

