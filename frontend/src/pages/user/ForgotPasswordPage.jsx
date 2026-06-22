import { useState } from 'react';
import { Link } from 'react-router-dom';
import '../../styles/auth.css';

const ForgotPasswordPage = () => {
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [sent, setSent] = useState(false);
    const [error, setError] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            const res = await fetch('/api/auth/forgot-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email.trim() }),
            });
            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                setError(data.message || 'Не вдалося надіслати лист. Спробуйте ще раз.');
                return;
            }
            setSent(true);
        } catch {
            setError('Помилка мережі. Спробуйте ще раз.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-page-card">
                <h1 className="auth-page-title">Відновлення пароля</h1>

                {sent ? (
                    <div className="auth-success">
                        <div className="auth-success-icon">✓</div>
                        <p className="auth-success-text">
                            Якщо акаунт з адресою <strong>{email}</strong> існує, ми надіслали інструкції для відновлення пароля.
                        </p>
                        <p className="auth-success-hint">Перевірте пошту (і папку «Спам»).</p>
                        <Link to="/" className="btn-submit auth-page-link-btn">На головну</Link>
                    </div>
                ) : (
                    <>
                        <p className="auth-page-subtitle">
                            Введіть email вашого акаунта — надішлемо посилання для встановлення нового пароля.
                        </p>
                        <form className="auth-form" onSubmit={handleSubmit}>
                            {error && <div className="auth-error-box">{error}</div>}
                            <div className="form-group">
                                <label className="form-label">Email</label>
                                <input
                                    type="email"
                                    className="form-input"
                                    placeholder="you@example.com"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    required
                                    autoComplete="email"
                                />
                            </div>
                            <button type="submit" className="btn-submit" disabled={loading}>
                                {loading ? 'Надсилання…' : 'Надіслати посилання'}
                            </button>
                        </form>
                    </>
                )}

                <p className="auth-page-footer">
                    <Link to="/">← Повернутися до входу</Link>
                </p>
            </div>
        </div>
    );
};

export default ForgotPasswordPage;
