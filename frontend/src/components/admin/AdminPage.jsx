import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthService from '../user/AuthService.jsx';
import ConfirmModal from '../common/ConfirmModal.jsx';
import '../../styles/adminPage.css';

const authHeaders = (token) => ({
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
});

const fmtMoney = (n) =>
    `₴${Number(n ?? 0).toLocaleString('uk-UA', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`;

const fmtDate = (s) => s ? new Date(s).toLocaleDateString('uk-UA') : '—';
const fmtDateTime = (s) => s ? new Date(s).toLocaleString('uk-UA', { day:'2-digit', month:'2-digit', year:'numeric', hour:'2-digit', minute:'2-digit' }) : '—';

const useDebounce = (value, delay = 350) => {
    const [dv, setDv] = useState(value);
    useEffect(() => {
        const t = setTimeout(() => setDv(value), delay);
        return () => clearTimeout(t);
    }, [value, delay]);
    return dv;
};

const Pagination = ({ page, total, onChange }) => {
    if (total <= 1) return null;
    const pages = [];
    const start = Math.max(0, page - 2);
    const end   = Math.min(total - 1, page + 2);
    for (let i = start; i <= end; i++) pages.push(i);
    return (
        <div className="admin-pagination">
            <button className="page-btn" disabled={page === 0} onClick={() => onChange(0)}>«</button>
            <button className="page-btn" disabled={page === 0} onClick={() => onChange(page - 1)}>‹</button>
            {start > 0 && <span style={{color:'#9ca3af'}}>…</span>}
            {pages.map(p => (
                <button key={p} className={`page-btn${p === page ? ' active' : ''}`} onClick={() => onChange(p)}>{p + 1}</button>
            ))}
            {end < total - 1 && <span style={{color:'#9ca3af'}}>…</span>}
            <button className="page-btn" disabled={page === total - 1} onClick={() => onChange(page + 1)}>›</button>
            <button className="page-btn" disabled={page === total - 1} onClick={() => onChange(total - 1)}>»</button>
        </div>
    );
};

