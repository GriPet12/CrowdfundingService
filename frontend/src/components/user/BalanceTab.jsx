import { useState, useEffect, useCallback } from 'react';
import '../../styles/balanceTab.css';

const fmtMoney = (n) => {
    const val = Number(n);
    if (Number.isNaN(val)) return '₴0';
    return `₴${val.toLocaleString('uk-UA', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
};

const statusLabel = (status) => {
    switch (status) {
        case 'COMPLETED': return 'Виконано';
        case 'PROCESSING': return 'Обробка';
        case 'PENDING': return 'Очікує';
        case 'FAILED': return 'Помилка';
        case 'REJECTED': return 'Відхилено';
        default: return status;
    }
};

const BalanceTab = ({ token }) => {
    const [balance, setBalance] = useState(null);
    const [withdrawals, setWithdrawals] = useState([]);
    const [connectStatus, setConnectStatus] = useState(null);
    const [amount, setAmount] = useState('');
    const [loading, setLoading] = useState(true);
    const [withdrawing, setWithdrawing] = useState(false);
    const [connecting, setConnecting] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const headers = { Authorization: `Bearer ${token}` };

    const loadData = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const [balRes, wRes, cRes] = await Promise.all([
                fetch('/api/balance', { headers }),
                fetch('/api/balance/withdrawals', { headers }),
                fetch('/api/balance/connect/status', { headers }),
            ]);
            if (balRes.ok) setBalance(await balRes.json());
            if (wRes.ok) setWithdrawals(await wRes.json());
            if (cRes.ok) setConnectStatus(await cRes.json());
        } catch {
            setError('Не вдалося завантажити дані балансу');
        } finally {
            setLoading(false);
        }
    }, [token]);

    useEffect(() => { loadData(); }, [loadData]);

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        if (params.get('connect') === 'done') {
            setSuccess('Stripe підключено. Перевірте статус виплат.');
            loadData();
        }
    }, [loadData]);

    const handleConnect = async () => {
        setConnecting(true);
        setError('');
        try {
            const res = await fetch('/api/balance/connect/onboarding', {
                method: 'POST',
                headers,
            });
            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                throw new Error(data.message || 'Не вдалося відкрити Stripe');
            }
            const { url } = await res.json();
            window.location.href = url;
        } catch (err) {
            setError(err.message);
        } finally {
            setConnecting(false);
        }
    };

    const handleWithdraw = async (e) => {
        e.preventDefault();
        const value = parseFloat(amount);
        if (!value || value <= 0) {
            setError('Введіть коректну суму');
            return;
        }
        setWithdrawing(true);
        setError('');
        setSuccess('');
        try {
            const res = await fetch('/api/balance/withdraw', {
                method: 'POST',
                headers: { ...headers, 'Content-Type': 'application/json' },
                body: JSON.stringify({ amount: value }),
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok) throw new Error(data.message || 'Не вдалося створити заявку');
            setSuccess(
                data.status === 'COMPLETED'
                    ? `Виведено ${fmtMoney(data.amount)} на ваш Stripe-акаунт`
                    : data.status === 'PENDING'
                        ? `Заявку на ${fmtMoney(data.amount)} прийнято. ${data.failureReason || ''}`
                        : `Заявку створено (${statusLabel(data.status)})`
            );
            setAmount('');
            loadData();
        } catch (err) {
            setError(err.message);
        } finally {
            setWithdrawing(false);
        }
    };

    if (loading) {
        return <div className="balance-tab-loading">Завантаження балансу…</div>;
    }

    const available = Number(balance?.availableBalance ?? 0);
    const minWithdrawal = Number(balance?.minWithdrawal ?? 100);

    return (
        <div className="balance-tab">
            <div className="balance-cards">
                <div className="balance-card balance-card--main">
                    <span className="balance-card-label">Доступно для виведення</span>
                    <span className="balance-card-value">{fmtMoney(available)}</span>
                </div>
                <div className="balance-card">
                    <span className="balance-card-label">Зароблено всього</span>
                    <span className="balance-card-value balance-card-value--sm">
                        {fmtMoney(balance?.totalEarned)}
                    </span>
                </div>
                <div className="balance-card">
                    <span className="balance-card-label">Виведено</span>
                    <span className="balance-card-value balance-card-value--sm">
                        {fmtMoney(balance?.totalWithdrawn)}
                    </span>
                </div>
            </div>

            {balance?.platformFeePercent > 0 && (
                <p className="balance-fee-note">
                    Комісія платформи: {balance.platformFeePercent}% (вже віднята від заробітку)
                </p>
            )}

            <div className="balance-stripe-section">
                <h4 className="balance-section-title">Виплати через Stripe</h4>
                {connectStatus?.payoutsEnabled ? (
                    <div className="balance-stripe-ready">
                        <span className="balance-stripe-badge">✓ Stripe підключено</span>
                        <span className="balance-stripe-hint">Виведення надходять на ваш банківський рахунок через Stripe</span>
                    </div>
                ) : (
                    <div className="balance-stripe-setup">
                        <p className="balance-stripe-hint">
                            Підключіть Stripe, щоб автоматично отримувати виплати на картку або рахунок.
                        </p>
                        <button
                            type="button"
                            className="balance-connect-btn"
                            onClick={handleConnect}
                            disabled={connecting}
                        >
                            {connecting ? 'Підключення…' : 'Підключити Stripe'}
                        </button>
                    </div>
                )}
            </div>

            <form className="balance-withdraw-form" onSubmit={handleWithdraw}>
                <h4 className="balance-section-title">Вивести кошти</h4>
                <div className="balance-withdraw-row">
                    <input
                        type="number"
                        className="balance-withdraw-input"
                        placeholder={`Мін. ${minWithdrawal} ₴`}
                        min={minWithdrawal}
                        max={available}
                        step="1"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        disabled={available < minWithdrawal}
                    />
                    <button
                        type="submit"
                        className="balance-withdraw-btn"
                        disabled={withdrawing || available < minWithdrawal}
                    >
                        {withdrawing ? 'Обробка…' : 'Вивести'}
                    </button>
                </div>
                {available < minWithdrawal && (
                    <p className="balance-hint">Мінімальна сума виведення — {fmtMoney(minWithdrawal)}</p>
                )}
            </form>

            {error && <div className="balance-message balance-message--error">{error}</div>}
            {success && <div className="balance-message balance-message--success">{success}</div>}

            <div className="balance-history">
                <h4 className="balance-section-title">Історія виведень</h4>
                {withdrawals.length === 0 ? (
                    <p className="balance-empty">Виведень ще не було</p>
                ) : (
                    <div className="balance-history-list">
                        {withdrawals.map((w) => (
                            <div key={w.withdrawalId} className="balance-history-item">
                                <div className="balance-history-main">
                                    <span className="balance-history-amount">{fmtMoney(w.amount)}</span>
                                    <span className={`balance-history-status balance-history-status--${w.status.toLowerCase()}`}>
                                        {statusLabel(w.status)}
                                    </span>
                                </div>
                                <div className="balance-history-meta">
                                    {new Date(w.createdAt).toLocaleString('uk-UA')}
                                    {w.failureReason && (
                                        <span className="balance-history-reason"> · {w.failureReason}</span>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default BalanceTab;
