import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import '../../styles/auth.css';

const VerifyEmailPage = () => {
    const [params] = useSearchParams();
    const navigate = useNavigate();
    const [status, setStatus] = useState('loading'); 
    const [message, setMessage] = useState('');
    const [resendEmail, setResendEmail] = useState('');
    const [resendStatus, setResendStatus] = useState('');

    useEffect(() => {
        const token = params.get('token');

        const doVerify = async () => {
            if (!token) {
                setStatus('error');
                setMessage('Токен відсутній.');
                return;
            }
            try {
                const r = await fetch(`/api/auth/verify-email?token=${encodeURIComponent(token)}`);
                const data = await r.json();
                if (r.ok) {
                    setStatus('success');
                } else {
                    setStatus(data.error === 'TOKEN_EXPIRED' ? 'expired' : 'error');
                    setMessage(
                        data.error === 'TOKEN_EXPIRED'
                            ? 'Термін дії посилання закінчився. Запросіть нове.'
                            : 'Недійсний або вже використаний токен.'
                    );
                }
            } catch {
                setStatus('error');
                setMessage('Помилка мережі.');
            }
        };

        doVerify();
    }, []); 

    const handleResend = async (e) => {
        e.preventDefault();
        setResendStatus('sending');
        try {
            const r = await fetch('/api/auth/resend-verification', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: resendEmail }),
            });
            const data = await r.json();
            if (r.ok) setResendStatus('sent');
            else setResendStatus(data.error === 'ALREADY_VERIFIED' ? 'already' : 'error');
        } catch { setResendStatus('error'); }
    };

    return (
        <div style={{ maxWidth: 480, margin: '80px auto', padding: '0 20px', textAlign: 'center' }}>
            {status === 'loading' && <p>Перевірка посилання…</p>}

            {status === 'success' && (
                <>
                    <div style={{ fontSize: 48, color: '#059669' }}>
                        <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                    </div>
                    <h2 style={{ marginTop: 16 }}>Email підтверджено!</h2>
                    <p style={{ color: '#555' }}>Ваша електронна пошта успішно підтверджена.</p>
                    <button className="btn-submit" style={{ marginTop: 24 }} onClick={() => navigate('/')}>
                        На головну
                    </button>
                </>
            )}

            {(status === 'error' || status === 'expired') && (
                <>
                    <div style={{ fontSize: 48, color: status === 'expired' ? '#d97706' : '#dc2626' }}>
                        {status === 'expired'
                            ? <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                            : <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                        }
                    </div>
                    <h2 style={{ marginTop: 16 }}>
                        {status === 'expired' ? 'Посилання застаріло' : 'Помилка підтвердження'}
                    </h2>
                    <p style={{ color: '#555' }}>{message}</p>

                    <p style={{ marginTop: 28, fontWeight: 600 }}>Надіслати новий лист:</p>
                    <form className="auth-form" onSubmit={handleResend} style={{ marginTop: 12 }}>
                        <input
                            className="form-input"
                            type="email"
                            placeholder="Введіть ваш email"
                            value={resendEmail}
                            onChange={e => setResendEmail(e.target.value)}
                            required
                        />
                        <button className="btn-submit" type="submit" disabled={resendStatus === 'sending'}>
                            {resendStatus === 'sending' ? 'Надсилання…' : 'Надіслати'}
                        </button>
                    </form>

                    {resendStatus === 'sent' && <p style={{ color: 'green', marginTop: 12 }}>Лист надіслано! Перевірте пошту.</p>}
                    {resendStatus === 'already' && <p style={{ color: '#888', marginTop: 12 }}>Email вже підтверджено.</p>}
                    {resendStatus === 'error' && <p style={{ color: 'red', marginTop: 12 }}>Помилка. Перевірте email і спробуйте ще раз.</p>}
                </>
            )}
        </div>
    );
};

export default VerifyEmailPage;

