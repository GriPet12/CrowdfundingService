import { useState, useEffect } from 'react';
import {
    AreaChart, Area, BarChart, Bar,
    XAxis, YAxis, CartesianGrid, Tooltip,
    ResponsiveContainer, PieChart, Pie, Cell
} from 'recharts';
import '../../styles/statsTab.css';

const fmt = (n) => n >= 1_000_000 ? `${(n / 1_000_000).toFixed(1)}M`
                 : n >= 1_000     ? `${(n / 1_000).toFixed(1)}K`
                 : String(n ?? 0);

const fmtMoney = (n) => `₴${Number(n ?? 0).toLocaleString('uk-UA', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`;

const shortDate = (str) => {
    const d = new Date(str);
    return `${d.getDate().toString().padStart(2,'0')}.${(d.getMonth()+1).toString().padStart(2,'0')}`;
};

const StatCard = ({ icon, label, value, sub }) => (
    <div className="st-card">
        <div className="st-card-icon">{icon}</div>
        <div className="st-card-body">
            <span className="st-card-value">{value}</span>
            <span className="st-card-label">{label}</span>
            {sub && <span className="st-card-sub">{sub}</span>}
        </div>
    </div>
);

const PIE_COLORS = ['#1a1a1a', '#555', '#999', '#ccc'];

const ChartTooltip = ({ active, payload, label }) => {
    if (!active || !payload?.length) return null;
    return (
        <div className="st-tooltip">
            <p className="st-tooltip-label">{label}</p>
            {payload.map((p) => (
                <p key={p.name} style={{ color: p.color }}>
                    {p.name}: <strong>{p.value}</strong>
                </p>
            ))}
        </div>
    );
};

