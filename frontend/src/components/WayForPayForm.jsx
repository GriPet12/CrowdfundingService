import React, { useState } from 'react';

const WayForPayForm = ({ orderId, amount }) => {
    const [paymentData, setPaymentData] = useState(null);
    const [loading, setLoading] = useState(false);

    const handleInitPayment = async () => {
        setLoading(true);
        try {
            const response = await fetch('http://localhost:8081/api/payment/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orderId, amount }),
            });

            const data = await response.json();
            setPaymentData(data);

            setTimeout(() => {
                document.getElementById('wayforpay-form').submit();
            }, 200);

        } catch (error) {
            console.error("Помилка при ініціалізації платежу:", error);
            alert("Не вдалося створити платіж");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="payment-container">
            <button
                onClick={handleInitPayment}
                disabled={loading}
                className="pay-button"
            >
                {loading ? 'Підготовка...' : `Оплатити ${amount} UAH`}
            </button>

            {paymentData && (
                <form
                    id="wayforpay-form"
                    action="https://secure.wayforpay.com/pay"
                    method="post"
                    acceptCharset="utf-16"
                    style={{ display: 'none' }}
                >
                    {Object.entries(paymentData).map(([key, value]) => (
                        <input key={key} type="hidden" name={key} value={value} />
                    ))}
                </form>
            )}
        </div>
    );
};

export default WayForPayForm;