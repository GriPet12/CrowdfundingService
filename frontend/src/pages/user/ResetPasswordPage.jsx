import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import '../../styles/auth.css';

const PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]).{10,}$/;

const ResetPasswordPage = () => {
    const [params] = useSearchParams();
    const navigate = useNavigate();
    const token = params.get('token') ?? '';

    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [done, setDone] = useState(false);

    const validate = () => {
        if (!token) return 'Посилання недійсне. Запросіть нове відновлення пароля.';
        if (password.length < 10) return 'Пароль повинен містити мінімум 10 символів';
        if (!PASSWORD_REGEX.test(password)) {
            return 'Пароль повинен містити велику літеру, цифру та спецсимвол';
        }
        if (password !== confirmPassword) return 'Паролі не співпадають';
        return '';
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        const validationError = validate();
        if (validationError) {
            setError(validationError);
            return;
        }
        setError('');
        setLoading(true);
        try {
            const res = await fetch('/api/auth/reset-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ token, newPassword: password }),
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                if (data.error === 'TOKEN_EXPIRED') {
                    setError('Термін дії посилання закінчився. Запросіть нове відновлення пароля.');
                } else {
                    setError('Посилання недійсне або вже використане.');
                }
                return;
            }
            setDone(true);
        } catch {
            setError('Помилка мережі. Спробуйте ще раз.');
        } finally {
            setLoading(false);
        }
    };

    if (!token && !done) {
        return (
            <div className="auth-page">
                <div className="auth-page-card">
                    <h1 className="auth-page-title">Недійсне посилання</h1>
                    <p className="auth-page-subtitle">Запросіть нове відновлення пароля.</p>
                    <Link to="/forgot-password" className="btn-submit auth-page-link-btn">Запросити посилання</Link>
                </div>
            </div>
        );
    }

    return (
        <div className="auth-page">
            <div className="auth-page-card">
                <h1 className="auth-page-title">Новий пароль</h1>

                {done ? (
                    <div className="auth-success">
                        <div className="auth-success-icon">✓</div>
                        <p className="auth-success-text">Пароль успішно змінено. Тепер можете увійти з новим паролем.</p>
                        <button type="button" className="btn-submit" onClick={() => navigate('/')}>
                            На головну
                        </button>
                    </div>
                ) : (
                    <>
                        <p className="auth-page-subtitle">Введіть новий пароль для вашого акаунта.</p>
                        <form className="auth-form" onSubmit={handleSubmit}>
                            {error && <div className="auth-error-box">{error}</div>}
                            <div className="form-group">
                                <label className="form-label">Новий пароль</label>
                                <input
                                    type="password"
                                    className="form-input"
                                    placeholder="Мін. 10 символів"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                    autoComplete="new-password"
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Підтвердження пароля</label>
                                <input
                                    type="password"
                                    className="form-input"
                                    placeholder="Повторіть пароль"
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    required
                                    autoComplete="new-password"
                                />
                            </div>
                            <button type="submit" className="btn-submit" disabled={loading}>
                                {loading ? 'Збереження…' : 'Зберегти пароль'}
                            </button>
                        </form>
                    </>
                )}

                <p className="auth-page-footer">
                    <Link to="/forgot-password">Запросити нове посилання</Link>
                </p>
            </div>
        </div>
    );
};

export default ResetPasswordPage;
