export default function ClaimOverview() {
  return (
    <div className="h-full flex flex-col space-y-4">
      <div className="flex gap-4 flex-1 min-h-0">
        <div className="w-1/2">
          <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-2 px-1">Extracted FNOL</div>
          <div className="card border border-slate-200 p-4">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <span className="text-xs text-slate-500 uppercase tracking-wide">Date of Loss</span>
                <span className="badge bg-amber-100 text-amber-700">Low Confidence 71%</span>
              </div>
              <button className="text-slate-400 hover:text-slate-600">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                </svg>
              </button>
            </div>
            <div className="border-2 border-amber-300 rounded-sm p-3 mb-4 bg-amber-50/50">
              <span className="text-sm font-semibold text-slate-900 tabular-nums">12/11/2024</span>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between py-2 border-b border-slate-100">
                <div className="flex items-center gap-2">
                  <span className="text-xs text-slate-500 uppercase tracking-wide">Location</span>
                  <button className="text-slate-400 hover:text-slate-600">
                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                    </svg>
                  </button>
                </div>
                <span className="text-sm font-medium text-slate-900">Rotterdam Port</span>
              </div>

              <div className="py-2 border-b border-slate-100">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs text-slate-500 uppercase tracking-wide">Incident Description</span>
                  <button className="text-slate-400 hover:text-slate-600">
                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                    </svg>
                  </button>
                </div>
                <p className="text-sm text-slate-700 leading-relaxed">Hull damage to starboard side aft following allision with moored dumb barge during berthing operations. Shell plating indentation 4.2m x 1.8m at frame 142-147.</p>
              </div>

              <div className="flex items-center justify-between py-2 border-b border-slate-100">
                <div className="flex items-center gap-2">
                  <span className="text-xs text-slate-500 uppercase tracking-wide">Estimated Value</span>
                  <button className="text-slate-400 hover:text-slate-600">
                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                    </svg>
                  </button>
                </div>
                <span className="text-sm font-semibold text-slate-900 tabular-nums">EUR 2,400,000</span>
              </div>
            </div>
          </div>
        </div>

        <div className="w-1/2">
          <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-2 px-1">Policy Verification</div>
          <div className="card border border-slate-200 p-4 bg-green-50/30">
            <div className="flex items-center gap-2 mb-4">
              <svg className="w-5 h-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span className="text-xs font-semibold text-green-700 uppercase tracking-wide">Verified Active Policy</span>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between py-2 border-b border-green-100">
                <span className="text-xs text-slate-500 uppercase tracking-wide">Policy Number</span>
                <span className="text-sm font-semibold text-slate-900 tabular-nums">MSC-HULL-2024-8891</span>
              </div>

              <div className="flex items-center justify-between py-2 border-b border-green-100">
                <span className="text-xs text-slate-500 uppercase tracking-wide">Status</span>
                <span className="badge bg-green-100 text-green-700">Active</span>
              </div>

              <div className="flex items-center justify-between py-2 border-b border-green-100">
                <span className="text-xs text-slate-500 uppercase tracking-wide">Coverage</span>
                <span className="text-sm font-semibold text-slate-900">Hull & Machinery</span>
              </div>

              <div className="flex items-center justify-between py-2 border-b border-green-100">
                <span className="text-xs text-slate-500 uppercase tracking-wide">Sum Insured</span>
                <span className="text-sm font-semibold text-slate-900 tabular-nums">EUR 15,000,000</span>
              </div>

              <div className="flex items-center justify-between py-2 border-b border-green-100">
                <span className="text-xs text-slate-500 uppercase tracking-wide">Deductible</span>
                <span className="text-sm font-semibold text-slate-900 tabular-nums">EUR 50,000</span>
              </div>

              <div className="flex items-center justify-between py-2">
                <span className="text-xs text-slate-500 uppercase tracking-wide">Policy Period</span>
                <span className="text-sm font-medium text-slate-700 tabular-nums">01/01/2024 - 31/12/2024</span>
              </div>
            </div>

            <div className="mt-4 pt-4 border-t border-green-200">
              <div className="flex items-center justify-between">
                <span className="text-xs text-green-600">Policy matches loss date</span>
                <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-center py-2">
        <svg className="w-48 h-8" viewBox="0 0 192 32">
          <line x1="0" y1="16" x2="70" y2="16" stroke="#94a3b8" strokeWidth="2" strokeDasharray="4 2" />
          <circle cx="80" cy="16" r="6" fill="#f59e0b" stroke="#fbbf24" strokeWidth="2" />
          <line x1="96" y1="16" x2="192" y2="16" stroke="#94a3b8" strokeWidth="2" strokeDasharray="4 2" />
        </svg>
      </div>

      <div className="flex items-center justify-end gap-3 pt-2 border-t border-slate-200">
        <button className="px-4 py-2 rounded-sm text-sm font-medium border border-slate-300 text-slate-700 bg-white hover:bg-slate-50 transition-colors">
          Override Low Confidence
        </button>
        <button className="px-4 py-2 rounded-sm text-sm font-medium border border-slate-300 text-slate-700 bg-white hover:bg-slate-50 transition-colors">
          Request More Info
        </button>
        <button className="px-4 py-2 rounded-sm text-sm font-medium border border-slate-300 text-slate-700 bg-white hover:bg-slate-50 transition-colors">
          Route to HITL
        </button>
        <button className="btn-primary px-6 py-2">
          Approve STP
        </button>
      </div>
    </div>
  )
}