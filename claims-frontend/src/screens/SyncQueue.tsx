import { useState, useEffect } from 'react'
import { syncApi, type SyncEvent, type SyncStats } from '../api/systems'

function timeAgo(isoString?: string): string {
  if (!isoString) return '—'
  const diff = Date.now() - new Date(isoString).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}min ago`
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
    return <div className="flex items-center justify-center h-64 text-sm text-slate-500">Loading sync data...</div>
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-800">Sync Queue & History</h1>
        <div className="flex items-center gap-3">
          {stats && (
            <div className="flex items-center gap-2">
              <span className={`h-2 w-2 rounded-full ${stats.targetSystemActive ? 'bg-status-verified' : 'bg-slate-300'}`} />
              <span className="text-xs text-slate-500">Target: {stats.targetSystem}</span>
            </div>
          )}
          <button onClick={loadData} className="text-xs text-brand-secondary hover:text-brand-primary">↻ Refresh</button>
        </div>
      </div>

      {/* KPI cards */}
      <div className="grid grid-cols-4 gap-4">
        <div className="card p-4 border border-slate-200">
          <div className="text-xs text-slate-500 font-medium uppercase tracking-wide mb-2">Pending</div>
          <div className={`text-2xl font-bold tabular-nums ${(stats?.pending ?? 0) > 0 ? 'text-status-warning' : 'text-slate-700'}`}>
            {stats?.pending ?? '—'}
          </div>
        </div>
        <div className="card p-4 border border-slate-200">
          <div className="text-xs text-slate-500 font-medium uppercase tracking-wide mb-2">Success Today</div>
          <div className="text-2xl font-bold text-status-verified tabular-nums">{stats?.successToday ?? '—'}</div>
        </div>
        <div className="card p-4 border border-slate-200">
          <div className="text-xs text-slate-500 font-medium uppercase tracking-wide mb-2">Failed</div>
          <div className={`text-2xl font-bold tabular-nums ${(stats?.failed ?? 0) > 0 ? 'text-status-critical' : 'text-slate-700'}`}>
            {stats?.failed ?? '—'}
          </div>
        </div>
        <div className="card p-4 border border-slate-200">
          <div className="text-xs text-slate-500 font-medium uppercase tracking-wide mb-2">Target System</div>
          <div className="flex items-center gap-2 mt-1">
            <span className={`h-2 w-2 rounded-full ${stats?.targetSystemActive ? 'bg-status-verified' : 'bg-slate-300'}`} />
            <span className="text-sm font-medium text-slate-700">{stats?.targetSystem || '—'}</span>
          </div>
        </div>
      </div>

      {error && (
        <div className="text-xs text-status-critical bg-red-50 border border-red-200 px-3 py-2 rounded-sm">
          ⚠ {error} — showing cached/empty data
        </div>
      )}

      {/* Tabs */}
      <div className="flex gap-1 border-b border-slate-200">
        {(['pending', 'history'] as const).map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
              activeTab === tab
                ? 'border-brand-primary text-brand-primary'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            {tab === 'pending' ? `Pending Queue (${pending.length})` : 'Sync History'}
          </button>
        ))}
      </div>

      {/* Pending Queue */}
      {activeTab === 'pending' && (
        <div className="card border border-slate-200">
          <div className="px-4 py-3 border-b border-slate-200 bg-slate-50/50 flex items-center justify-between">
            <span className="text-xs text-slate-500">Outbound sync events awaiting processing</span>
            <button
              onClick={handleSyncAll}
              className="btn-primary text-xs px-3 py-1.5"
              disabled={pending.length === 0}
            >
              Sync All Pending Now
            </button>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50">
                {['Claim ID', 'Trigger', 'Attempts', 'Oldest', 'Created', 'Actions'].map(h => (
                  <th key={h} className="text-left px-4 py-2.5 text-xs font-semibold text-slate-600 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {pending.map((event, i) => (
                <tr key={event.id} className={`border-b border-slate-100 hover:bg-slate-50 ${i % 2 === 1 ? 'bg-slate-50/40' : ''}`}>
                  <td className="px-4 py-2.5 font-medium text-brand-primary tabular-nums text-xs">{event.claimId}</td>
                  <td className="px-4 py-2.5">
                    <span className="badge badge-received text-xs">{event.triggerType}</span>
                  </td>
                  <td className="px-4 py-2.5 text-slate-600 tabular-nums text-xs">{event.attemptCount}</td>
                  <td className="px-4 py-2.5 text-slate-500 tabular-nums text-xs">{timeAgo(event.lastAttemptAt)}</td>
                  <td className="px-4 py-2.5 text-slate-500 tabular-nums text-xs">{formatTime(event.createdAt)}</td>
                  <td className="px-4 py-2.5 flex gap-2">
                    <button onClick={() => handleRetry(event.id)} className="text-xs text-brand-secondary hover:text-brand-primary">Retry</button>
                    <button className="text-xs text-slate-400 hover:text-slate-600">View</button>
                  </td>
                </tr>
              ))}
              {pending.length === 0 && (
                <tr>
                  <td colSpan={6} className="text-center text-xs text-slate-400 py-8">
                    No pending sync events — queue is clear ✅
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Sync History */}
      {activeTab === 'history' && (
        <div className="card border border-slate-200">
          <div className="px-4 py-3 border-b border-slate-200 bg-slate-50/50">
            <span className="text-xs text-slate-500">Last 100 sync events</span>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50">
                {['Claim ID', 'Trigger', 'Status', 'Response', 'Last Attempt', 'Created', 'Actions'].map(h => (
                  <th key={h} className="text-left px-4 py-2.5 text-xs font-semibold text-slate-600 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {history.map((event, i) => (
                <tr key={event.id} className={`border-b border-slate-100 hover:bg-slate-50 ${i % 2 === 1 ? 'bg-slate-50/40' : ''}`}>
                  <td className="px-4 py-2.5 font-medium text-brand-primary tabular-nums text-xs">{event.claimId}</td>
                  <td className="px-4 py-2.5"><span className="badge badge-received text-xs">{event.triggerType}</span></td>
                  <td className="px-4 py-2.5">
                    <span className={`badge text-xs ${
                      event.status === 'SUCCESS' ? 'badge-stp' :
                      event.status === 'FAILED' ? 'badge-hitl' :
                      event.status === 'RETRYING' ? 'badge-extracting' : 'badge-received'
                    }`}>
                      {event.status}
                    </span>
                  </td>
                  <td className={`px-4 py-2.5 tabular-nums text-xs ${event.responseStatus === 200 ? 'text-status-verified' : 'text-status-critical'}`}>
                    {event.responseStatus || '—'}
                  </td>
                  <td className="px-4 py-2.5 text-slate-500 tabular-nums text-xs">{timeAgo(event.lastAttemptAt)}</td>
                  <td className="px-4 py-2.5 text-slate-500 tabular-nums text-xs">{formatTime(event.createdAt)}</td>
                  <td className="px-4 py-2.5">
                    <button className="text-xs text-slate-400 hover:text-slate-600">View</button>
                  </td>
                </tr>
              ))}
              {history.length === 0 && (
                <tr>
                  <td colSpan={7} className="text-center text-xs text-slate-400 py-8">
                    No sync history yet — process a claim to see events here.
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
