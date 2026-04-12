export default function ClaimOverview() {
  return (
    <div className="h-full flex flex-col space-y-4">
      {/* Claim header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div>
            <div className="flex items-center gap-2.5">
              <span className="font-display text-xl font-bold text-md-on-surface tabular-nums">CLM-2024-08891</span>
              <span className="badge badge-hitl">HITL REVIEW</span>
            </div>
            <div className="text-xs text-md-on-surface-var mt-0.5">MSC Eleonora · Rotterdam Port · Hull &amp; Machinery</div>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button className="btn-ghost text-xs px-3 py-1.5">Override Confidence</button>
          <button className="btn-ghost text-xs px-3 py-1.5">Request More Info</button>
          <button className="btn-ghost text-xs px-3 py-1.5">Route to HITL</button>
          <button className="btn-primary text-xs px-4 py-1.5">Approve STP</button>
        </div>
      </div>

      {/* Body */}
      <div className="flex gap-4 flex-1 min-h-0">
        {/* ── Left: FNOL Extraction ── */}
        <div className="w-1/2 flex flex-col gap-4">
          <div className="card p-4">
            <div className="flex items-center justify-between mb-4">
              <div className="section-label mb-0">Extracted FNOL</div>
              <span className="badge badge-hitl">Low Confidence 71%</span>
            </div>

            {/* Highlighted field */}
            <div className="mb-4">
              <div className="input-label">Date of Loss</div>
              <div
                className="px-3 py-2.5 rounded-lg text-sm font-semibold text-status-warning tabular-nums"
                style={{ background: 'rgba(251,191,36,0.08)', border: '1px solid rgba(251,191,36,0.25)' }}
              >
                12/11/2024
              </div>
            </div>

            <div className="space-y-0">
              {[
                { label: 'Location',    value: 'Rotterdam Port' },
                { label: 'Est. Value',  value: 'EUR 2,400,000' },
              ].map(({ label, value }) => (
                <div key={label} className="flex items-center justify-between py-2.5" style={{ borderBottom: '1px solid rgba(70,69,85,0.2)' }}>
                  <span className="text-xs text-md-on-surface-var uppercase tracking-[0.06em]">{label}</span>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium text-md-on-surface tabular-nums">{value}</span>
                    <button className="text-md-on-surface-var/40 hover:text-md-on-surface-var transition-colors">
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                      </svg>
                    </button>
                  </div>
                </div>
              ))}

              <div className="py-3">
                <div className="text-xs text-md-on-surface-var uppercase tracking-[0.06em] mb-2">Incident Description</div>
                <p className="text-sm text-md-on-surface leading-relaxed">
                  Hull damage to starboard side aft following allision with moored dumb barge during berthing operations.
                  Shell plating indentation 4.2m × 1.8m at frame 142–147.
                </p>
              </div>
            </div>
          </div>

          {/* AI Confidence card */}
          <div className="card p-4">
            <div className="section-label mb-3">AI Extraction Confidence</div>
            <div className="space-y-3">
              {[
                { field: 'Insured Name',   score: 97, color: '#4ade80' },
                { field: 'Date of Loss',   score: 71, color: '#fbbf24' },
                { field: 'Vessel Name',    score: 95, color: '#4ade80' },
                { field: 'Claim Value',    score: 88, color: '#4ade80' },
                { field: 'Incident Type',  score: 63, color: '#fb923c' },
              ].map(({ field, score, color }) => (
                <div key={field} className="flex items-center gap-3">
                  <div className="text-xs text-md-on-surface-var w-28 shrink-0">{field}</div>
                  <div className="flex-1 h-1.5 rounded-full bg-md-container-high overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all"
                      style={{ width: `${score}%`, background: color }}
                    />
                  </div>
                  <span className="text-xs tabular-nums font-semibold w-8 text-right" style={{ color }}>{score}%</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ── Right: Policy Verification ── */}
        <div className="w-1/2 flex flex-col gap-4">
          <div
            className="card p-4"
            style={{ borderLeft: '2px solid #4ade80' }}
          >
            <div className="flex items-center gap-2 mb-4">
              <svg className="w-5 h-5 text-status-verified" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span className="text-sm font-semibold text-status-verified">Verified Active Policy</span>
            </div>

            <div className="space-y-0">
              {[
                { label: 'Policy Number', value: 'MSC-HULL-2024-8891', mono: true },
                { label: 'Status',        value: 'Active',             badge: 'stp' },
                { label: 'Coverage',      value: 'Hull & Machinery',   mono: false },
                { label: 'Sum Insured',   value: 'EUR 15,000,000',     mono: true },
                { label: 'Deductible',    value: 'EUR 50,000',         mono: true },
                { label: 'Policy Period', value: '01/01/2024 – 31/12/2024', mono: true },
              ].map(({ label, value, mono, badge }) => (
                <div key={label} className="flex items-center justify-between py-2.5" style={{ borderBottom: '1px solid rgba(74,222,128,0.12)' }}>
                  <span className="text-xs text-md-on-surface-var uppercase tracking-[0.06em]">{label}</span>
                  {badge ? (
                    <span className={`badge badge-${badge}`}>{value}</span>
                  ) : (
                    <span className={`text-sm font-medium text-md-on-surface ${mono ? 'tabular-nums font-mono' : ''}`}>{value}</span>
                  )}
                </div>
              ))}
            </div>

            <div className="mt-3 flex items-center gap-1.5 text-xs text-status-verified">
              <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
              Policy matches loss date — in-force at time of incident
            </div>
          </div>

          {/* Workflow stepper */}
          <div className="card p-4">
            <div className="section-label mb-3">Workflow Status</div>
            <div className="flex items-center overflow-x-auto pb-1">
              {[
                { label: 'FNOL',     status: 'complete' },
                { label: 'Extract', status: 'complete' },
                { label: 'Verify',  status: 'complete' },
                { label: 'Entity',  status: 'complete' },
                { label: 'Forensics', status: 'current' },
                { label: 'Dedup',   status: 'pending' },
                { label: 'Review',  status: 'pending' },
              ].map((step, i, arr) => (
                <div key={step.label} className="flex items-center">
                  <div className="flex flex-col items-center gap-1.5">
                    <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                      step.status === 'complete'
                        ? 'bg-status-verified/20 text-status-verified'
                        : step.status === 'current'
                          ? 'bg-md-primary-container text-white ring-2 ring-md-primary/40'
                          : 'bg-md-container-high text-md-on-surface-var/40'
                    }`}>
                      {step.status === 'complete' ? (
                        <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                        </svg>
                      ) : i + 1}
                    </div>
                    <span className={`text-[9px] font-medium whitespace-nowrap ${
                      step.status === 'current' ? 'text-md-primary' : 'text-md-on-surface-var/50'
                    }`}>{step.label}</span>
                  </div>
                  {i < arr.length - 1 && (
                    <div className={`w-8 h-0.5 mx-1 mb-4 ${step.status === 'complete' ? 'bg-status-verified/40' : 'bg-md-outline-var/30'}`} />
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
