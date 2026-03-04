import React, { useState, useEffect } from 'react';
import AuthService from './user/AuthService';
import { useNavigate, useLocation, NavLink } from 'react-router-dom';
import '../styles/header.css';

const Header = ({ onLoginClick, onRegisterClick }) => {
    const [currentUser, setCurrentUser] = useState(() => AuthService.getCurrentUser());
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        if (document.activeElement instanceof HTMLElement) {
            document.activeElement.blur();
        }
    }, [location.pathname]);

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (!user || user.imageId != null) return;

        fetch('/api/users/me', {
            headers: { 'Authorization': `Bearer ${user.token}` }
        })
            .then(r => r.ok ? r.json() : null)
            .then(data => {
                if (!data) return;
                const updated = {
                    ...user,
                    id: data.id,
                    imageId: data.imageId,
                    role: data.role ?? user.role,
                    roles: data.roles ?? user.roles,
                };
                localStorage.setItem('user', JSON.stringify(updated));
                setCurrentUser(updated);
            })
            .catch(() => {});
    }, []);

    const logOut = () => {
        AuthService.logout();
        setCurrentUser(undefined);
        window.location.reload();
    };

    const isAdmin = (u) => {
        if (!u) return false;
        if (u.role === 'ADMIN') return true;
        return Array.isArray(u.roles) && u.roles.some(r => r === 'ROLE_ADMIN' || r === 'ADMIN');
    };

    return (
        <header className="site-header">
            <div className="header-container">
                <div className="logo" onClick={() => navigate('/')}>Crowdfunding</div>
                <div className="auth-section">
                    {currentUser ? (
                        <div className="user-profile">
                            <span className="user-name">{currentUser.username}</span>
                            <div
                                className="avatar-wrapper"
                                onClick={() => navigate('/me')}
                                title="Мій профіль"
                            >
                                {currentUser.imageId ? (
                                    <img
                                        src={`/api/files/${currentUser.imageId}`}
                                        alt={currentUser.username}
                                        className="user-avatar"
                                    />
                                ) : (
                                    <div className="user-avatar-placeholder">
                                        {currentUser.username ? currentUser.username.charAt(0).toUpperCase() : 'U'}
                                    </div>
                                )}
                            </div>
                            {isAdmin(currentUser) && (
                                <NavLink
                                    to="/admin"
                                    className="btn-admin"
                                    title="Адмін-панель"
                                >
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                                    </svg>
                                    <span className="btn-admin-text">Адмін</span>
                                </NavLink>
                            )}
                            <button className="btn-logout" onClick={logOut} title="Вийти">
                                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                                    <polyline points="16 17 21 12 16 7"/>
                                    <line x1="21" y1="12" x2="9" y2="12"/>
                                </svg>
                                <span className="btn-logout-text">Вийти</span>
                            </button>
                        </div>
                    ) : (
                        <div className="auth-buttons">
                            <button className="btn-login" onClick={onLoginClick}>Вхід</button>
                            <button className="btn-register" onClick={onRegisterClick}>Реєстрація</button>
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
};

export default Header;
