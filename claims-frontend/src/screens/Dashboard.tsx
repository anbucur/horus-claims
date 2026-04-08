import { useState, useEffect } from 'react'
import { claimsApi, type ClaimSummary } from '../api/claims'

const statusBadge: Record<string, string> = {
  RECEIVED: 'badge-received',
  EXTRACTING: 'badge-extracting',
  VERIFYING: 'badge-verifying',
  ENTITY_MATCHING: 'badge-verifying',
  FORENSICS: 'badge-extracting',
  DUPLICATE_CHECK: 'badge-extracting',
  HITL_REVIEW: 'badge-hitl',
  STP: 'badge-stp',
  COMPLETED: 'badge-stp',
}

function SyncStatusSidebar({ pendingSync }: { pendingSync: number }) {
  return (
    <div className="card border border-slate-200 p-3 space-y-3">
      <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide">Quick Stats</div>
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <span className="text-xs text-slate-500">Pending Sync</span>
          <span className={`text-xs font-medium tabular-nums ${pendingSync > 0 ? 'text-status-warning' : 'text-slate-700'}`}>
            {pendingSync}
          </span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-xs text-slate-500">Claims Today</span>
          <span className="text-xs font-medium text-brand-primary tabular-nums">—</span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-xs text-slate-500">STP Rate</span>
          <span className="text-xs font-medium text-status-verified tabular-nums">—</span>
        </div>
      </div>
      <div className="pt-2 border-t border-slate-100">
        <div className="flex items-center gap-1.5">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-status-verified opacity-75" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-status-verified" />
          </span>
          <span className="text-xs text-slate-500">Core System Live</span>
        </div>
      </div>
    </div>
  )
}

export default function Dashboard() {
  const [claims, setClaims] = useState<ClaimSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    claimsApi.list()
      .then(data => { setClaims(data); setLoading(false) })
      .catch(err => {
        console.error('Failed to load claims:', err)
        setError(err.message || 'Failed to load claims')
        setLoading(false)
      })
  }, [])

  if (loading) {
    return <div className="flex items-center justify-center h-64"><span className="text-sm text-slate-500">Loading claims...</span></div>
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <div className="text-sm text-status-critical mb-2">⚠ {error}</div>
          <div className="text-xs text-slate-500">Connect the Docker stack to see real data.</div>
        </div>
      </div>
    )
  }

  const displayClaims = claims.length > 0 ? claims : null

  return (
    <div className="flex gap-4 h-full">
      <div className="flex-1 space-y-4">
        <div className="grid grid-cols-4 gap-4">
          {[
            { label: 'Intake Volume', value: displayClaims ? String(displayClaims.length) : '—', sub: 'Active claims in pipeline', color: 'text-brand-primary' },
            { label: 'STP Rate', value: '—', sub: 'Configure AI threshold', color: 'text-status-verified' },
            { label: 'Pending HITL', value: displayClaims ? String(displayClaims.filter(c => c.workflowStatus === 'HITL_REVIEW').length) : '—', sub: 'Awaiting human review', color: 'text-status-warning' },
            { label: 'Fraud Flags', value: '—', sub: 'Requires forensics module', color: 'text-status-critical' },
          ].map(({ label, value, sub, color }) => (
            <div key={label} className="card p-4 border border-slate-200 hover:border-slate-300 transition-colors">
              <div className="text-xs text-slate-500 font-medium uppercase tracking-wide mb-2">{label}</div>
              <div className={`text-2xl font-bold tabular-nums ${color}`}>{value}</div>
              <div className="text-xs text-slate-400 mt-1.5">{sub}</div>
            </div>
          ))}
        </div>

        <div className="card border border-slate-200">
          <div className="px-4 py-3 border-b border-slate-200 bg-slate-50/50 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-800">Active Claims Pipeline</h2>
            <button
              onClick={() => claimsApi.list().then(setClaims).catch(console.error)}
              className="text-xs text-brand-secondary hover:text-brand-primary"
            >
              ↻ Refresh
            </button>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50">
                {['Claim ID', 'Insured', 'Vessel', 'LOB', 'Status', 'Date of Loss'].map(h => (
                  <th key={h} className="text-left px-4 py-2.5 text-xs font-semibold text-slate-600 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {(displayClaims || []).map((claim, i) => (
                <tr key={claim.id} className={`border-b border-slate-100 hover:bg-slate-50 ${i % 2 === 1 ? 'bg-slate-50/40' : ''}`}>
                  <td className="px-4 py-2.5 font-medium text-brand-primary tabular-nums text-xs">{claim.claimNumber}</td>
                  <td className="px-4 py-2.5 text-slate-700 text-xs">{claim.insuredName || '—'}</td>
                  <td className="px-4 py-2.5 text-slate-900 font-semibold tabular-nums text-xs">{claim.vesselName || '—'}</td>
                  <td className="px-4 py-2.5 text-slate-600 text-xs">{claim.lineOfBusiness || '—'}</td>
                  <td className="px-4 py-2.5">
                    <span className={`badge ${statusBadge[claim.workflowStatus] || 'badge-received'} text-xs`}>
                      {claim.workflowStatus}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 text-slate-500 tabular-nums text-xs">{claim.dateOfLoss || '—'}</td>
                </tr>
              ))}
              {!displayClaims && (
                <tr>
                  <td colSpan={6} className="text-center text-xs text-slate-400 py-8">
                    API unavailable — connect Docker stack to see real data
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
      <div className="w-56 shrink-0">
        <SyncStatusSidebar pendingSync={0} />
      </div>
    </div>
  )
}
