const currentFNOL = {
  claimId: 'CLM-2024-0891',
  vessel: 'MSC Oscar',
  dateOfLoss: '12/11/2024',
  location: 'Rotterdam Port',
  description: 'Hull damage to starboard side aft following allision with moored dumb barge during berthing operations',
  value: 'EUR 2,400,000',
}

const historicalClaims = [
  { id: 'CLM-2023-0542', dateOfLoss: '15/06/2023', vessel: 'MSC Oscar', description: 'Deck machinery damage during cargo operations - Antwerp', similarity: 34 },
  { id: 'CLM-2023-0218', dateOfLoss: '22/03/2023', vessel: 'MSC Oscar', description: 'Propeller fouling incident - Hamburg anchorage', similarity: 28 },
  { id: 'CLM-2022-0891', dateOfLoss: '08/11/2022', vessel: 'MSC Oscar', description: 'Hull scratches at frame 88-92 port side - Rotterdam', similarity: 41 },
  { id: 'CLM-2021-0347', dateOfLoss: '19/04/2021', vessel: 'MSC Oscar', description: 'Engine room flooding - English Channel', similarity: 22 },
  { id: 'CLM-2024-0789', dateOfLoss: '03/10/2024', vessel: 'MSC Oscar', description: 'Shell plate damage starboard bow - Rotterdam Berth 12', similarity: 91 },
]

export default function Duplicates() {
  return (
    <div className="h-full flex flex-col space-y-4">
      <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-2 px-1">Duplicate Detection</div>
      
      <div className="h-[40%] card border border-slate-200 p-4">
        <div className="flex items-center gap-2 mb-3">
          <span className="badge bg-blue-100 text-blue-700">Current FNOL</span>
        </div>
        <div className="grid grid-cols-6 gap-4">
          <div>
            <div className="text-xs text-slate-500 uppercase tracking-wide mb-1">Claim ID</div>
            <div className="text-sm font-semibold text-brand-primary tabular-nums">{currentFNOL.claimId}</div>
          </div>
          <div>
            <div className="text-xs text-slate-500 uppercase tracking-wide mb-1">Vessel</div>
            <div className="text-sm font-semibold text-slate-900">{currentFNOL.vessel}</div>
          </div>
          <div>
            <div className="text-xs text-slate-500 uppercase tracking-wide mb-1">Date of Loss</div>
            <div className="text-sm font-medium text-slate-700 tabular-nums">{currentFNOL.dateOfLoss}</div>
          </div>
          <div>
            <div className="text-xs text-slate-500 uppercase tracking-wide mb-1">Location</div>
            <div className="text-sm font-medium text-slate-700">{currentFNOL.location}</div>
          </div>
          <div className="col-span-2">
            <div className="text-xs text-slate-500 uppercase tracking-wide mb-1">Description</div>
            <div className="text-sm text-slate-700 leading-tight">{currentFNOL.description}</div>
          </div>
        </div>
        <div className="mt-3 pt-3 border-t border-slate-200 flex items-center gap-4">
          <div>
            <div className="text-xs text-slate-500 uppercase tracking-wide mb-1">Estimated Value</div>
            <div className="text-lg font-bold text-slate-900 tabular-nums">{currentFNOL.value}</div>
          </div>
        </div>
      </div>

      <div className="flex-1 min-h-0 card border border-slate-200 flex flex-col">
        <div className="px-4 py-3 border-b border-slate-200 bg-slate-50/50">
          <h2 className="text-sm font-semibold text-slate-800">Historical Claims - Potential Duplicates</h2>
        </div>
        <div className="flex-1 overflow-y-auto">
          <table className="w-full text-sm">
            <thead className="sticky top-0 bg-slate-50">
              <tr className="border-b border-slate-200">
                {['Claim ID', 'Date of Loss', 'Vessel', 'Description', 'Similarity', 'Actions'].map(h => (
                  <th key={h} className="text-left px-4 py-2.5 text-xs font-semibold text-slate-600 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {historicalClaims.map((claim) => (
                <tr 
                  key={claim.id} 
                  className={`border-b border-slate-100 transition-colors ${
                    claim.similarity >= 90 
                      ? 'bg-amber-50 hover:bg-amber-100' 
                      : 'hover:bg-slate-50'
                  }`}
                >
                  <td className="px-4 py-2.5 font-medium text-brand-primary tabular-nums text-xs">{claim.id}</td>
                  <td className="px-4 py-2.5 text-slate-600 tabular-nums text-xs">{claim.dateOfLoss}</td>
                  <td className="px-4 py-2.5 text-slate-900 font-semibold text-xs">{claim.vessel}</td>
                  <td className="px-4 py-2.5 text-slate-600 text-xs max-w-xs truncate">{claim.description}</td>
                  <td className="px-4 py-2.5">
                    <span className={`text-xs font-semibold tabular-nums ${claim.similarity >= 90 ? 'text-amber-600' : 'text-slate-500'}`}>
                      {claim.similarity}%
                    </span>
                  </td>
                  <td className="px-4 py-2.5">
                    <div className="flex gap-2">
                      <button className="text-xs font-medium text-brand-primary border border-brand-primary px-2 py-1 rounded-sm hover:bg-brand-primary hover:text-white transition-colors">
                        Link as Subsequent Report
                      </button>
                      <button className="text-xs font-medium text-slate-500 border border-slate-300 px-2 py-1 rounded-sm hover:bg-slate-100 transition-colors">
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