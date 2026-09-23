<script setup lang="ts">
// Filing an issue. Labelled panels in two columns rather than one long stack of
// inputs, with an error summary that links to the field that needs fixing.
import { ref } from 'vue'

const form = ref({
  project: 'checkout-service',
  title: 'Legacy card import drops the billing address',
  description: 'Import a card with no billing address, then check out. Expected a card error; got a generic 500 page.',
  severity: 'CRITICAL',
  status: 'REPORTED',
  dueAt: '2026-09-12',
  repository: 'clamped/checkout-service',
  commitHash: '',
  eventApp: 'checkout-service',
  eventHost: 'prod-checkout-02',
  eventType: 'java.lang.NullPointerException',
  eventExtra: '{"sourceLine": 42}',
  notify: true,
})

const projects = ['api-gateway', 'checkout-service', 'clamped-web']
const severities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
const statuses = ['REPORTED', 'IN_PROGRESS', 'PATCHED', 'UNDER_REVIEW', 'VERIFIED']

// A due date in the past, named exactly rather than "something went wrong".
const dueError = 'September 12, 2026 has already passed. Choose today or a later date.'
</script>

<template>
  <v-container fluid class="pa-6">
    <v-breadcrumbs
      :items="[{ title: 'Dashboard', to: '/dashboard' }, { title: 'My Issues', to: '/issues' }, { title: 'Report Issue' }]"
      density="compact"
      class="px-0"
    />

    <h1 class="text-h5 font-weight-bold text-primary">Report an Issue</h1>
    <p class="text-body-2 text-medium-emphasis mb-3">
      Fields marked <span class="text-error">*</span> are required. Everything else can be filled in later.
    </p>

    <!-- Names the problem and links to the field, rather than "something went wrong" -->
    <v-alert type="error" variant="tonal" role="alert" class="mb-4">
      <strong>This issue was not filed.</strong> One field needs attention:
      <a href="#issue-due" class="text-error">Due At is in the past</a>.
    </v-alert>

    <v-form>
      <v-row>
        <v-col cols="12" md="6">
          <v-card variant="elevated" elevation="2" class="mb-3">
            <v-card-title class="text-subtitle-1">Basics</v-card-title>
            <v-card-text>
              <v-select v-model="form.project" :items="projects" label="Project *" density="compact" class="mb-2" />
              <v-text-field
                v-model="form.title"
                label="Title *"
                density="compact"
                hint="Say what breaks, not what you think causes it."
                persistent-hint
                class="mb-2"
              />
              <v-textarea
                v-model="form.description"
                label="Description"
                rows="2"
                density="compact"
                hint="The steps a teammate would follow to see it happen."
                persistent-hint
              />
            </v-card-text>
          </v-card>

          <v-card variant="elevated" elevation="2">
            <v-card-title class="text-subtitle-1">Classification</v-card-title>
            <v-card-text>
              <!-- Four fixed values, all worth seeing at once -->
              <v-radio-group v-model="form.severity" inline density="compact" label="Severity *" hide-details>
                <v-radio v-for="s in severities" :key="s" :label="s" :value="s" />
              </v-radio-group>
              <p class="text-caption text-medium-emphasis mb-3">
                Critical means customers cannot complete a core task right now.
              </p>

              <v-row dense>
                <v-col cols="6">
                  <v-select v-model="form.status" :items="statuses" label="Status *" density="compact" hide-details />
                </v-col>
                <v-col cols="6">
                  <v-text-field
                    id="issue-due"
                    v-model="form.dueAt"
                    label="Due At *"
                    type="date"
                    density="compact"
                    :error-messages="dueError"
                  />
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card variant="elevated" elevation="2" class="mb-3">
            <v-card-title class="text-subtitle-1">Linked Code</v-card-title>
            <v-card-text>
              <v-row dense>
                <v-col cols="6">
                  <v-text-field v-model="form.repository" label="Repository" density="compact" hide-details />
                </v-col>
                <v-col cols="6">
                  <v-text-field v-model="form.commitHash" label="Commit / PR ID" placeholder="e.g. abc1234" density="compact" hide-details />
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <v-card variant="elevated" elevation="2" class="mb-3">
            <v-card-title class="text-subtitle-1">Event Details</v-card-title>
            <v-card-text>
              <p class="text-caption text-medium-emphasis mb-2">
                Optional. Paste these if you already have the crash in front of you.
              </p>
              <v-row dense>
                <v-col cols="6"><v-text-field v-model="form.eventApp" label="App" density="compact" hide-details /></v-col>
                <v-col cols="6"><v-text-field v-model="form.eventHost" label="Host" density="compact" hide-details /></v-col>
                <v-col cols="6"><v-text-field v-model="form.eventType" label="Event Type" density="compact" hide-details /></v-col>
                <v-col cols="6"><v-text-field v-model="form.eventExtra" label="Extra" density="compact" hide-details /></v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <!-- Submit shares the last panel, so both columns finish level -->
          <v-card variant="elevated" elevation="2">
            <v-card-text class="d-flex flex-wrap align-center justify-space-between ga-3">
              <v-checkbox v-model="form.notify" label="Notify the project." density="compact" hide-details />
              <div class="d-flex ga-2">
                <v-btn variant="outlined" to="/issues">Cancel</v-btn>
                <v-btn variant="outlined" type="reset">Clear</v-btn>
                <v-btn color="info" type="submit">File Issue</v-btn>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-form>
  </v-container>
</template>
