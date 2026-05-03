<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSidebar } from '@/composables/useSidebar'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import type { UserDTO } from '@/dto/user-dto'

// ── Notification types ─────────────────────────────────────────
interface NotificationItem {
  id: number
  type: string
  message: string
  relatedProjectId: number | null
  relatedVulnId: number | null
  read: boolean
  createdAt: string
}

defineProps<{
  user: UserDTO | null
}>()

// ── Notification state ─────────────────────────────────────────
const notifOpen = ref(false)
const notifications = ref<NotificationItem[]>([])
const unreadCount = ref(0)
const notifLoading = ref(false)

const notifTypeIcon = (type: string) => {
  const map: Record<string, string> = {
    VULN_ASSIGNED:       'mdi-shield-check-outline',
    VULN_UNASSIGNED:     'mdi-shield-remove-outline',
    VULN_REPORTED:       'mdi-shield-alert-outline',
    VULN_STATUS_CHANGED: 'mdi-shield-refresh-outline',
    MEMBER_SELF_ASSIGNED:'mdi-account-check-outline',
    MEMBER_SELF_REVOKED: 'mdi-account-minus-outline',
    PROJECT_ADDED:       'mdi-folder-plus-outline',
    PROJECT_REMOVED:     'mdi-folder-remove-outline',
  }
  return map[type] ?? 'mdi-bell-outline'
}

const notifTypeColor = (type: string) => {
  if (type.startsWith('VULN_REPORT')) return 'warning'
  if (type.startsWith('VULN_ASSIGN') || type === 'MEMBER_SELF_ASSIGNED') return 'success'
  if (type.includes('UNASSIGN') || type.includes('REVOK') || type === 'PROJECT_REMOVED') return 'error'
  if (type === 'PROJECT_ADDED') return 'info'
  return 'secondary'
}

const fetchUnreadCount = async () => {
  try {
    const res = await fetchWithAuth('/api/v1/notifications/unread-count')
    if (res.ok) {
      const d = await res.json()
      unreadCount.value = d.data ?? 0
    }
  } catch { /* silent */ }
}

const fetchNotifications = async () => {
  notifLoading.value = true
  try {
    const res = await fetchWithAuth('/api/v1/notifications')
    if (res.ok) {
      const d = await res.json()
      notifications.value = d.data ?? []
    }
  } catch { /* silent */ } finally {
    notifLoading.value = false
  }
}

const openNotifPanel = async () => {
  notifOpen.value = true
  await fetchNotifications()
}

