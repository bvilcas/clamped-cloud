export interface EventGroupDTO {
    id: number
    fingerprint: string
    message: string
    source: string
    environment: string
    severity: string
    count: number
    firstSeen: string
    lastSeen: string
    muted: boolean
    tags: string | null
    exceptionClass: string | null
    stacktrace: string | null
    sourceFile: string | null
    sourceLine: number | null
    sourceMethod: string | null
    linkedIssueId: number | null
    projectId: number
}
