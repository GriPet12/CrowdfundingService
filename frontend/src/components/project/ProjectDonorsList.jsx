import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const fmtMoney = (n) => {
    const val = Number(n);
    if (Number.isNaN(val)) return '₴0';
    return `₴${val.toLocaleString('uk-UA', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
};

const fmtDate = (ts) => {
    if (!ts) return '';
    const d = new Date(ts);
    if (Number.isNaN(d.getTime())) return '';
    return d.toLocaleDateString('uk-UA', { day: 'numeric', month: 'short', year: 'numeric' });
};

const ProjectDonorsList = ({ projectId }) => {
    const navigate = useNavigate();
    const [donors, setDonors] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        if (!projectId) return;
        setLoading(true);
        setError('');
        fetch(`/api/projects/${projectId}/donors`)
            .then(async (res) => {
                if (!res.ok) {
                    const msg = await res.text();
                    throw new Error(msg || 'Не вдалося завантажити меценатів');
                }
                return res.json();
            })
            .then((data) => setDonors(Array.isArray(data) ? data : []))
            .catch((err) => setError(err.message || 'Помилка завантаження'))
            .finally(() => setLoading(false));
    }, [projectId]);

    if (loading) {
        return <p className="project-donors-loading">Завантаження меценатів…</p>;
    }

    if (error) {
        return <p className="project-donors-error">{error}</p>;
    }

    if (donors.length === 0) {
        return <p className="project-donors-empty">Поки немає підтверджених донатів.</p>;
    }

    const handleClick = (donor) => {
        if (donor.anonymous || !donor.donorId) return;
        navigate(`/author/${donor.donorId}`);
    };

    return (
        <ul className="project-donors-list">
            {donors.map((donor) => {
                const clickable = !donor.anonymous && donor.donorId;
                return (
                    <li key={donor.anonymous ? 'anonymous' : donor.donorId}>
                        <button
                            type="button"
                            className={`project-donor-item${clickable ? ' project-donor-item--clickable' : ''}`}
                            onClick={() => handleClick(donor)}
                            disabled={!clickable}
                            title={clickable ? `Перейти до профілю ${donor.username}` : undefined}
                        >
                            <div className="project-donor-avatar-wrap">
                                {!donor.anonymous && donor.imageId ? (
                                    <img
                                        src={`/api/files/${donor.imageId}/preview?w=80`}
                                        alt={donor.username}
                                        className="project-donor-avatar"
                                    />
                                ) : (
                                    <div className="project-donor-avatar-placeholder">
                                        {donor.anonymous ? '?' : (donor.username?.charAt(0)?.toUpperCase() || 'U')}
                                    </div>
                                )}
                            </div>
                            <div className="project-donor-info">
                                <span className="project-donor-name">{donor.username}</span>
                                <span className="project-donor-meta">
                                    {fmtMoney(donor.totalAmount)}
                                    {donor.donationsCount > 1 && ` · ${donor.donationsCount} донати`}
                                    {donor.lastDonatedAt && ` · ${fmtDate(donor.lastDonatedAt)}`}
                                </span>
                            </div>
                            {clickable && <span className="project-donor-arrow">→</span>}
                        </button>
                    </li>
                );
            })}
        </ul>
    );
};

export default ProjectDonorsList;
