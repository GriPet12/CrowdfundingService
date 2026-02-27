import { useState, useEffect } from 'react';
import WayForPayForm from '../pay/WayForPayForm.jsx';
import AuthService from '../user/AuthService.jsx';
import '../../styles/subscriptionTiers.css';

const SubscriptionTiersSection = ({ creatorId }) => {
    const currentUser = AuthService.getCurrentUser();
    const [tiers, setTiers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activeTierId, setActiveTierId] = useState(null); // which tier's pay form is open

    useEffect(() => {
        fetch(`/api/subscription-tiers/creator/${creatorId}`)
            .then(r => r.ok ? r.json() : [])
            .then(data => setTiers(data.sort((a, b) => a.level - b.level)))
            .catch(() => setTiers([]))
            .finally(() => setLoading(false));
    }, [creatorId]);

    if (loading) return <div className="st-loading">Завантаження підписок…</div>;
    if (tiers.length === 0) return null;

    return (
        <div className="st-section">
            <h3 className="st-section-title">Підтримати автора</h3>
            <div className="st-list">
                {tiers.map(tier => {
                    const isActive = activeTierId === tier.tierId;
                    return (
                        <div key={tier.tierId} className="st-card">
                            <div className="st-card-badge">Рівень {tier.level}</div>
                            <div className="st-card-body">
                                <h4 className="st-card-name">{tier.name}</h4>
                                {tier.description && (
                                    <p className="st-card-desc">{tier.description}</p>
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
                                        onClick={() => {
                                            if (!currentUser) {
                                                alert('Увійдіть в акаунт щоб підписатися');
                                                return;
                                            }
                                            setActiveTierId(tier.tierId);
                                        }}
                                    >
                                        Підписатись
                                    </button>
                                ) : (
                                    <div className="st-pay-row">
                                        <WayForPayForm
                                            amount={Number(tier.amount)}
                                            type="SUBSCRIPTION"
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
                                        />
                                        <button
                                            className="st-pay-cancel"
                                            onClick={() => setActiveTierId(null)}
                                        >
                                            ✕
                                        </button>
                                    </div>
                                )}
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default SubscriptionTiersSection;

