import '../../styles/userItem.css';
import DonateSection from '../pay/DonateSection.jsx';
import AuthService from '../user/AuthService.jsx';
import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';

const UserItem = ({ user, initialFollowing = false, onFollowChange }) => {
    const navigate = useNavigate();
    const currentUser = AuthService.getCurrentUser();

    const [following, setFollowing] = useState(initialFollowing);
    const [followLoading, setFollowLoading] = useState(false);

    useEffect(() => {
        setFollowing(initialFollowing);
    }, [initialFollowing]);

    const handleFollow = async (e) => {
        e.stopPropagation();
        if (!currentUser) { alert('Увійдіть щоб підписатись на автора'); return; }
        setFollowLoading(true);
        try {
            const res = await fetch(`/api/follows/authors/${user.id}`, {
                method: 'POST',
                headers: { Authorization: `Bearer ${currentUser.token}` },
            });
            if (res.ok) {
                const newVal = (await res.json()).following;
                setFollowing(newVal);
                onFollowChange?.(user.id, newVal);
            }
        } finally {
            setFollowLoading(false);
        }
    };

    return (
        <div className="author-card">
            <div className="author-avatar-wrapper">
                <div
                    className="author-avatar-link"
                    onClick={() => navigate(`/author/${user.id}`)}
                    title={`Перейти до профілю ${user.username}`}
                >
                    {user.imageId ? (
                        <img
                            src={`/api/files/${user.imageId}`}
                            alt={user.username}
                            className="author-avatar"
                            loading="lazy"
                        />
                    ) : (
                        <div className="author-avatar-placeholder">
                            {user.username ? user.username.charAt(0).toUpperCase() : 'U'}
                        </div>
                    )}
                </div>
                {currentUser && String(currentUser.id) !== String(user.id) && (
                    <button
                        className={`author-follow-btn ${following ? 'author-follow-btn--active' : ''}`}
                        onClick={handleFollow}
                        disabled={followLoading}
                        title={following ? 'Відписатись' : 'Підписатись на автора'}
                    >
                        {following ? '★' : '☆'}
                    </button>
                )}
            </div>

            <div className="author-name">{user.username}</div>

            <DonateSection
                orderId={`user_${user.id || user.username}`}
                type="DONATION"
                paymentPayload={{
                    donateId: user.id,
                    donor: currentUser?.id ?? 0,
                    project: 0,
                    creator: user.id,
                    reward: 0,
                    paymentStatus: 'PENDING',
                    isAnonymous: !currentUser,
                }}
                wrapperClass="author-donate-section"
                btnStartClass="author-donate-start-wrapper"
                btnInputClass="author-donate-input-wrapper"
                inputGroupClass="author-donate-input-group"
                startBtnClass="author-donate-btn"
                cancelBtnClass="author-donate-cancel"
                inputClass="author-donate-input"
                confirmClass="author-donate-confirm"
                confirmLabel="✓"
                placeholder="₴"
            />
        </div>
    );
};

export default UserItem;
