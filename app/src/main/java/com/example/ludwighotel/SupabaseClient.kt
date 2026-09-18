package com.example.ludwighotel

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.functions.Functions


object SupabaseClientProvider {

    
    private const val SUPABASE_URL = "https://wnkfbohqydlrzjqttpew.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Indua2Zib2hxeWRscnpqcXR0cGV3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg5ODU4NTQsImV4cCI6MjEwNDU2MTg1NH0.yuSrsBZWl8il4dRKUD2yMfDFsf-gutL1rEJPbiqaeUU"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Auth)
        install(Storage)
        install(Realtime)
        install(Functions)
    }
}