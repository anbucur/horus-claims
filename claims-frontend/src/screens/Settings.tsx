export default function Settings() {
  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="font-display text-xl font-bold text-md-on-surface">Settings</h1>
        <div className="text-xs text-md-on-surface-var mt-0.5">Platform configuration and preferences</div>
      </div>

      {[
        {
          title: 'AI Processing Mode',
          desc: 'Control how claims are processed through the AI pipeline.',
          options: [
            { label: 'AI Assisted', value: 'AI_ASSISTED', active: true, desc: 'Default — AI extracts and routes, humans review exceptions' },
            { label: 'Full Manual', value: 'FULL_MANUAL', active: false, desc: 'Skip AI, route all claims to HITL' },
            { label: 'Hybrid',      value: 'HYBRID',      active: false, desc: 'Mix AI and manual based on claim type' },
            { label: 'AI Fallback', value: 'AI_FALLBACK', active: false, desc: 'Try AI, fall back to manual if Azure down' },
          ],
        },
      ].map(({ title, desc, options }) => (
        <div key={title} className="card p-5">
          <div className="section-label mb-0.5">{title}</div>
          <div className="text-xs text-md-on-surface-var mb-4">{desc}</div>
          <div className="space-y-2">
            {options.map(({ label, active, desc: optDesc }) => (
              <label
                key={label}
                className={`flex items-start gap-3 p-3 rounded-lg cursor-pointer transition-all ${
                  active ? 'bg-md-primary-container/20 border border-md-primary/20' : 'hover:bg-md-container-high border border-transparent'
                }`}
              >
                <div className={`mt-0.5 w-4 h-4 rounded-full border-2 flex items-center justify-center shrink-0 ${
                  active ? 'border-md-primary' : 'border-md-outline-var/60'
                }`}>
                  {active && <div className="w-2 h-2 rounded-full bg-md-primary" />}
                </div>
                <div>
                  <div className="text-sm font-semibold text-md-on-surface">{label}</div>
                  <div className="text-xs text-md-on-surface-var mt-0.5">{optDesc}</div>
                </div>
              </label>
            ))}
          </div>
        </div>
      ))}

      <div className="card p-5">
        <div className="section-label mb-4">AI Thresholds</div>
        <div className="space-y-4">
          {[
            { label: 'STP Confidence Threshold', value: 85, color: '#4ade80' },
            { label: 'Duplicate Similarity Alert', value: 80, color: '#fbbf24' },
            { label: 'Forensics Risk Score', value: 40, color: '#fb923c' },
          ].map(({ label, value, color }) => (
            <div key={label}>
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-xs text-md-on-surface-var">{label}</span>
                <span className="text-xs font-bold tabular-nums" style={{ color }}>{value}%</span>
              </div>
              <div className="h-1.5 rounded-full bg-md-container-high overflow-hidden">
                <div className="h-full rounded-full" style={{ width: `${value}%`, background: color }} />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
