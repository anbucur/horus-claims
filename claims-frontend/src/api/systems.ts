import api from './client'

// ─── Types ────────────────────────────────────────────────────────────────────

export interface TargetSystem {
  id: number
  name: string
  baseUrl: string
  authType: 'API_KEY' | 'OAUTH2' | 'BASIC' | 'NONE'
  maskedApiKey?: string
  endpoints: string
  retryPolicy: string
  sandboxMode: boolean
  active: boolean
}

export interface FieldMapping {
  id: number
  workbenchField: string
  targetField: string
  transform?: string
  staticValue?: string
  sortOrder: number
  active: boolean
}

export interface SyncTrigger {
  id: number
  name: string
  triggerType: 'ON_STATUS_CHANGE' | 'ON_FIELD_CHANGE' | 'ON_MANUAL' | 'ON_SCHEDULE'
  condition: string
  filterCondition?: string
  targetEndpoint: string
  httpMethod: string
  enabled: boolean
  active: boolean
}

export interface SyncEvent {
  id: number
  claimId: number
  triggerType: string
  targetEndpoint: string
  httpMethod: string
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'RETRYING'
  responseStatus?: number
  attemptCount: number
  lastAttemptAt?: string
  createdAt: string
}

export interface SyncStats {
  pending: number
  successToday: number
  failed: number
  targetSystem: string
  targetSystemActive: boolean
}

// ─── Target Systems API ──────────────────────────────────────────────────────

export const systemsApi = {
  list: async (): Promise<TargetSystem[]> => {
    const res = await api.get('/systems')
    return res.data
  },

  get: async (id: number): Promise<TargetSystem> => {
    const res = await api.get(`/systems/${id}`)
    return res.data
  },

  create: async (data: Partial<TargetSystem>): Promise<TargetSystem> => {
    const res = await api.post('/systems', data)
    return res.data
  },

  update: async (id: number, data: Partial<TargetSystem>): Promise<TargetSystem> => {
    const res = await api.put(`/systems/${id}`, data)
    return res.data
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/systems/${id}`)
  },

  activate: async (id: number): Promise<TargetSystem> => {
    const res = await api.post(`/systems/${id}/activate`)
    return res.data
  },

  getMappings: async (systemId: number): Promise<FieldMapping[]> => {
    const res = await api.get(`/systems/${systemId}/mappings`)
    return res.data
  },

  createMapping: async (systemId: number, data: Partial<FieldMapping>): Promise<FieldMapping> => {
    const res = await api.post(`/systems/${systemId}/mappings`, data)
    return res.data
  },

  updateMapping: async (systemId: number, mappingId: number, data: Partial<FieldMapping>): Promise<FieldMapping> => {
    const res = await api.put(`/systems/${systemId}/mappings/${mappingId}`, data)
    return res.data
  },

  deleteMapping: async (systemId: number, mappingId: number): Promise<void> => {
    await api.delete(`/systems/${systemId}/mappings/${mappingId}`)
  },

  getTriggers: async (systemId: number): Promise<SyncTrigger[]> => {
    const res = await api.get(`/systems/${systemId}/triggers`)
    return res.data
  },

  createTrigger: async (systemId: number, data: Partial<SyncTrigger>): Promise<SyncTrigger> => {
    const res = await api.post(`/systems/${systemId}/triggers`, data)
    return res.data
  },

  updateTrigger: async (systemId: number, triggerId: number, data: Partial<SyncTrigger>): Promise<SyncTrigger> => {
    const res = await api.put(`/systems/${systemId}/triggers/${triggerId}`, data)
    return res.data
  },

  deleteTrigger: async (systemId: number, triggerId: number): Promise<void> => {
    await api.delete(`/systems/${systemId}/triggers/${triggerId}`)
  },
}

// ─── Sync API ─────────────────────────────────────────────────────────────────

export const syncApi = {
  getQueue: async (): Promise<SyncEvent[]> => {
    const res = await api.get('/sync/queue')
    return res.data
  },

  getHistory: async (): Promise<SyncEvent[]> => {
    const res = await api.get('/sync/history')
    return res.data
  },

  getStats: async (): Promise<SyncStats> => {
    const res = await api.get('/sync/stats')
    return res.data
  },

  retryEvent: async (eventId: number): Promise<void> => {
    await api.post(`/sync/events/${eventId}/retry`)
  },

  syncClaim: async (claimId: number): Promise<void> => {
    await api.post(`/sync/claims/${claimId}/sync`)
  },

  syncAll: async (): Promise<{ queued: number; message: string }> => {
    const res = await api.post('/sync/sync-all')
    return res.data
  },

  getHealth: async (): Promise<Record<string, unknown>> => {
    const res = await api.get('/sync/health')
    return res.data
  },

  getClaimSyncState: async (claimId: number): Promise<{ dirty: boolean; lastSyncAt?: string }> => {
    const res = await api.get(`/claims/${claimId}/sync-state`)
    return res.data
  },
}
