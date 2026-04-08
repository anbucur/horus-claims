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

const statusBadge: Record<string, string> = {
  Extracting: 'badge-extracting',
  'Querying Policy': 'badge-verifying',
  'Awaiting HITL': 'badge-hitl',
  STP: 'badge-stp',
  Verifying: 'badge-verifying',
  Received: 'badge-received',
}

export default function Dashboard() {
  return (
    <div className="space-y-4">
      {/* KPI Cards */}
      <div className="grid grid-cols-4 gap-4">
        {[
          { label: 'Intake Volume', value: '1,247', sub: '+12% vs last week' },
          { label: 'STP Rate', value: '78.3%', sub: '+3.1pp vs last week' },
          { label: 'Pending HITL', value: '23', sub: 'Avg 4.2h queue time' },
          { label: 'Fraud Flags', value: '4', sub: 'Requires immediate review' },
        ].map(({ label, value, sub }) => (
          <div key={label} className="card p-4">
            <div className="text-xs text-slate-500 mb-1">{label}</div>
            <div className="text-2xl font-semibold text-slate-900 tabular-nums">{value}</div>
            <div className="text-xs text-slate-400 mt-1">{sub}</div>
          </div>
        ))}
      </div>

      {/* Claims Table */}
      <div className="card">
        <div className="px-4 py-3 border-b border-slate-200">
          <h2 className="text-sm font-semibold text-slate-900">Active Claims Pipeline</h2>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-100 bg-slate-50">
              {['Claim ID', 'Insured', 'Vessel', 'LOB', 'Agent Status', 'Timestamp'].map(h => (
                <th key={h} className="text-left px-4 py-2 text-xs font-medium text-slate-500 uppercase tracking-wide">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {mockClaims.map((claim, i) => (
              <tr key={claim.id} className={`border-b border-slate-50 hover:bg-slate-50 ${i % 2 === 1 ? 'bg-slate-50/50' : ''}`}>
                <td className="px-4 py-2 font-medium text-brand-primary tabular-nums">{claim.id}</td>
                <td className="px-4 py-2 text-slate-700">{claim.insured}</td>
                <td className="px-4 py-2 text-slate-900 font-medium tabular-nums">{claim.vessel}</td>
                <td className="px-4 py-2 text-slate-600">{claim.lob}</td>
                <td className="px-4 py-2">
                  <span className={`badge ${statusBadge[claim.status] || 'badge-received'}`}>
                    {claim.status}
                  </span>
                </td>
                <td className="px-4 py-2 text-slate-500 tabular-nums">{claim.timestamp}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
