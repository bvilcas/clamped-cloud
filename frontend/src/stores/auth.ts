import { defineStore } from 'pinia';
import { ref } from 'vue'
import type { AuthType } from '@/types/auth-type'
import { refreshToken, startRefreshInterval, stopRefreshInterval } from "@/utils/authService";
import router from '@/router'

/*
  Manages global auth state (isAuthenticated) and exposes login, logout, and logoutAllSessions. 
  On app load it checks localStorage token timestamps, refreshes if needed, and schedules 
  background refreshes. Does not handle react navigation (component files + App.js do that)
*/
// Handles frontend auth state + token management

// We dont put the exported "global" vars here b/c we need pinia wrapper

const EXPIRES_IN = 15 * 60 * 1000; // 15 min

export const useAuthStore = defineStore('auth', () => {
  // assign/access the authenticated property within AuthType... start with null
  // THIS IS THE GLOBAL STATE
  const isAuthenticated = ref<AuthType['authenticated']>(null);

  // === Helper function
  const clearLocalAuth = () => {
    localStorage.removeItem("accessToken")
    localStorage.removeItem("accessTokenIssuedAt")
  }

  // === Auth Process on Startup (Checks Auth Validity at render)
  const initAuth = async (): Promise<void> => {
    const token = localStorage.getItem("accessToken") // implied string
    const issuedAt = Number(localStorage.getItem("accessTokenIssuedAt") || "0");

    // Handles missing access token
    if (!token) {
      console.log("Clearing [any] token data from localStorage"); // precaution
      clearLocalAuth()
      isAuthenticated.value = false
      return;
    }

    // Handles expired access tokens 
    const isExpired = Date.now() > issuedAt + EXPIRES_IN;
    if (isExpired) {
      // Token expired, try refresh
      console.log("Token expired. Trying silent refresh...")
      try {
        await refreshToken()
        // Breakpoint: Successful fresh access token
        startRefreshInterval()
        isAuthenticated.value = true
      } catch (err: unknown) {
        // Refresh failed, log out
        if (err instanceof Error) {
          console.warn("Silent refresh failed:", err.message)
        } else {
          console.warn("Silent refresh failed with unknown error")
        }
        stopRefreshInterval()
        clearLocalAuth()
        isAuthenticated.value = false
      }
      return;
    }

    // Handles existing, valid token — schedule refresh based on remaining lifetime
    console.log('Valid Token')
    const buffer = 60 * 1000 // refresh 1 min before expiry
    const remaining = (issuedAt + EXPIRES_IN - buffer) - Date.now()
    startRefreshInterval(Math.max(remaining, 0)) // if already within buffer, refresh immediately
    isAuthenticated.value = true
  }

  // All these functions modifies the global state
  const login = (token: string) => {
    localStorage.setItem("accessToken", token)
    localStorage.setItem("accessTokenIssuedAt", Date.now().toString())

    stopRefreshInterval();

    // Schedule next refresh 1 minute before expiry
    const buffer = 60 * 1000
    startRefreshInterval(EXPIRES_IN - buffer);

    isAuthenticated.value = true
    console.log('Login successful!')
  }

  const logout = async () => {
    console.log("Logging out and clearing storage")
    console.log("Before logout:", document.cookie)
    stopRefreshInterval()
    console.log("After stopRefreshInterval:", document.cookie)

    try {
      await fetch("/api/v1/auth/logout", {
        method: "POST",
        credentials: "include",
      });
      console.log("Backend session revoked")
    } catch (err: unknown) {
      if (err instanceof Error) {
        console.warn("Backend logout failed:", err.message)
      } else {
        console.warn("Backend logout failed with no error message")
      }
    }

    clearLocalAuth()
    isAuthenticated.value = false
    console.log("Logout successful!")
    router.push('/login')
  }

  const logoutAllSessions = async () => {
    console.log("Logging out of all sessions and clearing storage")
    console.log("Before complete logout:", document.cookie)
    stopRefreshInterval()
    console.log("After stopRefreshInterval:", document.cookie)

    try {
      await fetch("/api/v1/auth/logoutAllSessions", {
        method: "POST",
        credentials: "include",
      });
      console.log("Backend revoked all sessions");
    } catch (err: unknown) {
      if (err instanceof Error) {
        console.warn("Backend complete logout failed:", err.message)
      }
      else {
        console.warn("Backend complete logout failed with no error message")
      }
    }
    clearLocalAuth()
    isAuthenticated.value = false
    console.log("Logged out of all sessions!")
    router.push('/login')
  }

  // PUBLIC API (Accessible state and actions to that state)
  return {
    // State
    isAuthenticated,

    // Actions
    initAuth,
    login,
    logout,
    logoutAllSessions
  }
}
)

