import { lazy, Suspense, useState } from "react";
import { Routes, Route } from "react-router-dom";
import Header from "./components/Header.jsx";
import AuthModal from "./components/AuthModal.jsx";

import ProjectList from "./components/project/ProjectList.jsx";
import UserList from "./components/user/UserList.jsx";

const UserPage    = lazy(() => import("./components/user/UserPage.jsx"));
const ProjectPage = lazy(() => import("./components/project/ProjectPage.jsx"));
const MyPage      = lazy(() => import("./components/user/MyPage.jsx"));
const CreateProjectPage  = lazy(() => import("./components/project/CreateProjectPage.jsx"));
const EditProjectPage    = lazy(() => import("./components/project/EditProjectPage.jsx"));
const AdminPage       = lazy(() => import("./components/admin/AdminPage.jsx"));
const VerifyEmailPage = lazy(() => import("./components/user/VerifyEmailPage.jsx"));
const OAuth2CallbackPage = lazy(() => import("./components/user/OAuth2CallbackPage.jsx"));

function App() {
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authModalTab, setAuthModalTab] = useState('login');

  const openAuthModal = (tab = 'login') => {
      setAuthModalTab(tab);
      setIsAuthModalOpen(true);
  };

  return (
      <>
          <Header
              onLoginClick={() => openAuthModal('login')}
              onRegisterClick={() => openAuthModal('register')}
          />

          <AuthModal
              isOpen={isAuthModalOpen}
              onClose={() => setIsAuthModalOpen(false)}
              initialTab={authModalTab}
          />

          <Suspense fallback={<div style={{textAlign:'center',padding:'60px',color:'#888'}}>Завантаження…</div>}>
              <Routes>
                  <Route path="/" element={
                      <>
                          <ProjectList />
                          <UserList />
                      </>
                  } />
                  <Route path="/author/:id"  element={<UserPage />} />
                  <Route path="/project/:id" element={<ProjectPage />} />
                  <Route path="/projects/new" element={<CreateProjectPage />} />
                  <Route path="/projects/:id/edit" element={<EditProjectPage />} />
                  <Route path="/me"          element={<MyPage />} />
                  <Route path="/admin"       element={<AdminPage />} />
                  <Route path="/verify-email" element={<VerifyEmailPage />} />
                  <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />
              </Routes>
          </Suspense>
      </>
  )
}

export default App;
