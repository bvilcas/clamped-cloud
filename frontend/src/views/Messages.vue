<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

interface Project {
  id: string
  name: string
  myRole: string
}

interface MessageDto {
  id: number
  content: string
  senderId: number
  senderFirstname: string
  senderLastname: string
  projectId: number
  sentAt: string
}

const projects = ref<Project[]>([])
const messages = ref<MessageDto[]>([])
const loading = ref(true)
const messagesLoading = ref(false)
const sending = ref(false)
const selectedProjectId = ref<string | null>(null)
const messageInput = ref('')
const currentUserId = ref<number | null>(null)
const messageThread = ref<HTMLElement | null>(null)

const selectedProject = computed(() =>
  projects.value.find(p => p.id === selectedProjectId.value) ?? null
)

onMounted(async () => {
  try {
    const [projectsRes, profileRes] = await Promise.all([
      fetchWithAuth('/api/v1/userprojects/all'),
      fetchWithAuth('/api/v1/users/profile')
    ])

    if (!projectsRes.ok || !profileRes.ok) { authStore.logout(); return }

    const projectsData = await projectsRes.json()
    const profileData = await profileRes.json()

    projects.value = projectsData.data || []
    currentUserId.value = profileData.data?.id ?? null

    if (projects.value.length > 0 && projects.value[0]) {
      selectedProjectId.value = projects.value[0].id
    }
  } catch {
    authStore.logout()
  } finally {
    loading.value = false
  }
})

watch(selectedProjectId, async (projectId) => {
  if (!projectId) { messages.value = []; return }
  messagesLoading.value = true
  try {
    const res = await fetchWithAuth(`/api/v1/messages/${projectId}`)
    if (res.ok) {
      const data = await res.json()
      messages.value = data.data || []
      await nextTick()
      scrollToBottom()
    }
  } finally {
    messagesLoading.value = false
  }
})

const scrollToBottom = () => {
  if (messageThread.value) {
    messageThread.value.scrollTop = messageThread.value.scrollHeight
  }
}

const sendMessage = async () => {
  const content = messageInput.value.trim()
  if (!content || !selectedProjectId.value || sending.value) return

  sending.value = true
  try {
    const res = await fetchWithAuth('/api/v1/messages', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ projectId: Number(selectedProjectId.value), content })
    })
    if (res.ok) {
      const data = await res.json()
      messages.value.push(data.data)
      messageInput.value = ''
      await nextTick()
      scrollToBottom()
    }
  } finally {
    sending.value = false
  }
}

const formatTime = (isoString: string) => {
  const date = new Date(isoString)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

const formatDate = (isoString: string) => {
  const date = new Date(isoString)
  const today = new Date()
  if (date.toDateString() === today.toDateString()) return 'Today'
  const yesterday = new Date(today)
  yesterday.setDate(today.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) return 'Yesterday'
  return date.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })
}

// Group messages by date for date separators
const groupedMessages = computed(() => {
  const groups: { date: string; items: MessageDto[] }[] = []
  for (const msg of messages.value) {
    const date = formatDate(msg.sentAt)
    const last = groups[groups.length - 1]
    if (!last || last.date !== date) {
      groups.push({ date, items: [msg] })
    } else {
      last.items.push(msg)
    }
  }
  return groups
})
</script>

