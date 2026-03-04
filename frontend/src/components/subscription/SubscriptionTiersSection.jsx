import { useState, useEffect } from 'react';
import WayForPayForm from '../pay/WayForPayForm.jsx';
import AuthService from '../user/AuthService.jsx';
import analyticsService from '../../services/analyticsService.js';
import '../../styles/subscriptionTiers.css';

const PAYMENT_MODELS = [
    { key: 'SUBSCRIPTION', label: 'Підписка',        hint: 'Щомісячний платіж, автоматичне поновлення' },
    { key: 'PREORDER',     label: 'Передзамовлення', hint: 'Одноразова оплата для отримання винагороди' },
    { key: 'DONATION',     label: 'Разовий донат',   hint: 'Довільна сума, без зобов\'язань' },
];

const SubscriptionTiersSection = ({ creatorId }) => {
    const currentUser = AuthService.getCurrentUser();
    const [tiers, setTiers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activeTierId, setActiveTierId] = useState(null);
    const [paymentModel, setPaymentModel] = useState('SUBSCRIPTION');
    const [activeSubs, setActiveSubs] = useState([]);

    useEffect(() => {
        fetch(`/api/subscription-tiers/creator/${creatorId}`)
            .then(r => r.ok ? r.json() : [])
            .then(data => setTiers(data.sort((a, b) => a.level - b.level)))
            .catch(() => setTiers([]))
            .finally(() => setLoading(false));
    }, [creatorId]);

    useEffect(() => {
        if (!currentUser) return;
        fetch(`/api/subscriptions/status/${creatorId}`, {
            headers: { Authorization: `Bearer ${currentUser.token}` },
        })
            .then(r => r.ok ? r.json() : [])
            .then(setActiveSubs)
            .catch(() => setActiveSubs([]));
    }, [creatorId]); 

    const maxActiveLevel = activeSubs.length > 0
        ? Math.max(...activeSubs.map(s => s.tierLevel))
        : null;

    const formatDate = (iso) => iso ? new Date(iso).toLocaleDateString('uk-UA') : null;

    const daysLeft = (iso) => {
        if (!iso) return null;
        return Math.ceil((new Date(iso) - new Date()) / (1000 * 60 * 60 * 24));
    };

    
    const getSubForTier = (tierLevel) =>
        activeSubs.find(s => s.tierLevel === tierLevel) ?? null;

    const openTier = (tierId) => {
        if (!currentUser) { alert('Увійдіть в акаунт щоб підписатися'); return; }
        setActiveTierId(tierId);
        setPaymentModel('SUBSCRIPTION');
    };

    const closeTier = () => setActiveTierId(null);

    if (loading) return <div className="st-loading">Завантаження підписок…</div>;
    if (tiers.length === 0) return null;

    return (
        <div className="st-section">
            <h3 className="st-section-title">Підтримати автора</h3>

            {maxActiveLevel !== null && (
                <div className="st-active-notice">
                    Ваша активна підписка: <strong>Рівень {maxActiveLevel}</strong>
                    {activeSubs.find(s => s.tierLevel === maxActiveLevel)?.expiresAt && (
                        <span className="st-active-notice-expiry">
                            &nbsp;· діє до {formatDate(activeSubs.find(s => s.tierLevel === maxActiveLevel).expiresAt)}
                        </span>
                    )}
                    {activeSubs.find(s => s.tierLevel === maxActiveLevel)?.grantType === 'AUTO' && (
                        <span className="st-active-notice-auto">&nbsp;авто</span>
                    )}
                </div>
            )}

            <div className="st-list">
                {tiers.map(tier => {
                    const isActive = activeTierId === tier.tierId;
                    const isSubscribed = activeSubs.some(s => s.tierLevel >= tier.level);
                    const subInfo = getSubForTier(tier.level);
                    const selectedModel = PAYMENT_MODELS.find(m => m.key === paymentModel);
                    const days = subInfo?.expiresAt ? daysLeft(subInfo.expiresAt) : null;
                    return (
                        <div key={tier.tierId} className={`st-card ${isSubscribed ? 'st-card--subscribed' : ''}`}>
                            <div className="st-card-badge">Рівень {tier.level}</div>
                            {isSubscribed && <div className="st-card-subscribed-label">Підписані</div>}
                            <div className="st-card-body">
                                <h4 className="st-card-name">{tier.name}</h4>
                                {tier.description && (
                                    <p className="st-card-desc">{tier.description}</p>
                                )}
                                {isSubscribed && subInfo?.expiresAt && (
                                    <div className="st-card-expiry">
                                        <span className="st-card-expiry-label">Діє до:</span>
                                        <span className="st-card-expiry-date">{formatDate(subInfo.expiresAt)}</span>
                                        {days !== null && (
                                            <span className={`st-card-expiry-days ${days <= 7 ? 'st-card-expiry-days--warn' : ''}`}>
                                                {days > 0 ? `(${days} дн.)` : 'Закінчується сьогодні'}
                                            </span>
                                        )}
                                        {subInfo.grantType === 'AUTO' && (
                                            <span className="st-card-expiry-auto">авто</span>
                                        )}
                                    </div>
                                )}
                            </div>
                            <div className="st-card-footer">
                                <span className="st-card-price">
                                    ₴{tier.amount}
                                    <span className="st-card-period">/міс</span>
                                </span>

                                {!isActive ? (
                                    <button
                                        className="st-subscribe-btn"
                                        onClick={() => openTier(tier.tierId)}
                                    >
                                        {isSubscribed ? 'Продовжити' : 'Підтримати'}
                                    </button>
                                ) : (
                                    <div className="st-payment-model-panel">
                                        
                                        <p className="st-model-label">Оберіть модель підтримки:</p>
                                        <div className="st-model-tabs">
                                            {PAYMENT_MODELS.map(m => (
                                                <button
                                                    key={m.key}
                                                    className={`st-model-tab ${paymentModel === m.key ? 'st-model-tab--active' : ''}`}
                                                    onClick={() => setPaymentModel(m.key)}
                                                    type="button"
                                                >
                                                    {m.label}
                                                </button>
                                            ))}
                                        </div>
                                        {selectedModel && (
                                            <p className="st-model-hint">{selectedModel.hint}</p>
                                        )}

                                        <div className="st-pay-row">
                                            <WayForPayForm
                                                amount={Number(tier.amount)}
                                                type={paymentModel}
                                                paymentPayload={{
                                                    donor: currentUser?.id ?? 0,
                                                    creator: creatorId,
                                                    reward: tier.tierId,
                                                    project: 0,
                                                    donateId: currentUser?.id ?? 0,
                                                    paymentStatus: 'PENDING',
                                                    isAnonymous: false,
                                                }}
                                                label={`Сплатити ₴${tier.amount}`}
                                                confirmClass="st-pay-btn"
                                                onBeforeSubmit={() => analyticsService.creatorSubscribe(creatorId)}
                                            />
                                            <button className="st-pay-cancel" onClick={closeTier}>✕</button>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    );
                })}
            </div>

            {currentUser && (
                <p className="st-auto-hint">
                    Підписка активується автоматично, якщо ваші донати автору за 30 днів досягнуть суми рівня.
                </p>
            )}
        </div>
    );
};

export default SubscriptionTiersSection;