const StatsTab = ({ userId, token }) => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!userId || !token) return;
        let cancelled = false;
        fetch(`/api/analytics/creator/${userId}/dashboard`, {
            headers: { Authorization: `Bearer ${token}` },
        })
            .then(r => { if (!r.ok) throw new Error(r.status); return r.json(); })
            .then(data => { if (!cancelled) { setStats(data); setLoading(false); } })
            .catch(e => { if (!cancelled) { setError(e.message); setLoading(false); } });
        return () => { cancelled = true; };
    }, [userId, token]);

    if (loading) return <div className="st-loading"><div className="st-spinner"/><p>Завантаження статистики…</p></div>;
    if (error)   return <div className="st-error"><p>Не вдалося завантажити статистику.</p></div>;
    if (!stats)  return null;

    const hasActivity = stats.activityByDay?.some(d => d.views + d.follows + d.donates + d.subscriptions > 0);
    const hasDonations = stats.donationsByType?.some(d => d.value > 0);

    return (
        <div className="st-root">

            
            <section className="st-section">
                <h3 className="st-section-title">Загальна статистика</h3>
                <div className="st-cards-grid">
                    <StatCard icon={
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                    } label="Переглядів" value={fmt(stats.totalViews)} sub="за рік" />

                    <StatCard icon={
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                    } label="Підписників" value={fmt(stats.totalSubscribers)} sub="активних" />

                    <StatCard icon={
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                    } label="Лайків" value={fmt(stats.totalLikes)} sub="на всіх постах" />

                    <StatCard icon={
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                    } label="Коментарів" value={fmt(stats.totalComments)} sub="на всіх постах" />

                    <StatCard icon={
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>
                    } label="Постів" value={fmt(stats.totalPosts)} />

                    <StatCard icon={
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
                    } label="Зібрано донатів" value={fmtMoney(stats.totalDonationsAmount)} sub={`${fmt(stats.totalDonationsCount)} транзакцій`} />

                    <StatCard icon={
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                    } label="Фоловерів" value={fmt(stats.totalFollowers)} sub="всього" />
                </div>
            </section>

            
            <section className="st-section">
                <h3 className="st-section-title">Активність за 30 днів</h3>
                {!hasActivity ? (
                    <p className="st-empty">Даних поки недостатньо</p>
                ) : (
                    <div className="st-chart-box">
                        <ResponsiveContainer width="100%" height={260}>
                            <AreaChart data={stats.activityByDay} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
                                <defs>
                                    <linearGradient id="gViews" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#1a1a1a" stopOpacity={0.15}/>
                                        <stop offset="95%" stopColor="#1a1a1a" stopOpacity={0}/>
                                    </linearGradient>
                                    <linearGradient id="gFollows" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#555" stopOpacity={0.15}/>
                                        <stop offset="95%" stopColor="#555" stopOpacity={0}/>
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0"/>
                                <XAxis dataKey="date" tickFormatter={shortDate} tick={{ fontSize: 11, fill: '#aaa' }} interval={4}/>
                                <YAxis tick={{ fontSize: 11, fill: '#aaa' }} allowDecimals={false}/>
                                <Tooltip content={<ChartTooltip />} formatter={(v, n) => [v, n === 'views' ? 'Перегляди' : n === 'follows' ? 'Фоловери' : n === 'donates' ? 'Донати' : 'Підписки']}/>
                                <Area type="monotone" dataKey="views"         name="Перегляди"  stroke="#1a1a1a" fill="url(#gViews)"  strokeWidth={2}/>
                                <Area type="monotone" dataKey="follows"       name="Фоловери"   stroke="#555"    fill="url(#gFollows)" strokeWidth={2}/>
                                <Area type="monotone" dataKey="subscriptions" name="Підписки"   stroke="#888"    fill="none"           strokeWidth={2} strokeDasharray="4 2"/>
                                <Area type="monotone" dataKey="donates"       name="Донати"     stroke="#bbb"    fill="none"           strokeWidth={2} strokeDasharray="4 2"/>
                            </AreaChart>
                        </ResponsiveContainer>
                        <div className="st-chart-legend">
                            <span className="st-legend-item st-legend-views">Перегляди</span>
                            <span className="st-legend-item st-legend-follows">Фоловери</span>
                            <span className="st-legend-item st-legend-subs">Підписки</span>
                            <span className="st-legend-item st-legend-donates">Донати</span>
                        </div>
                    </div>
                )}
            </section>

            
            <div className="st-bottom-row">

                
                <section className="st-section st-section--half">
                    <h3 className="st-section-title">Джерела донатів</h3>
                    {!hasDonations ? (
                        <p className="st-empty">Донатів ще немає</p>
                    ) : (
                        <div className="st-chart-box">
                            <ResponsiveContainer width="100%" height={220}>
                                <PieChart>
                                    <Pie data={stats.donationsByType} dataKey="value" nameKey="label" cx="50%" cy="50%" outerRadius={80} label={({ label, percent }) => `${label} ${(percent*100).toFixed(0)}%`} labelLine={false}>
                                        {stats.donationsByType.map((_, i) => (
                                            <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]}/>
                                        ))}
                                    </Pie>
                                    <Tooltip formatter={(v) => fmtMoney(v)}/>
                                </PieChart>
                            </ResponsiveContainer>
                        </div>
                    )}
                </section>

                
                <section className="st-section st-section--half">
                    <h3 className="st-section-title">Топ постів за лайками</h3>
                    {!stats.topPosts?.length ? (
                        <p className="st-empty">Постів ще немає</p>
                    ) : (
                        <div className="st-chart-box">
                            <ResponsiveContainer width="100%" height={220}>
                                <BarChart data={stats.topPosts} layout="vertical" margin={{ top: 0, right: 16, left: 0, bottom: 0 }}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" horizontal={false}/>
                                    <XAxis type="number" tick={{ fontSize: 11, fill: '#aaa' }} allowDecimals={false}/>
                                    <YAxis type="category" dataKey="title" tick={{ fontSize: 11, fill: '#555' }} width={90} tickFormatter={t => t.length > 12 ? t.slice(0,12)+'…' : t}/>
                                    <Tooltip content={<ChartTooltip />} formatter={(v, n) => [v, n === 'likes' ? 'Лайки' : 'Коментарі']}/>
                                    <Bar dataKey="likes"    name="Лайки"      fill="#1a1a1a" radius={[0,4,4,0]}/>
                                    <Bar dataKey="comments" name="Коментарі"  fill="#aaa"    radius={[0,4,4,0]}/>
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    )}
                </section>

            </div>
        </div>
    );
};

export default StatsTab;

