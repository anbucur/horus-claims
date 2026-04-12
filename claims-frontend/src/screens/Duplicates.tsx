const currentFNOL = {
  claimId:     'CLM-2024-0891',
  vessel:      'MSC Oscar',
  dateOfLoss:  '12/11/2024',
  location:    'Rotterdam Port',
  description: 'Hull damage to starboard side aft following allision with moored dumb barge during berthing operations',
  value:       'EUR 2,400,000',
}

const historicalClaims = [
  { id: 'CLM-2024-0789', dateOfLoss: '03/10/2024', vessel: 'MSC Oscar', description: 'Shell plate damage starboard bow — Rotterdam Berth 12', similarity: 91 },
  { id: 'CLM-2022-0891', dateOfLoss: '08/11/2022', vessel: 'MSC Oscar', description: 'Hull scratches at frame 88-92 port side — Rotterdam', similarity: 41 },
  { id: 'CLM-2023-0542', dateOfLoss: '15/06/2023', vessel: 'MSC Oscar', description: 'Deck machinery damage during cargo operations — Antwerp', similarity: 34 },
  { id: 'CLM-2023-0218', dateOfLoss: '22/03/2023', vessel: 'MSC Oscar', description: 'Propeller fouling incident — Hamburg anchorage', similarity: 28 },
  { id: 'CLM-2021-0347', dateOfLoss: '19/04/2021', vessel: 'MSC Oscar', description: 'Engine room flooding — English Channel', similarity: 22 },
]

function similarityColor(s: number): string {
  if (s >= 80) return 'text-status-critical'
  if (s >= 50) return 'text-status-warning'
  return 'text-status-verified'
}

function similarityBg(s: number): string {
  if (s >= 80) return 'rgba(255,107,107,0.12)'
  if (s >= 50) return 'rgba(251,191,36,0.08)'
  return 'transparent'
}

export default function Duplicates() {
  return (
    <div className="h-full flex flex-col space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-xl font-bold text-md-on-surface">Duplicate Detection</h1>
          <div className="text-xs text-md-on-surface-var mt-0.5">Semantic similarity analysis against historical claims</div>
        </div>
        <div className="flex items-center gap-2">
          <span className="stat-chip">
            <span className="text-status-critical font-bold tabular-nums">1</span>
            <span className="text-md-on-surface-var">High similarity</span>
          </span>
          <span className="stat-chip">
            <span className="text-md-on-surface font-bold tabular-nums">{historicalClaims.length}</span>
            <span className="text-md-on-surface-var">total matches</span>
          </span>
        </div>
      </div>

      {/* Current FNOL card */}
      <div
        className="card p-4"
        style={{ borderLeft: '2px solid rgba(195,192,255,0.5)' }}
      >
        <div className="flex items-center gap-2 mb-3">
          <span className="badge badge-received">Current FNOL</span>
          <span className="text-xs text-md-on-surface-var">Under analysis</span>
        </div>
        <div className="grid grid-cols-6 gap-4">
          <div>
            <div className="input-label">Claim ID</div>
            <div className="text-sm font-mono font-semibold text-md-primary tabular-nums">{currentFNOL.claimId}</div>
          </div>
          <div>
            <div className="input-label">Vessel</div>
            <div className="text-sm font-semibold text-md-on-surface">{currentFNOL.vessel}</div>
          </div>
          <div>
            <div className="input-label">Date of Loss</div>
            <div className="text-sm font-medium text-md-on-surface tabular-nums">{currentFNOL.dateOfLoss}</div>
          </div>
          <div>
            <div className="input-label">Location</div>
            <div className="text-sm font-medium text-md-on-surface">{currentFNOL.location}</div>
          </div>
          <div className="col-span-2">
            <div className="input-label">Description</div>
            <div className="text-sm text-md-on-surface-var leading-snug">{currentFNOL.description}</div>
          </div>
        </div>
        <div className="mt-3 pt-3 flex items-center gap-4" style={{ borderTop: '1px solid rgba(70,69,85,0.25)' }}>
          <div>
            <div className="input-label">Estimated Value</div>
            <div className="text-xl font-display font-bold text-md-on-surface tabular-nums">{currentFNOL.value}</div>
          </div>
        </div>
      </div>

      {/* Historical matches table */}
      <div className="flex-1 min-h-0 card flex flex-col overflow-hidden">
        <div
          className="px-4 py-3 shrink-0"
          style={{ background: '#191b22', borderBottom: '1px solid rgba(70,69,85,0.3)' }}
        >
          <h2 className="text-sm font-semibold text-md-on-surface">Historical Claims — Potential Duplicates</h2>
        </div>

        <div className="flex-1 overflow-y-auto">
          <table className="data-table">
            <thead>
              <tr>
                {['Claim ID', 'Date of Loss', 'Vessel', 'Description', 'Similarity', 'Actions'].map(h => (
                  <th key={h}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {historicalClaims.map((claim) => (
                <tr
                  key={claim.id}
                  style={{ background: similarityBg(claim.similarity) }}
                >
                  <td className="font-mono text-md-primary text-xs tabular-nums font-semibold">{claim.id}</td>
                  <td className="text-md-on-surface-var tabular-nums text-xs">{claim.dateOfLoss}</td>
                  <td className="text-md-on-surface text-xs font-semibold">{claim.vessel}</td>
                  <td className="text-md-on-surface-var text-xs max-w-xs truncate">{claim.description}</td>
                  <td>
                    <div className="flex items-center gap-2">
                      <div className="w-16 h-1.5 rounded-full bg-md-container-high overflow-hidden">
                        <div
                          className="h-full rounded-full"
                          style={{
                            width: `${claim.similarity}%`,
                            background: claim.similarity >= 80 ? '#ff6b6b' : claim.similarity >= 50 ? '#fbbf24' : '#4ade80',
                          }}
                        />
                      </div>
                      <span className={`text-xs font-bold tabular-nums ${similarityColor(claim.similarity)}`}>
                        {claim.similarity}%
                      </span>
                    </div>
                  </td>
                  <td>
                    <div className="flex gap-2">
                      <button className="text-xs font-medium text-md-primary border border-md-primary/30 px-2 py-1 rounded-md hover:bg-md-primary/10 transition-colors">
                        Link Report
                      </button>
                      <button className="text-xs text-md-on-surface-var/50 border border-md-outline-var/30 px-2 py-1 rounded-md hover:bg-md-container-high transition-colors">
                        Dismiss
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
