const mockPending = [
  { id: 'CLM-2024-0891', trigger: 'ON_FIELD_CHANGE', attempts: 2, oldest: '12min ago' },
  { id: 'CLM-2024-0899', trigger: 'ON_STATUS_CHANGE', attempts: 3, oldest: '8min ago' },
  { id: 'CLM-2024-0901', trigger: 'ON_SCHEDULE', attempts: 1, oldest: '3min ago' },
  { id: 'CLM-2024-0903', trigger: 'ON_MANUAL', attempts: 1, oldest: '1min ago' },
  { id: 'CLM-2024-0905', trigger: 'ON_STATUS_CHANGE', attempts: 1, oldest: '45sec ago' },
]

const mockHistory = [
  { id: 'CLM-2024-0887', trigger: 'ON_STATUS_CHANGE', status: 'SUCCESS', statusBadge: 'badge-stp', response: 200, timestamp: '2024-11-14 09:12' },
  { id: 'CLM-2024-0885', trigger: 'ON_FIELD_CHANGE', status: 'SUCCESS', statusBadge: 'badge-stp', response: 200, timestamp: '2024-11-14 09:08' },
  { id: 'CLM-2024-0883', trigger: 'ON_STATUS_CHANGE', status: 'FAILED', statusBadge: 'badge-hitl', response: 503, timestamp: '2024-11-14 08:55' },
  { id: 'CLM-2024-0881', trigger: 'ON_SCHEDULE', status: 'SUCCESS', statusBadge: 'badge-stp', response: 200, timestamp: '2024-11-14 08:30' },
  { id: 'CLM-2024-0879', trigger: 'ON_MANUAL', status: 'SUCCESS', statusBadge: 'badge-stp', response: 200, timestamp: '2024-11-14 08:15' },
]

export default function SyncQueue() {
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-800">Sync Queue & History</h1>
        <div className="flex items-center gap-2">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-status-verified opacity-75" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-status-verified" />
          </span>
          <span className="text-xs text-slate-500">Target: MSIG Core API</span>
        </div>
      </div>

      <div className="grid grid-cols-4 gap-4">
        <div className="card p-4 border border-slate-200">
          <div className="text-xs text-slate-500 font-medium uppercase tracking-wide mb-2">Pending</div>
          <div className="text-2xl font-bold text-status-warning tabular-nums">7</div>
        </div>
        <div className="card p-4 border border-slate-200">
          <div className="text-xs text-slate-500 font-medium uppercase tracking-wide mb-2">Success Today</div>
          <div className="text-2xl font-bold text-status-verified tabular-nums">142</div>
        </div>
        <div className="card p-4 border border-slate-200">
          <div className="text-xs text-slate-500 font-medium uppercase tracking-wide mb-2">Failed</div>
          <div className="text-2xl font-bold text-status-critical tabular-nums">3</div>
        </div>
        <div className="card p-4 border border-slate-200">
          <div className="text-xs text-slate-500 font-medium uppercase tracking-wide mb-2">Target System</div>
          <div className="flex items-center gap-2 mt-2">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-status-verified opacity-75" />
              <span className="relative inline-flex rounded-full h-2 w-2 bg-status-verified" />
            </span>
            <span className="text-sm font-medium text-slate-700">MSIG Core API</span>
          </div>
        </div>
      </div>

      <div className="card border border-slate-200">
        <div className="flex border-b border-slate-200 bg-slate-50/50">
          {['Pending Queue', 'Sync History'].map((tab, i) => (
            <button
              key={tab}
              className={`px-4 py-3 text-sm font-medium border-r border-slate-200 transition-colors ${
                i === 0 ? 'text-brand-primary border-b-2 border-b-brand-primary bg-white' : 'text-slate-500 hover:text-slate-700 hover:bg-slate-50'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        <div className="p-0">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50">
                {['Claim ID', 'Trigger', 'Attempts', 'Oldest', 'Actions'].map(h => (
                  <th key={h} className="text-left px-4 py-2.5 text-xs font-semibold text-slate-600 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {mockPending.map((row, i) => (
                <tr key={row.id} className={`border-b border-slate-100 hover:bg-slate-50 transition-colors ${i % 2 === 1 ? 'bg-slate-50/40' : ''}`}>
                  <td className="px-4 py-2.5 font-medium text-brand-primary tabular-nums text-xs">{row.id}</td>
                  <td className="px-4 py-2.5">
                    <span className="badge badge-received text-xs">{row.trigger}</span>
                  </td>
                  <td className="px-4 py-2.5 text-slate-600 tabular-nums text-xs">{row.attempts}</td>
                  <td className="px-4 py-2.5 text-slate-500 tabular-nums text-xs">{row.oldest}</td>
                  <td className="px-4 py-2.5">
                    <div className="flex gap-3">
                      <button className="text-xs text-blue-600 hover:text-blue-800 font-medium">Retry</button>
                      <button className="text-xs text-slate-500 hover:text-slate-700">View</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card border border-slate-200">
        <div className="p-4 flex items-center justify-between border-b border-slate-200">
          <div className="card p-3 border border-slate-200 flex items-center gap-4">
            <div className="flex items-center gap-2">
              <svg className="w-4 h-4 text-status-verified" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div>
                <div className="text-xs font-semibold text-slate-600">Target System Health</div>
                <div className="text-xs text-slate-500">API: MSIG Core API</div>
                <div className="text-xs text-slate-500">Last ping: 2 min ago</div>
              </div>
            </div>
          </div>
          <button className="btn-primary px-6 py-2.5">Sync All Pending Now</button>
        </div>
      </div>

      <div className="card border border-slate-200">
        <div className="px-4 py-3 border-b border-slate-200 bg-slate-50/50">
          <h2 className="text-sm font-semibold text-slate-800">Sync History</h2>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50">
              {['Claim ID', 'Trigger', 'Status', 'Response', 'Timestamp', 'Actions'].map(h => (
                <th key={h} className="text-left px-4 py-2.5 text-xs font-semibold text-slate-600 uppercase tracking-wide">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {mockHistory.map((row, i) => (
              <tr key={row.id} className={`border-b border-slate-100 hover:bg-slate-50 transition-colors ${i % 2 === 1 ? 'bg-slate-50/40' : ''}`}>
                <td className="px-4 py-2.5 font-medium text-brand-primary tabular-nums text-xs">{row.id}</td>
                <td className="px-4 py-2.5">
                  <span className="badge badge-received text-xs">{row.trigger}</span>
                </td>
                <td className="px-4 py-2.5">
                  <span className={`badge ${row.statusBadge} text-xs`}>{row.status}</span>
                </td>
                <td className="px-4 py-2.5 text-slate-600 tabular-nums text-xs">{row.response}</td>
                <td className="px-4 py-2.5 text-slate-500 tabular-nums text-xs">{row.timestamp}</td>
                <td className="px-4 py-2.5">
                  <div className="flex gap-3">
                    {row.status === 'FAILED' && (
                      <button className="text-xs text-blue-600 hover:text-blue-800 font-medium">Retry</button>
                    )}
                    <button className="text-xs text-slate-500 hover:text-slate-700">View</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}