export default function Forensics() {
  return (
    <div className="h-full flex flex-col space-y-4">
      <div className="flex gap-4 flex-1 min-h-0">
        <div className="w-[70%] relative">
          <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-2 px-1">Image Analysis</div>
          <div className="card border border-slate-200 h-full relative overflow-hidden">
            <div className="absolute inset-0 bg-gradient-to-br from-slate-700 via-slate-600 to-slate-800 flex flex-col items-center justify-center">
              <svg className="w-16 h-16 text-slate-400 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6.827 6.175A2.31 2.31 0 015.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 00-1.134-.175 2.31 2.31 0 01-1.64-1.055l-.822-1.316a2.192 2.192 0 00-1.736-1.039 48.774 48.774 0 00-5.232 0 2.192 2.192 0 00-1.736 1.039l-.821 1.316z" />
                <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 12.75a4.5 4.5 0 11-9 0 4.5 4.5 0 019 0zM18.75 10.5h.008v.008h-.008V10.5z" />
              </svg>
              <div className="text-white font-semibold text-lg">Hull Damage - MSC Oscar</div>
              <div className="text-slate-400 text-sm mt-1">Frame 142-147, Starboard Side Aft</div>
              <div className="text-slate-500 text-xs mt-4">Timestamp: 2024-11-12 06:15 UTC</div>
            </div>

            <div className="absolute top-3 left-3 bg-red-600 text-white px-3 py-1.5 rounded-sm text-xs font-semibold shadow-lg">
              <div className="flex items-center gap-2">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                Digital Manipulation Detected - 34% pixels flagged
              </div>
            </div>

            <div className="absolute bottom-3 left-3 right-3 bg-black/60 backdrop-blur-sm text-white p-3 rounded-sm">
              <div className="flex items-center justify-between">
                <div className="text-xs">
                  <span className="text-slate-400">Metadata:</span> <span className="tabular-nums">EXIF Date matches loss date</span>
                </div>
                <div className="text-xs">
                  <span className="text-slate-400">GPS:</span> <span className="tabular-nums">52.3676°N, 4.9041°E (Rotterdam)</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="w-[30%] flex flex-col space-y-4">
          <div className="card border border-red-200 bg-red-50/50 p-4">
            <div className="text-xs font-semibold text-red-700 uppercase tracking-wide mb-2">Authenticity Score</div>
            <div className="text-5xl font-bold text-red-600 tabular-nums">12%</div>
            <div className="text-xs text-red-600 mt-1">High Risk - Further Review Required</div>
          </div>

          <div className="card border border-slate-200 p-4 flex-1">
            <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-3">Narrative Consistency</div>
            <div className="space-y-3">
              <div className="p-2 bg-slate-50 rounded-sm">
                <div className="text-xs text-slate-500 uppercase tracking-wide mb-1">Extracted Text</div>
                <div className="text-xs text-slate-700">"Hull damage starboard side aft during berthing"</div>
              </div>
              <div className="flex items-center justify-center">
                <svg className="w-4 h-4 text-amber-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M18.5 14.25l-4.5-4.5-4.5 4.5M18.5 14.25l-4.5 4.5-4.5-4.5" />
                </svg>
              </div>
              <div className="p-2 bg-red-50 rounded-sm border border-red-200">
                <div className="text-xs text-red-600 uppercase tracking-wide mb-1">AI Classification</div>
                <div className="text-xs text-red-700">"Pre-existing damage pattern inconsistent with allision claim"</div>
              </div>
            </div>

            <div className="mt-4 pt-4 border-t border-slate-200">
              <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-3">Analysis Options</div>
              <div className="space-y-2">
                <label className="flex items-center justify-between text-xs">
                  <span className="text-slate-600">Show Heatmap Overlay</span>
                  <input type="checkbox" className="rounded border-slate-300" />
                </label>
                <label className="flex items-center justify-between text-xs">
                  <span className="text-slate-600">Edge Detection</span>
                  <input type="checkbox" className="rounded border-slate-300" />
                </label>
                <label className="flex items-center justify-between text-xs">
                  <span className="text-slate-600">Metadata Verification</span>
                  <input type="checkbox" className="rounded border-slate-300" checked />
                </label>
              </div>
            </div>
          </div>

          <button className="w-full bg-red-600 text-white px-4 py-3 rounded-sm text-sm font-semibold hover:bg-red-700 transition-colors flex items-center justify-center gap-2">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            Route to SIU
          </button>
        </div>
      </div>
    </div>
  )
}