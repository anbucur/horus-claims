const anomalies = [
  { label: 'Digital Manipulation', severity: 'CRITICAL', detail: '34% pixels flagged — JPEG block inconsistencies' },
  { label: 'Metadata Mismatch',    severity: 'WARNING',  detail: 'GPS coords do not match declared Rotterdam location' },
  { label: 'Compression Artifact', severity: 'INFO',     detail: 'Re-compression detected in region around frame 142' },
]

const severityStyle: Record<string, { badge: string; dot: string }> = {
  CRITICAL: { badge: 'text-status-critical border-status-critical/30', dot: '#ff6b6b' },
  WARNING:  { badge: 'text-status-warning border-status-warning/30',   dot: '#fbbf24' },
  INFO:     { badge: 'text-status-info border-status-info/30',         dot: '#60a5fa' },
}

export default function Forensics() {
  return (
    <div className="h-full flex gap-4">
      {/* ── Image viewer ─── */}
      <div className="flex-1 flex flex-col gap-4">
        {/* FLAGGED banner */}
        <div
          className="flex items-center gap-2 px-4 py-2.5 rounded-lg text-sm font-semibold"
          style={{ background: 'rgba(147,0,10,0.2)', border: '1px solid rgba(255,107,107,0.3)', color: '#ff6b6b' }}
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          FORENSICS ALERT — Digital manipulation detected — SIU referral recommended
        </div>

        {/* Image area */}
        <div className="card flex-1 relative overflow-hidden">
          {/* Simulated dark image background */}
          <div
            className="absolute inset-0 flex flex-col items-center justify-center"
            style={{ background: 'linear-gradient(135deg, #1a1d2e 0%, #0c0e1a 50%, #1a1220 100%)' }}
          >
            <svg className="w-20 h-20 text-md-on-surface-var/20 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6.827 6.175A2.31 2.31 0 015.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 00-1.134-.175 2.31 2.31 0 01-1.64-1.055l-.822-1.316a2.192 2.192 0 00-1.736-1.039 48.774 48.774 0 00-5.232 0 2.192 2.192 0 00-1.736 1.039l-.821 1.316z" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 12.75a4.5 4.5 0 11-9 0 4.5 4.5 0 019 0z" />
            </svg>
            <div className="text-md-on-surface font-semibold">Hull Damage — MSC Oscar</div>
            <div className="text-md-on-surface-var text-sm mt-1">Frame 142–147 · Starboard Side Aft</div>
            <div className="text-md-on-surface-var/50 text-xs mt-3 font-mono">2024-11-12 06:15 UTC</div>
          </div>

          {/* Flagged region annotation overlay */}
          <div
            className="absolute text-xs font-semibold px-3 py-1.5 rounded-lg"
            style={{
              top: '30%', left: '25%',
              background: 'rgba(147,0,10,0.8)',
              border: '1px solid rgba(255,107,107,0.5)',
              color: '#ff6b6b',
              backdropFilter: 'blur(4px)',
            }}
          >
            ⚠ MANIPULATION DETECTED
          </div>

          {/* Bottom metadata strip */}
          <div
            className="absolute bottom-0 left-0 right-0 px-4 py-3 text-xs"
            style={{ background: 'rgba(12,14,20,0.85)', backdropFilter: 'blur(8px)', borderTop: '1px solid rgba(70,69,85,0.3)' }}
          >
            <div className="flex items-center justify-between text-md-on-surface-var">
              <span><span className="text-md-on-surface-var/50">Metadata:</span> EXIF Date matches loss date</span>
              <span><span className="text-md-on-surface-var/50">GPS:</span> <span className="font-mono">52.3676°N, 4.9041°E (Rotterdam)</span></span>
              <div className="flex gap-2">
                {['Heatmap', 'Edge Detect', 'Metadata'].map(label => (
                  <button key={label} className="px-2 py-0.5 rounded bg-md-container-high text-md-on-surface-var hover:text-md-primary transition-colors">
                    {label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ── Analysis panel ─── */}
      <div className="w-72 shrink-0 flex flex-col gap-4">
        {/* Authenticity score */}
        <div
          className="card p-4"
          style={{ borderLeft: '2px solid #ff6b6b' }}
        >
          <div className="section-label mb-1">Authenticity Score</div>
          <div className="text-5xl font-display font-bold tabular-nums text-status-critical">12%</div>
          <div className="text-xs text-status-critical/80 mt-1">High Risk — SIU referral required</div>
          {/* Score bar */}
          <div className="mt-3 h-2 rounded-full bg-md-container-high overflow-hidden">
            <div className="h-full rounded-full bg-status-critical" style={{ width: '12%' }} />
          </div>
        </div>

        {/* Anomaly list */}
        <div className="card p-4 flex-1 overflow-y-auto">
          <div className="section-label mb-3">Detected Anomalies</div>
          <div className="space-y-3">
            {anomalies.map(({ label, severity, detail }) => (
              <div key={label} className="space-y-1">
                <div className="flex items-center gap-2">
                  <span
                    className={`inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md text-[9px] font-bold uppercase tracking-wide border ${severityStyle[severity].badge}`}
                  >
                    <span className="w-1.5 h-1.5 rounded-full shrink-0" style={{ background: severityStyle[severity].dot }} />
                    {severity}
                  </span>
                  <span className="text-xs font-semibold text-md-on-surface">{label}</span>
                </div>
                <div className="text-[11px] text-md-on-surface-var leading-snug pl-1">{detail}</div>
              </div>
            ))}
          </div>

          <hr className="divider my-4" />

          {/* Narrative consistency */}
          <div className="section-label mb-3">Narrative Consistency</div>
          <div className="space-y-2">
            <div
              className="p-2.5 rounded-lg text-xs text-md-on-surface-var"
              style={{ background: '#191b22' }}
            >
              <div className="text-[9px] font-semibold uppercase tracking-widest text-md-on-surface-var/50 mb-1">Extracted Text</div>
              "Hull damage starboard side aft during berthing"
            </div>
            <div className="flex justify-center text-md-on-surface-var/30 text-xs">↓ AI analysis ↓</div>
            <div
              className="p-2.5 rounded-lg text-xs text-status-critical"
              style={{ background: 'rgba(147,0,10,0.1)', border: '1px solid rgba(255,107,107,0.15)' }}
            >
              <div className="text-[9px] font-semibold uppercase tracking-widest text-status-critical/60 mb-1">AI Classification</div>
              "Pre-existing damage pattern inconsistent with allision claim"
            </div>
          </div>
        </div>

        {/* SIU button */}
        <button
          className="w-full px-4 py-3 rounded-lg text-sm font-semibold flex items-center justify-center gap-2 transition-all hover:opacity-90 active:scale-95"
          style={{ background: 'rgba(147,0,10,0.9)', color: '#ff6b6b', border: '1px solid rgba(255,107,107,0.3)' }}
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          Route to SIU
        </button>
      </div>
    </div>
  )
}
