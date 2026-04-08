const steps = [
  { label: 'Email Ingest', status: 'complete' },
  { label: 'AI Extract', status: 'complete' },
  { label: 'Policy Verify', status: 'complete' },
  { label: 'Entity Match', status: 'complete' },
  { label: 'Image Forensics', status: 'current' },
  { label: 'Duplicate Check', status: 'pending' },
  { label: 'Final Review', status: 'pending' },
]

export default function HITLConsole() {
  return (
    <div className="h-full flex flex-col space-y-4">
      <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-2 px-1">HITL Console</div>

      <div className="card border border-slate-200 p-4">
        <div className="flex items-center justify-between mb-4">
          <span className="text-sm font-semibold text-slate-800">Processing Pipeline</span>
          <span className="badge bg-amber-100 text-amber-700">Step 5 of 7</span>
        </div>
        <div className="flex items-center">
          {steps.map((step, index) => (
            <div key={step.label} className="flex items-center">
              <div className="flex flex-col items-center">
                <div 
                  className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-semibold ${
                    step.status === 'complete' 
                      ? 'bg-green-500 text-white' 
                      : step.status === 'current'
                        ? 'bg-amber-400 text-white ring-4 ring-amber-100'
                        : 'bg-slate-200 text-slate-500'
                  }`}
                >
                  {step.status === 'complete' ? (
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  ) : (
                    index + 1
                  )}
                </div>
                <span className={`text-xs mt-1.5 ${step.status === 'current' ? 'text-amber-600 font-medium' : 'text-slate-500'}`}>
                  {step.label}
                </span>
              </div>
              {index < steps.length - 1 && (
                <div className={`w-12 h-0.5 mx-1 ${step.status === 'complete' ? 'bg-green-400' : 'bg-slate-200'}`} />
              )}
            </div>
          ))}
        </div>
      </div>

      <div className="flex-1 card border border-slate-200 p-4">
        <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-3 px-1">Adjuster Review</div>
        <div className="space-y-4">
          <div>
            <label className="text-xs text-slate-500 uppercase tracking-wide mb-1.5 block">Adjuster Notes</label>
            <textarea 
              className="w-full h-24 px-3 py-2 text-sm border border-slate-200 rounded-sm focus:outline-none focus:border-brand-primary resize-none"
              placeholder="Enter review notes and observations..."
            />
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="text-xs text-slate-500 uppercase tracking-wide mb-1.5 block">Reserve Amount</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-slate-500">EUR</span>
                <input 
                  type="text" 
                  defaultValue="2,400,000"
                  className="w-full pl-12 pr-3 py-2 text-sm border-2 border-amber-300 rounded-sm focus:outline-none focus:border-amber-400 bg-amber-50 tabular-nums"
                />
              </div>
            </div>
            <div>
              <label className="text-xs text-slate-500 uppercase tracking-wide mb-1.5 block">Deductible</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-slate-500">EUR</span>
                <input 
                  type="text" 
                  defaultValue="50,000"
                  className="w-full pl-12 pr-3 py-2 text-sm border border-green-300 rounded-sm focus:outline-none focus:border-green-400 bg-green-50 tabular-nums"
                />
              </div>
            </div>
            <div>
              <label className="text-xs text-slate-500 uppercase tracking-wide mb-1.5 block">Payout Recommendation</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-slate-500">EUR</span>
                <input 
                  type="text" 
                  defaultValue="2,350,000"
                  className="w-full pl-12 pr-3 py-2 text-sm border border-slate-200 rounded-sm focus:outline-none focus:border-brand-primary tabular-nums"
                />
              </div>
            </div>
          </div>

          <div className="pt-4 border-t border-slate-200">
            <div className="flex items-center justify-between">
              <div className="text-xs text-slate-500">
                <span className="font-medium text-slate-700">Net Claim Value:</span> EUR 2,350,000
              </div>
              <div className="flex gap-3">
                <button className="px-4 py-2 rounded-sm text-sm font-medium border border-slate-300 text-slate-700 bg-white hover:bg-slate-50 transition-colors">
                  Request More Information
                </button>
                <button className="btn-primary px-6 py-2.5 text-base">
                  Approve & Commit to Core
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}