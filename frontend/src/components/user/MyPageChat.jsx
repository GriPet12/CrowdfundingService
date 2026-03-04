import { useState, useEffect, useRef } from 'react';
import AuthService from './AuthService.jsx';
import '../../styles/chat.css';

const MyPageChat = () => {
    const currentUser = AuthService.getCurrentUser();
    const [tiers, setTiers] = useState([]);
    const [settings, setSettings] = useState(null);   
    const [messages, setMessages] = useState([]);
    const [loading, setLoading] = useState(true);
    const [savingSettings, setSavingSettings] = useState(false);
    const [selectedLevel, setSelectedLevel] = useState('');  
    const bottomRef = useRef(null);

    const authorId = currentUser?.id;

    
    useEffect(() => {
        if (!authorId) return;
        const headers = { Authorization: `Bearer ${currentUser.token}` };

        Promise.all([
            fetch(`/api/subscription-tiers/creator/${authorId}`, { headers }).then(r => r.ok ? r.json() : []),
            fetch(`/api/chat/${authorId}/settings`, { headers }).then(r => r.ok ? r.json() : null),
            fetch(`/api/chat/${authorId}/messages`, { headers }).then(r => r.ok ? r.json() : []),
        ]).then(([t, s, m]) => {
            setTiers(t.sort((a, b) => a.level - b.level));
            setSettings(s);
            setSelectedLevel(s?.minSubscriptionLevel != null ? String(s.minSubscriptionLevel) : '');
            setMessages(m);
        }).finally(() => setLoading(false));
    }, [authorId]); 

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const handleSaveSettings = async () => {
        setSavingSettings(true);
        const minLevel = selectedLevel === '' ? null : Number(selectedLevel);
        try {
            const res = await fetch(`/api/chat/${authorId}/settings`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${currentUser.token}`,
                },
                body: JSON.stringify({ authorId, minSubscriptionLevel: minLevel }),
            });
            if (res.ok) {
                setSettings({ authorId, minSubscriptionLevel: minLevel });
            }
        } finally {
            setSavingSettings(false);
        }
    };

    const handleDelete = async (messageId) => {
        const res = await fetch(`/api/chat/${authorId}/messages/${messageId}`, {
            method: 'DELETE',
            headers: { Authorization: `Bearer ${currentUser.token}` },
        });
        if (res.ok) {
            setMessages(prev => prev.filter(m => m.messageId !== messageId));
        }
    };

    const formatTime = (iso) => {
        const d = new Date(iso);
        return d.toLocaleString('uk-UA', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
    };

    if (loading) return <div className="chat-loading"><p>Завантаження…</p></div>;

    return (
        <div className="chat-owner-panel">
            
            <div className="chat-settings-block">
                <h3 className="chat-settings-title">Налаштування чату</h3>
                <div className="chat-settings-row">
                    <label className="chat-settings-label">Мінімальний рівень підписки для доступу:</label>
                    <select
                        className="chat-settings-select"
                        value={selectedLevel}
                        onChange={e => setSelectedLevel(e.target.value)}
                    >
                        <option value="">Відкритий для всіх</option>
                        {tiers.map(tier => (
                            <option key={tier.tierId} value={String(tier.level)}>
                                Рівень {tier.level} — {tier.name}
                            </option>
                        ))}
                    </select>
                    <button
                        className="chat-settings-save-btn"
                        onClick={handleSaveSettings}
                        disabled={savingSettings}
                    >
                        {savingSettings ? 'Збереження…' : 'Зберегти'}
                    </button>
                </div>
                {settings !== null && (
                    <p className="chat-settings-status">
                        {settings.minSubscriptionLevel == null
                            ? 'Чат відкритий для всіх відвідувачів'
                            : `Чат доступний від рівня ${settings.minSubscriptionLevel}`}
                    </p>
                )}
            </div>

            
            <div className="chat-container">
                <div className="chat-messages">
                    {messages.length === 0 && (
                        <div className="chat-empty"><p>Повідомлень поки немає.</p></div>
                    )}
                    {messages.map(msg => (
                        <div key={msg.messageId} className="chat-message">
                            <div className="chat-message-avatar">
                                {msg.senderImageId ? (
                                    <img src={`/api/files/${msg.senderImageId}`} alt={msg.senderName} />
                                ) : (
                                    <span>{msg.senderName?.charAt(0).toUpperCase()}</span>
                                )}
                            </div>
                            <div className="chat-message-body">
                                <div className="chat-message-header">
                                    <span className="chat-message-name">{msg.senderName}</span>
                                    <span className="chat-message-time">{formatTime(msg.createdAt)}</span>
                                    <button
                                        className="chat-message-delete"
                                        onClick={() => handleDelete(msg.messageId)}
                                        title="Видалити повідомлення"
                                    >
                                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6M14 11v6"/></svg>
                                    </button>
                                </div>
                                <p className="chat-message-text">{msg.text}</p>
                            </div>
                        </div>
                    ))}
                    <div ref={bottomRef} />
                </div>
            </div>
        </div>
    );
};

export default MyPageChat;

