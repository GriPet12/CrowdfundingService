import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { loadStripe } from '@stripe/stripe-js';
import { Elements, PaymentElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { useStripePayment } from './StripePaymentContext.jsx';
import '../../styles/auth.css';

const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY);

/* ── Форма всередині Elements ──────────────────────────────────────── */
const CheckoutForm = ({ amount, onBeforeSubmit, onError }) => {
    const stripe   = useStripe();
    const elements = useElements();
    const [processing, setProcessing] = useState(false);
    const [message, setMessage]       = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!stripe || !elements) return;
        setProcessing(true);
        setMessage('');
        onBeforeSubmit?.();

        const { error } = await stripe.confirmPayment({
            elements,
            confirmParams: { return_url: `${window.location.origin}/payment/result` },
        });

        if (error) {
            setMessage(error.message ?? 'Помилка оплати');
            onError?.(error);
        }
        setProcessing(false);
    };

    return (
        <form onSubmit={handleSubmit} className="auth-form">
            <div className="auth-modal-header">
                <h2 className="auth-modal-title">Оплата</h2>
                {amount > 0 && (
                    <p style={{ margin: '-8px 0 0', fontSize: 15, color: '#7c3aed', fontWeight: 700 }}>
                        {Number(amount).toLocaleString('uk-UA', { minimumFractionDigits: 2 })} USD
                    </p>
                )}
            </div>

            <PaymentElement />

            {message && (
                <p className="auth-error-box" style={{ margin: 0 }}>{message}</p>
            )}

            <button
                type="submit"
                disabled={!stripe || processing}
                className="btn-submit"
                style={{ opacity: (!stripe || processing) ? 0.6 : 1 }}
            >
                {processing ? 'Обробка…' : 'Сплатити'}
            </button>
        </form>
    );
};

/* ── Сама модалка — рендериться через portal у document.body ───────── */
const StripeModal = () => {
    const { paymentState, closePayment } = useStripePayment();

    // Блокуємо скрол body поки модалка відкрита
    useEffect(() => {
        if (!paymentState) return;
        document.body.style.overflow = 'hidden';
        return () => { document.body.style.overflow = ''; };
    }, [paymentState]);

    // Escape — закрити
    useEffect(() => {
        if (!paymentState) return;
        const handler = (e) => { if (e.key === 'Escape') closePayment(); };
        window.addEventListener('keydown', handler);
        return () => window.removeEventListener('keydown', handler);
    }, [paymentState, closePayment]);

    if (!paymentState) return null;

    const { clientSecret, amount, onBeforeSubmit, onError } = paymentState;
    const appearance = { theme: 'stripe', variables: { borderRadius: '6px' } };

    return createPortal(
        <div className="auth-modal-overlay auth-modal-overlay--scrollable" onClick={closePayment}>
            <div className="auth-modal-content auth-modal-content--scrollable" onClick={(e) => e.stopPropagation()}>
                <button className="btn-close-modal" onClick={closePayment} title="Закрити">✕</button>
                <Elements stripe={stripePromise} options={{ clientSecret, appearance }}>
                    <CheckoutForm
                        amount={amount}
                        onBeforeSubmit={onBeforeSubmit}
                        onError={onError}
                    />
                </Elements>
            </div>
        </div>,
        document.body
    );
};

export default StripeModal;

