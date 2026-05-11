import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { loadStripe } from '@stripe/stripe-js';
import '../../styles/paymentResult.css';

const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY);

const PaymentResultPage = () => {
    const [params] = useSearchParams();
    const navigate = useNavigate();

    // Stripe redirect params
    const redirectStatus    = params.get('redirect_status') ?? '';
    const paymentIntentId   = params.get('payment_intent') ?? '';
    const clientSecret      = params.get('payment_intent_client_secret') ?? '';

    const [statusInfo, setStatusInfo] = useState({ status: 'pending', amount: '', currency: 'USD', orderRef: '' });
    const [countdown, setCountdown]   = useState(null);

    useEffect(() => {
        if (!clientSecret) {
            // Fallback: derive status from redirect_status query param only
            const approved = redirectStatus === 'succeeded';
            const declined = redirectStatus === 'failed' || redirectStatus === 'canceled';
            const derivedStatus = approved ? 'approved' : declined ? 'declined' : 'pending';
            setStatusInfo(prev => ({ ...prev, status: derivedStatus }));
            if (approved) setCountdown(6);
            return;
        }

        stripePromise.then(async (stripe) => {
            if (!stripe) return;
            const { paymentIntent, error } = await stripe.retrievePaymentIntent(clientSecret);
            if (error) {
                setStatusInfo({ status: 'declined', amount: '', currency: 'USD', orderRef: paymentIntentId });
                return;
            }
            const s = paymentIntent.status;
            const approved = s === 'succeeded';
            const declined = s === 'canceled' || s === 'requires_payment_method';
            setStatusInfo({
                status:   approved ? 'approved' : declined ? 'declined' : 'pending',
                amount:   paymentIntent.amount ? (paymentIntent.amount / 100).toFixed(2) : '',
                currency: (paymentIntent.currency ?? 'usd').toUpperCase(),
                orderRef: paymentIntent.metadata?.orderReference ?? paymentIntentId,
            });
            if (approved) setCountdown(6);
        });
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [clientSecret, paymentIntentId, redirectStatus]);

    useEffect(() => {
        if (countdown === null) return;
        if (countdown <= 0) { navigate('/'); return; }
        const t = setTimeout(() => setCountdown(c => c - 1), 1000);
        return () => clearTimeout(t);
    }, [countdown, navigate]);

    const { status, amount, currency, orderRef } = statusInfo;
    const approved = status === 'approved';
    const declined = status === 'declined';

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
                        ? 'Платіж не вдався. Спробуйте ще раз.'
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

