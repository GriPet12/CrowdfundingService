import '../../styles/projectItem.css';
import DonateSection from '../pay/DonateSection.jsx';
import AuthService from '../user/AuthService.jsx';
import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';

const getLabel = (category) => {
    if (!category) return null;
    if (typeof category === 'string') return category;
    return category.name ?? category.title ?? category.label ?? null;
};

const ProjectItem = ({ project, initialFollowing = false, onFollowChange }) => {
    const percentage = Math.min((project.collectedAmount / project.goalAmount) * 100, 100);
    const currentUser = AuthService.getCurrentUser();
    const navigate = useNavigate();

    const [following, setFollowing] = useState(initialFollowing);
    const [followLoading, setFollowLoading] = useState(false);

    useEffect(() => {
        setFollowing(initialFollowing);
    }, [initialFollowing]);

    const categories = Array.isArray(project.categories) && project.categories.length > 0
        ? project.categories : null;

    const handleFollow = async (e) => {
        e.stopPropagation();
        if (!currentUser) { alert('Увійдіть щоб відстежувати проекти'); return; }
        setFollowLoading(true);
        try {
            const res = await fetch(`/api/follows/projects/${project.projectId}`, {
                method: 'POST',
                headers: { Authorization: `Bearer ${currentUser.token}` },
            });
            if (res.ok) {
                const newVal = (await res.json()).following;
                setFollowing(newVal);
                onFollowChange?.(project.projectId, newVal);
            }
        } finally {
            setFollowLoading(false);
        }
    };

    return (
        <div className="project-card">
            <div className="project-card-image-wrapper">
                <img
                    src={`/api/files/${project.mainImage}`}
                    alt={project.title}
                    className="project-image project-image-clickable"
                    onClick={() => navigate(`/project/${project.projectId}`)}
                    title="Відкрити проект"
                    loading="lazy"
                />
                {currentUser && String(currentUser.id) !== String(project.creatorId) && (
                    <button
                        className={`project-follow-btn ${following ? 'project-follow-btn--active' : ''}`}
                        onClick={handleFollow}
                        disabled={followLoading}
                        title={following ? 'Відписатись від проекту' : 'Відстежувати проект'}
                    >
                        {following ? '♥' : '♡'}
                    </button>
                )}
            </div>
            <h3>{project.title}</h3>

            <div className="project-categories">
                {categories?.length > 0 ? (
                    <>
                        {categories.slice(0, 6).map((category, index) => {
                            const label = getLabel(category);
                            return label ? <span key={index} className="category-tag">{label}</span> : null;
                        })}
                        {categories.length > 6 && <span className="category-tag category-tag-more">…</span>}
                    </>
                ) : null}
            </div>

            <div className="progress-bar-container">
                <div className="progress-bar-fill" style={{ width: `${percentage}%` }} />
            </div>

            <div className="project-stats">
                <div className="project-collected">Зібрано: <strong>₴{project.collectedAmount}</strong></div>
                <div className="project-goal">Ціль: <strong>₴{project.goalAmount}</strong></div>
            </div>

            <DonateSection
                type="DONATION"
                paymentPayload={{
                    donateId: currentUser?.id ?? 0,
                    donor: currentUser?.id ?? 0,
                    project: project.projectId,
                    creator: project.creatorId ?? 0,
                    reward: 0,
                    paymentStatus: 'PENDING',
                    isAnonymous: !currentUser,
                }}
                confirmLabel="✓"
            />
        </div>
    );
};

export default ProjectItem;

