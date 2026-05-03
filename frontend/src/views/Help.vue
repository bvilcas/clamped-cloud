<script setup lang="ts">
</script>

<template>
  <v-container class="pa-8">
    <h1 class="text-info mb-4">Help & Documentation</h1>
    <p class="intro mb-8">
      Welcome to Clamped! Here you'll find guidance on how to use the system,
      report vulnerabilities, and manage your security workflow.
    </p>

    <v-card variant="elevated" elevation="2" class="help-card">
      <h2 class="card-title">Getting Started</h2>
      <p>
        After logging in, use the sidebar to navigate to your Profile, Vulnerabilities,
        and other sections. Your session is secured using JSESSIONID cookies and
        refresh token rotation for continuous access.
      </p>
    </v-card>

    <v-card variant="elevated" elevation="2" class="help-card">
      <h2 class="card-title">Reporting Vulnerabilities</h2>
      <p class="mb-2">
        Go to the <strong>Report</strong> page to submit a new vulnerability. Fill in details like:
      </p>
      <ul class="help-list">
        <li>Title and description</li>
        <li>Severity level (Low, Medium, High, Critical)</li>
        <li>CVE or CWE ID if available</li>
        <li>Repository and commit hash</li>
        <li>Due date for resolution</li>
      </ul>
    </v-card>

    <v-card variant="elevated" elevation="2" class="help-card">
      <h2 class="card-title">Managing Assignments</h2>
      <p class="mb-2">
        Vulnerabilities are categorized into:
      </p>
      <ul class="help-list mb-3">
        <li><strong>Reported</strong> -- Issues you've submitted.</li>
        <li><strong>Assigned</strong> -- Issues you are responsible for resolving.</li>
        <li><strong>Verified</strong> -- Issues you have confirmed as fixed.</li>
      </ul>
      <p>
        You can also self-assign to open vulnerabilities if your team allows it.
      </p>
    </v-card>

    <v-card variant="elevated" elevation="2" class="help-card">
      <h2 class="card-title">Security & Sessions</h2>
      <p>
        Clamped! uses filtered refresh token rotation and JSESSIONID cookies for safe,
        unlimited usage. If you change your email or password, all sessions will be
        invalidated, and you'll be required to log in again.
      </p>
    </v-card>

    <v-card variant="elevated" elevation="2" class="help-card">
      <h2 class="card-title">FAQ</h2>
      <ul class="help-list">
        <li><strong>Why was I logged out after updating my email?</strong><br />
        For security, changing your email or password revokes all active sessions.</li>
        <li><strong>What happens if my access token expires?</strong><br />
        The refresh token system will check your JSESSIONID cookie and issue a new access token on success.</li>
        <li><strong>How do I reset my password?</strong><br />
        Go to Profile then Change Password. This will log you out everywhere for security.</li>
      </ul>
    </v-card>
  </v-container>
</template>

<style scoped>
.intro {
  font-size: 1.1rem;
}

.help-card {
  margin-bottom: 2rem;
  padding: 1.5rem;
}

.card-title {
  margin-bottom: 0.6rem;
}

.help-list {
  padding-left: 1.2rem;
  list-style: disc;
}

.help-list li {
  margin-bottom: 0.5rem;
}
</style>
