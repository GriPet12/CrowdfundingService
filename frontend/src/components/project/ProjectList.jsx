import { useState, useEffect, useCallback, useRef } from 'react';
import ProjectItem from './ProjectItem.jsx';
import AuthService from '../user/AuthService.jsx';
import '../../styles/projectItem.css';

const ProjectList = () => {
    const [projects, setProjects] = useState([]);
    const [followedIds, setFollowedIds] = useState(new Set());
    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [error, setError] = useState(null);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(false);

    const currentUserRef = useRef(AuthService.getCurrentUser());
    const fetchedFollowRef = useRef(false);

    const fetchFollowStatuses = useCallback(async (projectList) => {
        const currentUser = currentUserRef.current;
        if (!currentUser || projectList.length === 0) return;
        const ids = projectList.map(p => p.projectId).filter(Boolean);
        if (ids.length === 0) return;
        try {
            const res = await fetch('/api/follows/projects/batch-status', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${currentUser.token}`,
                },
                body: JSON.stringify(ids),
            });
            if (res.ok) setFollowedIds(new Set(await res.json()));
        } catch { /* ignore */ }
    }, []);

    const fetchData = useCallback(async (pageNum) => {
        const response = await fetch(`/api/projects?page=${pageNum}`);
        if (!response.ok) throw new Error(`Помилка сервера: ${response.status}`);
        const data = await response.json();
        const newProjects = data.content || [];
        setProjects(prev => pageNum === 0 ? newProjects : [...prev, ...newProjects]);
        setHasMore(data.currentPage + 1 < data.totalPages);
        return newProjects;
    }, []);

    useEffect(() => {
        setLoading(true);
        fetchData(0)
            .then(newProjects => {
                if (!fetchedFollowRef.current) {
                    fetchedFollowRef.current = true;
                    fetchFollowStatuses(newProjects);
                }
            })
            .catch(err => setError(err.message))
            .finally(() => setLoading(false));
    }, []);

    const handleLoadMore = async () => {
        const nextPage = page + 1;
        const currentUser = currentUserRef.current;
        setLoadingMore(true);
        try {
            const newProjects = await fetchData(nextPage);
            setPage(nextPage);
            if (currentUser && newProjects.length > 0) {
                const ids = newProjects.map(p => p.projectId).filter(Boolean);
                const res = await fetch('/api/follows/projects/batch-status', {
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

    if (loading) return <p>Завантаження проєктів...</p>;
    if (error) return <p style={{ color: 'red' }}>Сталася помилка: {error}</p>;

    return (
        <div>
            <div className="projects-grid">
                {projects.map((project) => (
                    <ProjectItem
                        key={project.projectId}
                        project={project}
                        initialFollowing={followedIds.has(project.projectId)}
                    />
                ))}
            </div>
            {hasMore && (
                <div style={{ textAlign: 'center', marginTop: '20px' }}>
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

export default ProjectList;