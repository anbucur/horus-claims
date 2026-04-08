const extractedEntities = [
  { type: 'Assured', value: 'OceanTech Shipping Ltd' },
  { type: 'Broker', value: 'Marsh Ltd' },
  { type: 'Vessel', value: 'MSC Oscar' },
]

const proposedMatches = [
  { id: 1, name: 'OceanTech Shipping Ltd', location: 'Singapore', status: 'Active', score: 94 },
  { id: 2, name: 'Marsh & McLennan Companies', location: 'New York, NY', status: 'Active', score: 87 },
  { id: 3, name: 'Mediterranean Shipping Co', location: 'Geneva, Switzerland', status: 'Active', score: 99 },
]

export default function Entities() {
  return (
    <div className="h-full flex flex-col space-y-4">
      <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-2 px-1">Entity Matching Engine</div>
      
      <div className="flex gap-4 flex-1 min-h-0">
        <div className="w-1/3 card border border-slate-200 p-4">
          <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-3 px-1">Extracted Entities</div>
          <div className="space-y-2">
            {extractedEntities.map((entity) => (
              <div key={entity.type} className="p-3 bg-slate-50 border border-slate-200 rounded-sm">
                <div className="text-xs text-slate-500 uppercase tracking-wide mb-1">{entity.type}</div>
                <div className="text-sm font-semibold text-slate-900">{entity.value}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="w-1/3 card border border-slate-200 p-4">
          <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-3 px-1">Proposed Matches</div>
          <div className="relative mb-3">
            <input
              type="text"
              placeholder="Search entities..."
              className="w-full px-3 py-2 text-xs border border-slate-200 rounded-sm focus:outline-none focus:border-brand-primary"
            />
            <svg className="w-4 h-4 text-slate-400 absolute right-3 top-1/2 -translate-y-1/2" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <div className="space-y-2">
            {proposedMatches.map((match) => (
              <div key={match.id} className="p-3 border border-slate-200 rounded-sm hover:border-brand-primary hover:bg-slate-50/50 transition-colors">
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <div className="text-sm font-semibold text-slate-900">{match.name}</div>
                    <div className="text-xs text-slate-500">{match.location}</div>
                  </div>
                </div>
                <button className="w-full mt-2 px-3 py-1.5 text-xs font-medium text-brand-primary border border-brand-primary rounded-sm hover:bg-brand-primary hover:text-white transition-colors">
                  Accept Match
                </button>
              </div>
            ))}
          </div>
        </div>

        <div className="w-1/3 card border border-slate-200 p-4">
          <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-3 px-1">Match Confidence</div>
          <div className="space-y-3">
            <div className="flex items-center justify-between p-3 bg-green-50 border border-green-200 rounded-sm">
              <div>
                <div className="text-xs text-slate-500">OceanTech Shipping</div>
                <div className="text-sm font-semibold text-slate-900">vs DB Record</div>
              </div>
              <div className="text-xl font-bold text-green-600 tabular-nums">94%</div>
            </div>
            <div className="flex items-center justify-between p-3 bg-amber-50 border border-amber-200 rounded-sm">
              <div>
                <div className="text-xs text-slate-500">Marsh Ltd</div>
                <div className="text-sm font-semibold text-slate-900">vs DB Record</div>
              </div>
              <div className="text-xl font-bold text-amber-600 tabular-nums">87%</div>
            </div>
            <div className="flex items-center justify-between p-3 bg-green-50 border border-green-200 rounded-sm">
              <div>
                <div className="text-xs text-slate-500">MSC Oscar</div>
                <div className="text-sm font-semibold text-slate-900">vs DB Record</div>
              </div>
              <div className="text-xl font-bold text-green-600 tabular-nums">99%</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}