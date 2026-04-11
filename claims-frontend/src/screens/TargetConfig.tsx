import { useState, useEffect } from 'react'
import { systemsApi, type TargetSystem, type FieldMapping, type SyncTrigger } from '../api/systems'

type Tab = 'connection' | 'mappings' | 'triggers'
type AuthType = 'API_KEY' | 'OAUTH2' | 'BASIC' | 'NONE'

interface SystemForm {
  name: string
  baseUrl: string
  authType: AuthType
  apiKey: string
  sandboxMode: boolean
}

export default function TargetConfig() {
  const [activeTab, setActiveTab] = useState<Tab>('connection')
  const [systems, setSystems] = useState<TargetSystem[]>([])
  const [activeSystem, setActiveSystem] = useState<TargetSystem | null>(null)
  const [mappings, setMappings] = useState<FieldMapping[]>([])
  const [triggers, setTriggers] = useState<SyncTrigger[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [form, setForm] = useState<SystemForm>({
    name: '', baseUrl: '', authType: 'API_KEY', apiKey: '', sandboxMode: true,
  })

  async function loadSystems() {
    setLoading(true)
    try {
      const data = await systemsApi.list()
      setSystems(data)
      const active = data.find((s: TargetSystem) => s.active) || data[0] || null
      setActiveSystem(active)
      if (active) {
        setForm({ name: active.name, baseUrl: active.baseUrl, authType: (active.authType as AuthType) || 'API_KEY', apiKey: '', sandboxMode: active.sandboxMode ?? true })
        const [maps, trigs] = await Promise.all([systemsApi.getMappings(active.id), systemsApi.getTriggers(active.id)])
        setMappings(maps)
        setTriggers(trigs)
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadSystems() }, [])

  async function handleSave() {
    if (!activeSystem) return
    setSaving(true); setMessage(null)
    try {
      const payload: Record<string, unknown> = { name: form.name, baseUrl: form.baseUrl, authType: form.authType, sandboxMode: form.sandboxMode }
      if (form.apiKey) payload.apiKey = form.apiKey
      await systemsApi.update(activeSystem.id, payload as Partial<TargetSystem>)
      setMessage('Configuration saved successfully')
      setForm(f => ({ ...f, apiKey: '' }))
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  async function handleActivate(id: number) {
    try { await systemsApi.activate(id); await loadSystems(); setMessage('Target system activated') }
    catch (e: unknown) { setError(e instanceof Error ? e.message : 'Activation failed') }
  }

  async function handleToggleTrigger(trigger: SyncTrigger) {
    if (!activeSystem) return
    try { await systemsApi.updateTrigger(activeSystem.id, trigger.id, { ...trigger, enabled: !trigger.enabled }); await loadSystems() }
    catch { setError('Failed to update trigger') }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center gap-3 text-md-on-surface-var">
          <div className="w-4 h-4 rounded-full border-2 border-md-primary border-t-transparent animate-spin" />
          <span className="text-sm">Loading configuration…</span>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-xl font-bold text-md-on-surface">Target System Configuration</h1>
          <div className="text-xs text-md-on-surface-var mt-0.5">Sync engine and Guidewire integration settings</div>
        </div>
        {activeSystem && (
          <div className="flex items-center gap-2">
            <span className={`h-2 w-2 rounded-full ${activeSystem.active ? 'bg-status-verified' : 'bg-md-on-surface-var/30'}`} />
            <span className="text-xs text-md-on-surface-var">{activeSystem.name}</span>
          </div>
        )}
      </div>

      {/* Alerts */}
      {message && (
        <div
          className="flex items-center gap-2 px-4 py-2.5 rounded-lg text-xs text-status-verified"
          style={{ background: 'rgba(74,222,128,0.08)', border: '1px solid rgba(74,222,128,0.2)' }}
          role="alert"
        >
          ✅ {message}
        </div>
      )}
      {error && (
        <div
          className="flex items-center gap-2 px-4 py-2.5 rounded-lg text-xs text-status-critical"
          style={{ background: 'rgba(147,0,10,0.15)', border: '1px solid rgba(255,107,107,0.2)' }}
          role="alert"
        >
          ⚠ {error}
        </div>
      )}

      <div className="card overflow-hidden">
        {/* Tab bar + save */}
        <div
          className="flex items-center"
          style={{ background: '#191b22', borderBottom: '1px solid rgba(70,69,85,0.3)' }}
        >
          {(['connection', 'mappings', 'triggers'] as Tab[]).map(key => (
            <button
              key={key}
              onClick={() => setActiveTab(key)}
              className={`px-5 py-3 text-sm font-medium capitalize transition-colors border-b-2 ${
                activeTab === key
                  ? 'text-md-primary border-md-primary'
                  : 'text-md-on-surface-var hover:text-md-on-surface border-transparent'
              }`}
            >
              {key === 'connection' ? 'Connection' : key === 'mappings' ? 'Field Mappings' : 'Triggers'}
            </button>
          ))}
          <div className="flex-1" />
          <button
            onClick={handleSave}
            disabled={saving || !activeSystem}
            className="btn-primary text-xs m-2 px-4 py-1.5 disabled:opacity-50"
          >
            {saving ? 'Saving…' : 'Save Configuration'}
          </button>
        </div>

        <div className="p-6">
          {/* ─── Connection ─── */}
          {activeTab === 'connection' && activeSystem && (
            <div className="grid grid-cols-2 gap-6">
              <div className="space-y-4">
                {[
                  { label: 'Target System Name', key: 'name', type: 'text', placeholder: '' },
                  { label: 'Base URL', key: 'baseUrl', type: 'text', placeholder: 'https://api.example.com/v1' },
                ].map(({ label, key, type, placeholder }) => (
                  <div key={key}>
                    <label className="input-label">{label}</label>
                    <input
                      type={type}
                      value={form[key as keyof SystemForm] as string}
                      placeholder={placeholder}
                      onChange={e => setForm(f => ({ ...f, [key]: e.target.value }))}
                      className="input-field"
                    />
                  </div>
                ))}

                <div>
                  <label className="input-label">Auth Type</label>
                  <select
                    value={form.authType}
                    onChange={e => setForm(f => ({ ...f, authType: e.target.value as AuthType }))}
                    className="input-field"
                    style={{ colorScheme: 'dark' }}
                  >
                    <option value="API_KEY">API Key</option>
                    <option value="OAUTH2">OAuth 2.0</option>
                    <option value="BASIC">Basic Auth</option>
                    <option value="NONE">None</option>
                  </select>
                </div>

                <div>
                  <label className="input-label">
                    API Key {activeSystem.maskedApiKey && <span className="text-md-on-surface-var/50 normal-case">({activeSystem.maskedApiKey})</span>}
                  </label>
                  <input
                    type="password"
                    value={form.apiKey}
                    placeholder="Leave blank to keep existing"
                    onChange={e => setForm(f => ({ ...f, apiKey: e.target.value }))}
                    className="input-field"
                  />
                </div>

                {/* Sandbox toggle */}
                <div className="flex items-center justify-between p-3 rounded-lg" style={{ background: '#191b22' }}>
                  <div>
                    <div className="text-sm font-medium text-md-on-surface">Sandbox Mode</div>
                    <div className="text-xs text-md-on-surface-var mt-0.5">
                      {form.sandboxMode ? 'Calls logged only — no real HTTP requests' : 'LIVE — real HTTP calls to target system'}
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setForm(f => ({ ...f, sandboxMode: !f.sandboxMode }))}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${form.sandboxMode ? 'bg-status-verified' : 'bg-md-container-highest'}`}
                  >
                    <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform shadow-md ${form.sandboxMode ? 'translate-x-6' : 'translate-x-1'}`} />
                  </button>
                </div>

                <button
                  onClick={() => handleActivate(activeSystem.id)}
                  className="btn-secondary text-xs px-4 py-2"
                  disabled={activeSystem.active && !form.sandboxMode}
                >
                  {activeSystem.active ? 'Currently Active' : 'Activate This System'}
                </button>
              </div>

              {/* Other systems */}
              <div>
                <div className="input-label mb-2">Other Target Systems</div>
                <div className="rounded-lg overflow-hidden" style={{ border: '1px solid rgba(70,69,85,0.3)' }}>
                  {systems.filter(s => s.id !== activeSystem.id).map((s, i, arr) => (
                    <div
                      key={s.id}
                      className="flex items-center justify-between px-4 py-3 hover:bg-md-container-high transition-colors"
                      style={{ borderBottom: i < arr.length - 1 ? '1px solid rgba(70,69,85,0.3)' : 'none' }}
                    >
                      <div>
                        <div className="text-sm text-md-on-surface">{s.name}</div>
                        <div className="text-xs text-md-on-surface-var font-mono">{s.baseUrl}</div>
                      </div>
                      <button onClick={() => handleActivate(s.id)} className="text-xs text-md-primary hover:text-md-primary/70 transition-colors">
                        Activate
                      </button>
                    </div>
                  ))}
                  {systems.length <= 1 && (
                    <div className="px-4 py-3 text-xs text-md-on-surface-var/50">No other systems configured</div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* ─── Field Mappings ─── */}
          {activeTab === 'mappings' && (
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="text-xs text-md-on-surface-var">{mappings.length} mappings configured</span>
                <button className="btn-secondary text-xs px-3 py-1.5">+ Add Mapping</button>
              </div>
              <table className="data-table">
                <thead>
                  <tr>
                    {['#', 'Workbench Field', 'Target Field', 'Transform', 'Static Value', 'Actions'].map(h => <th key={h}>{h}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {mappings.map((m, i) => (
                    <tr key={m.id}>
                      <td className="text-md-on-surface-var/40 tabular-nums text-xs">{i + 1}</td>
                      <td className="text-xs font-mono text-md-on-surface-var">{m.workbenchField}</td>
                      <td className="text-xs font-mono text-md-primary">{m.targetField}</td>
                      <td className="text-xs text-md-on-surface-var">{m.transform || '—'}</td>
                      <td className="text-xs text-md-on-surface-var">{m.staticValue || '—'}</td>
                      <td>
                        <div className="flex gap-3">
                          <button className="text-xs text-md-on-surface-var/50 hover:text-md-on-surface-var transition-colors">Edit</button>
                          <button className="text-xs text-status-critical hover:text-status-critical/70 transition-colors">Delete</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {mappings.length === 0 && (
                    <tr><td colSpan={6} className="text-center py-8 text-xs text-md-on-surface-var/40">No field mappings configured</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}

          {/* ─── Triggers ─── */}
          {activeTab === 'triggers' && (
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="text-xs text-md-on-surface-var">{triggers.length} triggers configured</span>
                <button className="btn-secondary text-xs px-3 py-1.5">+ Add Trigger</button>
              </div>
              <table className="data-table">
                <thead>
                  <tr>
                    {['Name', 'Type', 'Condition', 'Endpoint', 'Enabled', 'Actions'].map(h => <th key={h}>{h}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {triggers.map((t) => {
                    let condition = ''
                    try { const c = JSON.parse(t.condition); condition = c.equals ? `${c.field} = ${c.equals}` : c.set ? `${c.field} is set` : t.condition } catch { condition = t.condition }
                    return (
                      <tr key={t.id}>
                        <td className="text-xs font-medium text-md-on-surface">{t.name}</td>
                        <td>
                          <span className={`badge ${
                            t.triggerType === 'ON_STATUS_CHANGE' ? 'badge-verifying' :
                            t.triggerType === 'ON_FIELD_CHANGE'  ? 'badge-extracting' : 'badge-received'
                          }`}>{t.triggerType.replace('ON_', '')}</span>
                        </td>
                        <td className="text-xs text-md-on-surface-var">{condition}</td>
                        <td className="text-xs font-mono text-md-on-surface-var">{t.targetEndpoint}</td>
                        <td>
                          <button
                            onClick={() => handleToggleTrigger(t)}
                            className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${t.enabled ? 'bg-status-verified' : 'bg-md-container-highest'}`}
                          >
                            <span className={`inline-block h-3 w-3 transform rounded-full bg-white transition-transform shadow ${t.enabled ? 'translate-x-4' : 'translate-x-1'}`} />
                          </button>
                        </td>
                        <td>
                          <div className="flex gap-3">
                            <button className="text-xs text-md-on-surface-var/50 hover:text-md-on-surface-var transition-colors">Edit</button>
                            <button className="text-xs text-status-critical hover:text-status-critical/70 transition-colors">Delete</button>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                  {triggers.length === 0 && (
                    <tr><td colSpan={6} className="text-center py-8 text-xs text-md-on-surface-var/40">No triggers configured</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
