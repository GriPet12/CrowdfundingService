import React, { useState, useEffect, useRef } from 'react';

const WayForPayForm = ({
    amount,
    type = 'DONATION',
    paymentPayload = {},
    label = '✓',
    confirmClass = 'donate-button-confirm',
}) => {
    const [paymentData, setPaymentData] = useState(null);
    const [loading, setLoading]         = useState(false);
    const formRef      = useRef(null);
    const submittedRef = useRef(false);

    useEffect(() => {
        if (paymentData && formRef.current && !submittedRef.current) {
            submittedRef.current = true;
            formRef.current.submit();
        }
    }, [paymentData]);

    const handleInitPayment = async () => {
        if (!amount || amount <= 0) {
            alert('Введіть суму більше 0');
            return;
        }
        submittedRef.current = false;
        setPaymentData(null);
        setLoading(true);

        try {
            const response = await fetch(`/api/payment/${type}/generate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ...paymentPayload, amount }),
            });

            if (!response.ok) {
                let errMsg = `Статус: ${response.status}`;
                try { const t = await response.text(); if (t) errMsg += ` — ${t}`; } catch (_) {}
                console.error('Помилка платежу:', errMsg);
                alert(`Не вдалося створити платіж. ${errMsg}`);
                return;
            }

            const data = await response.json();
            setPaymentData(data);
        } catch (error) {
            console.error('Помилка при ініціалізації платежу:', error);
            alert('Не вдалося створити платіж. Перевірте з\'єднання та спробуйте ще раз.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <button
                type="button"
                onClick={handleInitPayment}
                disabled={loading || !amount || amount <= 0}
                className={confirmClass}
                title="Підтвердити оплату"
            >
                {loading ? '...' : label}
            </button>

            {paymentData && (
                <form
                    ref={formRef}
                    action="https://secure.wayforpay.com/pay"
                    method="post"
                    acceptCharset="utf-8"
                    style={{ display: 'none' }}
                >
                    {Object.entries(paymentData).map(([key, value]) => (
                        <input key={key} type="hidden" name={key} value={value} />
                    ))}
                </form>
            )}
        </>
    );
};

export default WayForPayForm;