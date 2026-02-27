import React, { useState, useEffect } from 'react';
import LoginUser from './user/LoginUser';
import RegisterUser from './user/RegisterUser';
import '../styles/auth.css';

const AuthModal = ({ isOpen, onClose, initialTab = 'login' }) => {
    const [activeTab, setActiveTab] = useState(initialTab);

    useEffect(() => {
        if (isOpen) {
            setActiveTab(initialTab);
        }
    }, [isOpen]);

    if (!isOpen) return null;

    return (
        <div className="auth-modal-overlay" onClick={onClose}>
            <div className="auth-modal-content" onClick={e => e.stopPropagation()}>
                <button className="btn-close-modal" onClick={onClose}>✕</button>

                <div className="auth-modal-header">
                    <h2 className="auth-modal-title">Ласкаво просимо!</h2>
                    <p>Будь ласка, увійдіть або зареєструйтесь</p>
                </div>

                <div className="auth-tabs">
                    <button
                        className={`auth-tab ${activeTab === 'login' ? 'active' : ''}`}
                        onClick={() => setActiveTab('login')}
                    >
                        Вхід
                    </button>
                    <button
                        className={`auth-tab ${activeTab === 'register' ? 'active' : ''}`}
                        onClick={() => setActiveTab('register')}
                    >
                        Реєстрація
                    </button>
                </div>

                {activeTab === 'login' ? (
                    <LoginUser />
                ) : (
                    <RegisterUser onSuccess={() => setActiveTab('login')} />
                )}
            </div>
        </div>
    );
};

export default AuthModal;

