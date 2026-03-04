import React, { useState } from 'react';
import AuthService from './AuthService';

const PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]).{10,}$/;

const getPasswordStrength = (pw) => {
    if (!pw) return null;
    let score = 0;
    if (pw.length >= 10) score++;
    if (/[A-Z]/.test(pw)) score++;
    if (/[0-9]/.test(pw)) score++;
    if (/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(pw)) score++;
    if (score <= 1) return { label: 'Слабкий', color: '#e53935' };
    if (score === 2) return { label: 'Середній', color: '#fb8c00' };
    if (score === 3) return { label: 'Хороший', color: '#fdd835' };
    return { label: 'Надійний', color: '#43a047' };
};

const BACKEND_URL = 'http://localhost:8081';
const FACEBOOK_ENABLED = false; // ← встановіть true після отримання Facebook App ID/Secret

const RegisterUser = ({ onSuccess }) => {
    const [data, setData] = useState({ username: '', email: '', password: '', confirmPassword: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [registered, setRegistered] = useState(false);
    const [registeredEmail, setRegisteredEmail] = useState('');

    const strength = getPasswordStrength(data.password);

    const validate = () => {
        if (data.password !== data.confirmPassword) return 'Паролі не співпадають';
        if (data.password.length < 10) return 'Пароль повинен містити мінімум 10 символів';
        if (!PASSWORD_REGEX.test(data.password))
            return 'Пароль повинен містити хоча б одну велику літеру, цифру та спеціальний символ';
        return null;
    };

    const handleRegister = (e) => {
        e.preventDefault();
        setError('');
        const validationError = validate();
        if (validationError) { setError(validationError); return; }
        setLoading(true);
        AuthService.register(data.username, data.email, data.password).then(
            () => {
                setRegisteredEmail(data.email);
                setRegistered(true);
                onSuccess?.();
            },
            (err) => {
                const msg = err?.response?.data?.message || err?.response?.data || '';
                if (msg.includes('USERNAME_TAKEN') || String(msg).includes('username'))
                    setError("Це ім'я користувача вже зайняте. Оберіть інше.");
                else if (msg.includes('EMAIL_TAKEN') || String(msg).includes('email'))
                    setError('Ця електронна адреса вже використовується.');
                else if (String(msg).includes('велику літеру') || String(msg).includes('Pattern'))
                    setError('Пароль повинен містити хоча б одну велику літеру, цифру та спеціальний символ');
                else
                    setError('Помилка реєстрації. Спробуйте ще раз.');
            }
        ).finally(() => setLoading(false));
    };

    const handleSocialLogin = (provider) => {
        window.location.href = `${BACKEND_URL}/api/oauth2/authorization/${provider}`;
    };

    if (registered) {
        return (
            <div className="auth-success">
                <div className="auth-success-icon">✓</div>
                <h3 className="auth-success-title">Реєстрацію завершено!</h3>
                <p className="auth-success-text">
                    На адресу <strong>{registeredEmail}</strong> надіслано листа для підтвердження акаунту.
                </p>
                <p className="auth-success-hint">
                    Перевірте папку «Спам», якщо листа немає у вхідних.
                </p>
                <button className="btn-submit" onClick={() => window.location.reload()}>
                    Увійти в акаунт
                </button>
            </div>
        );
    }

    return (
        <form className="auth-form" onSubmit={handleRegister}>
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
                    Продовжити з Google
                </button>
                {FACEBOOK_ENABLED && (
                    <button type="button" className="auth-social-btn auth-social-btn--facebook"
                        onClick={() => handleSocialLogin('facebook')}>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="#fff">
                            <path d="M24 12.073C24 5.405 18.627 0 12 0S0 5.405 0 12.073C0 18.1 4.388 23.094 10.125 24v-8.437H7.078v-3.49h3.047V9.413c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.49h-2.796V24C19.612 23.094 24 18.1 24 12.073z"/>
                        </svg>
                        Продовжити з Facebook
                    </button>
                )}
            </div>

            <div className="auth-divider"><span>або</span></div>

            <div className="form-group">
                <label className="form-label">Ім'я користувача</label>
                <input type="text" className="form-input" placeholder="Введіть ім'я користувача (мін. 4 символи)"
                    value={data.username} onChange={e => setData({...data, username: e.target.value})} required minLength={4} />
            </div>

            <div className="form-group">
                <label className="form-label">Email</label>
                <input type="email" className="form-input" placeholder="Введіть ваш email"
                    value={data.email} onChange={e => setData({...data, email: e.target.value})} required />
            </div>

            <div className="form-group">
                <label className="form-label">Пароль</label>
                <input type="password" className="form-input" placeholder="Мін. 10 символів"
                    value={data.password} onChange={e => setData({...data, password: e.target.value})} required />
                {strength && (
                    <div className="auth-password-strength">
                        <div className="auth-password-strength-bar-track">
                            <div className="auth-password-strength-bar" style={{ width: `${(['Слабкий','Середній','Хороший','Надійний'].indexOf(strength.label)+1)*25}%`, backgroundColor: strength.color }} />
                        </div>
                        <span style={{ color: strength.color, fontSize: 12, whiteSpace: 'nowrap' }}>{strength.label}</span>
                    </div>
                )}
                <ul className="auth-password-rules">
                    <li className={data.password.length >= 10 ? 'ok' : ''}>Мінімум 10 символів</li>
                    <li className={/[A-Z]/.test(data.password) ? 'ok' : ''}>Хоча б одна велика літера</li>
                    <li className={/[0-9]/.test(data.password) ? 'ok' : ''}>Хоча б одна цифра</li>
                    <li className={/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(data.password) ? 'ok' : ''}>Хоча б один спецсимвол</li>
                </ul>
            </div>

            <div className="form-group">
                <label className="form-label">Підтвердіть пароль</label>
                <input type="password" className="form-input" placeholder="Повторіть пароль"
                    value={data.confirmPassword} onChange={e => setData({...data, confirmPassword: e.target.value})} required />
                {data.confirmPassword && data.password !== data.confirmPassword && (
                    <p style={{ color: '#e53935', fontSize: 12, margin: '4px 0 0' }}>Паролі не співпадають</p>
                )}
            </div>

            <button type="submit" className="btn-submit" disabled={loading}>
                {loading ? 'Реєстрація…' : 'Зареєструватися'}
            </button>
        </form>
    );
};

export default RegisterUser;
