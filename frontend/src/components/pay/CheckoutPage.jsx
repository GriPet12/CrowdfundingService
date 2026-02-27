import { useState, useMemo } from 'react';
import WayForPayForm from './WayForPayForm.jsx';

const CheckoutPage = () => {
    const [amount, setAmount] = useState(150);
    // eslint-disable-next-line react-hooks/purity
    const orderId = useMemo(() => `ORDER_${Date.now()}`, []);

    return (
        <div style={{ maxWidth: 400, margin: '40px auto', padding: '20px' }}>
            <h1>Оформлення внеску</h1>
            <div style={{ marginBottom: 16 }}>
                <label htmlFor="amount" style={{ display: 'block', marginBottom: 8, fontWeight: 600 }}>
                    Сума (UAH)
                </label>
                <input
                    id="amount"
                    type="number"
                    min="1"
                    value={amount}
                    onChange={(e) => setAmount(parseFloat(e.target.value) || 0)}
                    className="donate-input"
                    style={{ width: '100%' }}
                />
            </div>
            <WayForPayForm orderId={orderId} amount={amount} />
        </div>
    );
};

export default CheckoutPage;