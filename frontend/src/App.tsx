import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AppShell } from '@/app/AppShell'
import { OverviewPage } from '@/features/dashboard/OverviewPage'
import { PlaceholderPage } from '@/app/PlaceholderPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: true,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<AppShell />}>
            <Route index element={<Navigate to="/overview" replace />} />
            <Route path="overview" element={<OverviewPage />} />
            <Route path="notams" element={<PlaceholderPage title="NOTAM publication log" />} />
            <Route path="publications" element={<PlaceholderPage title="AIP and AIC publications" />} />
            <Route path="library" element={<PlaceholderPage title="AIS library" />} />
            <Route path="complaints" element={<PlaceholderPage title="Customer complaints log" />} />
            <Route path="tasks" element={<PlaceholderPage title="Task list" />} />
            <Route path="users" element={<PlaceholderPage title="Users" />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
