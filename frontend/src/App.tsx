import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import Layout from "./components/Layout";
import SharesPage from "./pages/shares/SharesPage";
import UsersPage from "./pages/users/UsersPage";
import ConfigPage from "./pages/config/ConfigPage";

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
          <Route path="/users" element={<UsersPage />} />
          <Route path="/config" element={<ConfigPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
