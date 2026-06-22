import { useState, useEffect } from 'react';
import AuthService from '../user/AuthService.jsx';
import '../../styles/postCard.css';

const CommentsSection = ({
    entityId,
    ownerId,
    apiBasePath = '/api/posts',
    open,
    commentCount,
    onCountChange,
}) => {
    const currentUser = AuthService.getCurrentUser();
    const [comments, setComments] = useState([]);
    const [loaded, setLoaded] = useState(false);
    const [loading, setLoading] = useState(false);
    const [text, setText] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState('');

    useEffect(() => {
        setComments([]);
        setLoaded(false);
    }, [entityId, apiBasePath]);

    const loadComments = async () => {
        if (loaded) return;
        setLoading(true);
        try {
            const res = await fetch(`${apiBasePath}/${entityId}/comments`);
            if (res.ok) setComments(await res.json());
        } finally {
            setLoading(false);
            setLoaded(true);
        }
    };

    useEffect(() => {
        if (open) loadComments();
    }, [open, entityId, apiBasePath]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!text.trim() || !currentUser) return;
        setSubmitting(true);
        setSubmitError('');
        try {
            const res = await fetch(`${apiBasePath}/${entityId}/comments`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${currentUser.token}`,
                },
                body: JSON.stringify({ text: text.trim() }),
            });
            if (res.ok) {
                const newComment = await res.json();
                setComments(prev => [...prev, newComment]);
                onCountChange?.(1);
                setText('');
            } else {
                const data = await res.json().catch(() => ({}));
                setSubmitError(data.message || 'Не вдалося додати коментар. Спробуйте ще раз.');
            }
        } catch {
            setSubmitError('Не вдалося додати коментар. Спробуйте ще раз.');
        } finally {
            setSubmitting(false);
        }
    };

    const handleDelete = async (commentId) => {
        if (!currentUser) return;
        const res = await fetch(`${apiBasePath}/comments/${commentId}`, {
            method: 'DELETE',
            headers: { Authorization: `Bearer ${currentUser.token}` },
        });
        if (res.ok || res.status === 204) {
            setComments(prev => prev.filter(c => c.commentId !== commentId));
            onCountChange?.(-1);
        }
    };

    const fmtDate = (iso) => {
        const d = new Date(iso);
        return d.toLocaleDateString('uk-UA', {
            day: '2-digit',
            month: 'short',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    if (!open) return null;

    return (
        <div className="post-comments-body">
            {loading && <p className="post-comments-loading">Завантаження…</p>}

            {!loading && comments.length === 0 && (
                <p className="post-comments-empty">Коментарів поки немає. Будьте першим!</p>
            )}

            <div className="post-comments-list">
                {comments.map(c => {
                    const isAuthor = String(c.authorId) === String(ownerId);
                    return (
                        <div key={c.commentId} className={`post-comment ${isAuthor ? 'post-comment--author' : ''}`}>
                            <div className="post-comment-avatar">
                                {c.authorImageId ? (
                                    <img src={`/api/files/${c.authorImageId}`} alt={c.authorName} />
                                ) : (
                                    <span>{c.authorName?.charAt(0).toUpperCase()}</span>
                                )}
                            </div>
                            <div className="post-comment-content">
                                <div className="post-comment-header">
                                    <span className="post-comment-author">{c.authorName}</span>
                                    {isAuthor && <span className="post-comment-author-badge">Автор</span>}
                                    <span className="post-comment-date">{fmtDate(c.createdAt)}</span>
                                    {currentUser && String(currentUser.id) === String(c.authorId) && (
                                        <button
                                            className="post-comment-delete"
                                            onClick={() => handleDelete(c.commentId)}
                                            title="Видалити"
                                        >
                                            ✕
                                        </button>
                                    )}
                                </div>
                                <p className="post-comment-text">{c.commentText}</p>
                            </div>
                        </div>
                    );
                })}
            </div>

            {currentUser ? (
                <form className="post-comment-form" onSubmit={handleSubmit}>
                    <input
                        className="post-comment-input"
                        type="text"
                        placeholder="Написати коментар…"
                        value={text}
                        onChange={e => setText(e.target.value)}
                        maxLength={2000}
                    />
                    <button
                        className="post-comment-submit"
                        type="submit"
                        disabled={submitting || !text.trim()}
                    >
                        {submitting ? '…' : (
                            <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M2 21l21-9L2 3v7l15 2-15 2z"/>
                            </svg>
                        )}
                    </button>
                </form>
            ) : (
                <p className="post-comments-login-hint">Увійдіть, щоб залишити коментар.</p>
            )}
            {submitError && <p className="post-comment-error">{submitError}</p>}
        </div>
    );
};

export default CommentsSection;
