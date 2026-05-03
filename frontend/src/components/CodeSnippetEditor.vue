<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import loader from '@monaco-editor/loader'

const props = withDefaults(defineProps<{
  modelValue?: string
  language?: string
  readonly?: boolean
  height?: string
}>(), {
  modelValue: '',
  language: 'plaintext',
  readonly: false,
  height: '240px',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'update:language', value: string): void
}>()

const editorContainer = ref<HTMLElement | null>(null)
let monacoEditor: any = null
let monacoInstance: any = null

const LANGUAGES = [
  { title: 'Plain Text', value: 'plaintext' },
  { title: 'Java', value: 'java' },
  { title: 'Python', value: 'python' },
  { title: 'JavaScript', value: 'javascript' },
  { title: 'TypeScript', value: 'typescript' },
  { title: 'C', value: 'c' },
  { title: 'C++', value: 'cpp' },
  { title: 'C#', value: 'csharp' },
  { title: 'Go', value: 'go' },
  { title: 'Rust', value: 'rust' },
  { title: 'PHP', value: 'php' },
  { title: 'Ruby', value: 'ruby' },
  { title: 'SQL', value: 'sql' },
  { title: 'Bash', value: 'shell' },
  { title: 'XML', value: 'xml' },
  { title: 'YAML', value: 'yaml' },
  { title: 'JSON', value: 'json' },
  { title: 'Kotlin', value: 'kotlin' },
]

const selectedLanguage = ref(props.language || 'plaintext')

onMounted(async () => {
  if (!editorContainer.value) return

  const monaco = await loader.init()
  monacoInstance = monaco

  monacoEditor = monaco.editor.create(editorContainer.value, {
    value: props.modelValue ?? '',
    language: selectedLanguage.value,
    theme: 'vs-dark',
    readOnly: props.readonly,
    automaticLayout: true,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    fontSize: 13,
    fontFamily: "'Fira Code', 'Cascadia Code', monospace",
    lineNumbers: 'on',
    wordWrap: 'on',
    padding: { top: 8, bottom: 8 },
    renderLineHighlight: props.readonly ? 'none' : 'line',
  })

  if (!props.readonly) {
    monacoEditor.onDidChangeModelContent(() => {
      emit('update:modelValue', monacoEditor.getValue())
    })
  }
})

watch(() => props.modelValue, (newVal) => {
  if (monacoEditor && newVal !== monacoEditor.getValue()) {
    monacoEditor.setValue(newVal ?? '')
  }
})

watch(selectedLanguage, (lang) => {
  if (monacoEditor && monacoInstance) {
    monacoInstance.editor.setModelLanguage(monacoEditor.getModel(), lang)
    emit('update:language', lang)
  }
})

watch(() => props.language, (lang) => {
  if (lang) selectedLanguage.value = lang
})

onBeforeUnmount(() => {
  monacoEditor?.dispose()
})
</script>

<template>
  <div class="code-snippet-editor">
    <div v-if="!readonly" class="editor-toolbar">
      <v-select
        v-model="selectedLanguage"
        :items="LANGUAGES"
        label="Language"
        variant="outlined"
        density="compact"
        hide-details
        style="max-width: 180px"
      />
    </div>
    <div v-else-if="language && language !== 'plaintext'" class="editor-lang-badge">
      <v-chip size="small" color="info" variant="tonal">{{ language }}</v-chip>
    </div>
    <div ref="editorContainer" :style="{ height }" class="editor-mount" />
  </div>
</template>

<style scoped>
.code-snippet-editor {
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 8px;
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  background: #1e1e1e;
  border-bottom: 1px solid #333;
}

.editor-lang-badge {
  display: flex;
  align-items: center;
  padding: 4px 10px;
  background: #1e1e1e;
  border-bottom: 1px solid #333;
}

.editor-mount {
  background: #1e1e1e;
}
</style>
