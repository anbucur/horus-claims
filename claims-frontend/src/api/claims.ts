import api from './client'

// ─── Types ────────────────────────────────────────────────────────────────────

export type WorkflowStatus = 'RECEIVED' | 'EXTRACTING' | 'VERIFYING' | 'ENTITY_MATCHING' | 'FORENSICS' | 'DUPLICATE_CHECK' | 'HITL_REVIEW' | 'STP' | 'COMPLETED'

export interface ClaimSummary {
  id: number
  claimNumber: string
  insuredName: string
  vesselName: string
  lineOfBusiness: string
  workflowStatus: WorkflowStatus
  estimatedValue: number
  currency: string
  dateOfLoss: string
  createdAt: string
  updatedAt: string
}

export interface ClaimDetail {
  id: number
  claimNumber: string
  policyId: number
  policyNumber: string
  dateOfLoss: string
  incidentNarrative: string
  lossLocation: string
  estimatedValue: number
  currency: string
  workflowStatus: WorkflowStatus
  aiConfidenceScore: number
  createdAt: string
  updatedAt: string
  parties: Party[]
  subjectMatters: SubjectMatter[]
  financials: Financial[]
  evidences: Evidence[]
}

export interface Party {
  id: number
  partyType: 'ASSURED' | 'BROKER' | 'CARRIER'
  name: string
  email: string
  phone: string
  country: string
}

export interface SubjectMatter {
  id: number
  type: 'VESSEL' | 'CARGO'
  name: string
  imoNumber?: string
  vesselType?: string
  cargoDescription?: string
  tonnage?: string
}

export interface Financial {
  id: number
  category: 'INDEMNITY' | 'ALAE'
  amount: number
  currency: string
  status: 'RESERVE' | 'PAYMENT'
  transactionDate: string
}

export interface Evidence {
  id: number
  documentType: 'SURVEY_REPORT' | 'IMAGE' | 'EMAIL' | 'INVOICE'
  fileName: string
  fileUrl: string
  aiClassificationTags: string[]
  forensicsScore?: number
  isQuarantined: boolean
}

export interface PolicyVerification {
  policyId: number
  policyNumber: string
  status: string
  lineOfBusiness: string
  coverageDetails: string
  sumInsured: number
  deductible: number
  expiryDate: string
  inPeriod: boolean
}

export interface ProcessingStatus {
  claimId: number
  mode: 'AI_ASSISTED' | 'SEMI_AUTOMATIC' | 'FULL_MANUAL'
  aiAvailable: boolean
  currentStep: string
  completedSteps: string[]
  pendingSteps: string[]
  blocked: boolean
  blockReason?: string
}

// ─── Claims API ────────────────────────────────────────────────────────────────

export const claimsApi = {
  /** List all claims with optional status filter */
  list: async (status?: WorkflowStatus): Promise<ClaimSummary[]> => {
    const params = status ? { status } : {}
    const res = await api.get('/claims', { params })
    return res.data
  },

  /** Get single claim detail */
  get: async (id: number): Promise<ClaimDetail> => {
    const res = await api.get(`/claims/${id}`)
    return res.data
  },

  /** Get processing status for a claim */
  getProcessingStatus: async (id: number): Promise<ProcessingStatus> => {
    const res = await api.get(`/claims/${id}/processing-status`)
    return res.data
  },

  /** Verify policy for a claim */
  verifyPolicy: async (id: number): Promise<PolicyVerification> => {
    const res = await api.post(`/claims/${id}/verify-policy`)
    return res.data
  },

  /** Run full processing pipeline */
  process: async (id: number): Promise<{ message: string }> => {
    const res = await api.post(`/claims/${id}/process`)
    return res.data
  },

  /** Set processing mode for a claim */
  setMode: async (id: number, mode: string): Promise<{ message: string }> => {
    const res = await api.post(`/claims/${id}/set-mode`, null, { params: { mode } })
    return res.data
  },

  /** Create a new claim */
  create: async (data: Partial<ClaimDetail>): Promise<ClaimDetail> => {
    const res = await api.post('/claims', data)
    return res.data
  },

  /** Manual override for any field */
  override: async (id: number, field: string, value: unknown): Promise<void> => {
    await api.post(`/claims/${id}/override`, { field, value })
  },
}
