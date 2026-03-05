import { useState, useCallback } from 'react';
import { useStripePayment } from './StripePaymentContext.jsx';

/* ── Кнопка оплати — тригерить модалку через context ──────────────── */
const WayForPayForm = ({
    amount,
    type           = 'DONATION',
    paymentPayload = {},
    label          = '✓',
    confirmClass   = 'donate-button-confirm',
    onBeforeSubmit = null,
    onError        = null,
}) => {
    const { openPayment } = useStripePayment();
    const [loading, setLoading]     = useState(false);
    const [initError, setInitError] = useState('');

    const initPayment = useCallback(async () => {
        if (!amount || amount <= 0) {
            alert('Введіть суму більше 0');
            return;
        }
        setLoading(true);
        setInitError('');

        try {
            const res = await fetch(`/api/payment/${type}/generate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ...paymentPayload, amount }),
            });

            if (!res.ok) {
                const text = await res.text().catch(() => '');
                setInitError(`Статус: ${res.status}${text ? ` — ${text}` : ''}`);
                return;
            }

            const data = await res.json();
            openPayment({ clientSecret: data.clientSecret, amount, onBeforeSubmit, onError });
        } catch (err) {
            console.error('Помилка при ініціалізації платежу:', err);
            setInitError(err.message ?? 'Невідома помилка');
        } finally {
            setLoading(false);
        }
    }, [amount, type, paymentPayload, openPayment, onBeforeSubmit, onError]);

    return (
        <>
            <button
                type="button"
                onClick={initPayment}
                disabled={loading || !amount || amount <= 0}
                className={confirmClass}
                title="Підтвердити оплату"
            >
                {loading ? '...' : label}
            </button>

            {initError && (
                <p className="auth-error-box" style={{ margin: 0, fontSize: 12 }}>{initError}</p>
            )}
        </>
    );
};

export default WayForPayForm;

