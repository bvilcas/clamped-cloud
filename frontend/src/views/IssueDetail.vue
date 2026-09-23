<script setup lang="ts">
// One issue. Every action sits in a single bar under the title; the facts, the
// roles and the progress trail are split across two columns.
const issue = {
  key: 'CLM-139',
  title: 'NullPointerException in PaymentService.processPayment',
  severity: 'Critical',
  status: 'Patched',
  events: 54,
  project: 'checkout-service',
  due: '9/24/2026',
  reported: '9/18/2026',
  repository: 'clamped/checkout-service',
  commit: 'a91c4f2',
  description:
    'Checkout fails when a saved card has no billing address. PaymentService.processPayment reads address.getPostalCode() without a null check, so the request dies at line 42 and the customer sees a generic 500 page instead of a card error. Only cards imported from the old provider are affected, which is why the checkout tests missed it.',
}

const eventInfo = [
  ['App', 'checkout-service'],
  ['Host', 'prod-checkout-02'],
  ['Type', 'java.lang.NullPointerException'],
  ['Occurrences', '54 since 9/18/2026'],
]

const payload = `{ "sourceFile": "PaymentService.java", "sourceLine": 42,
  "sourceMethod": "processPayment", "billingAddress": null }`

const assignments = [
  { name: 'Dana Reyes', role: 'Reporter', color: 'grey' },
  { name: 'Brent Vilcas (you)', role: 'Assignee', color: 'info' },
  { name: 'Priya Nair', role: 'Verifier', color: 'success' },
]

const timeline = [
  { text: 'Reported by Dana Reyes', date: '9/18/2026', done: true },
  { text: 'In Progress, taken by you', date: '9/19/2026', done: true },
  { text: 'Patched, waiting on a verifier', date: '9/20/2026', current: true },
  { text: 'Under Review' },
  { text: 'Verified' },
]

// The server decides this. The greyed button is a courtesy, not the enforcement.
const canVerify = false
const verifyReason =
  'You patched this issue, so Priya Nair verifies it: a fix always needs a second person.'
</script>

<template>
  <v-container fluid class="pa-6">
    <v-breadcrumbs
      :items="[{ title: 'Dashboard', to: '/dashboard' }, { title: 'My Issues', to: '/issues' }, { title: issue.key }]"
      density="compact"
      class="px-0"
    />

    <div class="d-flex flex-wrap align-start justify-space-between mb-3">
      <div>
        <h1 class="text-h5 font-weight-bold text-primary">{{ issue.title }}</h1>
        <div class="d-flex align-center ga-2 mt-1">
          <v-chip color="red-darken-3" size="x-small" variant="flat">{{ issue.severity }}</v-chip>
          <v-chip color="success" size="x-small" variant="tonal">{{ issue.status }}</v-chip>
          <span class="text-caption text-medium-emphasis font-monospace">{{ issue.events }} events</span>
        </div>
      </div>
      <div class="d-flex ga-2">
        <v-btn variant="outlined" size="small">Edit Details</v-btn>
        <v-btn variant="outlined" size="small" to="/issues">Back to My Issues</v-btn>
      </div>
    </div>

    <!-- One bar. Delete sits apart from the rest. -->
    <v-card variant="elevated" elevation="2" class="pa-3 mb-3">
      <div class="d-flex flex-wrap align-center ga-2">
        <v-btn color="info" size="small">Move to Under Review</v-btn>
        <v-btn variant="outlined" size="small">Assign Member</v-btn>
        <v-btn variant="outlined" size="small">Revoke my role</v-btn>
        <v-btn variant="outlined" size="small" :disabled="!canVerify">Take as Verifier</v-btn>
        <v-spacer />
        <v-btn color="error" variant="outlined" size="small">Delete</v-btn>
      </div>
      <p v-if="!canVerify" class="text-caption text-medium-emphasis mt-2 mb-0">
        <v-icon icon="mdi-information-outline" size="x-small" class="mr-1" />{{ verifyReason }}
      </p>
    </v-card>

    <v-row>
      <v-col cols="12" md="8">
        <v-card variant="elevated" elevation="2" class="mb-3">
          <v-card-title class="text-subtitle-1">Description</v-card-title>
          <v-card-text>
            <p class="text-body-2">{{ issue.description }}</p>

            <v-divider class="my-3" />

            <p class="text-overline mb-1">Event Info</p>
            <v-row dense class="text-caption">
              <v-col v-for="[label, value] in eventInfo" :key="label" cols="6">
                <span class="text-medium-emphasis">{{ label }}</span>
                <span class="ml-2 font-monospace">{{ value }}</span>
              </v-col>
            </v-row>

            <!-- Long lines scroll inside the box rather than widening the page -->
            <v-sheet color="grey-darken-4" rounded class="pa-3 mt-3 overflow-x-auto">
              <pre class="text-caption font-monospace mb-0">{{ payload }}</pre>
            </v-sheet>
          </v-card-text>
        </v-card>

        <v-card variant="elevated" elevation="2">
          <v-card-title class="text-subtitle-1">Add a Note</v-card-title>
          <v-card-text>
            <v-textarea
              rows="2"
              density="compact"
              label="Note for the team"
              placeholder="What changed, and how can the verifier reproduce it?"
              hint="Everyone on checkout-service can read this."
              persistent-hint
            />
            <v-btn color="info" size="small" class="mt-3">Post Note</v-btn>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
        <v-card variant="elevated" elevation="2" class="mb-3">
          <v-card-title class="text-subtitle-1">Details</v-card-title>
          <v-card-text class="text-caption">
            <v-row dense>
              <v-col cols="6"><span class="text-medium-emphasis">Project</span></v-col>
              <v-col cols="6">{{ issue.project }}</v-col>
              <v-col cols="6"><span class="text-medium-emphasis">Events</span></v-col>
              <v-col cols="6">{{ issue.events }} since {{ issue.reported }}</v-col>
              <v-col cols="6"><span class="text-medium-emphasis">Due</span></v-col>
              <v-col cols="6">{{ issue.due }}</v-col>
              <v-col cols="6"><span class="text-medium-emphasis">Repository</span></v-col>
              <v-col cols="6" class="font-monospace">{{ issue.repository }}</v-col>
              <v-col cols="6"><span class="text-medium-emphasis">Commit</span></v-col>
              <v-col cols="6" class="font-monospace">{{ issue.commit }}</v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card variant="elevated" elevation="2" class="mb-3">
          <v-card-title class="text-subtitle-1">Current Assignments</v-card-title>
          <v-list density="compact" class="py-0">
            <v-list-item v-for="a in assignments" :key="a.name">
              <template #prepend>
                <v-avatar size="24" color="primary" class="text-caption">{{ a.name.slice(0, 1) }}</v-avatar>
              </template>
              <v-list-item-title class="text-body-2">{{ a.name }}</v-list-item-title>
              <template #append>
                <v-chip :color="a.color" size="x-small" variant="tonal">{{ a.role }}</v-chip>
              </template>
            </v-list-item>
          </v-list>
        </v-card>

        <v-card variant="elevated" elevation="2">
          <v-card-title class="text-subtitle-1">Progress</v-card-title>
          <v-card-text>
            <v-timeline side="end" density="compact" truncate-line="both">
              <v-timeline-item
                v-for="step in timeline"
                :key="step.text"
                :dot-color="step.done ? 'success' : step.current ? 'info' : 'grey-lighten-1'"
                size="x-small"
              >
                <div class="text-body-2">{{ step.text }}</div>
                <div v-if="step.date" class="text-caption text-medium-emphasis">{{ step.date }}</div>
              </v-timeline-item>
            </v-timeline>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
