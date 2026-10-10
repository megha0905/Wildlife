import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { Layout } from './components/Layout';
import { Loading } from './components/ui';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { AnimalsPage } from './pages/AnimalsPage';
import { ZonesPage } from './pages/ZonesPage';
import { PatrolsPage } from './pages/PatrolsPage';
import { IncidentsPage } from './pages/IncidentsPage';
import { ZoneMonthlyPage } from './pages/ZoneMonthlyPage';
import { RescuesPage } from './pages/RescuesPage';
import { ResourcesPage } from './pages/ResourcesPage';
import { ProgrammesPage } from './pages/ProgrammesPage';
import { ReportsPage } from './pages/ReportsPage';
import { UsersPage } from './pages/UsersPage';

export default function App() {
  const { user, loading, hasRole } = useAuth();
  if (loading) return <Loading text="Restoring your session…" />;

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      {user ? (
        <Route element={<Layout />}>
          <Route index element={<DashboardPage />} />
          <Route path="animals" element={<AnimalsPage />} />
          <Route path="zones" element={<ZonesPage />} />
          <Route path="patrols" element={<PatrolsPage />} />
          <Route path="incidents" element={<IncidentsPage />} />
          <Route path="incidents/monthly" element={<ZoneMonthlyPage />} />
          <Route path="rescues" element={<RescuesPage />} />
          <Route path="resources" element={<ResourcesPage />} />
          <Route path="programmes" element={<ProgrammesPage />} />
          <Route path="reports" element={<ReportsPage />} />
          <Route path="users" element={hasRole('ADMIN') ? <UsersPage /> : <Navigate to="/" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      ) : (
        <Route path="*" element={<Navigate to="/login" replace />} />
      )}
    </Routes>
  );
}
