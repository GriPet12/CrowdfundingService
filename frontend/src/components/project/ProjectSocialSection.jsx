import { useState } from 'react';
import AuthService from '../user/AuthService.jsx';
import CommentsSection from '../common/CommentsSection.jsx';
import '../../styles/postCard.css';

const ProjectSocialSection = ({
    projectId,
    creatorId,
    initialLikeCount = 0,
    initialLikedByMe = false,
    initialCommentCount = 0,
}) => {
    const currentUser = AuthService.getCurrentUser();
    const [likeCount, setLikeCount] = useState(initialLikeCount);
    const [likedByMe, setLikedByMe] = useState(initialLikedByMe);
    const [likeLoading, setLikeLoading] = useState(false);
    const [commentsOpen, setCommentsOpen] = useState(false);
    const [commentCount, setCommentCount] = useState(initialCommentCount);

    const handleLike = async () => {
        if (!currentUser || likeLoading) return;
        setLikeLoading(true);
        try {
            const res = await fetch(`/api/projects/${projectId}/like`, {
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
        <div className="project-page-social">
            <h2 className="project-page-social-title">Обговорення</h2>
            <div className="post-card-footer project-page-social-footer">
                <div className="post-card-actions">
                    <button
                        type="button"
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
                        type="button"
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
                    entityId={projectId}
                    ownerId={creatorId}
                    apiBasePath="/api/projects"
                    open={commentsOpen}
                    commentCount={commentCount}
                    onCountChange={(delta) => setCommentCount(v => v + delta)}
                />
            </div>
        </div>
    );
};

export default ProjectSocialSection;
