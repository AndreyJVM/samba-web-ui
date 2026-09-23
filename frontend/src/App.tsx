import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import Layout from "./components/Layout";
import SharesPage from "./pages/shares/SharesPage";
import UsersPage from "./pages/users/UsersPage";
import ConfigPage from "./pages/config/ConfigPage";
import GroupsPage from "./pages/groups/GroupsPage";
import FilesPage from "./pages/files/FilesPage";

function App() {
  return (
    <BrowserRouter basename="/ui">
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        
        {/* Защищенные роуты с боковой панелью */}
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/shares" replace />} />
          <Route path="/shares" element={<SharesPage />} />
          <Route path="/groups" element={<GroupsPage />} />
          <Route path="/users" element={<UsersPage />} />
          <Route path="/files" element={<FilesPage />} />
          <Route path="/config" element={<ConfigPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
