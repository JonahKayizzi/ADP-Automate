import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AppShell } from '@/app/AppShell'
import { OverviewPage } from '@/features/dashboard/OverviewPage'
import { PlaceholderPage } from '@/app/PlaceholderPage'
import { AuthProvider } from '@/features/auth/AuthContext'
import { AuthGate } from '@/features/auth/AuthGate'
import { UsersPage } from '@/features/users/UsersPage'
import { NotamsPage } from '@/features/notams/NotamsPage'
import { PublicationsPage } from '@/features/publications/PublicationsPage'

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
      <AuthProvider>
        <BrowserRouter>
          <AuthGate>
            <Routes>
              <Route element={<AppShell />}>
                <Route index element={<Navigate to="/overview" replace />} />
                <Route path="overview" element={<OverviewPage />} />
                <Route path="notams" element={<NotamsPage />} />
                <Route path="publications" element={<PublicationsPage />} />
                <Route path="library" element={<PlaceholderPage title="AIS library" />} />
                <Route path="complaints" element={<PlaceholderPage title="Customer complaints log" />} />
                <Route path="tasks" element={<PlaceholderPage title="Task list" />} />
                <Route path="users" element={<UsersPage />} />
              </Route>
            </Routes>
          </AuthGate>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}
