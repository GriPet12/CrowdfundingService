import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import '../../styles/paymentResult.css';

const PaymentResultPage = () => {
    const [params] = useSearchParams();
    const navigate = useNavigate();

    const status      = params.get('transactionStatus') ?? params.get('status') ?? '';
    const orderRef    = params.get('orderReference') ?? '';
    const amount      = params.get('amount') ?? '';
    const currency    = params.get('currency') ?? 'UAH';
    const reason      = params.get('reasonCode') ?? params.get('reason') ?? '';

    const approved = status === 'Approved';
    const declined = status === 'Declined' || status === 'Refunded' || status === 'Expired';

    
    const [countdown, setCountdown] = useState(approved ? 6 : null);

    useEffect(() => {
        if (!approved) return;
        const t = setInterval(() => {
            setCountdown(c => {
                if (c <= 1) { clearInterval(t); navigate('/'); return 0; }
                return c - 1;
            });
        }, 1000);
        return () => clearInterval(t);
    }, [approved, navigate]);

    return (
        <div className="pr-page">
            <div className={`pr-card ${approved ? 'pr-card--success' : declined ? 'pr-card--error' : 'pr-card--pending'}`}>

                <div className="pr-icon">
                    {approved ? (
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="12" cy="12" r="10"/>
                            <polyline points="9 12 11 14 15 10"/>
                        </svg>
                    ) : declined ? (
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="12" cy="12" r="10"/>
                            <line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
                        </svg>
                    ) : (
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="12" cy="12" r="10"/>
                            <line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
                        </svg>
                    )}
                </div>

                <h1 className="pr-title">
                    {approved ? 'Оплату підтверджено!' : declined ? 'Оплату відхилено' : 'Обробка платежу…'}
                </h1>

                <p className="pr-subtitle">
                    {approved
                        ? 'Дякуємо! Ваш платіж успішно оброблено.'
                        : declined
                        ? `Платіж не вдався.${reason ? ` Причина: ${reason}.` : ''}`
                        : 'Платіж ще обробляється. Перевірте статус пізніше.'}
                </p>

                {amount && (
                    <div className="pr-amount">
                        {Number(amount).toLocaleString('uk-UA', { minimumFractionDigits: 2 })} {currency}
                    </div>
                )}

                {orderRef && (
                    <p className="pr-order">№ {orderRef}</p>
                )}

                {approved && countdown !== null && (
                    <p className="pr-redirect">Перенаправлення на головну через {countdown} с…</p>
                )}

                <div className="pr-actions">
                    <button className="pr-btn pr-btn--primary" onClick={() => navigate('/')}>
                        На головну
                    </button>
                    <button className="pr-btn pr-btn--secondary" onClick={() => navigate(-2)}>
                        Назад
                    </button>
                </div>
            </div>
        </div>
    );
};

export default PaymentResultPage;

