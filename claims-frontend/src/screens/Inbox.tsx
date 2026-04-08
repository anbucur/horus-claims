const mockEmails = [
  {
    id: 1,
    sender: 'j.visser@marsh.com',
    subject: 'FNOL Notification - MSC Oscar - Hull Damage - 12 Nov 2024',
    timestamp: '2024-11-14 08:32',
    hasAttachment: true,
    attachmentName: 'SurveyorReport.pdf',
    body: `Dear Claims Team,

We write to notify you of a hull damage incident involving the vessel MSC Oscar (IMO 9457281) occurring on 12 November 2024 at approximately 0600 UTC while the vessel was berthed at Rotterdam Port, Berth 7.

Incident Description:
During berthing operations, the vessel sustained hull damage to the starboard side aft following an allision with a moored dumb barge. The damage comprises a 4.2m x 1.8m indentation to shell plating at frame 142-147, including three bent frames and minor paint removal.

The local Port Authority has been notified and an investigation is underway. A Lloyd's approved surveyor has been appointed and is expected on site by 1400 local time today.

Estimated Hull Damage: EUR 2,400,000

We request your immediate attention to this matter and look forward to your confirmation of cover.

Best regards,
J. Visser
Marine Claims Handler
Marsh Ltd`,
  },
  {
    id: 2,
    sender: 'surveyor@lloyd-surveyors.nl',
    subject: 'Re: Survey Appointment - MSC Oscar - 14 Nov 2024',
    timestamp: '2024-11-14 07:15',
    hasAttachment: true,
    attachmentName: 'PreliminarySurvey.pdf',
    body: `Dear Colleagues,

Confirming our appointment to conduct a preliminary survey of the MSC Oscar at Rotterdam on 14 November 2024 at 0900 hours.

We will focus on:
- Initial damage assessment
- Cause and circumstances documentation
- Preliminary repair cost estimation

Full report to follow within 48 hours.

Kind regards,
H. van der Berg
Lloyd's Surveyor`,
  },
  {
    id: 3,
    sender: 'claims@oceantech-shipping.com',
    subject: 'Loss Notice - Hull & Machinery - MSC Oscar',
    timestamp: '2024-11-13 16:45',
    hasAttachment: false,
    attachmentName: null,
    body: `To Whom It May Concern,

This serves as formal loss notice for the above-referenced vessel under the Hull & Machinery policy held with MSIG.

Policy Number: MSC-HULL-2024-8891
Date of Loss: 12 November 2024
Location: Rotterdam Port, Netherlands

Nature of Loss: Hull damage sustained during berthing operations

We appoint Marsh Ltd as our brokers for this claim and request that all communications be directed through them.

Regards,
OceanTech Shipping Ltd
Marine Claims Department`,
  },
]

export default function Inbox() {
  return (
    <div className="h-full flex flex-col space-y-4">
      <div className="flex gap-4 flex-1 min-h-0">
        <div className="w-2/5 flex flex-col min-h-0">
          <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-2 px-1">Email Inbox</div>
          <div className="flex-1 overflow-y-auto space-y-2">
            {mockEmails.map((email) => (
              <div key={email.id} className="card border border-slate-200 p-3 hover:border-brand-primary hover:shadow-sm transition-all cursor-pointer">
                <div className="flex items-start justify-between mb-1.5">
                  <div className="text-xs font-medium text-slate-800 truncate flex-1">{email.sender}</div>
                  <div className="text-xs text-slate-400 tabular-nums ml-2 shrink-0">{email.timestamp}</div>
                </div>
                <div className="text-xs font-semibold text-slate-900 mb-1.5 leading-tight">{email.subject}</div>
                {email.hasAttachment && (
                  <div className="flex items-center gap-1 text-xs text-amber-600">
                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                    </svg>
                    <span className="truncate">{email.attachmentName}</span>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        <div className="w-3/5 flex flex-col min-h-0">
          <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-2 px-1">AI Extraction Results</div>
          <div className="card border border-slate-200 p-4 flex-1 overflow-y-auto">
            <div className="flex items-center gap-3 mb-4">
              <span className="badge bg-green-100 text-green-700">FNOL - Hull & Machinery</span>
              <span className="text-xs text-slate-500">AI Confidence:</span>
              <span className="text-sm font-bold text-green-600 tabular-nums">98%</span>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-3">
                <div className="flex justify-between items-center py-2 border-b border-slate-100">
                  <span className="text-xs text-slate-500 uppercase tracking-wide">Policy</span>
                  <span className="text-sm font-semibold text-slate-900 tabular-nums">MSC-HULL-2024-8891</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b border-slate-100">
                  <span className="text-xs text-slate-500 uppercase tracking-wide">Date of Loss</span>
                  <span className="text-sm font-semibold text-slate-900 tabular-nums">12/11/2024</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b border-slate-100">
                  <span className="text-xs text-slate-500 uppercase tracking-wide">Location</span>
                  <span className="text-sm font-semibold text-slate-900">Rotterdam</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b border-slate-100">
                  <span className="text-xs text-slate-500 uppercase tracking-wide">Estimated Value</span>
                  <span className="text-sm font-semibold text-slate-900 tabular-nums">EUR 2.4M</span>
                </div>
              </div>
              <div className="space-y-3">
                <div className="flex justify-between items-center py-2 border-b border-slate-100">
                  <span className="text-xs text-slate-500 uppercase tracking-wide">Vessel</span>
                  <span className="text-sm font-semibold text-slate-900">MSC Oscar</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b border-slate-100">
                  <span className="text-xs text-slate-500 uppercase tracking-wide">Insured</span>
                  <span className="text-sm font-semibold text-slate-900">OceanTech Shipping Ltd</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b border-slate-100">
                  <span className="text-xs text-slate-500 uppercase tracking-wide">Broker</span>
                  <span className="text-sm font-semibold text-slate-900">Marsh Ltd</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b border-slate-100">
                  <span className="text-xs text-slate-500 uppercase tracking-wide">Cause</span>
                  <span className="text-sm font-semibold text-slate-900">Allision</span>
                </div>
              </div>
            </div>

            <div className="mt-4 pt-4 border-t border-slate-200">
              <div className="text-xs text-slate-500 uppercase tracking-wide mb-2">Email Body Preview</div>
              <div className="text-xs text-slate-600 leading-relaxed bg-slate-50 p-3 rounded-sm max-h-32 overflow-y-auto whitespace-pre-line">
                {mockEmails[0].body}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-end gap-3 pt-2 border-t border-slate-200">
        <button className="btn-secondary border border-cyan-600 text-cyan-600 bg-white hover:bg-slate-50 px-4 py-2 rounded-sm text-sm font-medium">
          Force Alternate Route
        </button>
        <button className="btn-primary px-6 py-2">
          Confirm & Continue
        </button>
      </div>
    </div>
  )
}