const markRead = async (n: NotificationItem) => {
  if (n.read) return
  try {
    await fetchWithAuth(`/api/v1/notifications/${n.id}/read`, { method: 'PUT' })
    n.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch { /* silent */ }
}

const markAllRead = async () => {
  try {
    await fetchWithAuth('/api/v1/notifications/read-all', { method: 'PUT' })
    notifications.value.forEach(n => { n.read = true })
    unreadCount.value = 0
  } catch { /* silent */ }
}

const deleteNotif = async (n: NotificationItem) => {
  try {
    await fetchWithAuth(`/api/v1/notifications/${n.id}`, { method: 'DELETE' })
    if (!n.read) unreadCount.value = Math.max(0, unreadCount.value - 1)
    notifications.value = notifications.value.filter(x => x.id !== n.id)
  } catch { /* silent */ }
}

const clearAll = async () => {
  try {
    await fetchWithAuth('/api/v1/notifications/clear-all', { method: 'DELETE' })
    notifications.value = []
    unreadCount.value = 0
  } catch { /* silent */ }
}

const notifNavigate = (n: NotificationItem) => {
  markRead(n)
  notifOpen.value = false
  if (n.relatedProjectId) {
    router.push(`/project/${n.relatedProjectId}`)
  }
}

const timeAgo = (iso: string) => {
  const diff = Date.now() - new Date(iso).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  return `${Math.floor(hrs / 24)}d ago`
}

// Poll unread count every 60 s
let pollInterval: ReturnType<typeof setInterval> | null = null

const router = useRouter()
const authStore = useAuthStore()
const menuOpen = ref(false)
const { sidebarVisible, toggleSidebar } = useSidebar()

// ── Search state ──────────────────────────────────────────────
const query = ref('')
const showDropdown = ref(false)
const activeIndex = ref(-1)
const searchLoading = ref(false)
const searchWrapperRef = ref<HTMLElement | null>(null)

// Teleported dropdown position (fixed, so stacking contexts don't matter)
const dropdownStyle = ref<Record<string, string>>({})

const positionDropdown = () => {
  if (!searchWrapperRef.value) return
  const rect = searchWrapperRef.value.getBoundingClientRect()
  dropdownStyle.value = {
    top: `${rect.bottom + 6}px`,
    left: `${rect.left}px`,
    width: `${rect.width}px`,
  }
}

// Cached dynamic data (loaded once on first open)
const projects = ref<{ id: string; name: string; description: string; myRole: string }[]>([])
const vulns = ref<{ id: string; title: string; severity: string; status: string; projectId: string; projectName: string }[]>([])
const dataLoaded = ref(false)

// ── Static page shortcuts ─────────────────────────────────────
const pages = [
  { key: 'dashboard',      icon: 'mdi-view-dashboard-outline', title: 'Dashboard',           subtitle: 'Home overview',                 path: '/dashboard' },
  { key: 'projects',       icon: 'mdi-folder-outline',         title: 'Projects',             subtitle: 'All your projects',             path: '/projects' },
  { key: 'create-project', icon: 'mdi-folder-plus-outline',    title: 'Create Project',       subtitle: 'Start a new project',           path: '/projects/create' },
  { key: 'vulns',          icon: 'mdi-shield-alert-outline',   title: 'My Vulnerabilities',   subtitle: 'Reported, assigned & verified', path: '/vulns' },
  { key: 'messages',       icon: 'mdi-message-outline',        title: 'Messages',             subtitle: 'Project channels',              path: '/messages' },
  { key: 'team',           icon: 'mdi-account-group-outline',  title: 'Team',                 subtitle: 'Members across all projects',   path: '/team' },
  { key: 'profile',        icon: 'mdi-account-outline',        title: 'Profile',              subtitle: 'Your profile settings',         path: '/profile' },
  { key: 'settings',       icon: 'mdi-cog-outline',            title: 'Settings',             subtitle: 'App preferences',               path: '/settings' },
  { key: 'help',           icon: 'mdi-help-circle-outline',    title: 'Help & Docs',          subtitle: 'Documentation',                 path: '/help' },
  { key: 'contact',        icon: 'mdi-email-outline',          title: 'Contact Us',           subtitle: 'Get in touch with us',          path: '/contact' },
  { key: 'about',          icon: 'mdi-information-outline',    title: 'About Clamped',        subtitle: 'About this platform',           path: '/about' },
]

// ── Load dynamic data once ────────────────────────────────────
const loadData = async () => {
  if (dataLoaded.value) return
  searchLoading.value = true
  try {
    const [projRes, repRes, asgRes, verRes] = await Promise.all([
      fetchWithAuth('/api/v1/userprojects/all'),
      fetchWithAuth('/api/v1/uservulns/reported'),
      fetchWithAuth('/api/v1/uservulns/assigned'),
      fetchWithAuth('/api/v1/uservulns/verified'),
    ])
    if (projRes.ok) {
      const d = await projRes.json()
      projects.value = d.data || []
    }
    const vulnMap = new Map()
    const absorb = async (res: Response) => {
      if (!res.ok) return
      const d = await res.json()
      for (const v of d.data || []) vulnMap.set(v.id, v)
    }
    await Promise.all([absorb(repRes), absorb(asgRes), absorb(verRes)])
    vulns.value = Array.from(vulnMap.values())
    dataLoaded.value = true
  } catch {
    // fail silently — static items still work
  } finally {
    searchLoading.value = false
  }
}

// ── Result types ──────────────────────────────────────────────
interface ResultItem {
  key: string
  icon: string
  iconColor: string
  title: string
  subtitle: string
  badge?: string
  badgeColor?: string
  navigate: () => void
}

interface ResultGroup {
  label: string
  items: ResultItem[]
}

// ── Filtered groups ───────────────────────────────────────────
const filteredGroups = computed<ResultGroup[]>(() => {
  const q = query.value.trim().toLowerCase()
  const groups: ResultGroup[] = []

  const matchedPages = pages
    .filter(p => !q || p.title.toLowerCase().includes(q) || p.subtitle.toLowerCase().includes(q))
    .slice(0, q ? 5 : 6)
    .map(p => ({
      key: p.key,
      icon: p.icon,
      iconColor: 'secondary',
      title: p.title,
      subtitle: p.subtitle,
      navigate: () => go(p.path),
    }))
  if (matchedPages.length) groups.push({ label: q ? 'Pages' : 'Quick Links', items: matchedPages })

  const matchedProjects = projects.value
    .filter(p => !q || p.name.toLowerCase().includes(q) || p.description?.toLowerCase().includes(q))
    .slice(0, 5)
    .map(p => ({
      key: `proj-${p.id}`,
      icon: 'mdi-folder-open-outline',
      iconColor: 'info',
      title: p.name,
      subtitle: p.description || 'No description',
      badge: p.myRole,
      badgeColor: p.myRole === 'LEAD' ? 'warning' : p.myRole === 'PROGRAMMER' ? 'info' : 'secondary',
      navigate: () => go(`/project/${p.id}`),
    }))
  if (matchedProjects.length) groups.push({ label: 'Projects', items: matchedProjects })

  if (q && vulns.value.length) {
    const sevColor: Record<string, string> = { LOW: 'success', MEDIUM: 'warning', HIGH: 'error', CRITICAL: 'error' }
    const matchedVulns = vulns.value
      .filter(v => v.title.toLowerCase().includes(q) || v.projectName?.toLowerCase().includes(q))
      .slice(0, 4)
      .map(v => ({
        key: `vuln-${v.id}`,
        icon: 'mdi-shield-bug-outline',
        iconColor: sevColor[v.severity] ?? 'secondary',
        title: v.title,
        subtitle: v.projectName,
        badge: v.severity,
        badgeColor: sevColor[v.severity] ?? 'secondary',
        navigate: () => go(`/project/${v.projectId}`),
      }))
    if (matchedVulns.length) groups.push({ label: 'Vulnerabilities', items: matchedVulns })
  }

  return groups
})

const flatResults = computed<ResultItem[]>(() => filteredGroups.value.flatMap(g => g.items))
const totalResults = computed(() => flatResults.value.length)

// ── Navigation ────────────────────────────────────────────────
const go = (path: string) => {
  showDropdown.value = false
  query.value = ''
  activeIndex.value = -1
  router.push(path)
}

// ── Open / close ──────────────────────────────────────────────
const openDropdown = () => {
  loadData()
  positionDropdown()
  showDropdown.value = true
}

const closeDropdown = () => {
  showDropdown.value = false
  activeIndex.value = -1
}

// Close when clicking outside the wrapper or the teleported dropdown
const handleDocumentClick = (e: MouseEvent) => {
  if (!showDropdown.value) return
  const target = e.target as Node
  const wrapper = searchWrapperRef.value
  const teleportedMenu = document.querySelector('.search-teleport')
  if (wrapper && !wrapper.contains(target) && teleportedMenu && !teleportedMenu.contains(target)) {
    closeDropdown()
  }
}

onMounted(() => {
  document.addEventListener('mousedown', handleDocumentClick)
  fetchUnreadCount()
  pollInterval = setInterval(fetchUnreadCount, 60_000)
})
onUnmounted(() => {
  document.removeEventListener('mousedown', handleDocumentClick)
  if (pollInterval) clearInterval(pollInterval)
})

// ── Keyboard navigation ───────────────────────────────────────
const onKeydown = (e: KeyboardEvent) => {
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    if (!showDropdown.value) { openDropdown(); return }
    e.preventDefault()
    const dir = e.key === 'ArrowDown' ? 1 : -1
    activeIndex.value = (activeIndex.value + dir + totalResults.value) % totalResults.value
  } else if (e.key === 'Enter' && activeIndex.value >= 0) {
    e.preventDefault()
    flatResults.value[activeIndex.value]?.navigate()
  } else if (e.key === 'Escape') {
    closeDropdown()
  }
}

