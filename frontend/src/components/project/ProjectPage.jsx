import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import DonateSection from '../pay/DonateSection.jsx';
import AuthService from '../user/AuthService.jsx';
import '../../styles/projectPage.css';

const getLabel = (category) => {
    if (!category) return null;
    if (typeof category === 'string') return category;
    return category.name ?? category.title ?? category.label ?? null;
};

const ProjectPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [project, setProject] = useState(null);
    const [author, setAuthor] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activeSlide, setActiveSlide] = useState(0);
    const [following, setFollowing]     = useState(false);
    const [followLoading, setFollowLoading] = useState(false);
    const currentUser = AuthService.getCurrentUser();

    useEffect(() => {
        const fetchProject = async () => {
            try {
                setLoading(true);
                const res = await fetch(`/api/projects/${id}`);
                if (!res.ok) throw new Error(`Помилка сервера: ${res.status}`);
                const data = await res.json();
                setProject(data);
                if (data.creator) {
                    const authorRes = await fetch(`/api/users/${data.creator}`);
                    if (authorRes.ok) setAuthor(await authorRes.json());
                }
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };
        fetchProject();
    }, [id]);

    useEffect(() => {
        if (!currentUser || !id) return;
        fetch(`/api/follows/projects/${id}/status`, {
            headers: { Authorization: `Bearer ${currentUser.token}` },
        })
            .then(r => r.ok ? r.json() : null)
            .then(data => { if (data) setFollowing(data.following); })
            .catch(() => {});
    }, [id]); // eslint-disable-line

    const handleFollowProject = async () => {
        if (!currentUser) { alert('Увійдіть щоб відстежувати проекти'); return; }
        setFollowLoading(true);
        try {
            const res = await fetch(`/api/follows/projects/${id}`, {
                method: 'POST',
                headers: { Authorization: `Bearer ${currentUser.token}` },
            });
            if (res.ok) setFollowing((await res.json()).following);
        } finally {
            setFollowLoading(false);
        }
    };

    if (loading) return <div className="project-page-loading"><p>Завантаження проекту...</p></div>;
    if (error) return (
        <div className="project-page-error">
            <p>Сталася помилка: {error}</p>
            <button className="load-more-btn" onClick={() => navigate('/')}>← Назад</button>
        </div>
    );
    if (!project) return null;

    const slides = (() => {
        const mediaItems = project.media ?? [];
        if (mediaItems.length === 0) {
            return project.mainImage
                ? [{ type: 'image', src: `/api/files/${project.mainImage}` }]
                : [];
        }
        const videos = mediaItems
            .filter(m => m.category === 'VIDEO')
            .map(m => ({ type: 'video', src: `/api/files/${m.id}`, mime: m.mimeType }));
        const photos = mediaItems
            .filter(m => m.category === 'PHOTO')
            .map(m => ({ type: 'image', src: `/api/files/${m.id}` }));
        return [...videos, ...photos];
    })();

    const prevSlide = () => setActiveSlide(i => (i - 1 + slides.length) % slides.length);
    const nextSlide = () => setActiveSlide(i => (i + 1) % slides.length);

    const percentage = Math.min((project.collectedAmount / project.goalAmount) * 100, 100);

    return (
        <div className="project-page">

            <div className="project-page-back">
                <button className="project-page-back-btn" onClick={() => navigate(-1)}>← Назад</button>
            </div>

            <div className="project-page-main">

                <div className="project-page-carousel">
                    <div className="carousel-track">
                        {slides.length === 0 ? (
                            <div className="carousel-placeholder">Немає медіа</div>
                        ) : slides[activeSlide].type === 'video' ? (
                            <video
                                key={activeSlide}
                                className="carousel-image"
                                controls
                                autoPlay={slides[activeSlide].type === 'video' && activeSlide === 0}
                                muted
                                src={slides[activeSlide].src}
                            >
                                <source src={slides[activeSlide].src} type={slides[activeSlide].mime} />
                            </video>
                        ) : (
                            <img
                                key={activeSlide}
                                src={slides[activeSlide].src}
                                alt={`Фото ${activeSlide + 1}`}
                                className="carousel-image"
                            />
                        )}

                        {slides.length > 1 && (
                            <>
                                <button className="carousel-btn carousel-btn-prev" onClick={prevSlide}>‹</button>
                                <button className="carousel-btn carousel-btn-next" onClick={nextSlide}>›</button>
                                <div className="carousel-dots">
                                    {slides.map((slide, i) => (
                                        <button
                                            key={i}
                                            className={`carousel-dot ${i === activeSlide ? 'active' : ''} ${slide.type === 'video' ? 'carousel-dot-video' : ''}`}
                                            onClick={() => setActiveSlide(i)}
                                            title={slide.type === 'video' ? 'Відео' : `Фото ${i + 1}`}
                                        />
                                    ))}
                                </div>
                            </>
                        )}
                    </div>
                </div>

                <div className="project-page-sidebar">

                    <h1 className="project-page-title">{project.title}</h1>

                    {project.categories?.length > 0 && (
                        <div className="project-page-categories">
                            {project.categories.map((cat, i) => {
                                const label = getLabel(cat);
                                return label ? (
                                    <span key={i} className="category-tag">{label}</span>
                                ) : null;
                            })}
                        </div>
                    )}

                    <div className="project-page-progress-container">
                        <div className="project-page-progress-fill" style={{ width: `${percentage}%` }} />
                    </div>
                    <div className="project-page-stats">
                        <span>Зібрано: <strong>₴{project.collectedAmount}</strong></span>
                        <span>Ціль: <strong>₴{project.goalAmount}</strong></span>
                    </div>

                    <div className="project-page-donate">
                        <DonateSection
                            type="DONATION"
                            paymentPayload={{
                                donateId: currentUser?.id ?? 0,
                                donor: currentUser?.id ?? 0,
                                project: project.projectId,
                                creator: project.creator ?? 0,
                                reward: 0,
                                paymentStatus: 'PENDING',
                                isAnonymous: !currentUser,
                            }}
                            wrapperClass="projectpage-donate-section"
                            btnStartClass="projectpage-donate-start-wrapper"
                            btnInputClass="projectpage-donate-input-wrapper"
                            inputGroupClass="projectpage-donate-input-group"
                            startBtnClass="projectpage-donate-btn"
                            cancelBtnClass="projectpage-donate-cancel"
                            inputClass="projectpage-donate-input"
                            confirmClass="projectpage-donate-confirm"
                            confirmLabel="✓"
                            placeholder="Сума (₴)"
                        />
                    </div>

                    {(!currentUser || String(currentUser.id) !== String(project.creator)) && (
                        <button
                            className={`project-page-follow-btn ${following ? 'project-page-follow-btn--active' : ''}`}
                            onClick={handleFollowProject}
                            disabled={followLoading}
                        >
                            {following ? '♥ Відстежується' : '♡ Відстежувати проект'}
                        </button>
                    )}

                    {author && (
                        <div
                            className="project-page-author"
                            onClick={() => navigate(`/author/${author.id}`)}
                            title={`Перейти до профілю ${author.username}`}
                        >
                            <div className="project-page-author-avatar-wrap">
                                {author.imageId ? (
                                    <img
                                        src={`/api/files/${author.imageId}`}
                                        alt={author.username}
                                        className="project-page-author-avatar"
                                    />
                                ) : (
                                    <div className="project-page-author-avatar-placeholder">
                                        {author.username?.charAt(0).toUpperCase() || 'A'}
                                    </div>
                                )}
                            </div>
                            <div className="project-page-author-info">
                                <span className="project-page-author-label">Автор</span>
                                <span className="project-page-author-name">{author.username}</span>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {project.description && (
                <div className="project-page-description-section">
                    <h2 className="project-page-description-title">Опис проекту</h2>
                    <div
                        className="project-page-description-text"
                        dangerouslySetInnerHTML={{ __html: project.description }}
                    />
                </div>
            )}
        </div>
    );
};

export default ProjectPage;

