<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchWithAuth } from '@/utils/fetchWithAuth'

const router = useRouter()

interface NotificationItem {
  id: number
  type: string
  message: string
  relatedProjectId: number | null
  relatedVulnId: number | null
  read: boolean
  createdAt: string
}

const notifications = ref<NotificationItem[]>([])
const loading = ref(true)
const filterRead = ref<'all' | 'unread' | 'read'>('all')

const filteredNotifications = computed(() => {
  if (filterRead.value === 'unread') return notifications.value.filter(n => !n.read)
  if (filterRead.value === 'read')   return notifications.value.filter(n => n.read)
  return notifications.value
})

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

const notifTypeIcon = (type: string) => {
  const map: Record<string, string> = {
    VULN_ASSIGNED:        'mdi-shield-check-outline',
    VULN_UNASSIGNED:      'mdi-shield-remove-outline',
    VULN_REPORTED:        'mdi-shield-alert-outline',
    VULN_STATUS_CHANGED:  'mdi-shield-refresh-outline',
    MEMBER_SELF_ASSIGNED: 'mdi-account-check-outline',
    MEMBER_SELF_REVOKED:  'mdi-account-minus-outline',
    PROJECT_ADDED:        'mdi-folder-plus-outline',
    PROJECT_REMOVED:      'mdi-folder-remove-outline',
  }
  return map[type] ?? 'mdi-bell-outline'
}

const notifTypeColor = (type: string) => {
  if (type === 'VULN_REPORTED')        return 'warning'
  if (type === 'VULN_ASSIGNED' || type === 'MEMBER_SELF_ASSIGNED') return 'success'
  if (type === 'VULN_UNASSIGNED' || type === 'MEMBER_SELF_REVOKED' || type === 'PROJECT_REMOVED') return 'error'
  if (type === 'PROJECT_ADDED')        return 'info'
  return 'secondary'
}

const notifTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    VULN_ASSIGNED:        'Assigned',
    VULN_UNASSIGNED:      'Unassigned',
    VULN_REPORTED:        'New Report',
    VULN_STATUS_CHANGED:  'Status Change',
    MEMBER_SELF_ASSIGNED: 'Self-Assigned',
    MEMBER_SELF_REVOKED:  'Self-Revoked',
    PROJECT_ADDED:        'Added to Project',
    PROJECT_REMOVED:      'Removed from Project',
  }
  return map[type] ?? type
}

const timeAgo = (iso: string) => {
  const diff = Date.now() - new Date(iso).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  const days = Math.floor(hrs / 24)
  if (days < 7) return `${days}d ago`
  return new Date(iso).toLocaleDateString()
}

const fetchNotifications = async () => {
  loading.value = true
  try {
    const res = await fetchWithAuth('/api/v1/notifications')
    if (res.ok) {
      const d = await res.json()
      notifications.value = d.data ?? []
    }
  } catch { /* silent */ } finally {
    loading.value = false
  }
}

const markRead = async (n: NotificationItem) => {
  if (n.read) return
  try {
    await fetchWithAuth(`/api/v1/notifications/${n.id}/read`, { method: 'PUT' })
    n.read = true
  } catch { /* silent */ }
}

const markAllRead = async () => {
  try {
    await fetchWithAuth('/api/v1/notifications/read-all', { method: 'PUT' })
    notifications.value.forEach(n => { n.read = true })
  } catch { /* silent */ }
}

const deleteNotif = async (n: NotificationItem) => {
  try {
    await fetchWithAuth(`/api/v1/notifications/${n.id}`, { method: 'DELETE' })
    notifications.value = notifications.value.filter(x => x.id !== n.id)
  } catch { /* silent */ }
}

const clearAll = async () => {
  try {
    await fetchWithAuth('/api/v1/notifications/clear-all', { method: 'DELETE' })
    notifications.value = []
  } catch { /* silent */ }
}

const navigate = (n: NotificationItem) => {
  markRead(n)
  if (n.relatedProjectId) router.push(`/project/${n.relatedProjectId}`)
}

onMounted(fetchNotifications)
</script>