// Watch query: open dropdown whenever the user types (catches missed focus events)
watch(query, () => {
  activeIndex.value = -1
  positionDropdown()
  if (!showDropdown.value) openDropdown()
})

// ── Absolute index within flat list ──────────────────────────
const absIdx = (groupIdx: number, itemIdx: number) => {
  let i = 0
  for (let g = 0; g < groupIdx; g++) i += filteredGroups.value[g]?.items.length ?? 0
  return i + itemIdx
}

// ── Highlight matching text ───────────────────────────────────
const highlight = (text: string) => {
  const q = query.value.trim()
  if (!q || !text) return text ?? ''
  const safe = q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${safe})`, 'gi'), '<mark>$1</mark>')
}
</script>

<template>
  <header class="topbar">
    <!-- LEFT -->
    <div class="d-flex align-center ga-3">
      <v-btn
        v-show="!sidebarVisible"
        icon="mdi-menu"
        variant="text"
        size="small"
        class="sidebar-toggle"
        @click="toggleSidebar"
      />

      <!-- Search wrapper — ref used to anchor the teleported dropdown -->
      <div ref="searchWrapperRef" class="search-wrapper">
        <v-text-field
          v-model="query"
          variant="outlined"
          density="compact"
          hide-details
          placeholder="Search pages, projects, vulns..."
          class="topbar-search"
          prepend-inner-icon="mdi-magnify"
          :loading="searchLoading"
          autocomplete="off"
          @focus="openDropdown"
          @click="openDropdown"
          @keydown="onKeydown"
        />
      </div>
    </div>

    <!-- RIGHT -->
    <div class="d-flex align-center ga-4">
      <!-- Bell with notification dropdown -->
      <v-menu
        v-model="notifOpen"
        location="bottom end"
        :close-on-content-click="false"
        max-width="380"
        @update:model-value="val => { if (val) openNotifPanel() }"
      >
        <template #activator="{ props: menuProps }">
          <v-badge
            :content="unreadCount > 0 ? (unreadCount > 99 ? '99+' : unreadCount) : undefined"
            :model-value="unreadCount > 0"
            color="error"
            overlap
          >
            <v-btn icon="mdi-bell-outline" variant="text" size="small" v-bind="menuProps" />
          </v-badge>
        </template>

        <v-card min-width="340" max-width="380">
          <!-- Header -->
          <div class="d-flex align-center justify-space-between px-4 pt-3 pb-2">
            <span class="text-subtitle-2 font-weight-bold">Notifications</span>
            <div class="d-flex ga-1">
              <v-btn
                v-if="notifications.length"
                size="x-small"
                variant="text"
                @click="markAllRead"
              >Mark all read</v-btn>
              <v-btn
                v-if="notifications.length"
                size="x-small"
                variant="text"
                color="error"
                @click="clearAll"
              >Clear all</v-btn>
              <v-btn
                size="x-small"
                variant="text"
                @click="notifOpen = false; router.push('/notifications')"
              >View all</v-btn>
            </div>
          </div>
          <v-divider />

          <!-- Loading -->
          <div v-if="notifLoading" class="d-flex justify-center pa-4">
            <v-progress-circular indeterminate size="24" />
          </div>

          <!-- Empty -->
          <div v-else-if="!notifications.length" class="d-flex flex-column align-center pa-6 text-secondary">
            <v-icon icon="mdi-bell-off-outline" size="32" class="mb-2" />
            <span class="text-body-2">No notifications</span>
          </div>

          <!-- List (max 8 shown) -->
          <v-list v-else density="compact" class="notif-list pa-0">
            <v-list-item
              v-for="n in notifications.slice(0, 8)"
              :key="n.id"
              :class="{ 'notif-unread': !n.read }"
              class="notif-item"
              @click="notifNavigate(n)"
            >
              <template #prepend>
                <v-icon
                  :icon="notifTypeIcon(n.type)"
                  :color="notifTypeColor(n.type)"
                  size="20"
                  class="mr-2"
                />
              </template>
              <v-list-item-title class="text-body-2 notif-msg">{{ n.message }}</v-list-item-title>
              <v-list-item-subtitle class="text-caption">{{ timeAgo(n.createdAt) }}</v-list-item-subtitle>
              <template #append>
                <v-btn
                  icon="mdi-close"
                  size="x-small"
                  variant="text"
                  @click.stop="deleteNotif(n)"
                />
              </template>
            </v-list-item>
          </v-list>

          <div v-if="notifications.length > 8" class="text-center py-2">
            <v-btn size="x-small" variant="text" @click="notifOpen = false; router.push('/notifications')">
              +{{ notifications.length - 8 }} more
            </v-btn>
          </div>
        </v-card>
      </v-menu>

      <template v-if="user">
        <div class="d-flex align-center ga-2">
          <span class="avatar">{{ user.firstname[0] }}{{ user.lastname[0] }}</span>
          <span class="name">{{ user.firstname }} {{ user.lastname }}</span>
        </div>

        <v-menu v-model="menuOpen" location="bottom end">
          <template #activator="{ props }">
            <v-btn icon="mdi-cog-outline" variant="text" size="small" v-bind="props" />
          </template>
          <v-list>
            <v-list-item prepend-icon="mdi-account-outline" @click="menuOpen = false; router.push('/profile')">Profile</v-list-item>
            <v-list-item prepend-icon="mdi-cog-outline" @click="menuOpen = false; router.push('/settings')">Settings</v-list-item>
            <v-list-item prepend-icon="mdi-logout" @click="menuOpen = false; authStore.logout()">Logout</v-list-item>
          </v-list>
        </v-menu>
      </template>
    </div>
  </header>

  <!-- Teleport to body — escapes all stacking contexts -->
  <Teleport to="body">
    <div
      v-if="showDropdown && (filteredGroups.length > 0 || (query.trim() && !searchLoading))"
      class="search-teleport"
      :style="dropdownStyle"
    >
      <!-- Results -->
      <template v-if="filteredGroups.length > 0">
        <template v-for="(group, gi) in filteredGroups" :key="group.label">
          <div class="search-group-label" :class="{ 'search-group-label--first': gi === 0 }">
            {{ group.label }}
          </div>
          <div
            v-for="(item, ii) in group.items"
            :key="item.key"
            class="search-item"
            :class="{ 'search-item--active': activeIndex === absIdx(gi, ii) }"
            @mousedown.prevent="item.navigate()"
          >
            <v-icon :icon="item.icon" :color="item.iconColor" size="18" class="search-item-icon" />
            <div class="search-item-text">
              <span class="search-item-title" v-html="highlight(item.title)" />
              <span class="search-item-subtitle" v-html="highlight(item.subtitle)" />
            </div>
            <v-chip
              v-if="item.badge"
              :color="item.badgeColor"
              size="x-small"
              variant="tonal"
              class="search-item-badge"
            >
              {{ item.badge }}
            </v-chip>
          </div>
        </template>

        <div class="search-footer">
          <span><v-icon icon="mdi-keyboard-return" size="12" /> select</span>
          <span><v-icon icon="mdi-chevron-up" size="12" /><v-icon icon="mdi-chevron-down" size="12" /> navigate</span>
          <span><v-icon icon="mdi-keyboard-esc" size="12" /> close</span>
        </div>
      </template>

      <!-- Empty state (only when user has typed something) -->
      <div v-else-if="query.trim() && !searchLoading" class="search-empty">
        <v-icon icon="mdi-magnify-close" size="20" class="mb-1" />
        <span>No results for "{{ query }}"</span>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid rgb(var(--v-theme-outline));
  background: rgb(var(--v-theme-surface));
  position: sticky;
  top: 0;
  z-index: 100;
}

.sidebar-toggle {
  color: rgb(var(--v-theme-on-surface)) !important;
}

.search-wrapper {
  width: 480px;
}

.topbar-search {
  width: 100%;
}

.avatar {
  background: rgb(var(--v-theme-avatar));
  color: white;
  font-size: 0.8rem;
  font-weight: bold;
  padding: 0.4rem 0.6rem;
  border-radius: 50%;
}

.name {
  font-size: 0.9rem;
}

.notif-item {
  cursor: pointer;
  border-bottom: 1px solid rgb(var(--v-theme-outline));
}
.notif-item:last-child {
  border-bottom: none;
}
.notif-unread {
  background: rgba(var(--v-theme-primary), 0.06);
}
.notif-msg {
  white-space: normal !important;
  line-height: 1.3;
}
.notif-list {
  max-height: 360px;
  overflow-y: auto;
}

.topbar :deep(.v-btn--variant-text > .v-btn__overlay) { opacity: 0 !important; }
.topbar :deep(.v-ripple__container) { color: rgb(var(--v-theme-nav-action)); }
.topbar :deep(.v-ripple__animation) { opacity: 0.18 !important; }
.topbar :deep(.v-btn--variant-text:hover) { background-color: rgb(var(--v-theme-surface-light)) !important; }
.topbar :deep(.v-btn--variant-text:active) { background-color: rgb(var(--v-theme-surface-light)) !important; filter: brightness(0.95); }
</style>

<!-- Global styles for the teleported dropdown (not scoped) -->
<style>
.search-teleport {
  position: fixed;
  background: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-outline));
  border-radius: 10px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.16);
  z-index: 99999;
  max-height: 480px;
  overflow-y: auto;
  overflow-x: hidden;
}

.search-group-label {
  padding: 8px 14px 4px;
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: rgb(var(--v-theme-secondary));
  border-top: 1px solid rgb(var(--v-theme-outline));
}

.search-group-label--first {
  border-top: none;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  cursor: pointer;
  transition: background 0.1s;
}

.search-item:hover,
.search-item--active {
  background: rgba(var(--v-theme-on-surface), 0.06);
}

.search-item-icon {
  flex-shrink: 0;
}

.search-item-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.search-item-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: rgb(var(--v-theme-on-surface));
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-item-subtitle {
  font-size: 0.75rem;
  color: rgb(var(--v-theme-secondary));
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-item-badge {
  flex-shrink: 0;
}

.search-item-title mark,
.search-item-subtitle mark {
  background: rgba(var(--v-theme-info), 0.22);
  color: inherit;
  border-radius: 2px;
  padding: 0 1px;
}

.search-footer {
  display: flex;
  gap: 16px;
  padding: 7px 14px;
  border-top: 1px solid rgb(var(--v-theme-outline));
  font-size: 0.68rem;
  color: rgb(var(--v-theme-secondary));
  background: rgba(var(--v-theme-surface-variant), 0.5);
}

.search-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  color: rgb(var(--v-theme-secondary));
  font-size: 0.875rem;
  gap: 4px;
}
</style>
