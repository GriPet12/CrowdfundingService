import React, { useState, useEffect } from 'react';
import ProjectItem from './ProjectItem.jsx';

const ProjectList = () => {
    const [projects, setProjects] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const response = await fetch('/api/projects');

                if (!response.ok) {
                    throw new Error(`Помилка сервера: ${response.status}`);
                }

                const data = await response.json();
                setProjects(data.content);
            } catch (err) {
                console.error("Деталі помилки:", err);
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, []);

    if (loading) return <p>Завантаження проєктів...</p>;
    if (error) return <p style={{ color: 'red' }}>Сталася помилка: {error}</p>;

    return (
        <div>
            <h2>Список проєктів ({projects.length})</h2>
            {projects.map((project) => (
                <ProjectItem key={project.projectId} project={project} />
            ))}
        </div>
    );
};

export default ProjectList;