<template>
  <div class="notifications-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">Notifications</h1>
        <p class="page-subtitle">
          {{ unreadCount > 0 ? `${unreadCount} unread` : 'All caught up' }}
        </p>
      </div>
      <div class="d-flex ga-2 align-center">
        <v-btn-toggle v-model="filterRead" mandatory density="compact" rounded="lg" variant="outlined">
          <v-btn value="all" size="small">All</v-btn>
          <v-btn value="unread" size="small">Unread</v-btn>
          <v-btn value="read" size="small">Read</v-btn>
        </v-btn-toggle>
        <v-btn
          v-if="unreadCount > 0"
          variant="tonal"
          size="small"
          prepend-icon="mdi-check-all"
          @click="markAllRead"
        >Mark all read</v-btn>
        <v-btn
          v-if="notifications.length > 0"
          variant="tonal"
          color="error"
          size="small"
          prepend-icon="mdi-delete-sweep-outline"
          @click="clearAll"
        >Clear all</v-btn>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="d-flex justify-center align-center pa-12">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <!-- Empty -->
    <div v-else-if="!filteredNotifications.length" class="empty-state">
      <v-icon icon="mdi-bell-off-outline" size="56" color="secondary" />
      <p class="text-h6 mt-3 text-secondary">
        {{ filterRead === 'all' ? 'No notifications yet' : `No ${filterRead} notifications` }}
      </p>
    </div>

    <!-- List -->
    <div v-else class="notif-list">
      <v-card
        v-for="n in filteredNotifications"
        :key="n.id"
        :class="['notif-card', { 'notif-card--unread': !n.read }]"
        variant="outlined"
        @click="navigate(n)"
      >
        <div class="notif-card-inner">
          <!-- Icon -->
          <div class="notif-icon-wrap">
            <v-icon :icon="notifTypeIcon(n.type)" :color="notifTypeColor(n.type)" size="22" />
          </div>

          <!-- Body -->
          <div class="notif-body">
            <div class="d-flex align-center ga-2 mb-1">
              <v-chip
                :color="notifTypeColor(n.type)"
                size="x-small"
                variant="tonal"
                density="comfortable"
              >{{ notifTypeLabel(n.type) }}</v-chip>
              <span v-if="!n.read" class="unread-dot" />
            </div>
            <p class="notif-message">{{ n.message }}</p>
            <span class="notif-time">{{ timeAgo(n.createdAt) }}</span>
          </div>

          <!-- Actions -->
          <div class="notif-actions">
            <v-btn
              v-if="!n.read"
              icon="mdi-check"
              size="x-small"
              variant="text"
              color="success"
              title="Mark as read"
              @click.stop="markRead(n)"
            />
            <v-btn
              icon="mdi-close"
              size="x-small"
              variant="text"
              color="error"
              title="Delete"
              @click.stop="deleteNotif(n)"
            />
          </div>
        </div>
      </v-card>
    </div>
  </div>
</template>

<style scoped>
.notifications-page {
  padding: 2rem;
  max-width: 760px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.page-title {
  font-size: 1.6rem;
  font-weight: 700;
  color: rgb(var(--v-theme-on-surface));
  margin: 0;
}

.page-subtitle {
  font-size: 0.875rem;
  color: rgb(var(--v-theme-secondary));
  margin: 0.2rem 0 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 5rem 2rem;
  color: rgb(var(--v-theme-secondary));
}

.notif-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.notif-card {
  cursor: pointer;
  transition: background 0.15s;
  border-radius: 10px !important;
}

.notif-card:hover {
  background: rgba(var(--v-theme-on-surface), 0.04) !important;
}

.notif-card--unread {
  border-color: rgb(var(--v-theme-primary)) !important;
  background: rgba(var(--v-theme-primary), 0.04) !important;
}

.notif-card-inner {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem 1rem 1rem 1rem;
}

.notif-icon-wrap {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(var(--v-theme-surface-variant), 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
}

.notif-body {
  flex: 1;
  min-width: 0;
}

.notif-message {
  font-size: 0.875rem;
  color: rgb(var(--v-theme-on-surface));
  margin: 0 0 0.25rem;
  line-height: 1.4;
}

.notif-time {
  font-size: 0.75rem;
  color: rgb(var(--v-theme-secondary));
}

.notif-actions {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex-shrink: 0;
}

.unread-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: rgb(var(--v-theme-primary));
  flex-shrink: 0;
}
</style>
