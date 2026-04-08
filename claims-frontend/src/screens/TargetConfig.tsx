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
        setForm({
          name: active.name,
          baseUrl: active.baseUrl,
          authType: (active.authType as AuthType) || 'API_KEY',
          apiKey: '',
          sandboxMode: active.sandboxMode ?? true,
        })
        const [maps, trigs] = await Promise.all([
          systemsApi.getMappings(active.id),
          systemsApi.getTriggers(active.id),
        ])
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
    setSaving(true)
    setMessage(null)
    try {
      const payload: Record<string, unknown> = {
        name: form.name,
        baseUrl: form.baseUrl,
        authType: form.authType,
        sandboxMode: form.sandboxMode,
      }
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
    try {
      await systemsApi.activate(id)
      await loadSystems()
      setMessage('Target system activated')
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Activation failed')
    }
  }

  async function handleToggleTrigger(trigger: SyncTrigger) {
    if (!activeSystem) return
    try {
      await systemsApi.updateTrigger(activeSystem.id, trigger.id, { ...trigger, enabled: !trigger.enabled })
      await loadSystems()
    } catch {
      setError('Failed to update trigger')
    }
  }

  if (loading) {
    return <div className="flex items-center justify-center h-64 text-sm text-slate-500">Loading configuration...</div>
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-800">Target System Configuration</h1>
        {activeSystem && (
          <div className="flex items-center gap-2">
            <span className={`h-2 w-2 rounded-full ${activeSystem.active ? 'bg-status-verified' : 'bg-slate-300'}`} />
            <span className="text-xs text-slate-500">{activeSystem.name}</span>
          </div>
        )}
      </div>

      {message && (
        <div className="text-xs text-status-verified bg-green-50 border border-green-200 px-3 py-2 rounded-sm" role="alert">
          ✅ {message}
        </div>
      )}
      {error && (
        <div className="text-xs text-status-critical bg-red-50 border border-red-200 px-3 py-2 rounded-sm" role="alert">
          ⚠ {error}
        </div>
      )}

      <div className="card border border-slate-200">
        {/* Tab bar */}
        <div className="border-b border-slate-200 bg-slate-50/50">
          <div className="flex">
            {(['connection', 'mappings', 'triggers'] as Tab[]).map(key => (
              <button
                key={key}
                onClick={() => setActiveTab(key)}
                className={`px-4 py-3 text-sm font-medium border-r border-slate-200 capitalize transition-colors ${
                  activeTab === key
                    ? 'text-brand-primary border-b-2 border-b-brand-primary bg-white'
                    : 'text-slate-500 hover:text-slate-700 hover:bg-slate-50'
                }`}
              >
                {key === 'connection' ? 'Connection' : key === 'mappings' ? 'Field Mappings' : 'Triggers'}
              </button>
            ))}
            <div className="flex-1" />
            <button onClick={handleSave} disabled={saving || !activeSystem} className="btn-primary text-xs m-2 px-4 py-1.5 disabled:opacity-50">
              {saving ? 'Saving...' : 'Save Configuration'}
            </button>
          </div>
        </div>

        <div className="p-6">
          {/* ─── Connection ─── */}
          {activeTab === 'connection' && activeSystem && (
            <div className="grid grid-cols-2 gap-6">
              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1.5">Target System Name</label>
                  <input type="text" value={form.name}
                    onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-sm text-sm focus:outline-none focus:ring-1 focus:ring-brand-primary" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1.5">Base URL</label>
                  <input type="text" value={form.baseUrl} placeholder="https://api.example.com/v1"
                    onChange={e => setForm(f => ({ ...f, baseUrl: e.target.value }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-sm text-sm focus:outline-none focus:ring-1 focus:ring-brand-primary" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1.5">Auth Type</label>
                  <select value={form.authType}
                    onChange={e => setForm(f => ({ ...f, authType: e.target.value as AuthType }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-sm text-sm focus:outline-none focus:ring-1 focus:ring-brand-primary bg-white">
                    <option value="API_KEY">API Key</option>
                    <option value="OAUTH2">OAuth 2.0</option>
                    <option value="BASIC">Basic Auth</option>
                    <option value="NONE">None</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1.5">
                    API Key {activeSystem.maskedApiKey && <span className="text-slate-400">({activeSystem.maskedApiKey})</span>}
                  </label>
                  <input type="password" value={form.apiKey} placeholder="Leave blank to keep existing"
                    onChange={e => setForm(f => ({ ...f, apiKey: e.target.value }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-sm text-sm focus:outline-none focus:ring-1 focus:ring-brand-primary" />
                </div>
                <div className="flex items-center gap-3 pt-2">
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input type="checkbox" checked={form.sandboxMode}
                      onChange={e => setForm(f => ({ ...f, sandboxMode: e.target.checked }))}
                      className="sr-only peer" />
                    <div className="w-9 h-5 bg-slate-200 peer-focus:ring-2 peer-focus:ring-brand-primary rounded-full peer peer-checked:after:translate-x-4 after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-status-verified" />
                    <span className="ml-3 text-sm text-slate-600">Sandbox Mode</span>
                  </label>
                </div>
                <div className="text-xs text-slate-400">
                  {form.sandboxMode ? 'Sandbox ON — sync calls are logged but no HTTP requests are made' : 'Sandbox OFF — real HTTP calls will be made to the target system'}
                </div>
                <button onClick={() => handleActivate(activeSystem.id)}
                  className="btn-secondary text-xs px-4 py-2"
                  disabled={activeSystem.active && !form.sandboxMode}>
                  {activeSystem.active ? 'Currently Active' : 'Activate This System'}
                </button>
              </div>
              <div className="space-y-4">
                <div className="bg-slate-50 border border-slate-200 rounded-sm p-4">
                  <div className="text-xs font-semibold text-slate-700 mb-3">Other Target Systems</div>
                  {systems.filter(s => s.id !== activeSystem.id).map(s => (
                    <div key={s.id} className="flex items-center justify-between py-1.5 border-b border-slate-100 last:border-0">
                      <div>
                        <div className="text-sm text-slate-700">{s.name}</div>
                        <div className="text-xs text-slate-400">{s.baseUrl}</div>
                      </div>
                      <button onClick={() => handleActivate(s.id)} className="text-xs text-brand-secondary hover:text-brand-primary">Activate</button>
                    </div>
                  ))}
                  {systems.length <= 1 && <div className="text-xs text-slate-400">No other systems configured</div>}
                </div>
              </div>
            </div>
          )}

          {/* ─── Field Mappings ─── */}
          {activeTab === 'mappings' && (
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="text-xs text-slate-500">{mappings.length} mappings configured</span>
                <button className="btn-secondary text-xs px-3 py-1.5">+ Add Mapping</button>
              </div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50">
                    {['#', 'Workbench Field', 'Target Field', 'Transform', 'Static Value', 'Actions'].map(h => (
                      <th key={h} className="text-left px-3 py-2 text-xs font-semibold text-slate-600 uppercase tracking-wide">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {mappings.map((m, i) => (
                    <tr key={m.id} className={`border-b border-slate-100 ${i % 2 === 1 ? 'bg-slate-50/40' : ''}`}>
                      <td className="px-3 py-2 text-slate-400 tabular-nums text-xs">{i + 1}</td>
                      <td className="px-3 py-2 text-xs font-mono text-slate-700">{m.workbenchField}</td>
                      <td className="px-3 py-2 text-xs font-mono text-brand-primary">{m.targetField}</td>
                      <td className="px-3 py-2 text-xs text-slate-500">{m.transform || '—'}</td>
                      <td className="px-3 py-2 text-xs text-slate-500">{m.staticValue || '—'}</td>
                      <td className="px-3 py-2">
                        <button className="text-xs text-slate-400 hover:text-slate-600 mr-2">Edit</button>
                        <button className="text-xs text-status-critical hover:text-red-700">Delete</button>
                      </td>
                    </tr>
                  ))}
                  {mappings.length === 0 && (
                    <tr><td colSpan={6} className="text-center text-xs text-slate-400 py-8">No field mappings configured</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}

          {/* ─── Triggers ─── */}
          {activeTab === 'triggers' && (
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="text-xs text-slate-500">{triggers.length} triggers configured</span>
                <button className="btn-secondary text-xs px-3 py-1.5">+ Add Trigger</button>
              </div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50">
                    {['Name', 'Type', 'Condition', 'Endpoint', 'Enabled', 'Actions'].map(h => (
                      <th key={h} className="text-left px-3 py-2 text-xs font-semibold text-slate-600 uppercase tracking-wide">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {triggers.map((t, i) => {
                    let condition = ''
                    try { const c = JSON.parse(t.condition); condition = c.equals ? `${c.field} = ${c.equals}` : c.set ? `${c.field} is set` : t.condition } catch { condition = t.condition }
                    return (
                      <tr key={t.id} className={`border-b border-slate-100 ${i % 2 === 1 ? 'bg-slate-50/40' : ''}`}>
                        <td className="px-3 py-2 text-xs font-medium text-slate-700">{t.name}</td>
                        <td className="px-3 py-2">
                          <span className={`badge text-xs ${
                            t.triggerType === 'ON_STATUS_CHANGE' ? 'badge-verifying' :
                            t.triggerType === 'ON_FIELD_CHANGE' ? 'badge-extracting' : 'badge-received'
                          }`}>{t.triggerType.replace('ON_', '')}</span>
                        </td>
                        <td className="px-3 py-2 text-xs text-slate-500">{condition}</td>
                        <td className="px-3 py-2 text-xs font-mono text-slate-600">{t.targetEndpoint}</td>
                        <td className="px-3 py-2">
                          <button onClick={() => handleToggleTrigger(t)}
                            className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${t.enabled ? 'bg-status-verified' : 'bg-slate-300'}`}>
                            <span className={`inline-block h-3 w-3 transform rounded-full bg-white transition-transform ${t.enabled ? 'translate-x-4' : 'translate-x-1'}`} />
                          </button>
                        </td>
                        <td className="px-3 py-2">
                          <button className="text-xs text-slate-400 hover:text-slate-600 mr-2">Edit</button>
                          <button className="text-xs text-status-critical hover:text-red-700">Delete</button>
                        </td>
                      </tr>
                    )
                  })}
                  {triggers.length === 0 && (
                    <tr><td colSpan={6} className="text-center text-xs text-slate-400 py-8">No triggers configured</td></tr>
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
