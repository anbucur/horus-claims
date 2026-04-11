const steps = [
  { label: 'Email Ingest',   status: 'complete' },
  { label: 'AI Extract',     status: 'complete' },
  { label: 'Policy Verify',  status: 'complete' },
  { label: 'Entity Match',   status: 'complete' },
  { label: 'Image Forensics',status: 'current' },
  { label: 'Duplicate Check',status: 'pending' },
  { label: 'Final Review',   status: 'pending' },
]

export default function HITLConsole() {
  return (
    <div className="h-full flex flex-col space-y-4">
      {/* Page title + meta */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-xl font-bold text-md-on-surface">HITL Console</h1>
          <div className="text-xs text-md-on-surface-var mt-0.5">CLM-2024-08891 · MSC Eleonora · Step 5 of 7</div>
        </div>
        <span className="badge badge-hitl text-sm px-3 py-1">Under Review</span>
      </div>

      {/* Pipeline stepper */}
      <div className="card p-4">
        <div className="flex items-center justify-between mb-4">
          <div className="section-label mb-0">Processing Pipeline</div>
          <span
            className="text-xs font-semibold tabular-nums px-2 py-1 rounded-lg"
            style={{ background: 'rgba(251,191,36,0.1)', color: '#fbbf24', border: '1px solid rgba(251,191,36,0.2)' }}
          >
            Step 5 / 7
          </span>
        </div>
        <div className="flex items-center overflow-x-auto">
          {steps.map((step, index) => (
            <div key={step.label} className="flex items-center">
              <div className="flex flex-col items-center gap-1.5">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                  step.status === 'complete'
                    ? 'bg-status-verified/20 text-status-verified'
                    : step.status === 'current'
                      ? 'bg-md-primary-container text-white ring-4 ring-md-primary/20'
                      : 'bg-md-container-high text-md-on-surface-var/40'
                }`}>
                  {step.status === 'complete' ? (
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  ) : (
                    index + 1
                  )}
                </div>
                <span className={`text-[10px] font-medium whitespace-nowrap ${
                  step.status === 'current'   ? 'text-md-primary' :
                  step.status === 'complete'  ? 'text-md-on-surface-var' :
                                               'text-md-on-surface-var/40'
                }`}>
                  {step.label}
                </span>
              </div>
              {index < steps.length - 1 && (
                <div className={`w-10 h-0.5 mx-1 mb-4 ${
                  step.status === 'complete' ? 'bg-status-verified/40' : 'bg-md-outline-var/30'
                }`} />
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Review workspace */}
      <div className="flex-1 card p-4 min-h-0 overflow-auto">
        <div className="section-label mb-4">Adjuster Review</div>

        <div className="space-y-5">
          {/* Notes */}
          <div>
            <label className="input-label">Adjuster Notes</label>
            <textarea
              className="input-field h-24 resize-none"
              placeholder="Enter review notes and observations…"
            />
          </div>

          {/* Financial fields */}
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="input-label">Reserve Amount</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-xs text-md-on-surface-var font-mono">EUR</span>
                <input
                  type="text"
                  defaultValue="2,400,000"
                  className="input-field pl-12 tabular-nums font-mono"
                  style={{ borderColor: 'rgba(251,191,36,0.4)', background: 'rgba(251,191,36,0.05)' }}
                />
              </div>
            </div>
            <div>
              <label className="input-label">Deductible</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-xs text-md-on-surface-var font-mono">EUR</span>
                <input
                  type="text"
                  defaultValue="50,000"
                  className="input-field pl-12 tabular-nums font-mono"
                  style={{ borderColor: 'rgba(74,222,128,0.3)', background: 'rgba(74,222,128,0.05)' }}
                />
              </div>
            </div>
            <div>
              <label className="input-label">Payout Recommendation</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-xs text-md-on-surface-var font-mono">EUR</span>
                <input
                  type="text"
                  defaultValue="2,350,000"
                  className="input-field pl-12 tabular-nums font-mono"
                />
              </div>
            </div>
          </div>

          {/* Summary + actions */}
          <div className="pt-4 flex items-center justify-between" style={{ borderTop: '1px solid rgba(70,69,85,0.3)' }}>
            <div className="text-sm text-md-on-surface-var">
              Net Claim Value:&nbsp;
              <span className="font-semibold text-md-on-surface tabular-nums font-mono">EUR 2,350,000</span>
            </div>
            <div className="flex gap-2">
              <button className="btn-ghost text-xs px-4 py-2">
                Request More Information
              </button>
              <button className="btn-danger text-xs px-4 py-2">
                Reject
              </button>
              <button className="btn-primary px-5 py-2">
                Approve &amp; Commit to Core
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
