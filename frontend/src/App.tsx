import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import Layout from "./components/Layout";
import PlaceholderPage from "./pages/PlaceholderPage";
import SharesPage from "./pages/shares/SharesPage";

function App() {
  return (
    <BrowserRouter basename="/ui">
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        
        {/* Защищенные роуты с боковой панелью */}
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/shares" element={<SharesPage />} />
          <Route 
            path="/users" 
            element={<PlaceholderPage title="Пользователи" description="Управление пользователями и правами" />} 
          />
          <Route 
            path="/config" 
            element={<PlaceholderPage title="Настройки (smb.conf)" description="Глобальная конфигурация Samba" />} 
          />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
