import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ProjectItem from '../project/ProjectItem.jsx';
import DonateSection from '../pay/DonateSection.jsx';
import SubscriptionTiersSection from '../subscription/SubscriptionTiersSection.jsx';
import PostCard from '../post/PostCard.jsx';
import AuthService from './AuthService.jsx';
import AuthorChat from './AuthorChat.jsx';
import analyticsService from '../../services/analyticsService.js';
import AdminBanButton from '../common/AdminBanButton.jsx';
import ErrorBoundary from '../common/ErrorBoundary.jsx';
import '../../styles/userPage.css';
import '../../styles/projectItem.css';
import '../../styles/postCard.css';

const UserPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [author, setAuthor] = useState(null);
    const [projects, setProjects] = useState([]);
    const [posts, setPosts] = useState([]);
    const [postsVisible, setPostsVisible] = useState(10);
    const [postsLoading, setPostsLoading] = useState(false);
    const [activeTab, setActiveTab] = useState('content');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [followingAuthor, setFollowingAuthor]     = useState(false);
    const [followAuthorLoading, setFollowAuthorLoading] = useState(false);
    const currentUser = AuthService.getCurrentUser();
    const analyticsLoggedRef = useRef(null); 

    useEffect(() => {
        const fetchAuthor = async () => {
            try {
                setLoading(true);
                const response = await fetch(`/api/users/${id}`);
                if (!response.ok) throw new Error(`Помилка сервера: ${response.status}`);
                const data = await response.json();
                setAuthor(data);
                
                if (analyticsLoggedRef.current !== id && String(currentUser?.id) !== String(id)) {
                    analyticsLoggedRef.current = id;
                    analyticsService.creatorView(id);
                }
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };
        fetchAuthor();
    }, [id]);

    useEffect(() => {
        if (!currentUser || !id) return;
        fetch(`/api/follows/authors/${id}/status`, {
            headers: { Authorization: `Bearer ${currentUser.token}` },
        })
            .then(r => r.ok ? r.json() : null)
            .then(data => { if (data) setFollowingAuthor(data.following); })
            .catch(() => {});
    }, [id]); 

    const handleFollowAuthor = async () => {
        setFollowAuthorLoading(true);
        try {
            const res = await fetch(`/api/follows/authors/${id}`, {
                method: 'POST',
                headers: { Authorization: `Bearer ${currentUser.token}` },
            });
            if (res.ok) {
                const newVal = (await res.json()).following;
                setFollowingAuthor(newVal);
                
                if (newVal) analyticsService.creatorFollow(id);
            }
        } finally {
            setFollowAuthorLoading(false);
        }
    };

    useEffect(() => {
        if (activeTab !== 'projects') return;
        const fetchProjects = async () => {
            try {
                const response = await fetch(`/api/projects?creatorId=${id}&page=0`);
                if (!response.ok) throw new Error(`Помилка сервера: ${response.status}`);
                const data = await response.json();
                setProjects(data.content || (Array.isArray(data) ? data : []));
            } catch (err) {
                console.error(err);
            }
        };
        fetchProjects();
    }, [id, activeTab]);

    useEffect(() => {
        if (activeTab !== 'content') return;
        setPostsLoading(true);
        fetch(`/api/posts/author/${id}`, {
            headers: currentUser ? { Authorization: `Bearer ${currentUser.token}` } : {},
        })
            .then(r => r.ok ? r.json() : [])
            .then(data => { setPosts(data); setPostsVisible(10); })
            .catch(() => setPosts([]))
            .finally(() => setPostsLoading(false));
    }, [id, activeTab]); 

    if (loading) return (
        <div className="user-page-loading">
            <p>Завантаження профілю...</p>
        </div>
    );

    if (error) return (
        <div className="user-page-error">
            <p>Сталася помилка: {error}</p>
            <button className="load-more-btn" onClick={() => navigate('/')}>← Назад</button>
        </div>
    );

    if (!author) return null;

    const isOwnProfile = currentUser && String(currentUser.id) === String(id);
    const isPrivate = author.isPrivate && !isOwnProfile;

    if (isPrivate) {
        return (
            <div className="user-page">
                <div className="user-page-back">
                    <button className="user-page-back-btn" onClick={() => navigate(-1)}>← Назад</button>
                </div>
                <div className="user-page-hero">
                    <div className="user-page-avatar-wrapper">
                        <div className="user-page-avatar-placeholder">
                            {author.username ? author.username.charAt(0).toUpperCase() : 'U'}
                        </div>
                    </div>
                    <div className="user-page-info">
                        <h1 className="user-page-name">{author.username}</h1>
                        <p className="user-page-private-notice">Профіль приховано</p>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="user-page">
            
            <div className="user-page-back">
                <button className="user-page-back-btn" onClick={() => navigate(-1)}>
                    ← Назад
                </button>
            </div>

            <div className="user-page-hero">
                <div className="user-page-avatar-wrapper">
                    {author.imageId ? (
                        <img
                            src={`/api/files/${author.imageId}`}
                            alt={author.username}
                            className="user-page-avatar"
                        />
                    ) : (
                        <div className="user-page-avatar-placeholder">
                            {author.username ? author.username.charAt(0).toUpperCase() : 'U'}
                        </div>
                    )}
                </div>

                <div className="user-page-info">
                    <h1 className="user-page-name">{author.username}</h1>
                    {author.email && (
                        <p className="user-page-email">{author.email}</p>
                    )}
                    {author.description && (
                        <p className="user-page-description">{author.description}</p>
                    )}

                    <div className="user-page-actions">
                        <div className="user-page-donate">
                            {!isOwnProfile && !currentUser?.banned && <DonateSection
                                type="DONATION"
                                paymentPayload={{
                                    donateId: currentUser?.id ?? 0,
                                    donor: currentUser?.id ?? 0,
                                    project: 0,
                                    creator: author.id,
                                    reward: 0,
                                    paymentStatus: 'PENDING',
                                    isAnonymous: !currentUser,
                                }}
                                wrapperClass="userpage-donate-section"
                                btnStartClass="userpage-donate-start-wrapper"
                                btnInputClass="userpage-donate-input-wrapper"
                                inputGroupClass="userpage-donate-input-group"
                                startBtnClass="userpage-donate-btn"
                                cancelBtnClass="userpage-donate-cancel"
                                inputClass="userpage-donate-input"
                                confirmClass="userpage-donate-confirm"
                                confirmLabel="✓"
                                placeholder="Сума (₴)"
                                onDonate={() => analyticsService.creatorDonate(author.id)}
                            />}
                        </div>

                        {currentUser && !currentUser.banned && !isOwnProfile && (
                            <button
                                className={`user-page-follow-author-btn ${followingAuthor ? 'user-page-follow-author-btn--active' : ''}`}
                                onClick={handleFollowAuthor}
                                disabled={followAuthorLoading}
                            >
                                {followingAuthor ? 'Ви підписані' : 'Підписатися на автора'}
                            </button>
                        )}

                        {!isOwnProfile && (
                            <AdminBanButton
                                type="user"
                                id={author.id}
                                label={author.username}
                                onDone={() => navigate('/')}
                                withText
                            />
                        )}
                    </div>
                </div>
            </div>

            {!isOwnProfile && (
                <div className="user-page-tiers-section">
                    <SubscriptionTiersSection creatorId={author.id} disabled={!!currentUser?.banned} />
                </div>
            )}

            <div className="user-page-tabs-container">
                <div className="user-page-tabs">
                    <button
                        className={`user-page-tab ${activeTab === 'content' ? 'active' : ''}`}
                        onClick={() => setActiveTab('content')}
                    >
                        Контент
                    </button>
                    <button
                        className={`user-page-tab ${activeTab === 'chat' ? 'active' : ''}`}
                        onClick={() => setActiveTab('chat')}
                    >
                        Чат
                    </button>
                    <button
                        className={`user-page-tab ${activeTab === 'projects' ? 'active' : ''}`}
                        onClick={() => setActiveTab('projects')}
                    >
                        Проекти
                    </button>
                </div>
            </div>

            <div className="user-page-tab-content">
                {currentUser?.banned ? (
                    <div className="user-page-banned-notice">
                        <div className="user-page-banned-icon">🚫</div>
                        <h2 className="user-page-banned-title">Ваш акаунт заблоковано</h2>
                        <p className="user-page-banned-text">
                            Адміністратор заблокував ваш акаунт. Перегляд контенту, підписки, відстеження та донати недоступні.
                        </p>
                    </div>
                ) : (
                <>
                {activeTab === 'content' && (
                    <div className="user-page-content-tab">
                        {postsLoading ? (
                            <div className="user-page-empty"><p>Завантаження постів…</p></div>
                        ) : posts.length === 0 ? (
                            <div className="user-page-empty">
                                <p>Автор поки що не додав контент.</p>
                            </div>
                        ) : (
                            <div className="user-page-posts">
                                {posts.slice(0, postsVisible).map(post => (
                                    <PostCard key={post.postId} post={post} />
                                ))}
                                {posts.length > postsVisible && (
                                    <div className="user-page-load-more">
                                        <button
                                            className="user-page-load-more-btn"
                                            onClick={() => setPostsVisible(v => v + 10)}
                                        >
                                            Завантажити ще ({posts.length - postsVisible} залишилось)
                                        </button>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'chat' && (
                    <div className="user-page-chat-tab">
                        <ErrorBoundary>
                            <AuthorChat authorId={Number(id)} isOwner={isOwnProfile} />
                        </ErrorBoundary>
                    </div>
                )}

                {activeTab === 'projects' && (
                    <div className="user-page-projects-tab">
                        {projects.length === 0 ? (
                            <div className="user-page-empty">
                                <p>Автор поки що не створив жодного проекту.</p>
                            </div>
                        ) : (
                            <div className="projects-grid">
                                {projects.map((project) => (
                                    <ProjectItem
                                        key={project.projectId}
                                        project={project}
                                        onCardClick={(projectId) => analyticsService.projectClick(projectId)}
                                    />
                                ))}
                            </div>
                        )}
                    </div>
                )}
                </>
                )}
            </div>
        </div>
    );
};

export default UserPage;

