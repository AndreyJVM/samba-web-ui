import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";

function App() {
  return (
    <BrowserRouter basename="/ui">
      <Routes>
        {/* Базовый роут пока редиректит на логин */}
        <Route path="/" element={<Navigate to="/login" replace />} />
        
        {/* Роут логина */}
        <Route path="/login" element={<LoginPage />} />
        
        {/* Роут дашборда (позже добавим проверку авторизации) */}
        <Route path="/dashboard" element={<DashboardPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
