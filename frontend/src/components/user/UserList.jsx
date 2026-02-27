import { useState, useEffect, useCallback, useRef } from 'react';
import UserItem from './UserItem.jsx';
import AuthService from './AuthService.jsx';
import '../../styles/userItem.css';
import '../../styles/projectItem.css';

const UserList = () => {
    const [users, setUsers] = useState([]);
    const [followedIds, setFollowedIds] = useState(new Set());
    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [error, setError] = useState(null);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(false);
    const currentUserRef = useRef(AuthService.getCurrentUser());
    const fetchedFollowRef = useRef(false);

    const fetchFollowStatuses = useCallback(async (userList) => {
        const currentUser = currentUserRef.current;
        if (!currentUser || userList.length === 0) return;
        const ids = userList.map(u => u.id).filter(Boolean);
        if (ids.length === 0) return;
        try {
            const res = await fetch('/api/follows/authors/batch-status', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${currentUser.token}`,
                },
                body: JSON.stringify(ids),
            });
            if (res.ok) setFollowedIds(new Set(await res.json()));
        } catch { /* ignore */ }
    }, []); // no deps — reads from stable ref

    const fetchUsers = useCallback(async (pageNum) => {
        const response = await fetch(`/api/creators?page=${pageNum}`);
        if (!response.ok) throw new Error(`Помилка сервера: ${response.status}`);
        const data = await response.json();
        const userList = Array.isArray(data) ? data : (data.content || []);
        setUsers(prev => pageNum === 0 ? userList : [...prev, ...userList]);
        if (!Array.isArray(data)) setHasMore(data.currentPage + 1 < data.totalPages);
        else setHasMore(false);
        return userList;
    }, []); // no deps — pure fetch

    useEffect(() => {
        setLoading(true);
        fetchUsers(0)
            .then(userList => {
                if (!fetchedFollowRef.current) {
                    fetchedFollowRef.current = true;
                    fetchFollowStatuses(userList);
                }
            })
            .catch(err => setError(err.message))
            .finally(() => setLoading(false));
    }, []); // eslint-disable-line react-hooks/exhaustive-deps — runs once on mount

    const handleLoadMore = async () => {
        const nextPage = page + 1;
        const currentUser = currentUserRef.current;
        setLoadingMore(true);
        try {
            const newUsers = await fetchUsers(nextPage);
            setPage(nextPage);
            if (currentUser && newUsers.length > 0) {
                const ids = newUsers.map(u => u.id).filter(Boolean);
                const res = await fetch('/api/follows/authors/batch-status', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        Authorization: `Bearer ${currentUser.token}`,
                    },
                    body: JSON.stringify(ids),
                });
                if (res.ok) {
                    const extra = await res.json();
                    setFollowedIds(prev => new Set([...prev, ...extra]));
                }
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setLoadingMore(false);
        }
    };

    if (loading) return <p style={{textAlign: 'center'}}>Завантаження авторів...</p>;
    if (error) return <p style={{margin: '10px', color: 'red', textAlign: 'center'}}>Сталася помилка при завантаженні авторів: {error}</p>;

    return (
        <div>
            <h2 style={{textAlign: 'center', marginTop: '40px'}}>Наші автори</h2>
            <div className="authors-grid">
                {users.map((user) => (
                    <UserItem
                        key={user.id || user.username}
                        user={user}
                        initialFollowing={followedIds.has(user.id)}
                    />
                ))}
            </div>
            {hasMore && (
                <div style={{ textAlign: 'center', marginTop: '20px', marginBottom: '40px' }}>
                    <button
                        className="load-more-btn"
                        onClick={handleLoadMore}
                        disabled={loadingMore}
                    >
                        {loadingMore ? 'Завантаження...' : 'Завантажити ще'}
                    </button>
                </div>
            )}
        </div>
    );
};

export default UserList;
