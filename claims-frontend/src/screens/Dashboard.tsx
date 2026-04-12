import { useState, useEffect } from 'react'
import { claimsApi, type ClaimSummary } from '../api/claims'

const statusBadge: Record<string, string> = {
  RECEIVED:        'badge-received',
  EXTRACTING:      'badge-extracting',
  VERIFYING:       'badge-verifying',
  ENTITY_MATCHING: 'badge-verifying',
  FORENSICS:       'badge-extracting',
  DUPLICATE_CHECK: 'badge-extracting',
  HITL_REVIEW:     'badge-hitl',
  STP:             'badge-stp',
  COMPLETED:       'badge-stp',
}

interface KpiCardProps {
  label: string
  value: string
  sub: string
  accent: string
  variant: 'primary' | 'success' | 'warning' | 'error'
}

function KpiCard({ label, value, sub, accent, variant }: KpiCardProps) {
  return (
    <div className={`kpi-card kpi-card-${variant} flex flex-col`}>
      <div className="text-[10px] font-semibold text-md-on-surface-var uppercase tracking-[0.08em] mb-2">{label}</div>
      <div className={`text-[30px] leading-none font-display font-bold tabular-nums ${accent}`}>{value}</div>
      <div className="text-[11px] text-md-on-surface-var/70 mt-2">{sub}</div>
    </div>
  )
}

function SyncStatusSidebar({ pendingSync }: { pendingSync: number }) {
  return (
    <div className="space-y-4">
      <div className="card p-4 space-y-3">
        <div className="section-label">Quick Stats</div>
        <div className="space-y-2.5">
          {[
            { label: 'Pending Sync', value: String(pendingSync), warn: pendingSync > 0 },
            { label: 'Claims Today', value: '—', warn: false },
            { label: 'STP Rate',     value: '—', warn: false },
            { label: 'Avg Handle Time', value: '—', warn: false },
          ].map(({ label, value, warn }) => (
            <div key={label} className="flex items-center justify-between">
              <span className="text-xs text-md-on-surface-var">{label}</span>
              <span className={`text-xs font-semibold tabular-nums ${warn ? 'text-status-warning' : 'text-md-on-surface'}`}>
                {value}
              </span>
            </div>
          ))}
        </div>
        <hr className="divider" />
        <div className="flex items-center gap-2">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-status-verified opacity-60" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-status-verified" />
          </span>
          <span className="text-xs text-md-on-surface-var">Core System Live</span>
        </div>
      </div>

      <div className="card p-4 space-y-2">
        <div className="section-label">AI Pipeline</div>
        {[
          { label: 'Mode', value: 'AI_ASSISTED', color: 'text-md-primary' },
          { label: 'Model', value: 'GPT-4o', color: 'text-md-on-surface' },
          { label: 'Threshold', value: '0.85', color: 'text-md-on-surface' },
        ].map(({ label, value, color }) => (
          <div key={label} className="flex items-center justify-between">
            <span className="text-xs text-md-on-surface-var">{label}</span>
            <span className={`text-xs font-semibold ${color}`}>{value}</span>
          </div>
        ))}
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
        setError(err.message || 'Failed to load claims')
        setLoading(false)
      })
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center gap-3 text-md-on-surface-var">
          <div className="w-4 h-4 rounded-full border-2 border-md-primary border-t-transparent animate-spin" />
          <span className="text-sm">Loading claims…</span>
        </div>
      </div>
    )
  }

  const displayClaims = claims.length > 0 ? claims : null
  const hitlCount = displayClaims ? displayClaims.filter(c => c.workflowStatus === 'HITL_REVIEW').length : 0

  return (
    <div className="flex gap-4 h-full">
      {/* ── Main ──────────────────────────────────────── */}
      <div className="flex-1 min-w-0 space-y-4">

        {/* KPI row */}
        <div className="grid grid-cols-4 gap-3">
          <KpiCard
            label="Intake Volume"
            value={displayClaims ? String(displayClaims.length) : '—'}
            sub="Active in pipeline"
            accent="text-md-primary"
            variant="primary"
          />
          <KpiCard
            label="STP Rate"
            value="—"
            sub="Configure AI threshold"
            accent="text-status-verified"
            variant="success"
          />
          <KpiCard
            label="Pending HITL"
            value={displayClaims ? String(hitlCount) : '—'}
            sub="Awaiting human review"
            accent="text-status-warning"
            variant="warning"
          />
          <KpiCard
            label="Fraud Flags"
            value="—"
            sub="Requires forensics"
            accent="text-status-critical"
            variant="error"
          />
        </div>

        {/* Error banner */}
        {error && (
          <div
            className="flex items-center gap-2 px-4 py-2.5 rounded-lg text-xs text-status-critical"
            style={{ background: 'rgba(147,0,10,0.15)', border: '1px solid rgba(255,107,107,0.2)' }}
          >
            <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
            </svg>
            {error} — connect Docker stack for real data
          </div>
        )}

        {/* Claims table */}
        <div className="card overflow-hidden">
          {/* Table header bar */}
          <div
            className="flex items-center justify-between px-4 py-3"
            style={{ background: '#191b22', borderBottom: '1px solid rgba(70,69,85,0.3)' }}
          >
            <div className="flex items-center gap-3">
              <h2 className="text-sm font-semibold text-md-on-surface">Active Claims Pipeline</h2>
              {displayClaims && (
                <span className="px-2 py-0.5 rounded-md bg-md-container-high text-[11px] font-medium text-md-on-surface-var">
                  {displayClaims.length}
                </span>
              )}
            </div>
            <button
              onClick={() => claimsApi.list().then(setClaims).catch(console.error)}
              className="flex items-center gap-1.5 text-xs text-md-on-surface-var hover:text-md-primary transition-colors"
            >
              <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Refresh
            </button>
          </div>

          <table className="data-table">
            <thead>
              <tr>
                {['Claim ID', 'Insured', 'Vessel', 'LOB', 'Status', 'Date of Loss'].map(h => (
                  <th key={h}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {(displayClaims || []).map((claim) => (
                <tr key={claim.id}>
                  <td className="font-mono text-md-primary text-xs tabular-nums font-semibold">{claim.claimNumber}</td>
                  <td className="text-md-on-surface text-xs">{claim.insuredName || '—'}</td>
                  <td className="text-md-on-surface text-xs font-semibold">{claim.vesselName || '—'}</td>
                  <td className="text-md-on-surface-var text-xs">{claim.lineOfBusiness || '—'}</td>
                  <td>
                    <span className={`badge ${statusBadge[claim.workflowStatus] || 'badge-received'}`}>
                      {claim.workflowStatus}
                    </span>
                  </td>
                  <td className="text-md-on-surface-var tabular-nums text-xs">{claim.dateOfLoss || '—'}</td>
                </tr>
              ))}
              {!displayClaims && (
                <tr>
                  <td colSpan={6} className="text-center py-12">
                    <div className="flex flex-col items-center gap-2 text-md-on-surface-var/50">
                      <svg className="w-10 h-10" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                      <span className="text-sm">No claims — connect Docker stack</span>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Sidebar ──────────────────────────────────── */}
      <div className="w-52 shrink-0">
        <SyncStatusSidebar pendingSync={0} />
      </div>
    </div>
  )
}
