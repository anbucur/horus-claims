import { useState, useEffect } from 'react'
import { syncApi, type SyncEvent, type SyncStats } from '../api/systems'

function timeAgo(isoString?: string): string {
  if (!isoString) return '—'
  const diff = Date.now() - new Date(isoString).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  return `${Math.floor(mins / 60)}h ago`
}

function formatTime(isoString?: string): string {
  if (!isoString) return '—'
  return new Date(isoString).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

export default function SyncQueue() {
  const [activeTab, setActiveTab] = useState<'pending' | 'history'>('pending')
  const [pending, setPending] = useState<SyncEvent[]>([])
  const [history, setHistory] = useState<SyncEvent[]>([])
  const [stats, setStats] = useState<SyncStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  async function loadData() {
    setLoading(true)
    setError(null)
    try {
      const [queue, hist, st] = await Promise.all([
        syncApi.getQueue(),
        syncApi.getHistory(),
        syncApi.getStats(),
      ])
      setPending(queue)
      setHistory(hist)
      setStats(st)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load sync data')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [])

  async function handleRetry(eventId: number) {
    await syncApi.retryEvent(eventId)
    await loadData()
  }

  async function handleSyncAll() {
    await syncApi.syncAll()
    await loadData()
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center gap-3 text-md-on-surface-var">
          <div className="w-4 h-4 rounded-full border-2 border-md-primary border-t-transparent animate-spin" />
          <span className="text-sm">Loading sync data…</span>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-xl font-bold text-md-on-surface">Sync Queue</h1>
          <div className="text-xs text-md-on-surface-var mt-0.5">Guidewire core system sync status</div>
        </div>
        <div className="flex items-center gap-3">
          {stats && (
            <div className="flex items-center gap-1.5">
              <span className={`h-2 w-2 rounded-full ${stats.targetSystemActive ? 'bg-status-verified' : 'bg-md-on-surface-var/30'}`} />
              <span className="text-xs text-md-on-surface-var">{stats.targetSystem}</span>
            </div>
          )}
          <button
            onClick={loadData}
            className="flex items-center gap-1.5 text-xs text-md-on-surface-var hover:text-md-primary transition-colors"
          >
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </button>
        </div>
      </div>

      {/* KPI cards */}
      <div className="grid grid-cols-4 gap-3">
        <div className="kpi-card kpi-card-warning">
          <div className="text-[10px] font-semibold text-md-on-surface-var uppercase tracking-[0.08em] mb-2">Pending</div>
          <div className={`text-[28px] font-display font-bold tabular-nums ${(stats?.pending ?? 0) > 0 ? 'text-status-warning' : 'text-md-on-surface'}`}>
            {stats?.pending ?? '—'}
          </div>
        </div>
        <div className="kpi-card kpi-card-success">
          <div className="text-[10px] font-semibold text-md-on-surface-var uppercase tracking-[0.08em] mb-2">Success Today</div>
          <div className="text-[28px] font-display font-bold text-status-verified tabular-nums">{stats?.successToday ?? '—'}</div>
        </div>
        <div className="kpi-card kpi-card-error">
          <div className="text-[10px] font-semibold text-md-on-surface-var uppercase tracking-[0.08em] mb-2">Failed</div>
          <div className={`text-[28px] font-display font-bold tabular-nums ${(stats?.failed ?? 0) > 0 ? 'text-status-critical' : 'text-md-on-surface'}`}>
            {stats?.failed ?? '—'}
          </div>
        </div>
        <div className="kpi-card">
          <div className="text-[10px] font-semibold text-md-on-surface-var uppercase tracking-[0.08em] mb-2">Target System</div>
          <div className="flex items-center gap-2 mt-1">
            <span className={`h-2 w-2 rounded-full ${stats?.targetSystemActive ? 'bg-status-verified' : 'bg-md-on-surface-var/30'}`} />
            <span className="text-sm font-semibold text-md-on-surface">{stats?.targetSystem || '—'}</span>
          </div>
        </div>
      </div>

      {/* Error */}
      {error && (
        <div
          className="flex items-center gap-2 px-4 py-2.5 rounded-lg text-xs text-status-critical"
          style={{ background: 'rgba(147,0,10,0.15)', border: '1px solid rgba(255,107,107,0.2)' }}
        >
          ⚠ {error} — showing cached/empty data
        </div>
      )}

      {/* Tabs */}
      <div className="tab-bar">
        {(['pending', 'history'] as const).map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`tab-item ${activeTab === tab ? 'active' : ''}`}
          >
            {tab === 'pending' ? `Pending Queue (${pending.length})` : 'Sync History'}
          </button>
        ))}
      </div>

      {/* Pending Queue */}
      {activeTab === 'pending' && (
        <div className="card overflow-hidden">
          <div
            className="flex items-center justify-between px-4 py-3"
            style={{ background: '#191b22', borderBottom: '1px solid rgba(70,69,85,0.3)' }}
          >
            <span className="text-xs text-md-on-surface-var">Outbound sync events awaiting processing</span>
            <button
              onClick={handleSyncAll}
              className="btn-primary text-xs px-3 py-1.5"
              disabled={pending.length === 0}
            >
              Sync All Now
            </button>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                {['Claim ID', 'Trigger', 'Attempts', 'Oldest', 'Created', 'Actions'].map(h => <th key={h}>{h}</th>)}
              </tr>
            </thead>
            <tbody>
              {pending.map((event) => (
                <tr key={event.id}>
                  <td className="font-mono text-md-primary text-xs tabular-nums font-semibold">{event.claimId}</td>
                  <td><span className="badge badge-received">{event.triggerType}</span></td>
                  <td className="text-md-on-surface-var tabular-nums text-xs">{event.attemptCount}</td>
                  <td className="text-md-on-surface-var tabular-nums text-xs">{timeAgo(event.lastAttemptAt)}</td>
                  <td className="text-md-on-surface-var tabular-nums text-xs font-mono">{formatTime(event.createdAt)}</td>
                  <td>
                    <div className="flex gap-3">
                      <button onClick={() => handleRetry(event.id)} className="text-xs text-md-primary hover:text-md-primary/70 transition-colors">Retry</button>
                      <button className="text-xs text-md-on-surface-var/50 hover:text-md-on-surface-var transition-colors">View</button>
                    </div>
                  </td>
                </tr>
              ))}
              {pending.length === 0 && (
                <tr>
                  <td colSpan={6} className="text-center py-12">
                    <div className="flex flex-col items-center gap-2 text-md-on-surface-var/40">
                      <svg className="w-10 h-10" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      <span className="text-sm">Queue clear — no pending sync events</span>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* History */}
      {activeTab === 'history' && (
        <div className="card overflow-hidden">
          <div
            className="px-4 py-3"
            style={{ background: '#191b22', borderBottom: '1px solid rgba(70,69,85,0.3)' }}
          >
            <span className="text-xs text-md-on-surface-var">Last 100 sync events</span>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                {['Claim ID', 'Trigger', 'Status', 'HTTP', 'Last Attempt', 'Created', ''].map(h => <th key={h}>{h}</th>)}
              </tr>
            </thead>
            <tbody>
              {history.map((event) => (
                <tr key={event.id}>
                  <td className="font-mono text-md-primary text-xs tabular-nums font-semibold">{event.claimId}</td>
                  <td><span className="badge badge-received">{event.triggerType}</span></td>
                  <td>
                    <span className={`badge ${
                      event.status === 'SUCCESS' ? 'badge-stp' :
                      event.status === 'FAILED'  ? 'badge-critical' :
                      event.status === 'RETRYING'? 'badge-extracting' : 'badge-received'
                    }`}>
                      {event.status}
                    </span>
                  </td>
                  <td className={`tabular-nums text-xs font-mono ${event.responseStatus === 200 ? 'text-status-verified' : 'text-status-critical'}`}>
                    {event.responseStatus || '—'}
                  </td>
                  <td className="text-md-on-surface-var tabular-nums text-xs">{timeAgo(event.lastAttemptAt)}</td>
                  <td className="text-md-on-surface-var tabular-nums text-xs font-mono">{formatTime(event.createdAt)}</td>
                  <td><button className="text-xs text-md-on-surface-var/50 hover:text-md-on-surface-var transition-colors">View</button></td>
                </tr>
              ))}
              {history.length === 0 && (
                <tr>
                  <td colSpan={7} className="text-center py-12">
                    <div className="flex flex-col items-center gap-2 text-md-on-surface-var/40">
                      <span className="text-sm">No sync history — process a claim to see events</span>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
