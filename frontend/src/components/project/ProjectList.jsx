import { useState, useEffect, useCallback, useRef } from 'react';
import ProjectItem from './ProjectItem.jsx';
import AuthService from '../user/AuthService.jsx';
import analyticsService from '../../services/analyticsService.js';
import '../../styles/projectItem.css';
import '../../styles/projectSearch.css';

const SORT_OPTIONS = [
    { value: 'hotnessScore',    label: 'Популярні' },
    { value: 'collectedAmount', label: 'За сумою' },
    { value: 'title',           label: 'За назвою' },
    { value: 'createdAt',       label: 'Нові' },
];

const ProjectList = () => {
    const [projects, setProjects]         = useState([]);
    const [followedIds, setFollowedIds]   = useState(new Set());
    const [loading, setLoading]           = useState(true);
    const [loadingMore, setLoadingMore]   = useState(false);
    const [error, setError]               = useState(null);
    const [page, setPage]                 = useState(0);
    const [hasMore, setHasMore]           = useState(false);

    const [search, setSearch]       = useState('');
    const [categories, setCategories] = useState([]);
    const [categoryId, setCategoryId] = useState('');
    const [sortBy, setSortBy]       = useState('hotnessScore');
    const [sortDir, setSortDir]     = useState('desc');

    const currentUserRef = useRef(AuthService.getCurrentUser());
    const filtersRef = useRef({ search, categoryId, sortBy, sortDir });
    filtersRef.current = { search, categoryId, sortBy, sortDir };

    
    useEffect(() => {
        fetch('/api/categories')
            .then(r => r.ok ? r.json() : [])
            .then(data => setCategories(Array.isArray(data) ? data : []))
            .catch(() => {});
    }, []);

    const fetchFollowStatuses = useCallback(async (projectList) => {
        const currentUser = currentUserRef.current;
        if (!currentUser || projectList.length === 0) return;
        const ids = projectList.map(p => p.projectId).filter(Boolean);
        if (ids.length === 0) return;
        try {
            const res = await fetch('/api/follows/projects/batch-status', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${currentUser.token}` },
                body: JSON.stringify(ids),
            });
            if (res.ok) setFollowedIds(new Set(await res.json()));
        } catch {  }
    }, []);

    
    const buildUrl = (pageNum, filters) => {
        const currentUser = currentUserRef.current;
        const { search, categoryId, sortBy, sortDir } = filters;
        const PAGE_SIZE = 6;

        
        const noFilters = !search && !categoryId && sortBy === 'hotnessScore' && sortDir === 'desc';
        if (noFilters && currentUser && pageNum === 0) {
            return {
                url: `/api/recommendations?page=${pageNum}&size=${PAGE_SIZE}`,
                headers: { Authorization: `Bearer ${currentUser.token}` },
            };
        }

        const params = new URLSearchParams({ page: pageNum, size: PAGE_SIZE, sortBy, sortDir });
        if (search)     params.set('search', search);
        if (categoryId) params.set('categoryId', categoryId);
        return { url: `/api/projects?${params}`, headers: {} };
    };

    const fetchData = useCallback(async (pageNum, filters) => {
        const { url, headers } = buildUrl(pageNum, filters);
        const response = await fetch(url, { headers });
        if (!response.ok) throw new Error(`Помилка сервера: ${response.status}`);
        const data = await response.json();
        const newProjects = data.content || [];
        setProjects(prev => pageNum === 0 ? newProjects : [...prev, ...newProjects]);
        const totalPages  = data.totalPages  ?? 1;
        const currentPage = data.currentPage ?? 0;
        setHasMore(newProjects.length > 0 && currentPage + 1 < totalPages);
        return newProjects;
    }, []); 

    useEffect(() => {
        const filters = { search, categoryId, sortBy, sortDir };
        setPage(0);
        setLoading(true);
        setError(null);
        fetchData(0, filters)
            .then(newProjects => fetchFollowStatuses(newProjects))
            .catch(err => setError(err.message))
            .finally(() => setLoading(false));
    }, [search, categoryId, sortBy, sortDir, fetchData, fetchFollowStatuses]);

    const handleLoadMore = async () => {
        const nextPage = page + 1;
        const currentUser = currentUserRef.current;
        setLoadingMore(true);
        try {
            const newProjects = await fetchData(nextPage, filtersRef.current);
            setPage(nextPage);
            if (currentUser && newProjects.length > 0) {
                const ids = newProjects.map(p => p.projectId).filter(Boolean);
                const res = await fetch('/api/follows/projects/batch-status', {
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
            <div className="project-search-bar">
                <input
                    className="project-search-input"
                    type="text"
                    placeholder="Пошук за назвою…"
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                />
                <div className="project-search-sort-row">
                    <select
                        className="project-search-select"
                        value={categoryId}
                        onChange={e => setCategoryId(e.target.value)}
                    >
                        <option value="">Всі категорії</option>
                        {categories.map(cat => (
                            <option key={cat.categoryId ?? cat.id} value={cat.categoryId ?? cat.id}>
                                {cat.categoryName ?? cat.name}
                            </option>
                        ))}
                    </select>
                    <select
                        className="project-search-select"
                        value={sortBy}
                        onChange={e => setSortBy(e.target.value)}
                    >
                        {SORT_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                    </select>
                    <button
                        className="project-search-dir-btn"
                        onClick={() => setSortDir(d => d === 'desc' ? 'asc' : 'desc')}
                        title={sortDir === 'desc' ? 'За спаданням' : 'За зростанням'}
                    >
                        {sortDir === 'desc' ? '↓' : '↑'}
                    </button>
                </div>
            </div>

            {loading ? (
                <p style={{ textAlign: 'center' }}>Завантаження проєктів...</p>
            ) : error ? (
                <p style={{ color: 'red', textAlign: 'center' }}>Сталася помилка: {error}</p>
            ) : projects.length === 0 ? (
                <p style={{ textAlign: 'center', color: '#888', padding: '40px 0' }}>Проєктів не знайдено</p>
            ) : (
                <div className="projects-grid">
                    {projects.map(project => (
                        <ProjectItem
                            key={project.projectId}
                            project={project}
                            initialFollowing={followedIds.has(project.projectId)}
                            onCardClick={id => analyticsService.projectClick(id)}
                        />
                    ))}
                </div>
            )}

            {hasMore && (
                <div style={{ textAlign: 'center', marginTop: '20px' }}>
                    <button className="load-more-btn" onClick={handleLoadMore} disabled={loadingMore}>
                        {loadingMore ? 'Завантаження...' : 'Завантажити ще'}
                    </button>
                </div>
            )}
        </div>
    );
};

export default ProjectList;

