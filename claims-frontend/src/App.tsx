import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Dashboard from './screens/Dashboard'
import Inbox from './screens/Inbox'
import Entities from './screens/Entities'
import Forensics from './screens/Forensics'
import ClaimOverview from './screens/ClaimOverview'
import Duplicates from './screens/Duplicates'
import HITLConsole from './screens/HITLConsole'
import Settings from './screens/Settings'
import Layout from './components/Layout'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="inbox" element={<Inbox />} />
          <Route path="entities" element={<Entities />} />
          <Route path="forensics" element={<Forensics />} />
          <Route path="claim-overview" element={<ClaimOverview />} />
          <Route path="duplicates" element={<Duplicates />} />
          <Route path="hitl-console" element={<HITLConsole />} />
          <Route path="settings" element={<Settings />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}