import { useState, useEffect } from 'react';
import AuthService from '../user/AuthService.jsx';
import '../../styles/subscriptionTiers.css';

const SubscriptionTiersManager = () => {
    const currentUser = AuthService.getCurrentUser();
    const [tiers, setTiers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);

    const [form, setForm] = useState({ name: '', description: '', amount: '', level: 1 });
    const [saving, setSaving] = useState(false);
    const [formError, setFormError] = useState('');

    useEffect(() => {
        if (!currentUser) return;
        fetch(`/api/subscription-tiers/creator/${currentUser.id}`, {
            headers: { Authorization: `Bearer ${currentUser.token}` },
        })
            .then(r => r.ok ? r.json() : [])
            .then(setTiers)
            .catch(() => setTiers([]))
            .finally(() => setLoading(false));
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    const handleCreate = async () => {
        if (!form.name.trim()) { setFormError('Назва обов\'язкова'); return; }
        if (!form.amount || Number(form.amount) <= 0) { setFormError('Сума має бути > 0'); return; }
        setFormError('');
        setSaving(true);
        try {
            const res = await fetch('/api/subscription-tiers', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${currentUser.token}`,
                },
                body: JSON.stringify({
                    name: form.name.trim(),
                    description: form.description.trim(),
                    amount: Number(form.amount),
                    level: Number(form.level),
                    creatorId: currentUser.id,
                }),
            });
            if (!res.ok) {
                setFormError('Не вдалося створити рівень. Спробуйте ще раз.');
                return;
            }
            const created = await res.json();
            setTiers(prev => [...prev, created].sort((a, b) => a.level - b.level));
            setForm({ name: '', description: '', amount: '', level: 1 });
            setShowForm(false);
        } catch {
            setFormError('Не вдалося створити рівень. Перевірте з\'єднання.');
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (tierId) => {
        if (!window.confirm('Видалити цей рівень підписки?')) return;
        try {
            const res = await fetch(`/api/subscription-tiers/${tierId}`, {
                method: 'DELETE',
                headers: { Authorization: `Bearer ${currentUser.token}` },
            });
            if (res.ok || res.status === 204) {
                setTiers(prev => prev.filter(t => t.tierId !== tierId));
            }
        } catch {
            alert('Не вдалося видалити рівень.');
        }
    };

    if (loading) return <div className="st-loading">Завантаження…</div>;

    return (
        <div className="st-manager">
            <div className="st-manager-header">
                <h3 className="st-manager-title">Рівні підписки</h3>
                <button
                    className="st-add-btn"
                    onClick={() => { setShowForm(v => !v); setFormError(''); }}
                >
                    {showForm ? '✕ Скасувати' : '+ Новий рівень'}
                </button>
            </div>

            {showForm && (
                <div className="st-form">
                    <div className="st-form-row">
                        <label className="st-label">Назва*</label>
                        <input
                            className="st-input"
                            type="text"
                            maxLength={60}
                            placeholder="Наприклад: Базовий"
                            value={form.name}
                            onChange={e => setForm(p => ({ ...p, name: e.target.value }))}
                        />
                    </div>
                    <div className="st-form-row">
                        <label className="st-label">Опис</label>
                        <textarea
                            className="st-textarea"
                            rows={3}
                            placeholder="Що отримає підписник?"
                            value={form.description}
                            onChange={e => setForm(p => ({ ...p, description: e.target.value }))}
                        />
                    </div>
                    <div className="st-form-row st-form-row--inline">
                        <div className="st-form-field">
                            <label className="st-label">Сума (₴/міс)*</label>
                            <input
                                className="st-input"
                                type="number"
                                min="1"
                                placeholder="50"
                                value={form.amount}
                                onChange={e => setForm(p => ({ ...p, amount: e.target.value }))}
                            />
                        </div>
                        <div className="st-form-field">
                            <label className="st-label">Рівень</label>
                            <input
                                className="st-input"
                                type="number"
                                min="1"
                                max="10"
                                value={form.level}
                                onChange={e => setForm(p => ({ ...p, level: e.target.value }))}
                            />
                        </div>
                    </div>
                    {formError && <p className="st-form-error">{formError}</p>}
                    <button className="st-save-btn" onClick={handleCreate} disabled={saving}>
                        {saving ? 'Збереження…' : 'Зберегти рівень'}
                    </button>
                </div>
            )}

            {tiers.length === 0 && !showForm ? (
                <div className="st-empty">
                    <p>У вас ще немає рівнів підписки. Натисніть «+ Новий рівень» щоб додати.</p>
                </div>
            ) : (
                <div className="st-list">
                    {tiers.map(tier => (
                        <div key={tier.tierId} className="st-card st-card--manage">
                            <div className="st-card-badge">Рівень {tier.level}</div>
                            <div className="st-card-body">
                                <h4 className="st-card-name">{tier.name}</h4>
                                {tier.description && (
                                    <p className="st-card-desc">{tier.description}</p>
                                )}
                            </div>
                            <div className="st-card-footer">
                                <span className="st-card-price">₴{tier.amount}<span className="st-card-period">/міс</span></span>
                                <button
                                    className="st-delete-btn"
                                    onClick={() => handleDelete(tier.tierId)}
                                    title="Видалити рівень"
                                >
                                    🗑
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default SubscriptionTiersManager;

