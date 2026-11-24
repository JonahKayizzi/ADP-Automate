import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AppShell } from '@/app/AppShell'
import { OverviewPage } from '@/features/dashboard/OverviewPage'
import { AuthProvider } from '@/features/auth/AuthContext'
import { AuthGate } from '@/features/auth/AuthGate'
import { UsersPage } from '@/features/users/UsersPage'
import { NotamsPage } from '@/features/notams/NotamsPage'
import { PublicationsPage } from '@/features/publications/PublicationsPage'
import { LibraryPage } from '@/features/library/LibraryPage'
import { ComplaintsPage } from '@/features/complaints/ComplaintsPage'
import { TasksPage } from '@/features/tasks/TasksPage'
import { SearchProvider } from '@/features/search/SearchContext'

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
            <SearchProvider>
              <Routes>
                <Route element={<AppShell />}>
                  <Route index element={<Navigate to="/overview" replace />} />
                  <Route path="overview" element={<OverviewPage />} />
                  <Route path="notams" element={<NotamsPage />} />
                  <Route path="publications" element={<PublicationsPage />} />
                  <Route path="library" element={<LibraryPage />} />
                  <Route path="complaints" element={<ComplaintsPage />} />
                  <Route path="tasks" element={<TasksPage />} />
                  <Route path="users" element={<UsersPage />} />
                </Route>
              </Routes>
            </SearchProvider>
          </AuthGate>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}
