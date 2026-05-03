<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchWithAuth } from '@/utils/fetchWithAuth'

const router = useRouter()

interface CalendarVuln {
  id: number
  title: string
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  status: string
  dueAt: string
  projectId: number
  projectName: string
}

const vulns = ref<CalendarVuln[]>([])
const loading = ref(true)
const error = ref('')
const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth()) // 0-indexed
const selectedDay = ref<number | null>(null)

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]
const DAY_NAMES = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

onMounted(async () => {
  try {
    const res = await fetchWithAuth('/api/v1/calendar/vulns')
    if (res.ok) {
      const data = await res.json()
      vulns.value = data.data || []
    } else {
      error.value = 'Failed to load calendar data'
    }
  } catch {
    error.value = 'Network error'
  } finally {
    loading.value = false
  }
})

function prevMonth() {
  selectedDay.value = null
  if (currentMonth.value === 0) {
    currentMonth.value = 11
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

function nextMonth() {
  selectedDay.value = null
  if (currentMonth.value === 11) {
    currentMonth.value = 0
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

function goToToday() {
  selectedDay.value = today.getDate()
  currentMonth.value = today.getMonth()
  currentYear.value = today.getFullYear()
}

/** Returns the grid cells: nulls for leading blanks, then day numbers */
const calendarCells = computed<(number | null)[]>(() => {
  const firstDow = new Date(currentYear.value, currentMonth.value, 1).getDay()
  const daysInMonth = new Date(currentYear.value, currentMonth.value + 1, 0).getDate()
  const cells: (number | null)[] = Array(firstDow).fill(null)
  for (let d = 1; d <= daysInMonth; d++) cells.push(d)
  // Pad to complete the last week row
  while (cells.length % 7 !== 0) cells.push(null)
  return cells
})

/** Returns vulns whose dueAt falls on a specific day in the displayed month */
function vulnsOnDay(day: number): CalendarVuln[] {
  return vulns.value.filter(v => {
    const d = new Date(v.dueAt)
    return (
      d.getFullYear() === currentYear.value &&
      d.getMonth() === currentMonth.value &&
      d.getDate() === day
    )
  })
}

function isToday(day: number): boolean {
  return (
    day === today.getDate() &&
    currentMonth.value === today.getMonth() &&
    currentYear.value === today.getFullYear()
  )
}

const selectedVulns = computed<CalendarVuln[]>(() =>
  selectedDay.value !== null ? vulnsOnDay(selectedDay.value) : []
)

const severityColor = (s: string) => {
  switch (s) {
    case 'CRITICAL': return 'error'
    case 'HIGH':     return 'deep-orange'
    case 'MEDIUM':   return 'warning'
    case 'LOW':      return 'success'
    default:         return 'grey'
  }
}

const statusColor = (s: string) => {
  switch (s) {
    case 'VERIFIED':     return 'success'
    case 'PATCHED':      return 'teal'
    case 'UNDER_REVIEW': return 'info'
    case 'IN_PROGRESS':  return 'warning'
    default:             return 'grey'
  }
}

/** Highest severity on a day drives the dot color */
function dayColor(day: number): string {
  const v = vulnsOnDay(day)
  if (v.some(x => x.severity === 'CRITICAL')) return 'error'
  if (v.some(x => x.severity === 'HIGH'))     return 'deep-orange'
  if (v.some(x => x.severity === 'MEDIUM'))   return 'warning'
  if (v.some(x => x.severity === 'LOW'))      return 'success'
  return 'grey'
}
</script>

<template>
  <v-container class="pa-6" style="max-width: 860px">
    <div class="d-flex align-center justify-space-between mb-4">
      <h1 class="text-h5 text-info">Due-Date Calendar</h1>
      <v-btn size="small" variant="tonal" color="info" @click="goToToday">Today</v-btn>
    </div>

    <v-alert v-if="error" type="error" density="compact" class="mb-4">{{ error }}</v-alert>

    <v-card variant="tonal" color="surface-variant" class="mb-4">
      <!-- Month navigation -->
      <div class="d-flex align-center justify-space-between px-4 pt-3 pb-2">
        <v-btn icon="mdi-chevron-left" variant="text" size="small" @click="prevMonth" />
        <span class="text-h6 font-weight-medium">
          {{ MONTH_NAMES[currentMonth] }} {{ currentYear }}
        </span>
        <v-btn icon="mdi-chevron-right" variant="text" size="small" @click="nextMonth" />
      </div>

      <!-- Day-of-week headers -->
      <div class="cal-grid cal-header px-2 pb-1">
        <div v-for="d in DAY_NAMES" :key="d" class="cal-cell cal-dow">{{ d }}</div>
      </div>

      <!-- Day cells -->
      <v-progress-linear v-if="loading" indeterminate color="info" class="mx-4 mb-2" />

      <div class="cal-grid px-2 pb-3">
        <div
          v-for="(cell, i) in calendarCells"
          :key="i"
          class="cal-cell"
          :class="{
            'cal-cell--today':    cell !== null && isToday(cell),
            'cal-cell--selected': cell !== null && selectedDay === cell,
            'cal-cell--empty':    cell === null,
            'cal-cell--clickable': cell !== null,
          }"
          @click="cell !== null ? (selectedDay = selectedDay === cell ? null : cell) : null"
        >
          <template v-if="cell !== null">
            <span class="cal-day-num">{{ cell }}</span>
            <!-- Severity dots -->
            <div class="cal-dots">
              <template v-for="v in vulnsOnDay(cell).slice(0, 3)" :key="v.id">
                <span
                  class="cal-dot"
                  :style="{ background: `rgb(var(--v-theme-${dayColor(cell)}))` }"
                />
              </template>
              <span
                v-if="vulnsOnDay(cell).length > 3"
                class="cal-dot-more text-caption"
              >+{{ vulnsOnDay(cell).length - 3 }}</span>
            </div>
          </template>
        </div>
      </div>
    </v-card>

    <!-- Selected day panel -->
    <v-expand-transition>
      <v-card v-if="selectedDay !== null" variant="outlined" class="mt-2">
        <v-card-title class="text-body-1 font-weight-medium pa-3 pb-1">
          {{ MONTH_NAMES[currentMonth] }} {{ selectedDay }}, {{ currentYear }}
          <span v-if="selectedVulns.length === 0" class="text-medium-emphasis text-body-2 ml-2">— no vulnerabilities due</span>
        </v-card-title>

        <v-list v-if="selectedVulns.length > 0" density="compact" class="pa-2">
          <v-list-item
            v-for="v in selectedVulns"
            :key="v.id"
            class="vuln-item mb-1"
            rounded="lg"
            @click="router.push(`/project/${v.projectId}`)"
          >
            <template #prepend>
              <v-icon
                :color="severityColor(v.severity)"
                icon="mdi-shield-alert"
                size="20"
                class="mr-2"
              />
            </template>
            <v-list-item-title class="text-body-2">{{ v.title }}</v-list-item-title>
            <v-list-item-subtitle class="text-caption">{{ v.projectName }}</v-list-item-subtitle>
            <template #append>
              <div class="d-flex ga-1">
                <v-chip :color="severityColor(v.severity)" size="x-small" label>{{ v.severity }}</v-chip>
                <v-chip :color="statusColor(v.status)" size="x-small" label variant="tonal">
                  {{ v.status.replace('_', ' ') }}
                </v-chip>
              </div>
            </template>
          </v-list-item>
        </v-list>
      </v-card>
    </v-expand-transition>

    <!-- Legend -->
    <div class="d-flex ga-3 mt-4 flex-wrap">
      <div v-for="[label, color] in [['Low','success'],['Medium','warning'],['High','deep-orange'],['Critical','error']]" :key="label" class="d-flex align-center ga-1">
        <span class="cal-dot" :style="{ background: `rgb(var(--v-theme-${color}))` }" />
        <span class="text-caption text-medium-emphasis">{{ label }}</span>
      </div>
    </div>
  </v-container>
</template>

<style scoped>
.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.cal-header .cal-cell {
  text-align: center;
  font-size: 0.7rem;
  font-weight: 600;
  color: rgb(var(--v-theme-secondary));
  padding: 4px 0;
}

.cal-cell {
  min-height: 64px;
  border-radius: 8px;
  padding: 6px 6px 4px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  position: relative;
  transition: background 0.15s;
}

.cal-cell--clickable {
  cursor: pointer;
}
.cal-cell--clickable:hover {
  background: rgba(var(--v-theme-on-surface), 0.06);
}

.cal-cell--today {
  background: rgba(var(--v-theme-info), 0.12);
  outline: 2px solid rgb(var(--v-theme-info));
  outline-offset: -2px;
}

.cal-cell--selected {
  background: rgba(var(--v-theme-info), 0.22) !important;
}

.cal-cell--empty {
  opacity: 0;
  pointer-events: none;
}

.cal-day-num {
  font-size: 0.8rem;
  font-weight: 500;
  line-height: 1;
  margin-bottom: 4px;
}

.cal-dots {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
  align-items: center;
}

.cal-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.cal-dot-more {
  font-size: 0.6rem;
  color: rgb(var(--v-theme-secondary));
  line-height: 1;
}

.vuln-item {
  background: rgba(var(--v-theme-on-surface), 0.04);
  cursor: pointer;
}
.vuln-item:hover {
  background: rgba(var(--v-theme-on-surface), 0.08);
}
</style>
