import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthService from './AuthService.jsx';
import ProjectItem from '../project/ProjectItem.jsx';
import UserItem from './UserItem.jsx';
import SubscriptionTiersManager from '../subscription/SubscriptionTiersManager.jsx';
import AuthorChat from './AuthorChat.jsx';
import PostCard from '../post/PostCard.jsx';
import ConfirmModal from '../common/ConfirmModal.jsx';
import StatsTab from './StatsTab.jsx';
import '../../styles/myPage.css';
import '../../styles/projectItem.css';
import '../../styles/postCard.css';

const ContentEditor = ({ onPublish, tiers = [] }) => {
    const [title, setTitle] = useState('');
    const [text, setText] = useState('');
    const [files, setFiles] = useState([]);
    
    const [visibilityMode, setVisibilityMode] = useState('public');
    const [requiredTierId, setRequiredTierId] = useState('');
    const [minDonationAmount, setMinDonationAmount] = useState('');
    const [publishing, setPublishing] = useState(false);
    const [publishError, setPublishError] = useState('');
    const fileRef = useRef();

    const handleFiles = (e) => {
        const selected = Array.from(e.target.files);
        setFiles(prev => [...prev, ...selected]);
        e.target.value = '';
    };

    const removeFile = (i) => setFiles(prev => prev.filter((_, idx) => idx !== i));

    const handlePublish = async () => {
        if (!title.trim() && !text.trim() && files.length === 0) return;
        setPublishing(true);
        setPublishError('');
        try {
            await onPublish({
                title,
                text,
                files,
                visibilityMode,
                requiredTierId: visibilityMode === 'tier' ? (requiredTierId || null) : null,
                minDonationAmount: visibilityMode === 'donation' ? (Number(minDonationAmount) || null) : null,
            });
            setTitle('');
            setText('');
            setFiles([]);
            setVisibilityMode('public');
            setRequiredTierId('');
            setMinDonationAmount('');
            setPublishError('');
        } catch (err) {
            setPublishError(err.message || 'Не вдалося опублікувати пост. Спробуйте ще раз.');
        } finally {
            setPublishing(false);
        }
    };

    const fileIcon = (file) => {
        if (file.type.startsWith('video/')) return (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/>
            </svg>
        );
        if (file.type.startsWith('image/')) return (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/>
            </svg>
        );
        return (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
            </svg>
        );
    };

    return (
        <div className="my-page-editor">
            <h3 className="my-page-editor-heading">Новий пост</h3>

            <input
                className="my-page-editor-title"
                type="text"
                placeholder="Заголовок"
                value={title}
                onChange={e => setTitle(e.target.value)}
                maxLength={120}
            />

            <textarea
                className="my-page-editor-text"
                placeholder="Напишіть щось для своєї аудиторії…"
                value={text}
                onChange={e => setText(e.target.value)}
                rows={6}
            />

            
            <div className="my-page-editor-access-row">
                <label className="my-page-editor-access-label">Видимість:</label>
                <select
                    className="my-page-editor-access-select"
                    value={visibilityMode}
                    onChange={e => setVisibilityMode(e.target.value)}
                >
                    <option value="public">Відкрито для всіх</option>
                    <option value="private">Тільки для мене</option>
                    <option value="tier">Рівень підписки</option>
                    <option value="donation">Мінімальна сума донату</option>
                </select>
            </div>

            
            {visibilityMode === 'tier' && (
                <div className="my-page-editor-access-row">
                    <label className="my-page-editor-access-label">Рівень:</label>
                    <select
                        className="my-page-editor-access-select"
                        value={requiredTierId}
                        onChange={e => setRequiredTierId(e.target.value)}
                    >
                        <option value="">— Оберіть рівень —</option>
                        {tiers.map(tier => (
                            <option key={tier.tierId} value={String(tier.tierId)}>
                                Рівень {tier.level} — {tier.name} (₴{tier.amount}/міс)
                            </option>
                        ))}
                    </select>
                </div>
            )}

            {visibilityMode === 'donation' && (
                <div className="my-page-editor-access-row">
                    <label className="my-page-editor-access-label">Мін. сума (₴):</label>
                    <input
                        className="my-page-editor-access-select"
                        type="number"
                        min="1"
                        placeholder="Наприклад: 100"
                        value={minDonationAmount}
                        onChange={e => setMinDonationAmount(e.target.value)}
                    />
                </div>
            )}

            {files.length > 0 && (
                <div className="my-page-editor-attachments">
                    {files.map((f, i) => (
                        <div key={i} className="my-page-editor-attachment">
                            {f.type.startsWith('image/') ? (
                                <img
                                    src={URL.createObjectURL(f)}
                                    alt={f.name}
                                    className="my-page-editor-preview-img"
                                />
                            ) : f.type.startsWith('video/') ? (
                                <video
                                    src={URL.createObjectURL(f)}
                                    className="my-page-editor-preview-video"
                                    muted
                                />
                            ) : (
                                <div className="my-page-editor-preview-file">
                                    {fileIcon(f)}<span>{f.name}</span>
                                </div>
                            )}
                            <button
                                className="my-page-editor-remove"
                                onClick={() => removeFile(i)}
                                title="Видалити"
                            >✕</button>
                        </div>
                    ))}
                </div>
            )}

            <div className="my-page-editor-actions">
                <button
                    className="my-page-editor-attach-btn"
                    onClick={() => fileRef.current.click()}
                    type="button"
                >
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/>
                    </svg>
                    Прикріпити файл
                </button>
                <input
                    ref={fileRef}
                    type="file"
                    multiple
                    accept="image/*,video/*,audio/*,.pdf,.doc,.docx"
                    style={{ display: 'none' }}
                    onChange={handleFiles}
                />
                <button
                    className="my-page-editor-publish-btn"
                    onClick={handlePublish}
                    disabled={publishing || (!title.trim() && !text.trim() && files.length === 0)}
                >
                    {publishing ? 'Публікація…' : 'Опублікувати'}
                </button>
            </div>
            {publishError && (
                <p className="my-page-editor-error">{publishError}</p>
            )}
        </div>
    );
};

