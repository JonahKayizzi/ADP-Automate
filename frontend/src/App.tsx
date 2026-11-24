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
import { AuditPage } from '@/features/audit/AuditPage'
import { SearchProvider } from '@/features/search/SearchContext'
import { ThemeProvider } from '@/features/theme/ThemeContext'

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
      <ThemeProvider>
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
                    <Route path="audit" element={<AuditPage />} />
                    <Route path="users" element={<UsersPage />} />
                  </Route>
                </Routes>
              </SearchProvider>
            </AuthGate>
          </BrowserRouter>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  )
}
