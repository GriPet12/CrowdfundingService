import { useState, useRef, useEffect } from 'react';
import AuthService from '../user/AuthService.jsx';
import ConfirmModal from '../common/ConfirmModal.jsx';
import AdminBanButton from '../common/AdminBanButton.jsx';
import '../../styles/postCard.css';

const EXT_ICONS = {
    pdf: 'PDF', doc: 'DOC', docx: 'DOC', xls: 'XLS', xlsx: 'XLS',
    ppt: 'PPT', pptx: 'PPT', txt: 'TXT', csv: 'CSV',
    mp3: 'MP3', wav: 'WAV', ogg: 'OGG', flac: 'FLAC', aac: 'AAC',
    mp4: 'MP4', avi: 'AVI', mov: 'MOV', mkv: 'MKV', webm: 'WEBM',
    jpg: 'JPG', jpeg: 'JPG', png: 'PNG', gif: 'GIF', webp: 'WEBP', svg: 'SVG',
};

const getExt = (fileName) => (fileName ?? '').split('.').pop().toLowerCase();

const getIcon = (fileName) => EXT_ICONS[getExt(fileName)] ?? null;

const isImage = (f) => f.category === 'PHOTO' || f.mimeType?.startsWith('image/');
const isVideo = (f) => f.category === 'VIDEO' || f.mimeType?.startsWith('video/');
const isAudio = (f) => f.category === 'AUDIO' || f.mimeType?.startsWith('audio/');
const isOther = (f) => !isImage(f) && !isVideo(f) && !isAudio(f);

const AudioPlayer = ({ file }) => {
    const audioRef    = useRef(null);
    const trackRef    = useRef(null);
    const fillRef     = useRef(null);
    const thumbRef    = useRef(null);
    const curTimeRef  = useRef(null);
    const isDragging  = useRef(false);
    const [playing, setPlaying] = useState(false);
    const [duration, setDuration] = useState(0);

    const fmt = (s) => {
        if (!isFinite(s) || s < 0) return '0:00';
        const m   = Math.floor(s / 60);
        const sec = Math.floor(s % 60);
        return `${m}:${sec.toString().padStart(2, '0')}`;
    };

    
    const setProgress = (pct) => {
        if (fillRef.current)    fillRef.current.style.width   = `${pct * 100}%`;
        if (thumbRef.current)   thumbRef.current.style.left   = `${pct * 100}%`;
        if (curTimeRef.current) curTimeRef.current.textContent = fmt((audioRef.current?.duration ?? duration) * pct);
    };

    
    const eventToPct = (e) => {
        const rect = trackRef.current.getBoundingClientRect();
        return Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    };

    const seekTo = (pct) => {
        const dur = audioRef.current?.duration;
        if (!dur || !isFinite(dur)) return;
        audioRef.current.currentTime = pct * dur;
        setProgress(pct);
    };

    const handleTimeUpdate = () => {
        if (isDragging.current) return;
        const audio = audioRef.current;
        if (!audio || !audio.duration) return;
        setProgress(audio.currentTime / audio.duration);
    };

    const handleLoadedMetadata = () => {
        setDuration(audioRef.current?.duration ?? 0);
        setProgress(0);
    };

    const handleEnded = () => setPlaying(false);

    const toggle = () => {
        const audio = audioRef.current;
        if (!audio) return;
        if (playing) { audio.pause(); setPlaying(false); }
        else         { audio.play();  setPlaying(true);  }
    };

    
    const handleTrackPointerDown = (e) => {
        e.currentTarget.setPointerCapture(e.pointerId);
        isDragging.current = true;
        const pct = eventToPct(e);
        setProgress(pct);
    };

    const handleTrackPointerMove = (e) => {
        if (!isDragging.current) return;
        setProgress(eventToPct(e));
    };

    const handleTrackPointerUp = (e) => {
        if (!isDragging.current) return;
        isDragging.current = false;
        seekTo(eventToPct(e));
    };

    return (
        <div className="post-audio-player">
            <audio
                ref={audioRef}
                src={`/api/files/${file.id}`}
                onTimeUpdate={handleTimeUpdate}
                onLoadedMetadata={handleLoadedMetadata}
                onEnded={handleEnded}
                preload="metadata"
            />
            <button className="post-audio-play-btn" onClick={toggle} title={playing ? 'Пауза' : 'Грати'}>
                {playing
                    ? <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>
                    : <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><polygon points="5,3 19,12 5,21"/></svg>
                }
            </button>
            <div className="post-audio-info">
                <span className="post-audio-name">{file.originalFileName}</span>
                <div className="post-audio-seek-row">
                    <span ref={curTimeRef} className="post-audio-time">0:00</span>

                    
                    <div
                        ref={trackRef}
                        className="post-audio-track"
                        onPointerDown={handleTrackPointerDown}
                        onPointerMove={handleTrackPointerMove}
                        onPointerUp={handleTrackPointerUp}
                        onPointerCancel={handleTrackPointerUp}
                    >
                        <div className="post-audio-track-bg" />
                        <div ref={fillRef}  className="post-audio-track-fill" />
                        <div ref={thumbRef} className="post-audio-track-thumb" style={{ left: '0%' }} />
                    </div>

                    <span className="post-audio-time">{fmt(duration)}</span>
                </div>
            </div>
        </div>
    );
};

