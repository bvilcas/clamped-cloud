<script setup lang="ts">
// Every issue you hold a role on, filtered and sorted. One row per issue, with
// the title as the column that gives way when the screen is narrow.
import { ref } from 'vue'

const showConfirmation = ref(true)

const filters = ref({ search: '', project: null, role: null, status: null, severity: 'Medium and above' })
const projects = ['api-gateway', 'checkout-service', 'clamped-web']
const roles = ['Reported by me', 'Assigned to me', 'Verifying']
const statuses = ['Reported', 'In Progress', 'Patched', 'Under Review', 'Verified']
const severities = ['Any', 'Medium and above', 'High and above', 'Critical only']

const headers = [
  { title: 'Key', key: 'key', width: '8%' },
  { title: 'Issue', key: 'title', width: '35%' },
  { title: 'Project', key: 'project', width: '14%' },
  { title: 'Severity', key: 'severity', width: '9%' },
  { title: 'Status', key: 'status', width: '11%' },
  { title: 'Owner', key: 'owner', width: '14%' },
  { title: 'Due', key: 'due', width: '9%' },
]

const issues = [
  { key: 'CLM-148', title: 'Checkout returns 500 when a saved card has no billing address', project: 'checkout-service', severity: 'Critical', status: 'Reported', owner: 'You reported it', due: '9/28/2026' },
  { key: 'CLM-139', title: 'NullPointerException in PaymentService.processPayment', project: 'checkout-service', severity: 'Critical', status: 'Patched', owner: 'You patched it', due: '9/24/2026' },
  { key: 'CLM-137', title: 'Session cookie is not cleared after logout', project: 'clamped-web', severity: 'High', status: 'Patched', owner: 'You verify next', due: '9/26/2026' },
  { key: 'CLM-134', title: 'Refresh token retry storm when the access token expires', project: 'api-gateway', severity: 'High', status: 'In Progress', owner: 'Dana R.', due: '10/1/2026' },
  { key: 'CLM-131', title: 'Rate limit exceeded for client IP 198.51.100.22', project: 'api-gateway', severity: 'Medium', status: 'Reported', owner: 'Unassigned', due: '10/3/2026' },
  { key: 'CLM-128', title: 'Team page loads member lists one project at a time', project: 'clamped-web', severity: 'Medium', status: 'In Progress', owner: 'You are fixing', due: '10/6/2026' },
  { key: 'CLM-119', title: 'Duplicate notification emails when a role changes', project: 'clamped-web', severity: 'Medium', status: 'Verified', owner: 'You verified', due: '9/15/2026' },
]

const severityColor = (s: string) =>
  ({ Low: 'success', Medium: 'orange', High: 'error', Critical: 'red-darken-3' }[s] ?? 'grey')

const statusColor = (s: string) =>
  ({ Reported: 'grey', 'In Progress': 'warning', Patched: 'success', 'Under Review': 'info', Verified: 'purple' }[s] ?? 'grey')
</script>

<template>
  <v-container fluid class="pa-6">
    <v-breadcrumbs :items="[{ title: 'Dashboard', to: '/dashboard' }, { title: 'My Issues' }]" density="compact" class="px-0" />

    <!-- The submit that brought you here. role=status so it is announced. -->
    <v-alert
      v-if="showConfirmation"
      type="success"
      variant="tonal"
      role="status"
      closable
      class="mb-4"
      @click:close="showConfirmation = false"
    >
      <strong>Issue reported successfully.</strong>
      CLM-148 was filed in checkout-service and now sits at the top of this list.
      <v-btn variant="text" size="small" to="/issues/CLM-148">Open CLM-148</v-btn>
    </v-alert>

    <div class="d-flex flex-wrap align-center justify-space-between mb-3">
      <h1 class="text-h5 font-weight-bold text-primary">My Issues</h1>
      <v-btn color="info" prepend-icon="mdi-plus" to="/report">Report Issue</v-btn>
    </div>

    <v-card variant="elevated" elevation="2" class="pa-3 mb-3">
      <v-row dense align="end">
        <v-col cols="12" md="3">
          <v-text-field v-model="filters.search" label="Search" placeholder="Title or key" density="compact" hide-details />
        </v-col>
        <v-col cols="6" md="2">
          <v-select v-model="filters.project" :items="projects" label="Project" density="compact" hide-details clearable />
        </v-col>
        <v-col cols="6" md="2">
          <v-select v-model="filters.role" :items="roles" label="My role" density="compact" hide-details clearable />
        </v-col>
        <v-col cols="6" md="2">
          <v-select v-model="filters.status" :items="statuses" label="Status" density="compact" hide-details clearable />
        </v-col>
        <v-col cols="6" md="2">
          <v-select v-model="filters.severity" :items="severities" label="Severity" density="compact" hide-details />
        </v-col>
        <v-col cols="12" md="1">
          <v-btn color="info" block>Apply</v-btn>
        </v-col>
      </v-row>
    </v-card>

    <!-- Filters in force stay visible and removable -->
    <div class="d-flex flex-wrap align-center ga-2 mb-2 text-body-2 text-medium-emphasis">
      <span>{{ issues.length }} of 48 issues</span>
      <v-chip size="small" closable variant="outlined" color="info" @click:close="filters.severity = 'Any'">
        Severity: {{ filters.severity }}
      </v-chip>
      <v-btn variant="text" size="small">Clear all filters</v-btn>
    </div>

    <v-card variant="elevated" elevation="2">
      <v-data-table
        :headers="headers"
        :items="issues"
        density="compact"
        items-per-page="10"
        hover
      >
        <template #item.key="{ item }">
          <RouterLink :to="`/issues/${item.key}`" class="text-primary text-decoration-none font-monospace text-caption">
            {{ item.key }}
          </RouterLink>
        </template>

        <template #item.title="{ item }">
          <RouterLink :to="`/issues/${item.key}`" class="text-high-emphasis text-decoration-none d-block text-truncate">
            {{ item.title }}
          </RouterLink>
        </template>

        <template #item.severity="{ item }">
          <v-chip :color="severityColor(item.severity)" size="x-small" variant="flat">{{ item.severity }}</v-chip>
        </template>

        <template #item.status="{ item }">
          <v-chip :color="statusColor(item.status)" size="x-small" variant="tonal">{{ item.status }}</v-chip>
        </template>

        <template #item.owner="{ item }">
          <div class="d-flex align-center ga-2">
            <v-avatar size="20" color="primary" class="text-caption">{{ item.owner.slice(0, 1) }}</v-avatar>
            <span class="text-caption">{{ item.owner }}</span>
          </div>
        </template>
      </v-data-table>
    </v-card>
  </v-container>
</template>
