const extractedEntities = [
  { type: 'Assured', value: 'OceanTech Shipping Ltd', icon: '🏢' },
  { type: 'Broker',  value: 'Marsh Ltd',              icon: '🤝' },
  { type: 'Vessel',  value: 'MSC Oscar',              icon: '🚢' },
]

const proposedMatches = [
  { id: 1, name: 'OceanTech Shipping Ltd',    location: 'Singapore',          status: 'Active', score: 94 },
  { id: 2, name: 'Marsh & McLennan Companies', location: 'New York, NY',      status: 'Active', score: 87 },
  { id: 3, name: 'Mediterranean Shipping Co', location: 'Geneva, Switzerland', status: 'Active', score: 99 },
]

function scoreColor(n: number): string {
  if (n >= 90) return '#4ade80'
  if (n >= 75) return '#fbbf24'
  return '#fb923c'
}

export default function Entities() {
  return (
    <div className="h-full flex flex-col space-y-4">
      {/* Header */}
      <div>
        <h1 className="font-display text-xl font-bold text-md-on-surface">Entity Matching Engine</h1>
        <div className="text-xs text-md-on-surface-var mt-0.5">AI-powered party resolution against master data</div>
      </div>

      <div className="flex gap-4 flex-1 min-h-0">
        {/* ── Extracted entities ─── */}
        <div className="w-1/3 card p-4">
          <div className="section-label mb-3">Extracted Entities</div>
          <div className="space-y-2">
            {extractedEntities.map((entity) => (
              <div
                key={entity.type}
                className="p-3 rounded-lg"
                style={{ background: '#191b22', border: '1px solid rgba(70,69,85,0.3)' }}
              >
                <div className="input-label mb-1">{entity.type}</div>
                <div className="text-sm font-semibold text-md-on-surface">{entity.value}</div>
              </div>
            ))}
          </div>
        </div>

        {/* ── Proposed matches ─── */}
        <div className="w-1/3 card p-4 flex flex-col">
          <div className="section-label mb-3">Proposed Matches</div>
          {/* Search */}
          <div className="relative mb-3">
            <svg className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-md-on-surface-var/40" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input className="input-field pl-9 text-xs" placeholder="Search entities…" />
          </div>
          <div className="space-y-2 flex-1 overflow-y-auto">
            {proposedMatches.map((match) => (
              <div
                key={match.id}
                className="p-3 rounded-lg hover:bg-md-container-high transition-colors cursor-pointer"
                style={{ border: '1px solid rgba(70,69,85,0.3)' }}
              >
                <div className="flex items-start justify-between gap-2 mb-2">
                  <div>
                    <div className="text-sm font-semibold text-md-on-surface">{match.name}</div>
                    <div className="text-xs text-md-on-surface-var mt-0.5">{match.location}</div>
                  </div>
                  <div className="flex flex-col items-end gap-1">
                    <span
                      className="text-lg font-display font-bold tabular-nums"
                      style={{ color: scoreColor(match.score) }}
                    >
                      {match.score}%
                    </span>
                    <span className="badge badge-stp text-[9px]">{match.status}</span>
                  </div>
                </div>
                {/* Score bar */}
                <div className="h-1 rounded-full bg-md-container-highest overflow-hidden">
                  <div
                    className="h-full rounded-full"
                    style={{ width: `${match.score}%`, background: scoreColor(match.score) }}
                  />
                </div>
                <button className="w-full mt-2.5 text-xs font-semibold text-md-primary border border-md-primary/30 px-3 py-1.5 rounded-lg hover:bg-md-primary/10 transition-colors">
                  Accept Match
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* ── Match confidence ─── */}
        <div className="w-1/3 card p-4">
          <div className="section-label mb-3">Match Confidence</div>
          <div className="space-y-3">
            {[
              { entity: 'OceanTech Shipping', against: 'DB Record', score: 94 },
              { entity: 'Marsh Ltd',          against: 'DB Record', score: 87 },
              { entity: 'MSC Oscar',          against: 'DB Record', score: 99 },
            ].map(({ entity, against, score }) => (
              <div
                key={entity}
                className="flex items-center justify-between p-3 rounded-lg"
                style={{
                  background: score >= 90 ? 'rgba(74,222,128,0.06)' : 'rgba(251,191,36,0.06)',
                  border: `1px solid ${score >= 90 ? 'rgba(74,222,128,0.2)' : 'rgba(251,191,36,0.2)'}`,
                }}
              >
                <div>
                  <div className="text-xs text-md-on-surface-var">{entity}</div>
                  <div className="text-sm font-semibold text-md-on-surface">vs {against}</div>
                </div>
                <div className="text-2xl font-display font-bold tabular-nums" style={{ color: scoreColor(score) }}>
                  {score}%
                </div>
              </div>
            ))}
          </div>

          <div className="mt-4 pt-4" style={{ borderTop: '1px solid rgba(70,69,85,0.3)' }}>
            <div className="section-label mb-2">Status</div>
            <div className="flex items-center gap-2">
              <span className="badge badge-stp">All entities resolved</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
