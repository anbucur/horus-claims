export default function TargetConfig() {
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-800">Target System Configuration</h1>
        <span className="text-xs text-slate-500">MSIG Claims Platform Sync</span>
      </div>

      <div className="card border border-slate-200">
        <div className="border-b border-slate-200 bg-slate-50/50">
          <div className="flex">
            {['Connection', 'Field Mappings', 'Triggers'].map((tab, i) => (
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
        </div>

        <div className="p-6 space-y-6">
          <div className="grid grid-cols-2 gap-6">
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1.5">Target System Name</label>
                <input
                  type="text"
                  defaultValue="MSIG Core API"
                  className="w-full px-3 py-2 border border-slate-200 rounded-sm text-sm focus:outline-none focus:ring-1 focus:ring-brand-primary"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1.5">Base URL</label>
                <input
                  type="text"
                  defaultValue="https://api.msigcore.com/v1"
                  placeholder="https://api.example.com/v1"
                  className="w-full px-3 py-2 border border-slate-200 rounded-sm text-sm focus:outline-none focus:ring-1 focus:ring-brand-primary"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1.5">Auth Type</label>
                <select
                  defaultValue="API_KEY"
                  className="w-full px-3 py-2 border border-slate-200 rounded-sm text-sm focus:outline-none focus:ring-1 focus:ring-brand-primary bg-white"
                >
                  <option value="API_KEY">API_KEY</option>
                  <option value="OAUTH2">OAUTH2</option>
                  <option value="BASIC">BASIC</option>
                  <option value="NONE">NONE</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1.5">API Key</label>
                <div className="relative">
                  <input
                    type="password"
                    defaultValue="msig_live_sk_7xQk9pL2mN4rT"
                    className="w-full px-3 py-2 pr-10 border border-slate-200 rounded-sm text-sm focus:outline-none focus:ring-1 focus:ring-brand-primary"
                  />
                  <button className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600">
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                  </button>
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between p-4 border border-slate-200 rounded-sm">
                <div>
                  <div className="text-sm font-medium text-slate-700">Sandbox Mode</div>
                  <div className="text-xs text-slate-500">Enable for testing without live data</div>
                </div>
                <div className="relative inline-block w-11 h-6">
                  <input type="checkbox" defaultChecked className="sr-only peer" />
                  <div className="w-11 h-6 bg-green-500 rounded-full peer-checked:bg-green-500"></div>
                  <div className="absolute left-1 top-1 w-4 h-4 bg-white rounded-full peer-checked:left-6 transition-all"></div>
                </div>
              </div>
              <div className="p-4 border border-slate-200 rounded-sm">
                <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-3">Connection Status</div>
                <div className="flex items-center gap-2">
                  <span className="relative flex h-2 w-2">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-status-verified opacity-75" />
                    <span className="relative inline-flex rounded-full h-2 w-2 bg-status-verified" />
                  </span>
                  <span className="text-sm text-slate-600">Connected to MSIG Core API</span>
                </div>
                <div className="mt-2 text-xs text-slate-500 tabular-nums">Last ping: 2 min ago</div>
              </div>
              <button className="w-full px-4 py-2 border border-blue-500 text-blue-600 rounded-sm text-sm font-medium hover:bg-blue-50 transition-colors">
                Test Connection
              </button>
            </div>
          </div>

          <div className="flex justify-end pt-4 border-t border-slate-200">
            <button className="btn-primary">Save Configuration</button>
          </div>
        </div>
      </div>

      <div className="card border border-slate-200">
        <div className="px-4 py-3 border-b border-slate-200 bg-slate-50/50">
          <h2 className="text-sm font-semibold text-slate-800">Field Mapping Rules</h2>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50">
              {['Workbench Field', 'Target Field', 'Transform', 'Actions'].map(h => (
                <th key={h} className="text-left px-4 py-2.5 text-xs font-semibold text-slate-600 uppercase tracking-wide">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {[
              { workbench: 'claim.id', target: 'claim_reference', transform: null },
              { workbench: 'claim.dateOfLoss', target: 'loss_date', transform: null },
              { workbench: 'claim.incidentNarrative', target: 'description', transform: null },
              { workbench: 'claim.estimatedValue', target: 'reserve_amount', transform: 'multiply:0.85' },
              { workbench: 'party.assuredName', target: 'policy_holder', transform: null },
              { workbench: 'vessel.name', target: 'vessel_name', transform: null },
              { workbench: 'vessel.imoNumber', target: 'vessel_id', transform: null },
              { workbench: 'status.STP', target: 'outcome_code', transform: 'static:SETTLED' },
            ].map((row, i) => (
              <tr key={i} className={`border-b border-slate-100 hover:bg-slate-50 transition-colors ${i % 2 === 1 ? 'bg-slate-50/40' : ''}`}>
                <td className="px-4 py-2.5 text-slate-700 text-xs font-mono">{row.workbench}</td>
                <td className="px-4 py-2.5 text-slate-700 text-xs font-mono">{row.target}</td>
                <td className="px-4 py-2.5 text-xs">
                  {row.transform ? (
                    <span className="badge badge-verifying text-xs">{row.transform}</span>
                  ) : (
                    <span className="text-slate-400 text-xs">—</span>
                  )}
                </td>
                <td className="px-4 py-2.5">
                  <button className="text-xs text-slate-500 hover:text-slate-700">Edit</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="px-4 py-3 border-t border-slate-200">
          <button className="px-3 py-1.5 border border-slate-200 text-slate-600 rounded-sm text-xs font-medium hover:bg-slate-50 transition-colors">
            + Add Mapping
          </button>
        </div>
      </div>

      <div className="card border border-slate-200">
        <div className="px-4 py-3 border-b border-slate-200 bg-slate-50/50">
          <h2 className="text-sm font-semibold text-slate-800">Sync Triggers</h2>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50">
              {['Name', 'Type', 'Condition', 'Enabled', 'Actions'].map(h => (
                <th key={h} className="text-left px-4 py-2.5 text-xs font-semibold text-slate-600 uppercase tracking-wide">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {[
              { name: 'Auto-sync STP', type: 'ON_STATUS_CHANGE', badge: 'badge-verifying', condition: 'status = STP', enabled: true },
              { name: 'Reserve sync', type: 'ON_FIELD_CHANGE', badge: 'badge-hitl', condition: 'reserveAmount set', enabled: true },
              { name: 'Fraud alert', type: 'ON_FIELD_CHANGE', badge: 'badge-hitl', condition: 'fraudFlag = true', enabled: false },
              { name: 'Batch 15min', type: 'ON_SCHEDULE', badge: 'badge-received', condition: 'every 15 min', enabled: false },
            ].map((row, i) => (
              <tr key={i} className={`border-b border-slate-100 hover:bg-slate-50 transition-colors ${i % 2 === 1 ? 'bg-slate-50/40' : ''}`}>
                <td className="px-4 py-2.5 text-slate-700 text-xs font-medium">{row.name}</td>
                <td className="px-4 py-2.5">
                  <span className={`badge ${row.badge} text-xs`}>{row.type}</span>
                </td>
                <td className="px-4 py-2.5 text-slate-600 text-xs tabular-nums">{row.condition}</td>
                <td className="px-4 py-2.5">
                  <div className="relative inline-block w-10 h-5">
                    <input type="checkbox" defaultChecked={row.enabled} className="sr-only peer" />
                    <div className={`w-10 h-5 rounded-full peer-checked:bg-green-500 ${row.enabled ? 'bg-green-500' : 'bg-slate-300'}`}></div>
                    <div className={`absolute left-0.5 top-0.5 w-4 h-4 bg-white rounded-full transition-all ${row.enabled ? 'translate-x-5' : ''}`}></div>
                  </div>
                </td>
                <td className="px-4 py-2.5">
                  <div className="flex gap-3">
                    <button className="text-xs text-slate-500 hover:text-slate-700">Edit</button>
                    <button className="text-xs text-slate-500 hover:text-slate-700">Pause</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="px-4 py-3 border-t border-slate-200 flex justify-between">
          <button className="px-4 py-2 border border-slate-200 text-slate-600 rounded-sm text-sm font-medium hover:bg-slate-50 transition-colors">
            Test Sandbox
          </button>
          <button className="btn-primary">Save Configuration</button>
        </div>
      </div>
    </div>
  )
}