<template>
  <v-container class="pa-8">
    <h1 class="text-info mb-1">Messages</h1>
    <p class="text-secondary mb-4">Project-based team messaging.</p>

    <p v-if="loading" class="text-secondary">Loading...</p>

    <template v-else-if="projects.length === 0">
      <p class="text-secondary">You're not a member of any projects. Join a project to start messaging.</p>
    </template>

    <v-row v-else style="height: calc(100vh - 240px); min-height: 400px;">
      <!-- Left: channel list -->
      <v-col cols="12" md="3" class="d-flex flex-column" style="height: 100%;">
        <v-card variant="elevated" elevation="2" style="height: 100%; overflow-y: auto;">
          <v-card-title class="text-caption text-secondary py-2 px-4">PROJECTS</v-card-title>
          <v-list density="compact" nav>
            <v-list-item
              v-for="p in projects"
              :key="p.id"
              :value="p.id"
              :active="selectedProjectId === p.id"
              active-color="info"
              rounded="lg"
              @click="selectedProjectId = p.id"
            >
              <template #prepend>
                <v-icon size="small">mdi-pound</v-icon>
              </template>
              <v-list-item-title class="text-body-2">{{ p.name }}</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-card>
      </v-col>

      <!-- Right: message area -->
      <v-col cols="12" md="9" class="d-flex flex-column" style="height: 100%;">
        <v-card variant="elevated" elevation="2" class="d-flex flex-column" style="height: 100%;">
          <!-- Header -->
          <v-card-title class="d-flex align-center ga-2 border-b pa-4">
            <v-icon>mdi-pound</v-icon>
            {{ selectedProject?.name ?? 'Select a project' }}
          </v-card-title>

          <!-- Message thread -->
          <div
            ref="messageThread"
            class="flex-1-1 pa-4"
            style="overflow-y: auto; flex: 1 1 0; min-height: 0;"
          >
            <!-- Loading -->
            <div v-if="messagesLoading" class="d-flex justify-center align-center h-100">
              <v-progress-circular indeterminate color="info" />
            </div>

            <!-- Empty state -->
            <div
              v-else-if="messages.length === 0"
              class="d-flex flex-column align-center justify-center h-100 text-center"
              style="min-height: 200px;"
            >
              <v-icon size="64" color="secondary" class="mb-4">mdi-message-outline</v-icon>
              <p class="text-secondary">
                This is the beginning of the <strong>#{{ selectedProject?.name }}</strong> channel.
              </p>
              <p class="text-caption text-secondary mt-1">Be the first to send a message!</p>
            </div>

            <!-- Messages grouped by date -->
            <template v-else>
              <div v-for="group in groupedMessages" :key="group.date">
                <!-- Date separator -->
                <div class="d-flex align-center ga-2 my-3">
                  <v-divider />
                  <span class="text-caption text-secondary text-no-wrap">{{ group.date }}</span>
                  <v-divider />
                </div>

                <!-- Messages in group -->
                <div
                  v-for="msg in group.items"
                  :key="msg.id"
                  class="d-flex ga-3 mb-3"
                  :class="msg.senderId === currentUserId ? 'flex-row-reverse' : 'flex-row'"
                >
                  <!-- Avatar -->
                  <v-avatar size="32" color="surface-variant" class="flex-shrink-0 mt-1">
                    <span class="text-caption font-weight-bold">
                      {{ msg.senderFirstname[0] }}{{ msg.senderLastname[0] }}
                    </span>
                  </v-avatar>

                  <!-- Bubble -->
                  <div style="max-width: 70%;">
                    <div
                      class="d-flex align-center ga-2 mb-1"
                      :class="msg.senderId === currentUserId ? 'flex-row-reverse' : 'flex-row'"
                    >
                      <span class="text-caption font-weight-bold">
                        {{ msg.senderId === currentUserId ? 'You' : `${msg.senderFirstname} ${msg.senderLastname}` }}
                      </span>
                      <span class="text-caption text-secondary">{{ formatTime(msg.sentAt) }}</span>
                    </div>
                    <v-card
                      :color="msg.senderId === currentUserId ? 'info' : 'surface-variant'"
                      variant="flat"
                      class="pa-3"
                      style="border-radius: 12px; word-break: break-word;"
                    >
                      <span class="text-body-2">{{ msg.content }}</span>
                    </v-card>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <!-- Input area -->
          <div class="pa-4 border-t">
            <div class="d-flex ga-2 align-center">
              <v-text-field
                v-model="messageInput"
                :label="`Message #${selectedProject?.name ?? '...'}`"
                variant="outlined"
                density="compact"
                hide-details
                :disabled="!selectedProjectId || sending"
                @keydown.enter.prevent="sendMessage"
              />
              <v-btn
                icon="mdi-send"
                color="info"
                :loading="sending"
                :disabled="!messageInput.trim() || !selectedProjectId"
                @click="sendMessage"
              />
            </div>
          </div>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
