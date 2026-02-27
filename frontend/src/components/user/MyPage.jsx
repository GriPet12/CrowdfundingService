import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthService from './AuthService.jsx';
import ProjectItem from '../project/ProjectItem.jsx';
import UserItem from './UserItem.jsx';
import SubscriptionTiersManager from '../subscription/SubscriptionTiersManager.jsx';
import '../../styles/myPage.css';
import '../../styles/projectItem.css';

const ContentEditor = ({ onPublish }) => {
    const [title, setTitle] = useState('');
    const [text, setText] = useState('');
    const [files, setFiles] = useState([]);
    const [publishing, setPublishing] = useState(false);
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
        try {
            await onPublish({ title, text, files });
            setTitle('');
            setText('');
            setFiles([]);
        } finally {
            setPublishing(false);
        }
    };

    const fileIcon = (file) => {
        if (file.type.startsWith('video/')) return '🎬';
        if (file.type.startsWith('image/')) return '🖼';
        return '📎';
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
                    📎 Прикріпити файл
                </button>
                <input
                    ref={fileRef}
                    type="file"
                    multiple
                    accept="image/*,video/*,.pdf,.doc,.docx"
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
    const [followedProjects, setFollowedProjects] = useState([]);
    const [followedAuthors, setFollowedAuthors]   = useState([]);
    const [mySubscriptions, setMySubscriptions] = useState([]);
    const [activeTab, setActiveTab] = useState('content');
    const [loading, setLoading] = useState(true);

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
                if (res.ok) setProfile(await res.json());
            } finally {
                setLoading(false);
            }
        };
        fetchProfile();
    }, []);

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
        fetch('/api/follows/subscriptions', {
            headers: { Authorization: `Bearer ${user.token}` },
        })
            .then(r => r.ok ? r.json() : [])
            .then(setMySubscriptions)
            .catch(() => {});
    }, [activeTab]);

    const handlePublish = async ({ title, text, files }) => {
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

        const payload = { title, content: text, mediaIds: uploadedIds.filter(Boolean) };
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
        }
    };

    if (!currentUser) return null;
    if (loading) return <div className="my-page-loading"><p>Завантаження...</p></div>;

    const display = profile ?? currentUser;

    return (
        <div className="my-page">

            <div className="my-page-back">
                <button className="my-page-back-btn" onClick={() => navigate(-1)}>← Назад</button>
            </div>

            <div className="my-page-hero">
                <div className="my-page-avatar-wrapper">
                    {display.imageId ? (
                        <img
                            src={`/api/files/${display.imageId}`}
                            alt={display.username}
                            className="my-page-avatar"
                        />
                    ) : (
                        <div className="my-page-avatar-placeholder">
                            {display.username?.charAt(0).toUpperCase() || 'U'}
                        </div>
                    )}
                </div>

                <div className="my-page-info">
                    <h1 className="my-page-name">{display.username}</h1>
                    {display.email && <p className="my-page-email">{display.email}</p>}

                    <div className="my-page-hero-actions">
                        <button
                            className="my-page-add-project-btn"
                            onClick={() => navigate('/projects/new')}
                        >
                            + Додати проект
                        </button>
                    </div>
                </div>
            </div>

            <div className="my-page-tabs-container">
                <div className="my-page-tabs">
                    {[
                        { key: 'content',          label: 'Контент' },
                        { key: 'chat',             label: 'Чат' },
                        { key: 'projects',         label: 'Проекти' },
                        { key: 'subscriptions',    label: 'Мої підписки' },
                        { key: 'following',        label: '♥ Проекти' },
                        { key: 'authors',          label: '★ Автори' },
                        { key: 'my-subscriptions', label: '💳 Підписки' },
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
            </div>

            <div className="my-page-tab-content">

                {activeTab === 'content' && (
                    <div className="my-page-content-tab">
                        <ContentEditor onPublish={handlePublish} />

                        {posts.length > 0 && (
                            <div className="my-page-posts">
                                {posts.map((post, i) => (
                                    <div key={post.id ?? i} className="my-page-post">
                                        {post.title && <h3 className="my-page-post-title">{post.title}</h3>}
                                        {post.content && <p className="my-page-post-text">{post.content}</p>}
                                    </div>
                                ))}
                            </div>
                        )}

                        {posts.length === 0 && (
                            <div className="my-page-empty">
                                <p>Ви ще не опублікували жодного посту.</p>
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'chat' && (
                    <div className="my-page-chat-tab">
                        <div className="my-page-empty">
                            <p>Чат буде доступний незабаром.</p>
                        </div>
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
                                    <ProjectItem key={p.projectId} project={p} />
                                ))}
                            </div>
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
                                {mySubscriptions.map(sub => (
                                    <div key={sub.subscriptionId} className="my-subs-card">
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
                                        </div>
                                        <div className="my-subs-price">
                                            ₴{sub.tierPrice}
                                            <span className="my-subs-period">/міс</span>
                                        </div>
                                        <div className={`my-subs-status my-subs-status--${sub.paymentStatus.toLowerCase()}`}>
                                            {sub.paymentStatus === 'APPROVED' ? '✓ Активна' : sub.paymentStatus}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default MyPage;

