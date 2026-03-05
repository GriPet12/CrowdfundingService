import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import AuthService from '../user/AuthService.jsx';
import '../../styles/chat.css';

const AuthorChat = ({ authorId, isOwner = false }) => {
    const currentUser = AuthService.getCurrentUser();
    const [access, setAccess] = useState(null);
    const [messages, setMessages] = useState([]);
    const [text, setText] = useState('');
    const [sending, setSending] = useState(false);
    const [loading, setLoading] = useState(true);
    const [connected, setConnected] = useState(false);
    const bottomRef = useRef(null);
    const isAtBottomRef = useRef(true);
    const stompClientRef = useRef(null);

    const [tiers, setTiers] = useState([]);
    const [selectedLevel, setSelectedLevel] = useState('');
    const [savingSettings, setSavingSettings] = useState(false);
    const [settingsSaved, setSettingsSaved] = useState(false);

    // Завантаження налаштувань чату для власника
    useEffect(() => {
        if (!isOwner || !currentUser) return;
        const headers = { Authorization: `Bearer ${currentUser.token}` };
        Promise.all([
            fetch(`/api/subscription-tiers/creator/${authorId}`, { headers }).then(r => r.ok ? r.json() : []),
            fetch(`/api/chat/${authorId}/settings`, { headers }).then(r => r.ok ? r.json() : null),
        ]).then(([t, s]) => {
            setTiers(t.sort((a, b) => a.level - b.level));
            setSelectedLevel(s?.minSubscriptionLevel != null ? String(s.minSubscriptionLevel) : '');
        });
    }, [isOwner, authorId]);

    // Перевірка доступу
    useEffect(() => {
        if (isOwner) {
            setAccess({ hasAccess: true });
            return;
        }
        setAccess(null);
        fetch(`/api/chat/${authorId}/access`, {
            headers: currentUser ? { Authorization: `Bearer ${currentUser.token}` } : {},
        })
            .then(r => r.ok ? r.json() : { hasAccess: false })
            .then(data => setAccess(data))
            .catch(() => setAccess({ hasAccess: false }));
    }, [authorId, isOwner]);

    // Завантаження повідомлень і WebSocket — залежить від hasAccess (примітив)
    const hasAccess = access?.hasAccess ?? false;

    useEffect(() => {
        if (access === null) return; // ще завантажується

        if (!hasAccess) { setLoading(false); return; }

        setLoading(true);
        fetch(`/api/chat/${authorId}/messages`, {
            headers: currentUser ? { Authorization: `Bearer ${currentUser.token}` } : {},
        })
            .then(r => r.ok ? r.json() : [])
            .then(data => {
                setMessages(data);
                setLoading(false);
                setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'instant' }), 50);
            })
            .catch(() => setLoading(false));

        let client;
        try {
            const wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
            const brokerURL = `${wsProtocol}://${window.location.host}/api/ws/websocket`;
            client = new Client({
                brokerURL,
                connectHeaders: currentUser ? { Authorization: `Bearer ${currentUser.token}` } : {},
                reconnectDelay: 5000,
                onConnect: () => {
                    setConnected(true);
                    client.subscribe(`/topic/chat/${authorId}`, (frame) => {
                        const msg = JSON.parse(frame.body);
                        setMessages(prev => {
                            if (prev.some(m => m.messageId === msg.messageId)) return prev;
                            const updated = [...prev, msg];
                            if (isAtBottomRef.current) {
                                setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'instant' }), 0);
                            }
                            return updated;
                        });
                    });
                },
                onDisconnect: () => setConnected(false),
                onStompError: () => setConnected(false),
            });
            client.activate();
            stompClientRef.current = client;
        } catch (err) {
            console.error('WebSocket init error:', err);
        }

        return () => {
            if (stompClientRef.current) {
                stompClientRef.current.deactivate();
                stompClientRef.current = null;
            }
            setConnected(false);
        };
    }, [authorId, access]);

    const handleScroll = (e) => {
        const el = e.currentTarget;
        isAtBottomRef.current = el.scrollHeight - el.scrollTop - el.clientHeight < 60;
    };

    const handleSaveSettings = async () => {
        setSavingSettings(true);
        setSettingsSaved(false);
        const minLevel = selectedLevel === '' ? null : Number(selectedLevel);
        try {
            const res = await fetch(`/api/chat/${authorId}/settings`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${currentUser.token}` },
                body: JSON.stringify({ authorId, minSubscriptionLevel: minLevel }),
            });
            if (res.ok) setSettingsSaved(true);
        } finally {
            setSavingSettings(false);
        }
    };

    const handleSend = () => {
        if (!text.trim() || sending || !stompClientRef.current?.connected) return;
        setSending(true);
        try {
            
            stompClientRef.current.publish({
                destination: `/app/chat/${authorId}/send`,
                body: JSON.stringify({ text: text.trim() }),
            });
            setText('');
            
            setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
        } finally {
            setSending(false);
        }
    };

    const handleDelete = async (messageId) => {
        const res = await fetch(`/api/chat/${authorId}/messages/${messageId}`, {
            method: 'DELETE',
            headers: { Authorization: `Bearer ${currentUser.token}` },
        });
        if (res.ok) setMessages(prev => prev.filter(m => m.messageId !== messageId));
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); }
    };

    const formatTime = (iso) => {
        const d = new Date(iso);
        return d.toLocaleString('uk-UA', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
    };

    if (access === null) return <div className="chat-loading"><p>Завантаження чату…</p></div>;

    if (!isOwner && !access.hasAccess) {
        return (
            <div className="chat-locked">
                <div className="chat-locked-icon">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                </div>
                <p className="chat-locked-title">Чат доступний тільки підписникам</p>
                {access.minSubscriptionLevel != null && (
                    <p className="chat-locked-desc">
                        Для доступу потрібен рівень підписки&nbsp;
                        <strong>
                            {access.requiredTierName ? `«${access.requiredTierName}»` : `≥ ${access.minSubscriptionLevel}`}
                        </strong>
                    </p>
                )}
                <p className="chat-locked-hint">Оформіть підписку нижче, щоб приєднатися до чату</p>
            </div>
        );
    }

    return (
        <div className="chat-owner-panel">
            {isOwner && (
                <div className="chat-settings-block">
                    <h3 className="chat-settings-title">Налаштування чату</h3>
                    <div className="chat-settings-row">
                        <label className="chat-settings-label">Мінімальний рівень підписки для доступу:</label>
                        <select
                            className="chat-settings-select"
                            value={selectedLevel}
                            onChange={e => { setSelectedLevel(e.target.value); setSettingsSaved(false); }}
                        >
                            <option value="">Відкритий для всіх</option>
                            {tiers.map(tier => (
                                <option key={tier.tierId} value={String(tier.level)}>
                                    Рівень {tier.level} — {tier.name}
                                </option>
                            ))}
                        </select>
                        <button className="chat-settings-save-btn" onClick={handleSaveSettings} disabled={savingSettings}>
                            {savingSettings ? 'Збереження…' : 'Зберегти'}
                        </button>
                    </div>
                    {settingsSaved && (
                        <p className="chat-settings-status">
                            {selectedLevel === '' ? 'Чат відкритий для всіх відвідувачів' : `Чат доступний від рівня ${selectedLevel}`}
                        </p>
                    )}
                </div>
            )}

            <div className="chat-container">
                {loading ? (
                    <div className="chat-loading"><p>Завантаження повідомлень…</p></div>
                ) : (
                    <div className="chat-messages" onScroll={handleScroll}>
                        {messages.length === 0 && (
                            <div className="chat-empty"><p>Поки що немає повідомлень. Будьте першим!</p></div>
                        )}
                        {messages.map(msg => {
                            const isOwnMsg = currentUser && msg.senderId === currentUser.id;
                            const isAuthorMsg = msg.senderId === Number(authorId);
                            return (
                                <div
                                    key={msg.messageId}
                                    className={[
                                        'chat-message',
                                        isOwnMsg ? 'chat-message--own' : '',
                                        isAuthorMsg ? 'chat-message--author' : '',
                                    ].filter(Boolean).join(' ')}
                                >
                                    <div className="chat-message-avatar">
                                        {msg.senderImageId
                                            ? <img src={`/api/files/${msg.senderImageId}`} alt={msg.senderName} />
                                            : <span>{msg.senderName?.charAt(0).toUpperCase()}</span>}
                                    </div>
                                    <div className="chat-message-body">
                                        <div className="chat-message-header">
                                            <span className={`chat-message-name ${isAuthorMsg ? 'chat-message-name--author' : ''}`}>
                                                {msg.senderName}
                                                {isAuthorMsg && <span className="chat-message-author-badge">Автор</span>}
                                            </span>
                                            <span className="chat-message-time">{formatTime(msg.createdAt)}</span>
                                            {isOwner && (
                                                <button className="chat-message-delete" onClick={() => handleDelete(msg.messageId)} title="Видалити">
                                                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/></svg>
                                                </button>
                                            )}
                                        </div>
                                        <p className="chat-message-text">{msg.text}</p>
                                    </div>
                                </div>
                            );
                        })}
                        <div ref={bottomRef} />
                    </div>
                )}

                {currentUser ? (
                    <div className="chat-input-area">
                        <textarea
                            className="chat-input"
                            rows={2}
                            maxLength={1000}
                            placeholder={connected ? 'Написати повідомлення… (Enter — надіслати)' : 'Підключення…'}
                            value={text}
                            onChange={e => setText(e.target.value)}
                            onKeyDown={handleKeyDown}
                            disabled={!connected}
                        />
                        <button
                            className="chat-send-btn"
                            onClick={handleSend}
                            disabled={sending || !text.trim() || !connected}
                        >
                            {sending ? '…' : <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M2 21l21-9L2 3v7l15 2-15 2z"/></svg>}
                        </button>
                    </div>
                ) : (
                    <p className="chat-login-hint">Увійдіть, щоб писати в чаті</p>
                )}
            </div>
        </div>
    );
};

export default AuthorChat;

