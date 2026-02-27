import React, { useState, useEffect } from 'react';
import AuthService from './user/AuthService';
import { useNavigate } from 'react-router-dom';
import '../styles/header.css';

const Header = ({ onLoginClick, onRegisterClick }) => {
    const [currentUser, setCurrentUser] = useState(() => AuthService.getCurrentUser());
    const navigate = useNavigate();

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (!user || user.imageId != null) return;

        fetch('/api/users/me', {
            headers: { 'Authorization': `Bearer ${user.token}` }
        })
            .then(r => r.ok ? r.json() : null)
            .then(data => {
                if (!data) return;
                const updated = { ...user, id: data.id, imageId: data.imageId };
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
                            <button className="btn-logout" onClick={logOut} title="Вийти">
                                Вийти
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
