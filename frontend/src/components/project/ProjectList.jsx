import { useState, useEffect, useCallback, useRef } from 'react';
import ProjectItem from './ProjectItem.jsx';
import AuthService from '../user/AuthService.jsx';
import analyticsService from '../../utils/analyticsService.js';
import '../../styles/projectItem.css';
import '../../styles/projectSearch.css';

const SORT_OPTIONS = [
    { value: 'hotnessScore',    label: 'Популярні' },
    { value: 'collectedAmount', label: 'За сумою' },
    { value: 'title',           label: 'За назвою' },
    { value: 'createdAt',       label: 'Нові' },
];

const PAGE_SIZE = 6;

const ProjectCardSkeleton = () => (
    <div className="project-card project-card--skeleton" aria-hidden="true">
        <div className="project-card-image-wrapper">
            <div className="project-image project-image--loading" />
        </div>
        <div className="project-skeleton-line project-skeleton-line--title" />
        <div className="project-skeleton-line project-skeleton-line--short" />
        <div className="project-skeleton-bar" />
        <div className="project-skeleton-line project-skeleton-line--stats" />
    </div>
);

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

    const filtersRef = useRef({ search, categoryId, sortBy, sortDir });
    filtersRef.current = { search, categoryId, sortBy, sortDir };

    const buildProjectsUrl = (pageNum, filters) => {
        const { search, categoryId, sortBy, sortDir } = filters;
        const params = new URLSearchParams({
            page: pageNum,
            size: PAGE_SIZE,
            sortBy,
            sortDir,
        });
        if (search) params.set('search', search);
        if (categoryId) params.set('categoryId', categoryId);
        return `/api/projects?${params}`;
    };

    const applyProjectsPage = (projectsData, pageNum) => {
        const newProjects = projectsData.content || [];
        setProjects(prev => pageNum === 0 ? newProjects : [...prev, ...newProjects]);
        const totalPages  = projectsData.totalPages  ?? 1;
        const currentPage = projectsData.currentPage ?? pageNum;
        setHasMore(newProjects.length > 0 && currentPage + 1 < totalPages);
        return newProjects;
    };

    const fetchInitialHome = useCallback(async (filters) => {
        const { search, categoryId, sortBy, sortDir } = filters;
        const params = new URLSearchParams({
            page: 0,
            size: PAGE_SIZE,
            sortBy,
            sortDir,
        });
        if (search) params.set('search', search);
        if (categoryId) params.set('categoryId', categoryId);

        const response = await fetch(`/api/home?${params}`);
        if (!response.ok) throw new Error(`Помилка сервера: ${response.status}`);
        const data = await response.json();
        setCategories(Array.isArray(data.categories) ? data.categories : []);
        setFollowedIds(new Set(data.followedProjectIds || []));
        return applyProjectsPage(data.projects || {}, 0);
    }, []);

    const fetchMoreProjects = useCallback(async (pageNum, filters) => {
        const response = await fetch(buildProjectsUrl(pageNum, filters));
        if (!response.ok) throw new Error(`Помилка сервера: ${response.status}`);
        const data = await response.json();
        return applyProjectsPage(data, pageNum);
    }, []);

    useEffect(() => {
        const filters = { search, categoryId, sortBy, sortDir };
        setPage(0);
        setLoading(true);
        setError(null);
        fetchInitialHome(filters)
            .catch(err => setError(err.message))
            .finally(() => setLoading(false));
    }, [search, categoryId, sortBy, sortDir, fetchInitialHome]);

    const handleLoadMore = async () => {
        const nextPage = page + 1;
        setLoadingMore(true);
        try {
            await fetchMoreProjects(nextPage, filtersRef.current);
            setPage(nextPage);
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
                <div className="projects-grid">
                    {Array.from({ length: PAGE_SIZE }, (_, i) => <ProjectCardSkeleton key={i} />)}
                </div>
            ) : error ? (
                <p style={{ color: 'red', textAlign: 'center' }}>Сталася помилка: {error}</p>
            ) : projects.length === 0 ? (
                <p style={{ textAlign: 'center', color: '#888', padding: '40px 0' }}>Проєктів не знайдено</p>
            ) : (
                <div className="projects-grid">
                    {projects.map((project, index) => (
                        <ProjectItem
                            key={project.projectId}
                            project={project}
                            initialFollowing={followedIds.has(project.projectId)}
                            imagePriority={index < 3}
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
