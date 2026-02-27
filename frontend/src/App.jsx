import { lazy, Suspense, useState } from "react";
import { Routes, Route } from "react-router-dom";
import Header from "./components/Header.jsx";
import AuthModal from "./components/AuthModal.jsx";

// Eagerly load home-page components (always shown first)
import ProjectList from "./components/project/ProjectList.jsx";
import UserList from "./components/user/UserList.jsx";

// Lazy-load route pages — only downloaded when navigated to
const UserPage    = lazy(() => import("./components/user/UserPage.jsx"));
const ProjectPage = lazy(() => import("./components/project/ProjectPage.jsx"));
const MyPage      = lazy(() => import("./components/user/MyPage.jsx"));

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
                  <Route path="/me"          element={<MyPage />} />
              </Routes>
          </Suspense>
      </>
  )
}

export default App;
