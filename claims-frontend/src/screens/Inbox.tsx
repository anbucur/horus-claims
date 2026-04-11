import { useState } from 'react'

const mockEmails = [
  {
    id: 1,
    sender: 'j.visser@marsh.com',
    senderInitial: 'JV',
    subject: 'FNOL Notification - MSC Oscar - Hull Damage - 12 Nov 2024',
    timestamp: '08:32',
    hasAttachment: true,
    attachmentName: 'SurveyorReport.pdf',
    tag: 'FNOL',
    unread: true,
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
    senderInitial: 'LS',
    subject: 'Re: Survey Appointment - MSC Oscar - 14 Nov 2024',
    timestamp: '07:15',
    hasAttachment: true,
    attachmentName: 'PreliminarySurvey.pdf',
    tag: 'EVIDENCE',
    unread: false,
    body: `Dear Colleagues,

Confirming our appointment to conduct a preliminary survey of the MSC Oscar at Rotterdam on 14 November 2024 at 0900 hours.

Full report to follow within 48 hours.

Kind regards,
H. van der Berg
Lloyd's Surveyor`,
  },
  {
    id: 3,
    sender: 'claims@oceantech-shipping.com',
    senderInitial: 'OT',
    subject: 'Loss Notice - Hull & Machinery - MSC Oscar',
    timestamp: '16:45',
    hasAttachment: false,
    attachmentName: null,
    tag: 'FNOL',
    unread: false,
    body: `To Whom It May Concern,

This serves as formal loss notice for the above-referenced vessel under the Hull & Machinery policy held with MSIG.

Policy Number: MSC-HULL-2024-8891
Date of Loss: 12 November 2024
Location: Rotterdam Port, Netherlands

Regards,
OceanTech Shipping Ltd`,
  },
]

const tagColor: Record<string, string> = {
  FNOL:     'text-md-primary border-md-primary/30',
  EVIDENCE: 'text-status-warning border-status-warning/30',
  REPLY:    'text-status-verified border-status-verified/30',
}

export default function Inbox() {
  const [selected, setSelected] = useState(mockEmails[0])

  return (
    <div className="h-full flex gap-4">
      {/* ── Thread list ─── */}
      <div className="w-[320px] shrink-0 flex flex-col gap-1 overflow-y-auto">
        {/* Search */}
        <div className="relative mb-2">
          <svg className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-md-on-surface-var/50" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input className="input-field pl-9 text-xs" placeholder="Search emails…" />
        </div>

        {mockEmails.map((email) => (
          <button
            key={email.id}
            onClick={() => setSelected(email)}
            className={`w-full text-left p-3 rounded-lg transition-all duration-150 ${
              selected.id === email.id
                ? 'bg-md-primary-container/20 border border-md-primary/20'
                : 'hover:bg-md-container-high border border-transparent'
            }`}
          >
            <div className="flex items-start gap-2.5">
              {/* Avatar */}
              <div className="w-7 h-7 rounded-full bg-md-container-high flex items-center justify-center text-[11px] font-bold text-md-primary shrink-0">
                {email.senderInitial}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-1 mb-0.5">
                  <span className={`text-xs font-semibold truncate ${email.unread ? 'text-md-on-surface' : 'text-md-on-surface-var'}`}>
                    {email.sender.split('@')[0].replace('.', ' ')}
                  </span>
                  <span className="text-[10px] text-md-on-surface-var/50 tabular-nums shrink-0">{email.timestamp}</span>
                </div>
                <div className={`text-[11px] leading-snug mb-1 line-clamp-2 ${email.unread ? 'text-md-on-surface' : 'text-md-on-surface-var'}`}>
                  {email.subject}
                </div>
                <div className="flex items-center gap-1.5">
                  <span className={`text-[9px] font-semibold uppercase tracking-wide px-1.5 py-0.5 rounded border ${tagColor[email.tag]}`}>
                    {email.tag}
                  </span>
                  {email.hasAttachment && (
                    <svg className="w-3 h-3 text-md-on-surface-var/40" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" />
                    </svg>
                  )}
                  {email.unread && <span className="ml-auto w-1.5 h-1.5 rounded-full bg-md-primary" />}
                </div>
              </div>
            </div>
          </button>
        ))}
      </div>

      {/* ── Email viewer ─── */}
      <div className="flex-1 min-w-0 card p-0 flex flex-col overflow-hidden">
        {/* Email header */}
        <div
          className="px-5 py-4 shrink-0"
          style={{ background: '#191b22', borderBottom: '1px solid rgba(70,69,85,0.3)' }}
        >
          <div className="flex items-start justify-between gap-4 mb-3">
            <div>
              <h2 className="text-sm font-semibold text-md-on-surface leading-snug">{selected.subject}</h2>
              <div className="text-xs text-md-on-surface-var mt-1">
                From: <span className="text-md-on-surface">{selected.sender}</span> · {selected.timestamp}
              </div>
            </div>
            <div className="flex gap-2 shrink-0">
              <button className="btn-primary text-xs px-3 py-1.5">Extract to Claim</button>
            </div>
          </div>

          {/* Linked claim chip */}
          <div className="flex items-center gap-2">
            <span className="badge badge-hitl">Linked: CLM-2024-08891</span>
            {selected.hasAttachment && selected.attachmentName && (
              <span className="flex items-center gap-1.5 text-[11px] text-md-on-surface-var px-2.5 py-1 rounded-md bg-md-container">
                <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                </svg>
                {selected.attachmentName}
              </span>
            )}
          </div>
        </div>

        {/* Email body */}
        <div className="flex-1 overflow-y-auto p-5">
          <pre className="text-sm text-md-on-surface leading-relaxed whitespace-pre-wrap font-sans">
            {selected.body}
          </pre>
        </div>

        {/* Reply composer */}
        <div
          className="p-4 shrink-0"
          style={{ borderTop: '1px solid rgba(70,69,85,0.3)' }}
        >
          <textarea
            className="input-field h-16 resize-none text-xs mb-2"
            placeholder="Reply to sender…"
          />
          <div className="flex items-center justify-end gap-2">
            <button className="btn-ghost text-xs px-3 py-1.5">Force Alternate Route</button>
            <button className="btn-primary text-xs px-4 py-1.5">Confirm &amp; Continue</button>
          </div>
        </div>
      </div>
    </div>
  )
}
