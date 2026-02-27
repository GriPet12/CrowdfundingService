import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ProjectItem from '../project/ProjectItem.jsx';
import DonateSection from '../pay/DonateSection.jsx';
import SubscriptionTiersSection from '../subscription/SubscriptionTiersSection.jsx';
import AuthService from './AuthService.jsx';
import '../../styles/userPage.css';
import '../../styles/projectItem.css';

const UserPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [author, setAuthor] = useState(null);
    const [projects, setProjects] = useState([]);
    const [activeTab, setActiveTab] = useState('content');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [followingAuthor, setFollowingAuthor]     = useState(false);
    const [followAuthorLoading, setFollowAuthorLoading] = useState(false);
    const currentUser = AuthService.getCurrentUser();

    useEffect(() => {
        const fetchAuthor = async () => {
            try {
                setLoading(true);
                const response = await fetch(`/api/users/${id}`);
                if (!response.ok) throw new Error(`Помилка сервера: ${response.status}`);
                const data = await response.json();
                setAuthor(data);
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
    }, [id]); // eslint-disable-line

    const handleFollowAuthor = async () => {
        if (!currentUser) { alert('Увійдіть щоб підписатись на автора'); return; }
        setFollowAuthorLoading(true);
        try {
            const res = await fetch(`/api/follows/authors/${id}`, {
                method: 'POST',
                headers: { Authorization: `Bearer ${currentUser.token}` },
            });
            if (res.ok) setFollowingAuthor((await res.json()).following);
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

    return (
        <div className="user-page">
            {/* Back button */}
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

                    <div className="user-page-donate">
                        {!isOwnProfile && <DonateSection
                            type="DONATION"
                            paymentPayload={{
                                donateId: author.id,
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
                        />}
                    </div>

                    {!isOwnProfile && (
                        <button
                            className={`user-page-follow-author-btn ${followingAuthor ? 'user-page-follow-author-btn--active' : ''}`}
                            onClick={handleFollowAuthor}
                            disabled={followAuthorLoading}
                        >
                            {followingAuthor ? '★ Ви підписані' : '☆ Підписатися на автора'}
                        </button>
                    )}

                    {!isOwnProfile && <SubscriptionTiersSection creatorId={author.id} />}
                </div>
            </div>

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
                {activeTab === 'content' && (
                    <div className="user-page-content-tab">
                        {author.content ? (
                            <div className="user-page-content-text"
                                 dangerouslySetInnerHTML={{ __html: author.content }}
                            />
                        ) : (
                            <div className="user-page-empty">
                                <p>Автор поки що не додав контент.</p>
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'chat' && (
                    <div className="user-page-chat-tab">
                        <div className="user-page-empty">
                            <p>Чат з автором буде доступний незабаром.</p>
                        </div>
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
                                    <ProjectItem key={project.projectId} project={project} />
                                ))}
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default UserPage;

