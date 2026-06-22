import { lazy, Suspense, useState } from "react";
import { Routes, Route } from "react-router-dom";
import Header from "./components/Header.jsx";
import AuthModal from "./components/AuthModal.jsx";
import { StripePaymentProvider } from "./components/pay/StripePaymentContext.jsx";
import StripeModal from "./components/pay/StripeModal.jsx";
import ErrorBoundary from "./components/common/ErrorBoundary.jsx";

import ProjectList from "./components/project/ProjectList.jsx";
import UserList from "./components/user/UserList.jsx";

const UserPage    = lazy(() => import("./pages/user/UserPage.jsx"));
const ProjectPage = lazy(() => import("./pages/project/ProjectPage.jsx"));
const MyPage      = lazy(() => import("./pages/user/MyPage.jsx"));
const CreateProjectPage  = lazy(() => import("./pages/project/CreateProjectPage.jsx"));
const EditProjectPage    = lazy(() => import("./pages/project/EditProjectPage.jsx"));
const AdminPage       = lazy(() => import("./pages/admin/AdminPage.jsx"));
const VerifyEmailPage = lazy(() => import("./pages/user/VerifyEmailPage.jsx"));
const ForgotPasswordPage = lazy(() => import("./pages/user/ForgotPasswordPage.jsx"));
const ResetPasswordPage = lazy(() => import("./pages/user/ResetPasswordPage.jsx"));
const OAuth2CallbackPage = lazy(() => import("./pages/user/OAuth2CallbackPage.jsx"));
const PaymentResultPage  = lazy(() => import("./pages/pay/PaymentResultPage.jsx"));

function App() {
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authModalTab, setAuthModalTab] = useState('login');

  const openAuthModal = (tab = 'login') => {
      setAuthModalTab(tab);
      setIsAuthModalOpen(true);
  };

  return (
      <StripePaymentProvider>
          <Header
              onLoginClick={() => openAuthModal('login')}
              onRegisterClick={() => openAuthModal('register')}
          />

          <AuthModal
              isOpen={isAuthModalOpen}
              onClose={() => setIsAuthModalOpen(false)}
              initialTab={authModalTab}
          />

          {/* Stripe-модалка живе тут — на верхньому рівні, поза будь-яким layout */}
          <StripeModal />

          <ErrorBoundary>
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
                  <Route path="/forgot-password" element={<ForgotPasswordPage />} />
                  <Route path="/reset-password" element={<ResetPasswordPage />} />
                  <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />
                  <Route path="/payment/result" element={<PaymentResultPage />} />
              </Routes>
          </Suspense>
          </ErrorBoundary>
      </StripePaymentProvider>
  )
}

export default App;
