import React, { useState } from 'react';
import AuthService from './AuthService.jsx';

const BACKEND_URL = 'http://localhost:8081';
const FACEBOOK_ENABLED = false; 

const LoginUser = ({ onSwitchToRegister }) => {
    const [data, setData] = useState({ username: '', password: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleLogin = (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        AuthService.login(data.username, data.password).then(
            () => { window.location.reload(); },
            () => { setError('Невірний логін або пароль'); }
        ).finally(() => setLoading(false));
    };

    const handleSocialLogin = (provider) => {
        window.location.href = `${BACKEND_URL}/api/oauth2/authorization/${provider}`;
    };

    return (
        <form className="auth-form" onSubmit={handleLogin}>
            {error && <div className="auth-error-box">{error}</div>}

            
            <div className="auth-social-btns">
                <button type="button" className="auth-social-btn auth-social-btn--google"
                    onClick={() => handleSocialLogin('google')}>
                    <svg width="18" height="18" viewBox="0 0 48 48">
                        <path fill="#EA4335" d="M24 9.5c3.14 0 5.95 1.08 8.17 2.84l6.1-6.1C34.46 3.09 29.52 1 24 1 14.82 1 7.07 6.48 3.76 14.22l7.13 5.54C12.6 13.65 17.84 9.5 24 9.5z"/>
                        <path fill="#4285F4" d="M46.52 24.5c0-1.61-.15-3.16-.42-4.65H24v9.3h12.67c-.55 2.96-2.2 5.47-4.68 7.16l7.18 5.58C43.4 37.77 46.52 31.6 46.52 24.5z"/>
                        <path fill="#FBBC05" d="M10.89 28.23A14.56 14.56 0 0 1 9.5 24c0-1.47.25-2.9.69-4.24L3.06 14.2A23.47 23.47 0 0 0 .5 24c0 3.77.9 7.34 2.5 10.48l7.89-6.25z"/>
                        <path fill="#34A853" d="M24 47c5.52 0 10.15-1.83 13.53-4.97l-7.18-5.58c-1.88 1.26-4.29 2.05-6.35 2.05-6.16 0-11.4-4.15-13.11-9.76l-7.13 5.54C7.07 41.52 14.82 47 24 47z"/>
                    </svg>
                    Увійти з Google
                </button>
                {FACEBOOK_ENABLED && (
                    <button type="button" className="auth-social-btn auth-social-btn--facebook"
                        onClick={() => handleSocialLogin('facebook')}>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="#fff">
                            <path d="M24 12.073C24 5.405 18.627 0 12 0S0 5.405 0 12.073C0 18.1 4.388 23.094 10.125 24v-8.437H7.078v-3.49h3.047V9.413c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.49h-2.796V24C19.612 23.094 24 18.1 24 12.073z"/>
                        </svg>
                        Увійти з Facebook
                    </button>
                )}
            </div>

            <div className="auth-divider"><span>або</span></div>

            <div className="form-group">
                <label className="form-label">Ім'я користувача</label>
                <input type="text" className="form-input" placeholder="Введіть ваше ім'я"
                    value={data.username} onChange={e => setData({...data, username: e.target.value})} required />
            </div>
            <div className="form-group">
                <label className="form-label">Пароль</label>
                <input type="password" className="form-input" placeholder="Введіть ваш пароль"
                    value={data.password} onChange={e => setData({...data, password: e.target.value})} required />
            </div>
            <button type="submit" className="btn-submit" disabled={loading}>
                {loading ? 'Вхід…' : 'Увійти'}
            </button>

            {onSwitchToRegister && (
                <p style={{ textAlign: 'center', fontSize: 13, color: '#666', marginTop: 8 }}>
                    Немає акаунту?{' '}
                    <button type="button" style={{ background: 'none', border: 'none', color: '#1a1a1a', fontWeight: 700, cursor: 'pointer', padding: 0 }}
                        onClick={onSwitchToRegister}>
                        Зареєструватися
                    </button>
                </p>
            )}
        </form>
    );
};

export default LoginUser;