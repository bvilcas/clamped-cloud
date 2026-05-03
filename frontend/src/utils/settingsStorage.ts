import { vuetify } from '@/plugins/vuetify'

const SETTINGS_KEY = "app.settings"

interface AppSettings {
  notificationsEnabled: boolean
  notificationEmail: string
  displayMode: string
  textSize: string
  defaultPage: string
  autoOpenLastProject: boolean
}

const defaultSettings: AppSettings = {
  notificationsEnabled: false,
  notificationEmail: "",
  displayMode: "system",
  textSize: "medium",
  defaultPage: "projects",
  autoOpenLastProject: true,
}

export function loadSettings(): AppSettings {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY)
    if (!raw) {
      return { ...defaultSettings }
    }
    const parsed = JSON.parse(raw)
    return { ...defaultSettings, ...parsed }
  } catch (err) {
    console.warn("Failed to load settings:", err)
    return { ...defaultSettings }
  }
}

export function saveSettings(settings: AppSettings): void {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings))
}

// Allows font and color scheme changes
export function applySettings(settings: AppSettings): void {
  const prefersDark = window.matchMedia &&
    window.matchMedia("(prefers-color-scheme: dark)").matches
  const resolvedMode = settings.displayMode === "system"
    ? (prefersDark ? "dark" : "light")
    : settings.displayMode

  vuetify.theme.global.name.value = resolvedMode

  const sizeMap: Record<string, string> = {
    small: "14px",
    medium: "16px",
    large: "18px",
  }
  const fontSize = sizeMap[settings.textSize] ?? "16px"
  document.documentElement.style.setProperty("--app-font-size", fontSize)
}