const UsersTab = ({ token }) => {
    const [users, setUsers]               = useState([]);
    const [search, setSearch]             = useState('');
    const [roleFilter, setRoleFilter]     = useState('');
    const [statusFilter, setStatusFilter] = useState('');
    const [sortBy, setSortBy]             = useState('id');
    const [sortDir, setSortDir]           = useState('asc');
    const [page, setPage]                 = useState(0);
    const [totalPages, setTotalPages]     = useState(1);
    const [totalElements, setTotalElements] = useState(0);
    const [loading, setLoading]           = useState(false);
    const [confirmModal, setConfirmModal] = useState(null);
    const [actionLoading, setActionLoading] = useState(false);
    const [toast, setToast]               = useState('');
    const dSearch = useDebounce(search);

    
    const [clientSort, setClientSort]     = useState(null); 

    const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(''), 3000); };

    const filtersRef = useRef({ search: dSearch, roleFilter, statusFilter, sortBy, sortDir });
    filtersRef.current = { search: dSearch, roleFilter, statusFilter, sortBy, sortDir };

    const fetchUsers = useCallback(async (pg = 0) => {
        const { search, roleFilter, statusFilter, sortBy, sortDir } = filtersRef.current;
        setLoading(true);
        try {
            const params = new URLSearchParams({ page: pg, size: 20, sort: `${sortBy},${sortDir}` });
            if (search.trim()) params.set('search', search.trim());
            if (roleFilter)    params.set('role', roleFilter);
            if (statusFilter)  params.set('status', statusFilter);
            const res = await fetch(`/api/admin/users?${params}`, { headers: authHeaders(token) });
            if (res.ok) {
                const data = await res.json();
                setUsers(data.content ?? []);
                setTotalPages(data.totalPages ?? 1);
                setTotalElements(data.totalElements ?? 0);
            }
        } finally { setLoading(false); }
    }, [token]);

    useEffect(() => { setPage(0); fetchUsers(0); }, [dSearch, roleFilter, statusFilter, sortBy, sortDir]); 
    const handlePage = (p) => { setPage(p); fetchUsers(p); };

    const pageRef = useRef(0);
    useEffect(() => { pageRef.current = page; }, [page]);

    
    const sort = (col) => {
        
        if (col === 'role') {
            setClientSort(prev =>
                prev?.col === 'role'
                    ? { col: 'role', dir: prev.dir === 'asc' ? 'desc' : 'asc' }
                    : { col: 'role', dir: 'asc' }
            );
            return;
        }
        setClientSort(null);
        if (sortBy === col) setSortDir(d => d === 'asc' ? 'desc' : 'asc');
        else { setSortBy(col); setSortDir('asc'); }
    };

    
    const displayedUsers = clientSort?.col === 'role'
        ? [...users].sort((a, b) => {
            const av = a.role === 'ADMIN' ? 0 : 1;
            const bv = b.role === 'ADMIN' ? 0 : 1;
            return clientSort.dir === 'asc' ? av - bv : bv - av;
          })
        : users;

    const runAction = (msg, confirmLabel, endpoint, method, successMsg) => {
        setConfirmModal({
            msg, confirmLabel,
            onConfirm: async () => {
                setActionLoading(true);
                try {
                    const res = await fetch(endpoint, { method, headers: authHeaders(token) });
                    if (res.ok) { showToast(successMsg); fetchUsers(pageRef.current); }
                    else showToast('Помилка виконання дії');
                } finally { setActionLoading(false); setConfirmModal(null); }
            },
        });
    };

    const SortIcon = ({ col }) => {
        const isActive = col === 'role'
            ? clientSort?.col === 'role'
            : sortBy === col && !clientSort;
        const dir = col === 'role' ? clientSort?.dir : sortDir;
        return (
            <span style={{ opacity: isActive ? 1 : .3, marginLeft: 4, fontSize: 11 }}>
                {isActive ? (dir === 'asc' ? '\u25B4' : '\u25BE') : '\u21C5'}
            </span>
        );
    };

    return (
        <div>
            <div className="admin-toolbar">
                <input className="admin-search" placeholder="Пошук за ім'ям або email…"
                    value={search} onChange={e => setSearch(e.target.value)} />
                <select className="admin-select" value={roleFilter} onChange={e => setRoleFilter(e.target.value)}>
                    <option value="">Всі ролі</option>
                    <option value="USER">Користувач</option>
                    <option value="ADMIN">Адміністратор</option>
                </select>
                <select className="admin-select" value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
                    <option value="">Всі статуси</option>
                    <option value="active">Активні</option>
                    <option value="banned">Заблоковані</option>
                </select>
                <button className="btn-secondary" onClick={() => fetchUsers(page)}>↻ Оновити</button>
                {totalElements > 0 && (
                    <span className="admin-count">Всього: {totalElements}</span>
                )}
            </div>

            {loading ? (
                <div className="admin-loading">Завантаження…</div>
            ) : users.length === 0 ? (
                <div className="admin-empty">Користувачів не знайдено</div>
            ) : (
                <div className="admin-table-wrap">
                    <table className="admin-table">
                        <thead>
                            <tr>
                                <th className={sortBy==='id' && !clientSort ?'sort-active':''} onClick={() => sort('id')}>
                                    # <SortIcon col="id"/>
                                </th>
                                <th className={sortBy==='username' && !clientSort ?'sort-active':''} onClick={() => sort('username')}>
                                    Користувач <SortIcon col="username"/>
                                </th>
                                <th className={sortBy==='email' && !clientSort ?'sort-active':''} onClick={() => sort('email')}>
                                    Email <SortIcon col="email"/>
                                </th>
                                <th className={`sort-th ${clientSort?.col==='role' ? 'sort-active' : ''}`} onClick={() => sort('role')} style={{cursor:'pointer'}}>
                                    Роль <SortIcon col="role"/>
                                </th>
                                <th>Статус</th>
                                <th className={sortBy==='createdAt' && !clientSort ?'sort-active':''} onClick={() => sort('createdAt')}>
                                    Дата реєстрації <SortIcon col="createdAt"/>
                                </th>
                                <th>Дії</th>
                            </tr>
                        </thead>
                        <tbody>
                            {displayedUsers.map(u => (
                                <tr key={u.id}>
                                    <td style={{color:'#9ca3af', fontSize:12}}>{u.id}</td>
                                    <td>
                                        <div className="admin-user-cell">
                                            {u.imageId
                                                ? <img src={`/api/files/${u.imageId}`} alt="" className="admin-avatar" />
                                                : <div className="admin-avatar-placeholder">{u.username?.charAt(0).toUpperCase()}</div>
                                            }
                                            <strong>{u.username}</strong>
                                        </div>
                                    </td>
                                    <td style={{color:'#6b7280'}}>{u.email}</td>
                                    <td>
                                        <span className={`badge ${u.role === 'ADMIN' ? 'badge-admin' : 'badge-user'}`}>
                                            {u.role === 'ADMIN' ? 'Адмін' : 'Юзер'}
                                        </span>
                                    </td>
                                    <td>
                                        <span className={`badge ${u.banned ? 'badge-banned' : 'badge-active'}`}>
                                            {u.banned ? 'Заблокований' : 'Активний'}
                                        </span>
                                    </td>
                                    <td style={{color:'#9ca3af', fontSize:12}}>{fmtDate(u.createdAt)}</td>
                                    <td>
                                        <div className="td-actions">
                                            {u.banned ? (
                                                <button className="btn-success" disabled={actionLoading}
                                                    onClick={() => runAction(`Розблокувати «${u.username}»?`, 'Розблокувати',
                                                        `/api/admin/users/${u.id}/unban`, 'POST', `${u.username} розблокований`)}>
                                                    Розблокувати
                                                </button>
                                            ) : (
                                                <button className="btn-danger" disabled={actionLoading}
                                                    onClick={() => runAction(`Заблокувати «${u.username}»?`, 'Заблокувати',
                                                        `/api/admin/users/${u.id}/ban`, 'POST', `${u.username} заблокований`)}>
                                                    Забанити
                                                </button>
                                            )}
                                            <button className="btn-warn" disabled={actionLoading}
                                                onClick={() => runAction(`Скасувати всі підписки «${u.username}»?`, 'Скасувати',
                                                    `/api/admin/users/${u.id}/cancel-subscriptions`, 'POST', 'Підписки скасовано')}>
                                                Підписки &times;
                                            </button>
                                            {u.role !== 'ADMIN' ? (
                                                <button className="btn-ghost" disabled={actionLoading}
                                                    onClick={() => runAction(`Надати адмін-права «${u.username}»?`, 'Надати',
                                                        `/api/admin/users/${u.id}/make-admin`, 'POST', `${u.username} тепер адмін`)}>
                                                    + Адмін
                                                </button>
                                            ) : (
                                                <button className="btn-ghost" disabled={actionLoading}
                                                    onClick={() => runAction(`Зняти адмін-права «${u.username}»?`, 'Зняти',
                                                        `/api/admin/users/${u.id}/remove-admin`, 'POST', `${u.username} більше не адмін`)}>
                                                    &minus; Зняти
                                                </button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <Pagination page={page} total={totalPages} onChange={handlePage} />

            {confirmModal && (
                <ConfirmModal
                    message={confirmModal.msg}
                    confirmLabel={confirmModal.confirmLabel}
                    cancelLabel="Скасувати"
                    onConfirm={confirmModal.onConfirm}
                    onCancel={() => setConfirmModal(null)}
                />
            )}
            {toast && <div className="admin-toast">{toast}</div>}
        </div>
    );
};

const ProjectsTab = ({ token }) => {
    const [projects, setProjects]         = useState([]);
    const [search, setSearch]             = useState('');
    const [statusFilter, setStatusFilter] = useState('');   
    const [sortBy, setSortBy]             = useState('projectId');
    const [sortDir, setSortDir]           = useState('desc');
    const [page, setPage]                 = useState(0);
    const [totalPages, setTotalPages]     = useState(1);
    const [totalElements, setTotalElements] = useState(0);
    const [loading, setLoading]           = useState(false);
    const [confirmModal, setConfirmModal] = useState(null);
    const [actionLoading, setActionLoading] = useState(false);
    const [toast, setToast]               = useState('');
    const dSearch = useDebounce(search);

    const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(''), 3000); };

    const filtersRef = useRef({ search: dSearch, statusFilter, sortBy, sortDir });
    filtersRef.current = { search: dSearch, statusFilter, sortBy, sortDir };

    const fetchProjects = useCallback(async (pg = 0) => {
        const { search, statusFilter, sortBy, sortDir } = filtersRef.current;
        setLoading(true);
        try {
            
            
            
            
            
            const showBanned = statusFilter === 'banned';
            const params = new URLSearchParams({
                page: pg,
                size: 20,
                showBanned,
                sort: `${sortBy},${sortDir}`,
            });
            if (search.trim()) params.set('search', search.trim());
            const res = await fetch(`/api/admin/projects?${params}`, { headers: authHeaders(token) });
            if (res.ok) {
                const data = await res.json();
                
                const content = data.content ?? [];
                setProjects(content);
                setTotalPages(data.totalPages ?? 1);
                setTotalElements(data.totalElements ?? 0);
            }
        } finally { setLoading(false); }
    }, [token]);

    useEffect(() => { setPage(0); fetchProjects(0); }, [dSearch, statusFilter, sortBy, sortDir]); 
    const handlePage = (p) => { setPage(p); fetchProjects(p); };

    const pageRef = useRef(0);
    useEffect(() => { pageRef.current = page; }, [page]);

    const sort = (col) => {
        if (sortBy === col) setSortDir(d => d === 'asc' ? 'desc' : 'asc');
        else { setSortBy(col); setSortDir('asc'); }
    };

    const runAction = (msg, confirmLabel, endpoint, method, successMsg) => {
        setConfirmModal({
            msg, confirmLabel,
            onConfirm: async () => {
                setActionLoading(true);
                try {
                    const res = await fetch(endpoint, { method, headers: authHeaders(token) });
                    if (res.ok) { showToast(successMsg); fetchProjects(pageRef.current); }
                    else showToast('Помилка');
                } finally { setActionLoading(false); setConfirmModal(null); }
            },
        });
    };

    const SortIcon = ({ col }) => (
        <span style={{ opacity: sortBy === col ? 1 : .3, marginLeft: 4, fontSize: 11 }}>
            {sortBy === col ? (sortDir === 'asc' ? '\u25B4' : '\u25BE') : '\u21C5'}
        </span>
    );

    return (
        <div>
            <div className="admin-toolbar">
                <input className="admin-search" placeholder="Пошук за назвою або автором…"
                    value={search} onChange={e => setSearch(e.target.value)} />
                <select className="admin-select" value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
                    <option value="">Всі проєкти</option>
                    <option value="active">Активні</option>
                    <option value="banned">Включно із заблокованими</option>
                </select>
                <button className="btn-secondary" onClick={() => fetchProjects(pageRef.current)}>↻ Оновити</button>
                {totalElements > 0 && (
                    <span className="admin-count">Всього: {totalElements}</span>
                )}
            </div>

            {loading ? (
                <div className="admin-loading">Завантаження…</div>
            ) : projects.length === 0 ? (
                <div className="admin-empty">Проєктів не знайдено</div>
            ) : (
                <div className="admin-table-wrap">
                    <table className="admin-table">
                        <thead>
                            <tr>
                                <th className={sortBy==='projectId'?'sort-active':''} onClick={() => sort('projectId')}>
                                    # <SortIcon col="projectId"/>
                                </th>
                                <th className={sortBy==='title'?'sort-active':''} onClick={() => sort('title')}>
                                    Назва <SortIcon col="title"/>
                                </th>
                                <th>Автор</th>
                                <th>Категорія</th>
                                <th className={sortBy==='goalAmount'?'sort-active':''} onClick={() => sort('goalAmount')}>
                                    Ціль <SortIcon col="goalAmount"/>
                                </th>
                                <th className={sortBy==='raisedAmount'?'sort-active':''} onClick={() => sort('raisedAmount')}>
                                    Зібрано <SortIcon col="raisedAmount"/>
                                </th>
                                <th>Статус</th>
                                <th>Дії</th>
                            </tr>
                        </thead>
                        <tbody>
                            {projects.map(p => (
                                <tr key={p.projectId}>
                                    <td style={{color:'#9ca3af',fontSize:12}}>{p.projectId}</td>
                                    <td><strong>{p.title}</strong></td>
                                    <td style={{color:'#6b7280'}}>{p.creatorName}</td>
                                    <td>{p.category ?? '—'}</td>
                                    <td>{fmtMoney(p.goalAmount)}</td>
                                    <td style={{color:'#059669',fontWeight:600}}>{fmtMoney(p.raisedAmount)}</td>
                                    <td>
                                        <span className={`badge ${p.banned ? 'badge-banned' : 'badge-active'}`}>
                                            {p.banned ? 'Заблокований' : 'Активний'}
                                        </span>
                                    </td>
                                    <td>
                                        <div className="td-actions">
                                            {p.banned ? (
                                                <button className="btn-success" disabled={actionLoading}
                                                    onClick={() => runAction(`Відновити «${p.title}»?`, 'Відновити',
                                        `/api/admin/projects/${p.projectId}/unban`, 'POST', 'Проєкт відновлено')}>
                                            Відновити
                                        </button>
                                    ) : (
                                        <button className="btn-warn" disabled={actionLoading}
                                            onClick={() => runAction(`Заблокувати «${p.title}»?`, 'Заблокувати',
                                                `/api/admin/projects/${p.projectId}/ban`, 'POST', 'Проєкт заблоковано')}>
                                            Заблокувати
                                        </button>
                                    )}
                                    <button className="btn-danger" disabled={actionLoading}
                                        onClick={() => runAction(`Видалити «${p.title}»? Цю дію не можна скасувати.`, 'Видалити',
                                            `/api/admin/projects/${p.projectId}`, 'DELETE', 'Проєкт видалено')}>
                                        Видалити
                                    </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <Pagination page={page} total={totalPages} onChange={handlePage} />
            {confirmModal && (
                <ConfirmModal message={confirmModal.msg} confirmLabel={confirmModal.confirmLabel}
                    cancelLabel="Скасувати" onConfirm={confirmModal.onConfirm} onCancel={() => setConfirmModal(null)} />
            )}
            {toast && <div className="admin-toast">{toast}</div>}
        </div>
    );
};

const PostsTab = ({ token }) => {
    const [posts, setPosts]               = useState([]);
    const [search, setSearch]             = useState('');
    const [statusFilter, setStatusFilter] = useState('');   
    const [page, setPage]                 = useState(0);
    const [totalPages, setTotalPages]     = useState(1);
    const [totalElements, setTotalElements] = useState(0);
    const [loading, setLoading]           = useState(false);
    const [confirmModal, setConfirmModal] = useState(null);
    const [actionLoading, setActionLoading] = useState(false);
    const [toast, setToast]               = useState('');
    const dSearch = useDebounce(search);

    const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(''), 3000); };

    const filtersRef = useRef({ search: dSearch, statusFilter });
    filtersRef.current = { search: dSearch, statusFilter };

    const fetchPosts = useCallback(async (pg = 0) => {
        const { search, statusFilter } = filtersRef.current;
        setLoading(true);
        try {
            const showBanned = statusFilter === 'banned';
            const params = new URLSearchParams({ page: pg, size: 20, showBanned });
            if (search.trim()) params.set('search', search.trim());
            const res = await fetch(`/api/admin/posts?${params}`, { headers: authHeaders(token) });
            if (res.ok) {
                const data = await res.json();
                setPosts(data.content ?? []);
                setTotalPages(data.totalPages ?? 1);
                setTotalElements(data.totalElements ?? 0);
            }
        } finally { setLoading(false); }
    }, [token]);

    useEffect(() => { setPage(0); fetchPosts(0); }, [dSearch, statusFilter]); 
    const handlePage = (p) => { setPage(p); fetchPosts(p); };

    const pageRef = useRef(0);
    useEffect(() => { pageRef.current = page; }, [page]);

    const runAction = (msg, confirmLabel, endpoint, method, successMsg) => {
        setConfirmModal({
            msg, confirmLabel,
            onConfirm: async () => {
                setActionLoading(true);
                try {
                    const res = await fetch(endpoint, { method, headers: authHeaders(token) });
                    if (res.ok) { showToast(successMsg); fetchPosts(pageRef.current); }
                    else showToast('Помилка');
                } finally { setActionLoading(false); setConfirmModal(null); }
            },
        });
    };

    return (
        <div>
            <div className="admin-toolbar">
                <input className="admin-search" placeholder="Пошук за заголовком або автором…"
                    value={search} onChange={e => setSearch(e.target.value)} />
                <select className="admin-select" value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
                    <option value="">Всі пости</option>
                    <option value="active">Активні</option>
                    <option value="banned">Включно із заблокованими</option>
                </select>
                <button className="btn-secondary" onClick={() => fetchPosts(pageRef.current)}>↻ Оновити</button>
                {totalElements > 0 && (
                    <span className="admin-count">Всього: {totalElements}</span>
                )}
            </div>

            {loading ? (
                <div className="admin-loading">Завантаження…</div>
            ) : posts.length === 0 ? (
                <div className="admin-empty">Постів не знайдено</div>
            ) : (
                <div className="admin-table-wrap">
                    <table className="admin-table">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Заголовок</th>
                                <th>Автор</th>
                                <th>Доступ</th>
                                <th>Статус</th>
                                <th>Дата</th>
                                <th>Дії</th>
                            </tr>
                        </thead>
                        <tbody>
                            {posts.map(p => (
                                <tr key={p.postId}>
                                    <td style={{color:'#9ca3af',fontSize:12}}>{p.postId}</td>
                                    <td><strong>{p.title || '(без заголовку)'}</strong></td>
                                    <td style={{color:'#6b7280'}}>{p.authorName}</td>
                                    <td>
                                        {p.requiredTierId
                                            ? <span className="badge badge-sub">Платний</span>
                                            : <span className="badge badge-active">Всі</span>
                                        }
                                    </td>
                                    <td>
                                        <span className={`badge ${p.banned ? 'badge-banned' : 'badge-active'}`}>
                                            {p.banned ? 'Заблокований' : 'Активний'}
                                        </span>
                                    </td>
                                    <td style={{color:'#9ca3af',fontSize:12}}>{fmtDate(p.createdAt)}</td>
                                    <td>
                                        <div className="td-actions">
                                            {p.banned ? (
                                                <button className="btn-success" disabled={actionLoading}
                                                    onClick={() => runAction(`Відновити пост «${p.title}»?`, 'Відновити',
                                        `/api/admin/posts/${p.postId}/unban`, 'POST', 'Пост відновлено')}>
                                            Відновити
                                        </button>
                                    ) : (
                                        <button className="btn-warn" disabled={actionLoading}
                                            onClick={() => runAction(`Заблокувати пост «${p.title}»?`, 'Заблокувати',
                                                `/api/admin/posts/${p.postId}/ban`, 'POST', 'Пост заблоковано')}>
                                            Заблокувати
                                        </button>
                                    )}
                                    <button className="btn-danger" disabled={actionLoading}
                                        onClick={() => runAction(`Видалити пост «${p.title}»? Цю дію не можна скасувати.`, 'Видалити',
                                            `/api/admin/posts/${p.postId}`, 'DELETE', 'Пост видалено')}>
                                        Видалити
                                    </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <Pagination page={page} total={totalPages} onChange={handlePage} />
            {confirmModal && (
                <ConfirmModal message={confirmModal.msg} confirmLabel={confirmModal.confirmLabel}
                    cancelLabel="Скасувати" onConfirm={confirmModal.onConfirm} onCancel={() => setConfirmModal(null)} />
            )}
            {toast && <div className="admin-toast">{toast}</div>}
        </div>
    );
};

const CategoryModal = ({ initial, onSave, onCancel, loading }) => {
    const [name, setName]         = useState(initial?.name ?? '');
    const [desc, setDesc]         = useState(initial?.description ?? '');
    const isEdit = !!initial?.id;

    return (
        <div className="cat-modal-overlay" onClick={onCancel}>
            <div className="cat-modal" onClick={e => e.stopPropagation()}>
                <h3>{isEdit ? 'Редагувати категорію' : 'Нова категорія'}</h3>
                <div className="form-group">
                    <label>Назва *</label>
                    <input value={name} onChange={e => setName(e.target.value)} placeholder="Назва категорії" maxLength={80} />
                </div>
                <div className="form-group">
                    <label>Опис</label>
                    <textarea value={desc} onChange={e => setDesc(e.target.value)}
                        placeholder="Короткий опис (необов'язково)" rows={3} />
                </div>
                <div className="cat-modal-actions">
                    <button className="btn-secondary" onClick={onCancel} disabled={loading}>Скасувати</button>
                    <button className="btn-primary" disabled={!name.trim() || loading}
                        onClick={() => onSave({ name: name.trim(), description: desc.trim() || null })}>
                        {loading ? 'Збереження…' : (isEdit ? 'Зберегти' : 'Створити')}
                    </button>
                </div>
            </div>
        </div>
    );
};

const CategoriesTab = ({ token }) => {
    const [cats, setCats]             = useState([]);
    const [loading, setLoading]       = useState(false);
    const [modal, setModal]           = useState(null); 
    const [saveLoading, setSaveLoading] = useState(false);
    const [confirmModal, setConfirmModal] = useState(null);
    const [toast, setToast]           = useState('');

    const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(''), 3000); };

    const fetchCats = useCallback(async () => {
        setLoading(true);
        try {
            const res = await fetch('/api/admin/categories', { headers: authHeaders(token) });
            if (res.ok) setCats(await res.json());
        } finally { setLoading(false); }
    }, [token]);

    useEffect(() => { fetchCats(); }, [fetchCats]);

    const handleSave = async (body) => {
        setSaveLoading(true);
        try {
            const isEdit = !!modal?.data?.id;
            const url    = isEdit ? `/api/admin/categories/${modal.data.id}` : '/api/admin/categories';
            const res    = await fetch(url, {
                method: isEdit ? 'PUT' : 'POST',
                headers: authHeaders(token),
                body: JSON.stringify(body),
            });
            if (res.ok) {
                showToast(isEdit ? 'Категорію оновлено' : 'Категорію створено');
                setModal(null);
                fetchCats();
            } else showToast('Помилка збереження');
        } finally { setSaveLoading(false); }
    };

    const handleDelete = (cat) => {
        setConfirmModal({
            msg: `Видалити категорію «${cat.name}»? Проєкти з цією категорією не видаляться.`,
            confirmLabel: 'Видалити',
            onConfirm: async () => {
                try {
                    const res = await fetch(`/api/admin/categories/${cat.id}`, {
                        method: 'DELETE', headers: authHeaders(token),
                    });
                    if (res.ok) { showToast('Категорію видалено'); fetchCats(); }
                    else showToast('Помилка видалення');
                } finally { setConfirmModal(null); }
            },
        });
    };

    return (
        <div>
            <div className="admin-toolbar">
                <div className="admin-toolbar-right">
                    <button className="btn-primary" onClick={() => setModal({ mode: 'create' })}>
                        + Нова категорія
                    </button>
                    <button className="btn-secondary" onClick={fetchCats}>↻ Оновити</button>
                </div>
            </div>

            {loading ? (
                <div className="admin-loading">Завантаження…</div>
            ) : cats.length === 0 ? (
                <div className="admin-empty">Категорій ще немає</div>
            ) : (
                <div className="admin-table-wrap">
                    <table className="admin-table">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Назва</th>
                                <th>Опис</th>
                                <th>Дії</th>
                            </tr>
                        </thead>
                        <tbody>
                            {cats.map(c => (
                                <tr key={c.id}>
                                    <td style={{color:'#9ca3af',fontSize:12}}>{c.id}</td>
                                    <td><strong>{c.name}</strong></td>
                                    <td style={{color:'#6b7280'}}>{c.description ?? '—'}</td>
                                    <td>
                                        <div className="td-actions">
                                            <button className="btn-ghost"
                                                onClick={() => setModal({ mode: 'edit', data: c })}>
                                                Редагувати
                                            </button>
                                            <button className="btn-danger" onClick={() => handleDelete(c)}>
                                                Видалити
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {modal && (
                <CategoryModal
                    initial={modal.data}
                    onSave={handleSave}
                    onCancel={() => setModal(null)}
                    loading={saveLoading}
                />
            )}
            {confirmModal && (
                <ConfirmModal message={confirmModal.msg} confirmLabel={confirmModal.confirmLabel}
                    cancelLabel="Скасувати" onConfirm={confirmModal.onConfirm} onCancel={() => setConfirmModal(null)} />
            )}
            {toast && <div className="admin-toast">{toast}</div>}
        </div>
    );
};

const TransactionsTab = ({ token }) => {
    const [txs, setTxs]               = useState([]);
    const [summary, setSummary]       = useState(null);
    const [type, setType]             = useState('');
    const [search, setSearch]         = useState('');
    const [from, setFrom]             = useState('');
    const [to, setTo]                 = useState('');
    const [page, setPage]             = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [loading, setLoading]       = useState(false);
    const dSearch = useDebounce(search);

    const filtersRef = useRef({ type, search: dSearch, from, to });
    filtersRef.current = { type, search: dSearch, from, to };

    const fetchSummary = useCallback(async () => {
        const res = await fetch('/api/admin/transactions/summary', { headers: authHeaders(token) });
        if (res.ok) setSummary(await res.json());
    }, [token]);

    const fetchTxs = useCallback(async (pg = 0) => {
        const { type, search, from, to } = filtersRef.current;
        setLoading(true);
        try {
            const params = new URLSearchParams({ page: pg, size: 20 });
            if (type)   params.set('type', type);
            if (search.trim()) params.set('search', search.trim());
            if (from)   params.set('from', from);
            if (to)     params.set('to', to);
            const res = await fetch(`/api/admin/transactions?${params}`, { headers: authHeaders(token) });
            if (res.ok) {
                const data = await res.json();
                setTxs(data.content ?? []);
                setTotalPages(data.totalPages ?? 1);
            }
        } finally { setLoading(false); }
    }, [token]);

    useEffect(() => { fetchSummary(); }, [fetchSummary]);
    useEffect(() => { setPage(0); fetchTxs(0); }, [dSearch, type, from, to]); 
    const handlePage = (p) => { setPage(p); fetchTxs(p); };

    const txBadge = (t) => {
        const map = {
            DONATION:    <span className="badge badge-donate">Донат</span>,
            SUBSCRIPTION:<span className="badge badge-sub">Підписка</span>,
            WITHDRAWAL:  <span className="badge badge-withdraw">Виведення</span>,
        };
        return map[t] ?? <span className="badge badge-user">{t}</span>;
    };

    const statusBadge = (s) => {
        if (s === 'SUCCESS' || s === 'PAID') return <span className="badge badge-done">{s}</span>;
        if (s === 'FAILED')  return <span className="badge badge-failed">{s}</span>;
        return <span className="badge badge-pending">{s}</span>;
    };

    return (
        <div>
            {summary && (
                <div className="admin-summary">
                    <div className="summary-card summary-card--green">
                        <div className="sc-label">Донати (сума)</div>
                        <div className="sc-value">{fmtMoney(summary.totalDonations)}</div>
                        <div className="sc-sub">{summary.donationsCount} операцій</div>
                    </div>
                    <div className="summary-card summary-card--purple">
                        <div className="sc-label">Підписки (сума)</div>
                        <div className="sc-value">{fmtMoney(summary.totalSubscriptions)}</div>
                        <div className="sc-sub">{summary.subscriptionsCount} операцій</div>
                    </div>
                    <div className="summary-card summary-card--orange">
                        <div className="sc-label">Виведення (сума)</div>
                        <div className="sc-value">{fmtMoney(summary.totalWithdrawals)}</div>
                        <div className="sc-sub">{summary.withdrawalsCount} операцій</div>
                    </div>
                    <div className="summary-card summary-card--blue">
                        <div className="sc-label">Загальний обсяг</div>
                        <div className="sc-value">{fmtMoney(summary.totalVolume)}</div>
                        <div className="sc-sub">{summary.totalCount} операцій</div>
                    </div>
                </div>
            )}

            <div className="admin-toolbar">
                <input className="admin-search" placeholder="Пошук за користувачем…"
                    value={search} onChange={e => setSearch(e.target.value)} />
                <select className="admin-select" value={type} onChange={e => setType(e.target.value)}>
                    <option value="">Всі типи</option>
                    <option value="DONATION">Донат</option>
                    <option value="SUBSCRIPTION">Підписка</option>
                    <option value="WITHDRAWAL">Виведення</option>
                </select>
                <input type="date" className="admin-date-input" value={from} onChange={e => setFrom(e.target.value)} title="Від" />
                <input type="date" className="admin-date-input" value={to}   onChange={e => setTo(e.target.value)}   title="До" />
                {(from || to || type || search) && (
                    <button className="btn-ghost" onClick={() => { setSearch(''); setType(''); setFrom(''); setTo(''); }}>
                        ✕ Скинути
                    </button>
                )}
                <button className="btn-secondary" onClick={() => fetchTxs(page)}>↻ Оновити</button>
            </div>

            {loading ? (
                <div className="admin-loading">Завантаження…</div>
            ) : txs.length === 0 ? (
                <div className="admin-empty">Транзакцій не знайдено</div>
            ) : (
                <div className="admin-table-wrap">
                    <table className="admin-table">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Тип</th>
                                <th>Від</th>
                                <th>Кому</th>
                                <th>Сума</th>
                                <th>Статус</th>
                                <th>Дата</th>
                            </tr>
                        </thead>
                        <tbody>
                            {txs.map(tx => (
                                <tr key={tx.id}>
                                    <td style={{color:'#9ca3af',fontSize:12}}>{tx.id}</td>
                                    <td>{txBadge(tx.type)}</td>
                                    <td>{tx.fromUser ?? '—'}</td>
                                    <td>{tx.toUser ?? '—'}</td>
                                    <td style={{fontWeight:700,color:'#059669'}}>{fmtMoney(tx.amount)}</td>
                                    <td>{statusBadge(tx.status)}</td>
                                    <td style={{color:'#9ca3af',fontSize:12}}>{fmtDateTime(tx.createdAt)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            <Pagination page={page} total={totalPages} onChange={handlePage} />
        </div>
    );
};

const TABS = [
    { id: 'users',        label: 'Користувачі', icon: null },
    { id: 'projects',     label: 'Проєкти',     icon: null },
    { id: 'posts',        label: 'Пости',        icon: null },
    { id: 'categories',   label: 'Категорії',    icon: null },
    { id: 'transactions', label: 'Транзакції',   icon: null },
];

const AdminPage = () => {
    const navigate  = useNavigate();
    const currentUser = AuthService.getCurrentUser();
    const [activeTab, setActiveTab] = useState('users');

    const isAdmin = (u) => {
        if (!u) return false;
        if (u.role === 'ADMIN') return true;
        return Array.isArray(u.roles) && u.roles.some(r => r === 'ROLE_ADMIN' || r === 'ADMIN');
    };

    if (!isAdmin(currentUser)) {
        return (
            <div className="admin-page-access-denied">
                <h2>Доступ заборонено</h2>
                <p>Ця сторінка доступна лише адміністраторам.</p>
                <button className="btn-primary" style={{margin:'20px auto',display:'inline-flex'}} onClick={() => navigate('/')}>
                    На головну
                </button>
            </div>
        );
    }

    const token = currentUser.token;

    return (
        <div className="admin-page">
            <div className="admin-page-header">
                <h1>Адмін-панель</h1>
                <span className="admin-badge">ADMIN</span>
            </div>

            <div className="admin-tabs">
                {TABS.map(t => (
                    <button
                        key={t.id}
                        className={`admin-tab${activeTab === t.id ? ' active' : ''}`}
                        onClick={() => setActiveTab(t.id)}
                    >
                        {t.label}
                    </button>
                ))}
            </div>

            {activeTab === 'users'        && <UsersTab        token={token} />}
            {activeTab === 'projects'     && <ProjectsTab     token={token} />}
            {activeTab === 'posts'        && <PostsTab        token={token} />}
            {activeTab === 'categories'   && <CategoriesTab   token={token} />}
            {activeTab === 'transactions' && <TransactionsTab token={token} />}
        </div>
    );
};

export default AdminPage;