const ChangePasswordModal = ({ token, onClose }) => {
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        if (newPassword.length < 6) {
            setError('Новий пароль повинен містити мінімум 6 символів.');
            return;
        }
        if (newPassword !== confirmPassword) {
            setError('Паролі не збігаються.');
            return;
        }
        setSaving(true);
        try {
            const res = await fetch('/api/users/me/password', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({ currentPassword, newPassword }),
            });
            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || 'Помилка зміни пароля');
            }
            setSuccess(true);
        } catch (err) {
            setError(err.message);
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="chpwd-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
            <div className="chpwd-modal">
                <button className="chpwd-close" onClick={onClose} aria-label="Закрити">✕</button>
                <h2 className="chpwd-title">Зміна пароля</h2>
                {success ? (
                    <div className="chpwd-success">
                        <p>Пароль успішно змінено!</p>
                        <button className="chpwd-submit-btn" onClick={onClose}>Закрити</button>
                    </div>
                ) : (
                    <form className="chpwd-form" onSubmit={handleSubmit}>
                        <div className="chpwd-field">
                            <label className="chpwd-label">Поточний пароль</label>
                            <input
                                className="chpwd-input"
                                type="password"
                                value={currentPassword}
                                onChange={e => setCurrentPassword(e.target.value)}
                                required
                                autoComplete="current-password"
                            />
                        </div>
                        <div className="chpwd-field">
                            <label className="chpwd-label">Новий пароль</label>
                            <input
                                className="chpwd-input"
                                type="password"
                                value={newPassword}
                                onChange={e => setNewPassword(e.target.value)}
                                required
                                minLength={6}
                                autoComplete="new-password"
                            />
                        </div>
                        <div className="chpwd-field">
                            <label className="chpwd-label">Підтвердити новий пароль</label>
                            <input
                                className="chpwd-input"
                                type="password"
                                value={confirmPassword}
                                onChange={e => setConfirmPassword(e.target.value)}
                                required
                                autoComplete="new-password"
                            />
                        </div>
                        {error && <p className="chpwd-error">{error}</p>}
                        <div className="chpwd-actions">
                            <button className="chpwd-submit-btn" type="submit" disabled={saving}>
                                {saving ? 'Збереження…' : 'Змінити пароль'}
                            </button>
                            <button className="chpwd-cancel-btn" type="button" onClick={onClose} disabled={saving}>
                                Скасувати
                            </button>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
};

const MyPage = () => {
    const navigate = useNavigate();
    const currentUser = AuthService.getCurrentUser();
    const currentUserRef = useRef(currentUser);

    const [profile, setProfile] = useState(null);
    const [projects, setProjects] = useState([]);
    const [posts, setPosts] = useState([]);
    const [postsVisible, setPostsVisible] = useState(10);
    const [myTiers, setMyTiers] = useState([]);
    const [followedProjects, setFollowedProjects] = useState([]);
    const [followedAuthors, setFollowedAuthors]   = useState([]);
    const [mySubscriptions, setMySubscriptions] = useState([]);
    const [myDonations, setMyDonations] = useState([]);
    const [activeTab, setActiveTab] = useState('content');
    const [loading, setLoading] = useState(true);

    
    const [editingProfile, setEditingProfile] = useState(false);
    const [profileForm, setProfileForm] = useState({ username: '', description: '', isPrivate: false });
    const [avatarFile, setAvatarFile] = useState(null);
    const [avatarPreview, setAvatarPreview] = useState(null);
    const [profileSaving, setProfileSaving] = useState(false);
    const [profileError, setProfileError] = useState('');
    const avatarInputRef = useRef();
    const tabsScrollRef = useRef();

    
    const [deletingProjectId, setDeletingProjectId] = useState(null);
    const [changePasswordOpen, setChangePasswordOpen] = useState(false);

    useEffect(() => {
        if (!currentUserRef.current) { navigate('/'); }
    }, [navigate]);

    useEffect(() => {
        const user = currentUserRef.current;
        if (!user) return;
        const fetchProfile = async () => {
            try {
                const res = await fetch('/api/users/me', {
                    headers: { 'Authorization': `Bearer ${user.token}` }
                });
                if (res.ok) {
                    const data = await res.json();
                    setProfile(data);
                    setProfileForm({
                        username: data.username ?? '',
                        description: data.description ?? '',
                        isPrivate: data.isPrivate ?? false,
                    });
                }
            } finally {
                setLoading(false);
            }
        };
        fetchProfile();
        
        fetch(`/api/subscription-tiers/creator/${user.id}`, {
            headers: { Authorization: `Bearer ${user.token}` },
        })
            .then(r => r.ok ? r.json() : [])
            .then(data => setMyTiers(data.sort((a, b) => a.level - b.level)))
            .catch(() => {});
    }, []);  

    
    useEffect(() => {
        const user = currentUserRef.current;
        if (activeTab !== 'content' || !user) return;
        fetch(`/api/posts/author/${user.id}`, {
            headers: { Authorization: `Bearer ${user.token}` },
        })
            .then(r => r.ok ? r.json() : [])
            .then(data => { setPosts(data); setPostsVisible(10); })
            .catch(() => {});
    }, [activeTab]);

    useEffect(() => {
        const user = currentUserRef.current;
        if (activeTab !== 'projects' || !user) return;
        fetch(`/api/projects?creatorId=${user.id}&page=0`, {
            headers: { 'Authorization': `Bearer ${user.token}` }
        })
            .then(r => r.ok ? r.json() : { content: [] })
            .then(data => setProjects(data.content || (Array.isArray(data) ? data : [])))
            .catch(() => {});
    }, [activeTab]);

    useEffect(() => {
        const user = currentUserRef.current;
        if (activeTab !== 'following' || !user) return;
        fetch('/api/follows/projects', {
            headers: { Authorization: `Bearer ${user.token}` },
        })
            .then(r => r.ok ? r.json() : [])
            .then(setFollowedProjects)
            .catch(() => {});
    }, [activeTab]);

    useEffect(() => {
        const user = currentUserRef.current;
        if (activeTab !== 'authors' || !user) return;
        fetch('/api/follows/authors', {
            headers: { Authorization: `Bearer ${user.token}` },
        })
            .then(r => r.ok ? r.json() : [])
            .then(setFollowedAuthors)
            .catch(() => {});
    }, [activeTab]);

    useEffect(() => {
        const user = currentUserRef.current;
        if (activeTab !== 'my-subscriptions' || !user) return;
        fetch('/api/subscriptions/my', {
            headers: { Authorization: `Bearer ${user.token}` },
        })
            .then(r => r.ok ? r.json() : [])
            .then(setMySubscriptions)
            .catch(() => {});
    }, [activeTab]);

    useEffect(() => {
        const user = currentUserRef.current;
        if (activeTab !== 'my-donations' || !user) return;
        fetch('/api/donations/my', {
            headers: { Authorization: `Bearer ${user.token}` },
        })
            .then(r => r.ok ? r.json() : [])
            .then(setMyDonations)
            .catch(() => {});
    }, [activeTab]);

    const handleSaveProfile = async () => {
        setProfileSaving(true);
        setProfileError('');
        try {
            let imageId = profile?.imageId ?? null;

            
            if (avatarFile) {
                const fd = new FormData();
                fd.append('file', avatarFile);
                const res = await fetch('/api/files/upload', {
                    method: 'POST',
                    headers: { Authorization: `Bearer ${currentUser.token}` },
                    body: fd,
                });
                if (res.ok) {
                    const data = await res.json();
                    imageId = data.id ?? data.fileId ?? imageId;
                }
            }

            const res = await fetch('/api/users/me', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${currentUser.token}`,
                },
                body: JSON.stringify({
                    username: profileForm.username.trim(),
                    description: profileForm.description.trim(),
                    isPrivate: profileForm.isPrivate,
                    imageId,
                }),
            });

            if (!res.ok) {
                const msg = await res.text();
                console.error('PUT /api/users/me failed', res.status, msg);
                throw new Error(msg || 'Помилка збереження профілю');
            }
            const updated = await res.json();
            setProfile(updated);
            setAvatarFile(null);
            setAvatarPreview(null);
            setEditingProfile(false);
        } catch (err) {
            setProfileError(err.message);
        } finally {
            setProfileSaving(false);
        }
    };

    const handleDeleteProject = async (projectId) => {
        try {
            
            const checkRes = await fetch(`/api/projects/${projectId}/can-delete`, {
                headers: { Authorization: `Bearer ${currentUser.token}` },
            });
            if (checkRes.ok) {
                const { canDelete, hasDonations } = await checkRes.json();
                if (!canDelete && hasDonations) {
                    alert('Цей проект має донати. Видалення заборонено, щоб захистити меценатів.');
                    setDeletingProjectId(null);
                    return;
                }
            }

            const res = await fetch(`/api/projects/${projectId}`, {
                method: 'DELETE',
                headers: { Authorization: `Bearer ${currentUser.token}` },
            });
            if (res.ok || res.status === 204) {
                setProjects(prev => prev.filter(p => p.projectId !== projectId));
            } else {
                const msg = await res.text();
                alert(msg || 'Не вдалося видалити проект');
            }
        } catch {
            alert('Помилка при видаленні проекту');
        } finally {
            setDeletingProjectId(null);
        }
    };

    const handlePublish = async ({ title, text, files, visibilityMode, requiredTierId, minDonationAmount }) => {
        const uploadedIds = [];
        for (const file of files) {
            const fd = new FormData();
            fd.append('file', file);
            const res = await fetch('/api/files/upload', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${currentUser.token}` },
                body: fd,
            });
            if (res.ok) {
                const data = await res.json();
                uploadedIds.push(data.id ?? data.fileId ?? null);
            }
        }

        const payload = {
            title,
            content: text,
            requiredTierId: requiredTierId ? Number(requiredTierId) : null,
            minDonationAmount: minDonationAmount ? Number(minDonationAmount) : null,
            isPrivate: visibilityMode === 'private',
            mediaIds: uploadedIds.filter(Boolean),
            likeCount: 0,
            commentCount: 0,
        };
        const res = await fetch('/api/posts', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${currentUser.token}`,
            },
            body: JSON.stringify(payload),
        });

        if (res.ok) {
            const newPost = await res.json();
            setPosts(prev => [newPost, ...prev]);
            setPostsVisible(v => Math.max(v, 10));
        } else {
            const msg = await res.text().catch(() => '');
            throw new Error(msg || `Помилка сервера: ${res.status}`);
        }
    };

    const handleDeletePost = async (postId) => {
        const res = await fetch(`/api/posts/${postId}`, {
            method: 'DELETE',
            headers: { Authorization: `Bearer ${currentUser.token}` },
        });
        if (res.ok || res.status === 204) {
            setPosts(prev => prev.filter(p => p.postId !== postId));
        }
    };

    if (!currentUser) return null;
    if (loading) return <div className="my-page-loading"><p>Завантаження...</p></div>;

    const display = profile ?? currentUser;

    return (
        <>
        <div className="my-page">

            <div className="my-page-back">
                <button className="my-page-back-btn" onClick={() => navigate(-1)}>← Назад</button>
            </div>

            <div className="my-page-hero">
                <div className="my-page-avatar-wrapper" style={{ position: 'relative', cursor: editingProfile ? 'pointer' : 'default' }}
                    onClick={() => editingProfile && avatarInputRef.current?.click()}
                    title={editingProfile ? 'Змінити аватар' : ''}
                >
                    {(avatarPreview || display.imageId) ? (
                        <img
                            src={avatarPreview ?? `/api/files/${display.imageId}`}
                            alt={display.username}
                            className="my-page-avatar"
                        />
                    ) : (
                        <div className="my-page-avatar-placeholder">
                            {display.username?.charAt(0).toUpperCase() || 'U'}
                        </div>
                    )}
                    {editingProfile && (
                        <div className="my-page-avatar-overlay">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
                        </div>
                    )}
                    <input
                        ref={avatarInputRef}
                        type="file"
                        accept="image/*"
                        style={{ display: 'none' }}
                        onChange={e => {
                            const f = e.target.files[0];
                            if (f) { setAvatarFile(f); setAvatarPreview(URL.createObjectURL(f)); }
                        }}
                    />
                </div>

                <div className="my-page-info">
                    {!editingProfile ? (
                        <>
                            <h1 className="my-page-name">{display.username}</h1>
                            {display.email && <p className="my-page-email">{display.email}</p>}
                            {display.isVerified
                                ? <span className="my-page-verified-badge">Email підтверджено</span>
                                : <span className="my-page-unverified-badge">Email не підтверджено</span>
                            }
                            {display.description && <p className="my-page-description">{display.description}</p>}
                            {display.isPrivate && <span className="my-page-private-badge">Приватний</span>}
                            <div className="my-page-hero-actions">
                                <button
                                    className="my-page-add-project-btn"
                                    onClick={() => navigate('/projects/new')}
                                >
                                    + Додати проект
                                </button>
                                <button
                                    className="my-page-edit-profile-btn"
                                    onClick={() => setEditingProfile(true)}
                                >
                                    Редагувати профіль
                                </button>
                                <button
                                    className="my-page-change-password-btn"
                                    onClick={() => setChangePasswordOpen(true)}
                                >
                                    Змінити пароль
                                </button>
                            </div>
                        </>
                    ) : (
                        <div className="my-page-profile-form">
                            <h2 className="my-page-profile-form-title">Редагування профілю</h2>

                            <div className="my-page-profile-field">
                                <label className="my-page-profile-label">Ім'я користувача</label>
                                <input
                                    className="my-page-profile-input"
                                    type="text"
                                    maxLength={60}
                                    value={profileForm.username}
                                    onChange={e => setProfileForm(p => ({ ...p, username: e.target.value }))}
                                />
                            </div>

                            <div className="my-page-profile-field">
                                <label className="my-page-profile-label">Опис профілю</label>
                                <textarea
                                    className="my-page-profile-textarea"
                                    rows={3}
                                    maxLength={500}
                                    placeholder="Розкажіть про себе…"
                                    value={profileForm.description}
                                    onChange={e => setProfileForm(p => ({ ...p, description: e.target.value }))}
                                />
                            </div>

                            <div className="my-page-profile-field my-page-profile-field--row">
                                <label className="my-page-profile-label">
                                    <input
                                        type="checkbox"
                                        checked={profileForm.isPrivate}
                                        onChange={e => setProfileForm(p => ({ ...p, isPrivate: e.target.checked }))}
                                        style={{ marginRight: 8 }}
                                    />
                                    Приватний профіль
                                </label>
                                <span className="my-page-profile-hint">Приватний профіль прихований від загального пошуку</span>
                            </div>

                            {profileError && <p className="my-page-profile-error">{profileError}</p>}

                            <div className="my-page-profile-actions">
                                <button
                                    className="my-page-profile-save-btn"
                                    onClick={handleSaveProfile}
                                    disabled={profileSaving}
                                >
                                    {profileSaving ? 'Збереження…' : 'Зберегти'}
                                </button>
                                <button
                                    className="my-page-profile-cancel-btn"
                                    onClick={() => {
                                        setEditingProfile(false);
                                        setAvatarFile(null);
                                        setAvatarPreview(null);
                                        setProfileError('');
                                    }}
                                    disabled={profileSaving}
                                >
                                    Скасувати
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            <div className="my-page-tabs-container">
                <button
                    className="my-page-tabs-arrow my-page-tabs-arrow-left"
                    onClick={() => tabsScrollRef.current && (tabsScrollRef.current.scrollLeft -= 160)}
                    aria-label="Прокрутити вліво"
                >‹</button>
                <div className="my-page-tabs" ref={tabsScrollRef}>
                    {[
                        { key: 'content',          label: 'Контент' },
                        { key: 'stats',            label: 'Статистика' },
                        { key: 'chat',             label: 'Чат' },
                        { key: 'projects',         label: 'Проекти' },
                        { key: 'subscriptions',    label: 'Мої підписки' },
                        { key: 'following',        label: 'Відстежувані' },
                        { key: 'authors',          label: 'Автори' },
                        { key: 'my-subscriptions', label: 'Підписки' },
                        { key: 'my-donations',     label: 'Донати' },
                    ].map(({ key, label }) => (
                        <button
                            key={key}
                            className={`my-page-tab ${activeTab === key ? 'active' : ''}`}
                            onClick={() => setActiveTab(key)}
                        >
                            {label}
                        </button>
                    ))}
                </div>
                <button
                    className="my-page-tabs-arrow my-page-tabs-arrow-right"
                    onClick={() => tabsScrollRef.current && (tabsScrollRef.current.scrollLeft += 160)}
                    aria-label="Прокрутити вправо"
                >›</button>
            </div>

            <div className="my-page-tab-content">

                {activeTab === 'content' && (
                    <div className="my-page-content-tab">
                        <ContentEditor onPublish={handlePublish} tiers={myTiers} />

                        {posts.length > 0 && (
                            <div className="my-page-posts">
                                {posts.slice(0, postsVisible).map((post) => (
                                    <PostCard key={post.postId} post={post} onDelete={handleDeletePost} />
                                ))}
                            </div>
                        )}

                        {posts.length > postsVisible && (
                            <div className="my-page-load-more">
                                <button
                                    className="my-page-load-more-btn"
                                    onClick={() => setPostsVisible(v => v + 10)}
                                >
                                    Завантажити ще ({posts.length - postsVisible} залишилось)
                                </button>
                            </div>
                        )}

                        {posts.length === 0 && (
                            <div className="my-page-empty">
                                <p>Ви ще не опублікували жодного посту.</p>
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'stats' && (
                    <div className="my-page-stats-tab">
                        <StatsTab userId={currentUser.id} token={currentUser.token} />
                    </div>
                )}

                {activeTab === 'chat' && (
                    <div className="my-page-chat-tab">
                        <AuthorChat authorId={currentUser.id} isOwner={true} />
                    </div>
                )}

                {activeTab === 'projects' && (
                    <div className="my-page-projects-tab">
                        <div className="my-page-projects-header">
                            <button
                                className="my-page-add-project-btn"
                                onClick={() => navigate('/projects/new')}
                            >
                                + Додати проект
                            </button>
                        </div>
                        {projects.length === 0 ? (
                            <div className="my-page-empty">
                                <p>Ви ще не створили жодного проекту.</p>
                            </div>
                        ) : (
                            <div className="projects-grid">
                                {projects.map(p => (
                                    <div key={p.projectId} className="my-page-project-wrapper">
                                        <ProjectItem project={p} />
                                        <div className="my-page-project-actions">
                                            <button
                                                className="my-page-project-edit-btn"
                                                onClick={() => navigate(`/projects/${p.projectId}/edit`)}
                                            >
                                                Редагувати
                                            </button>
                                            <button
                                                className="my-page-project-delete-btn"
                                                onClick={() => setDeletingProjectId(p.projectId)}
                                            >
                                                Видалити
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}

                        {deletingProjectId && (
                            <ConfirmModal
                                message="Видалити цей проект? Якщо є донати — видалення буде заборонено."
                                confirmLabel="Видалити"
                                cancelLabel="Скасувати"
                                onConfirm={() => handleDeleteProject(deletingProjectId)}
                                onCancel={() => setDeletingProjectId(null)}
                            />
                        )}
                    </div>
                )}

                {activeTab === 'subscriptions' && (
                    <div className="my-page-subscriptions-tab">
                        <SubscriptionTiersManager />
                    </div>
                )}

                {activeTab === 'following' && (
                    <div className="my-page-following-tab">
                        {followedProjects.length === 0 ? (
                            <div className="my-page-empty">
                                <p>Ви ще не відстежуєте жодного проекту. Натисніть ♡ на картці проекту.</p>
                            </div>
                        ) : (
                            <div className="projects-grid">
                                {followedProjects.map(p => (
                                    <ProjectItem
                                        key={p.projectId}
                                        project={p}
                                        initialFollowing={true}
                                        onFollowChange={(id, isFollowing) => {
                                            if (!isFollowing)
                                                setFollowedProjects(prev => prev.filter(x => x.projectId !== id));
                                        }}
                                    />
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'authors' && (
                    <div className="my-page-authors-tab">
                        {followedAuthors.length === 0 ? (
                            <div className="my-page-empty">
                                <p>Ви ще не підписані на жодного автора. Натисніть «☆ Підписатися» на сторінці автора.</p>
                            </div>
                        ) : (
                            <div className="authors-grid">
                                {followedAuthors.map(u => (
                                    <UserItem
                                        key={u.id}
                                        user={u}
                                        initialFollowing={true}
                                        onFollowChange={(id, isFollowing) => {
                                            if (!isFollowing)
                                                setFollowedAuthors(prev => prev.filter(x => x.id !== id));
                                        }}
                                    />
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'my-subscriptions' && (
                    <div className="my-page-my-subscriptions-tab">
                        {mySubscriptions.length === 0 ? (
                            <div className="my-page-empty">
                                <p>У вас ще немає активних підписок.</p>
                            </div>
                        ) : (
                            <div className="my-subs-list">
                                {mySubscriptions.map(sub => {
                                    const days = sub.expiresAt
                                        ? Math.ceil((new Date(sub.expiresAt) - new Date()) / (1000 * 60 * 60 * 24))
                                        : null;
                                    return (
                                    <div key={sub.subscriptionId} className={`my-subs-card ${!sub.isActive ? 'my-subs-card--expired' : ''}`}>
                                        <div className="my-subs-avatar">
                                            {sub.creatorImageId ? (
                                                <img
                                                    src={`/api/files/${sub.creatorImageId}`}
                                                    alt={sub.creatorName}
                                                />
                                            ) : (
                                                <span>{sub.creatorName?.charAt(0).toUpperCase()}</span>
                                            )}
                                        </div>
                                        <div className="my-subs-info">
                                            <p className="my-subs-creator">{sub.creatorName}</p>
                                            <p className="my-subs-tier">
                                                <span className="my-subs-tier-badge">Рівень {sub.tierLevel}</span>
                                                {sub.tierName}
                                            </p>
                                            {sub.grantType === 'AUTO' && (
                                                <span className="my-subs-grant-badge my-subs-grant-badge--auto">Авто</span>
                                            )}
                                            {sub.expiresAt && (
                                                <p className="my-subs-expiry">
                                                    Діє до: <strong>{new Date(sub.expiresAt).toLocaleDateString('uk-UA')}</strong>
                                                    {sub.isActive && days !== null && (
                                                        <span className={`my-subs-days-left ${days <= 7 ? 'my-subs-days-left--warn' : ''}`}>
                                                            {days > 0 ? ` · ${days} дн.` : ' · Закінчується сьогодні'}
                                                        </span>
                                                    )}
                                                </p>
                                            )}
                                        </div>
                                        <div className="my-subs-price">
                                            ₴{sub.tierPrice}
                                            <span className="my-subs-period">/міс</span>
                                        </div>
                                        <div className={`my-subs-status ${sub.isActive ? 'my-subs-status--active' : 'my-subs-status--expired'}`}>
                                            {sub.isActive ? 'Активна' : 'Закінчилась'}
                                        </div>
                                    </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                )}
                {activeTab === 'my-donations' && (
                    <div className="my-page-my-donations-tab">
                        {myDonations.length === 0 ? (
                            <div className="my-page-empty">
                                <p>Ви ще не робили жодного донату.</p>
                            </div>
                        ) : (
                            <div className="my-donations-list">
                                {myDonations.map((don, idx) => (
                                    <div key={don.donationId ?? idx} className="my-donations-card">
                                        <div className="my-donations-icon">
                                            {don.projectTitle ? 'П' : 'А'}
                                        </div>
                                        <div className="my-donations-info">
                                            <p className="my-donations-target">
                                                {don.projectTitle
                                                    ? <><strong>{don.projectTitle}</strong><span className="my-donations-type">Проект</span></>
                                                    : <><strong>{don.creatorName ?? 'Автор'}</strong><span className="my-donations-type">Автор</span></>
                                                }
                                            </p>
                                            {don.rewardName && (
                                                <p className="my-donations-reward">Винагорода: {don.rewardName}</p>
                                            )}
                                            <p className="my-donations-date">
                                                {don.createdAt ? new Date(don.createdAt).toLocaleDateString('uk-UA', {
                                                    day: '2-digit', month: 'short', year: 'numeric'
                                                }) : ''}
                                            </p>
                                        </div>
                                        <div className="my-donations-amount">
                                            ₴{don.amount}
                                        </div>
                                        <div className={`my-donations-status my-donations-status--${(don.paymentStatus ?? '').toLowerCase()}`}>
                                            {(don.paymentStatus === 'SUCCESS' || don.paymentStatus === 'APPROVED') ? 'Успішно'
                                                : don.paymentStatus === 'PENDING' ? 'Обробка'
                                                : (don.paymentStatus === 'FAILED' || don.paymentStatus === 'DECLINED') ? 'Помилка'
                                                : don.paymentStatus ?? ''}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

            </div>
        </div>

        {changePasswordOpen && (
            <ChangePasswordModal
                token={currentUser.token}
                onClose={() => setChangePasswordOpen(false)}
            />
        )}
        </>
    );
};

export default MyPage;

