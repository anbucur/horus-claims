const statusBadge: Record<string, string> = {
  Extracting: 'badge-extracting',
  'Querying Policy': 'badge-verifying',
  'Awaiting HITL': 'badge-hitl',
  STP: 'badge-stp',
  Verifying: 'badge-verifying',
  Received: 'badge-received',
}

const mockClaims = [
  { id: 'CLM-2024-0891', insured: 'OceanTech Shipping Ltd', vessel: 'MSC Oscar', lob: 'Hull & Machinery', status: 'Extracting', timestamp: '2024-11-14 08:32' },
  { id: 'CLM-2024-0890', insured: 'Maersk Chartering B.V.', vessel: 'Maersk Attikos', lob: 'Ocean Marine', status: 'Querying Policy', timestamp: '2024-11-14 08:15' },
  { id: 'CLM-2024-0889', insured: 'Nordic Bulk Carriers', vessel: 'Nordic Taurus', lob: 'Cargo', status: 'Awaiting HITL', timestamp: '2024-11-14 07:58' },
  { id: 'CLM-2024-0888', insured: 'Atlantic LNG Transport', vessel: 'Atlantic Pioneer', lob: 'Hull & Machinery', status: 'STP', timestamp: '2024-11-14 07:41' },
  { id: 'CLM-2024-0887', insured: 'Pacific Rim Towage', vessel: 'PRT Endeavour', lob: 'Protection & Indemnity', status: 'Verifying', timestamp: '2024-11-14 07:22' },
  { id: 'CLM-2024-0886', insured: 'Eurocargo Holdings', vessel: 'Euro Carrier III', lob: 'Cargo', status: 'Extracting', timestamp: '2024-11-14 07:05' },
  { id: 'CLM-2024-0885', insured: 'Venture Tankers Inc', vessel: 'VT Navigator', lob: 'Ocean Marine', status: 'Awaiting HITL', timestamp: '2024-11-14 06:48' },
  { id: 'CLM-2024-0884', insured: 'Celtic Marine SA', vessel: 'Celtic Star', lob: 'Hull & Machinery', status: 'Received', timestamp: '2024-11-14 06:30' },
]

function SyncStatusSidebar() {
  return (
    <div className="card border border-slate-200 p-3 space-y-3">
      <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide">Quick Stats</div>
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <span className="text-xs text-slate-500">Last Sync</span>
          <span className="text-xs font-medium text-slate-700 tabular-nums">14:32:08</span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-xs text-slate-500">Claims Today</span>
          <span className="text-xs font-medium text-brand-primary tabular-nums">47</span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-xs text-slate-500">STP Rate</span>
          <span className="text-xs font-medium text-status-verified tabular-nums">78.3%</span>
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
  return (
    <div className="flex gap-4 h-full">
      <div className="flex-1 space-y-4">
        <div className="grid grid-cols-4 gap-4">
          {[
            { label: 'Intake Volume', value: '1,247', sub: '+12% vs last week', color: 'text-brand-primary' },
            { label: 'STP Rate', value: '78.3%', sub: '+3.1pp vs last week', color: 'text-status-verified' },
            { label: 'Pending HITL', value: '23', sub: 'Avg 4.2h queue time', color: 'text-status-warning' },
            { label: 'Fraud Flags', value: '4', sub: 'Requires immediate review', color: 'text-status-critical' },
          ].map(({ label, value, sub, color }) => (
            <div key={label} className="card p-4 border border-slate-200 hover:border-slate-300 transition-colors">
              <div className="text-xs text-slate-500 font-medium uppercase tracking-wide mb-2">{label}</div>
              <div className={`text-2xl font-bold tabular-nums ${color}`}>{value}</div>
              <div className="text-xs text-slate-400 mt-1.5">{sub}</div>
            </div>
          ))}
        </div>

        <div className="card border border-slate-200">
          <div className="px-4 py-3 border-b border-slate-200 bg-slate-50/50">
            <h2 className="text-sm font-semibold text-slate-800">Active Claims Pipeline</h2>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50">
                {['Claim ID', 'Insured', 'Vessel', 'LOB', 'Agent Status', 'Timestamp'].map(h => (
                  <th key={h} className="text-left px-4 py-2.5 text-xs font-semibold text-slate-600 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {mockClaims.map((claim, i) => (
                <tr key={claim.id} className={`border-b border-slate-100 hover:bg-slate-50 transition-colors ${i % 2 === 1 ? 'bg-slate-50/40' : ''}`}>
                  <td className="px-4 py-2.5 font-medium text-brand-primary tabular-nums text-xs">{claim.id}</td>
                  <td className="px-4 py-2.5 text-slate-700 text-xs">{claim.insured}</td>
                  <td className="px-4 py-2.5 text-slate-900 font-semibold tabular-nums text-xs">{claim.vessel}</td>
                  <td className="px-4 py-2.5 text-slate-600 text-xs">{claim.lob}</td>
                  <td className="px-4 py-2.5">
                    <span className={`badge ${statusBadge[claim.status] || 'badge-received'} text-xs`}>
                      {claim.status}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 text-slate-500 tabular-nums text-xs">{claim.timestamp}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      <div className="w-56 shrink-0">
        <SyncStatusSidebar />
      </div>
    </div>
  )
}