const MediaCarousel = ({ slides }) => {
    
    const sorted = [...slides].sort((a, b) => {
        const aV = isVideo(a) ? 0 : 1;
        const bV = isVideo(b) ? 0 : 1;
        return aV - bV;
    });

    const [idx, setIdx] = useState(0);
    if (sorted.length === 0) return null;

    const prev = () => setIdx(i => (i - 1 + sorted.length) % sorted.length);
    const next = () => setIdx(i => (i + 1) % sorted.length);
    const cur  = sorted[idx];

    return (
        <div className="post-carousel">
            <div className="post-carousel-track">
                {isVideo(cur) ? (
                    <video
                        key={cur.id}
                        className="post-carousel-media"
                        controls
                        src={`/api/files/${cur.id}`}
                        muted
                    />
                ) : (
                    <img
                        key={cur.id}
                        className="post-carousel-media"
                        src={`/api/files/${cur.id}`}
                        alt={cur.originalFileName}
                    />
                )}

                {sorted.length > 1 && (
                    <>
                        <button className="post-carousel-btn post-carousel-btn--prev" onClick={prev}>‹</button>
                        <button className="post-carousel-btn post-carousel-btn--next" onClick={next}>›</button>
                        <div className="post-carousel-dots">
                            {sorted.map((s, i) => (
                                <button
                                    key={s.id}
                                    className={`post-carousel-dot ${i === idx ? 'post-carousel-dot--active' : ''} ${isVideo(s) ? 'post-carousel-dot--video' : ''}`}
                                    onClick={() => setIdx(i)}
                                    title={isVideo(s) ? 'Відео' : `Фото ${i + 1}`}
                                />
                            ))}
                        </div>
                    </>
                )}
            </div>
        </div>
    );
};

const FileAttachment = ({ file }) => {
    const icon = getIcon(file.originalFileName);
    const ext  = getExt(file.originalFileName);
    return (
        <a
            className="post-file-item"
            href={`/api/files/${file.id}/download`}
            download={file.originalFileName}
            title={`Завантажити ${file.originalFileName}`}
        >
            {icon ? (
                <span className="post-file-icon">{icon}</span>
            ) : (
                <span className="post-file-ext-badge">.{ext}</span>
            )}
            <span className="post-file-name">{file.originalFileName}</span>
        </a>
    );
};

