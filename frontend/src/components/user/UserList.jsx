import { useState, useEffect, useCallback, useRef } from 'react';
import UserItem from './UserItem.jsx';
import AuthService from './AuthService.jsx';
import '../../styles/userItem.css';
import '../../styles/projectItem.css';
import '../../styles/projectSearch.css';

const UserList = () => {
    const [users, setUsers]             = useState([]);
    const [followedIds, setFollowedIds] = useState(new Set());
    const [loading, setLoading]         = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [error, setError]             = useState(null);
    const [page, setPage]               = useState(0);
    const [hasMore, setHasMore]         = useState(false);

    const [search, setSearch]   = useState('');
    const [sortBy, setSortBy]   = useState('createdAt');
    const [sortDir, setSortDir] = useState('desc');

    const currentUserRef = useRef(AuthService.getCurrentUser());
    const filtersRef = useRef({ search, sortBy, sortDir });
    filtersRef.current = { search, sortBy, sortDir };

    const fetchFollowStatuses = useCallback(async (userList) => {
        const currentUser = currentUserRef.current;
        if (!currentUser || userList.length === 0) return;
        const ids = userList.map(u => u.id).filter(Boolean);
        if (ids.length === 0) return;
        try {
            const res = await fetch('/api/follows/authors/batch-status', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${currentUser.token}` },
                body: JSON.stringify(ids),
            });
            if (res.ok) setFollowedIds(new Set(await res.json()));
        } catch {  }
    }, []);

    const fetchUsers = useCallback(async (pageNum, filters) => {
        const { search, sortBy, sortDir } = filters;
        const params = new URLSearchParams({ page: pageNum, size: 12, sortBy, sortDir });
        if (search) params.set('search', search);
        const response = await fetch(`/api/creators?${params}`);
        if (!response.ok) throw new Error(`Помилка сервера: ${response.status}`);
        const data = await response.json();
        const userList = Array.isArray(data) ? data : (data.content || []);
        setUsers(prev => pageNum === 0 ? userList : [...prev, ...userList]);
        if (!Array.isArray(data)) {
            setHasMore((data.currentPage ?? 0) + 1 < (data.totalPages ?? 1));
        } else {
            setHasMore(false);
        }
        return userList;
    }, []);

    useEffect(() => {
        const filters = { search, sortBy, sortDir };
        setPage(0);
        setError(null);
        setLoading(true);
        fetchUsers(0, filters)
            .then(userList => fetchFollowStatuses(userList))
            .catch(err => setError(err.message))
            .finally(() => setLoading(false));
    }, [search, sortBy, sortDir, fetchUsers, fetchFollowStatuses]);

    const handleLoadMore = async () => {
        const nextPage = page + 1;
        const currentUser = currentUserRef.current;
        setLoadingMore(true);
        try {
            const newUsers = await fetchUsers(nextPage, filtersRef.current);
            setPage(nextPage);
            if (currentUser && newUsers.length > 0) {
                const ids = newUsers.map(u => u.id).filter(Boolean);
                const res = await fetch('/api/follows/authors/batch-status', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${currentUser.token}` },
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

    return (
        <div>
            <h2 style={{ textAlign: 'center', marginTop: '40px' }}>Наші автори</h2>

            <div className="project-search-bar">
                <input
                    className="project-search-input"
                    type="text"
                    placeholder="Пошук авторів…"
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                />
                <select className="project-search-select" value={sortBy} onChange={e => setSortBy(e.target.value)}>
                    <option value="createdAt">Нові</option>
                    <option value="username">За іменем</option>
                </select>
                <button
                    className="project-search-dir-btn"
                    onClick={() => setSortDir(d => d === 'desc' ? 'asc' : 'desc')}
                    title={sortDir === 'desc' ? 'За спаданням' : 'За зростанням'}
                >
                    {sortDir === 'desc' ? '↓' : '↑'}
                </button>
            </div>

            {loading ? (
                <p style={{ textAlign: 'center' }}>Завантаження авторів...</p>
            ) : error ? (
                <p style={{ margin: '10px', color: 'red', textAlign: 'center' }}>Сталася помилка: {error}</p>
            ) : users.length === 0 ? (
                <p style={{ textAlign: 'center', color: '#888', padding: '40px 0' }}>Авторів не знайдено</p>
            ) : (
                <div className="authors-grid">
                    {users.map(user => (
                        <UserItem
                            key={user.id || user.username}
                            user={user}
                            initialFollowing={followedIds.has(user.id)}
                        />
                    ))}
                </div>
            )}

            {hasMore && (
                <div style={{ textAlign: 'center', marginTop: '20px', marginBottom: '40px' }}>
                    <button className="load-more-btn" onClick={handleLoadMore} disabled={loadingMore}>
                        {loadingMore ? 'Завантаження...' : 'Завантажити ще'}
                    </button>
                </div>
            )}
        </div>
    );
};

export default UserList;
