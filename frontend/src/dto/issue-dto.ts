export interface IssueDTO {
    id: number
    title: string
    description: string | null
    type: string | null
    cveId: string | null
    cweId: string | null
    severity: string
    status: string
    updatedAt: string | null
    reportedAt: string | null
    dueAt: string | null
    patchedAt: string | null
    verifiedAt: string | null
    repository: string | null
    commitHash: string | null
    projectId: number
    projectName?: string
}