const CommentsSection = ({ postId, postAuthorId, open, onCountChange }) => {
    const currentUser = AuthService.getCurrentUser();
    const [comments, setComments] = useState([]);
    const [loaded, setLoaded] = useState(false);
    const [loading, setLoading] = useState(false);
    const [text, setText] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const loadComments = async () => {
        if (loaded) return;
        setLoading(true);
        try {
            const res = await fetch(`/api/posts/${postId}/comments`);
            if (res.ok) setComments(await res.json());
        } finally {
            setLoading(false);
            setLoaded(true);
        }
    };

    
    useEffect(() => {
        if (open) loadComments();
    }, [open]); 

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!text.trim() || !currentUser) return;
        setSubmitting(true);
        try {
            const res = await fetch(`/api/posts/${postId}/comments`, {
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
            }
        } finally {
            setSubmitting(false);
        }
    };

    const handleDelete = async (commentId) => {
        if (!currentUser) return;
        const res = await fetch(`/api/posts/comments/${commentId}`, {
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
        return d.toLocaleDateString('uk-UA', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
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
                    const isAuthor = String(c.authorId) === String(postAuthorId);
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
                                        >✕</button>
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
        </div>
    );
};

const PostCard = ({ post, onDelete }) => {
    const currentUser = AuthService.getCurrentUser();
    const locked  = !post.hasAccess;
    const banned  = !!post.banned;
    const files   = post.files ?? [];

    const [likeCount, setLikeCount] = useState(post.likeCount ?? 0);
    const [likedByMe, setLikedByMe] = useState(post.likedByMe ?? false);
    const [likeLoading, setLikeLoading] = useState(false);
    const [commentsOpen, setCommentsOpen] = useState(false);
    const [commentCount, setCommentCount] = useState(post.commentCount ?? 0);
    const [confirmDelete, setConfirmDelete] = useState(false);

    const carouselSlides = files.filter(f => isImage(f) || isVideo(f));
    const audioFiles     = files.filter(f => isAudio(f));
    const otherFiles     = files.filter(f => isOther(f));

    const handleLike = async () => {
        if (!currentUser || likeLoading) return;
        setLikeLoading(true);
        try {
            const res = await fetch(`/api/posts/${post.postId}/like`, {
                method: 'POST',
                headers: { Authorization: `Bearer ${currentUser.token}` },
            });
            if (res.ok) {
                const data = await res.json();
                setLikeCount(data.likeCount);
                setLikedByMe(data.likedByMe);
            }
        } finally {
            setLikeLoading(false);
        }
    };

    return (
        <div className={`post-card ${locked ? 'post-card--locked' : ''} ${banned ? 'post-card--banned' : ''}`}>

            
            <div className="post-card-header">
                <div className="post-card-title-row">
                    {post.title && <h3 className="post-card-title">{post.title}</h3>}
                    {banned && (
                        <span className="post-access-badge post-access-badge--banned">Заблоковано</span>
                    )}
                    {!banned && (post.requiredTierLevel != null ? (
                        <span className="post-access-badge post-access-badge--locked">
                            {post.requiredTierName ?? `Рівень ${post.requiredTierLevel}`}
                        </span>
                    ) : (
                        <span className="post-access-badge post-access-badge--public">Всі</span>
                    ))}
                </div>
                {onDelete && (
                    <button className="post-card-delete" onClick={() => setConfirmDelete(true)} title="Видалити">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <polyline points="3 6 5 6 21 6"/>
                            <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                            <path d="M10 11v6M14 11v6"/>
                            <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
                        </svg>
                    </button>
                )}
                <AdminBanButton
                    type="post"
                    id={post.postId}
                    label={post.title || `#${post.postId}`}
                    onDone={() => onDelete?.(post.postId)}
                />
            </div>

            {confirmDelete && (
                <ConfirmModal
                    message="Видалити цей пост? Цю дію неможливо скасувати."
                    confirmLabel="Видалити"
                    cancelLabel="Скасувати"
                    onConfirm={() => { setConfirmDelete(false); onDelete(post.postId); }}
                    onCancel={() => setConfirmDelete(false)}
                />
            )}

            
            {banned ? (
                <div className="post-card-banned-body">
                    <div className="post-card-banned-icon">
                        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                        </svg>
                    </div>
                    <p className="post-card-banned-title">Пост заблоковано адміністратором</p>
                    <p className="post-card-banned-hint">Вміст недоступний для перегляду</p>
                </div>
            ) : locked ? (
                
                <div className="post-card-locked-body">
                    <div className="post-card-lock-icon">
                        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                    </div>
                    <p className="post-card-lock-title">Контент доступний підписникам</p>
                    <p className="post-card-lock-hint">
                        Для перегляду потрібна підписка{' '}
                        <strong>
                            {post.requiredTierName ? `«${post.requiredTierName}»` : `Рівень ${post.requiredTierLevel}`}
                        </strong>
                    </p>
                </div>
            ) : (
                <div className="post-card-body">
                    {post.description && <p className="post-card-text">{post.description}</p>}
                    {carouselSlides.length > 0 && <MediaCarousel slides={carouselSlides} />}
                    {audioFiles.length > 0 && (
                        <div className="post-audio-list">
                            {audioFiles.map(f => <AudioPlayer key={f.id} file={f} />)}
                        </div>
                    )}
                    {otherFiles.length > 0 && (
                        <div className="post-files-grid">
                            {otherFiles.map(f => <FileAttachment key={f.id} file={f} />)}
                        </div>
                    )}
                </div>
            )}

            
            <div className="post-card-footer">
                
                <div className="post-card-actions">
                    <button
                        className={`post-like-btn ${likedByMe ? 'post-like-btn--active' : ''} ${!currentUser ? 'post-like-btn--disabled' : ''}`}
                        onClick={handleLike}
                        disabled={likeLoading || !currentUser}
                        title={currentUser ? (likedByMe ? 'Прибрати лайк' : 'Вподобати') : 'Увійдіть, щоб лайкати'}
                    >
                        <svg width="15" height="15" viewBox="0 0 24 24" fill={likedByMe ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                        </svg>
                        {likeCount > 0 && <span>{likeCount}</span>}
                    </button>

                    <button
                        className={`post-comments-toggle ${commentsOpen ? 'post-comments-toggle--open' : ''}`}
                        onClick={() => setCommentsOpen(v => !v)}
                    >
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                        </svg>
                        {commentsOpen ? 'Сховати' : 'Коментарі'}
                        {commentCount > 0 && <span>{commentCount}</span>}
                    </button>
                </div>

                
                <CommentsSection
                    postId={post.postId}
                    postAuthorId={post.masterId}
                    open={commentsOpen}
                    commentCount={commentCount}
                    onCountChange={(delta) => setCommentCount(v => v + delta)}
                />
            </div>
        </div>
    );
};

export default PostCard;
