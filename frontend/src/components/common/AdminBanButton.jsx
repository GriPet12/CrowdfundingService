import { useState } from 'react';
import AuthService from '../user/AuthService.jsx';
import ConfirmModal from './ConfirmModal.jsx';
import '../../styles/adminBanButton.css';

const AdminBanButton = ({
    type,
    id,
    label = '',
    onDone,
    style,
    withText = false,
    action,          
}) => {
    const currentUser = AuthService.getCurrentUser();
    const [confirm, setConfirm] = useState(false);
    const [loading, setLoading] = useState(false);
    const [done, setDone] = useState(false);

    const isAdmin = (u) => {
        if (!u) return false;
        if (u.role === 'ADMIN') return true;
        return Array.isArray(u.roles) && u.roles.some(r => r === 'ROLE_ADMIN' || r === 'ADMIN');
    };
    if (!isAdmin(currentUser) || done) return null;

    const resolvedAction = action ?? (type === 'user' ? 'ban' : 'delete');

    const cfg = {
        user: {
            ban:    { msg: `Заблокувати «${label}»?`,   endpoint: `/api/admin/users/${id}/ban`,     method: 'POST',   btnLabel: 'Заблокувати', text: 'Забанити',      icon: '\u2715' },
            unban:  { msg: `Розблокувати «${label}»?`,  endpoint: `/api/admin/users/${id}/unban`,   method: 'POST',   btnLabel: 'Розблокувати',text: 'Розблокувати',  icon: '\u2713' },
            delete: { msg: `Видалити «${label}»?`,      endpoint: `/api/admin/users/${id}`,         method: 'DELETE', btnLabel: 'Видалити',    text: 'Видалити',      icon: '\u2715' },
        },
        project: {
            ban:    { msg: `Заблокувати проєкт «${label}»?`, endpoint: `/api/admin/projects/${id}/ban`,   method: 'POST',   btnLabel: 'Заблокувати', text: 'Заблокувати', icon: '\u2715' },
            unban:  { msg: `Відновити проєкт «${label}»?`,   endpoint: `/api/admin/projects/${id}/unban`, method: 'POST',   btnLabel: 'Відновити',   text: 'Відновити',   icon: '\u2713' },
            delete: { msg: `Видалити проєкт «${label}»?`,    endpoint: `/api/admin/projects/${id}`,       method: 'DELETE', btnLabel: 'Видалити',    text: 'Видалити',    icon: '\u2715' },
        },
        post: {
            ban:    { msg: `Заблокувати пост «${label}»?`, endpoint: `/api/admin/posts/${id}/ban`,   method: 'POST',   btnLabel: 'Заблокувати', text: 'Заблокувати', icon: '\u2715' },
            unban:  { msg: `Відновити пост «${label}»?`,   endpoint: `/api/admin/posts/${id}/unban`, method: 'POST',   btnLabel: 'Відновити',   text: 'Відновити',   icon: '\u2713' },
            delete: { msg: `Видалити пост «${label}»?`,    endpoint: `/api/admin/posts/${id}`,       method: 'DELETE', btnLabel: 'Видалити',    text: 'Видалити',    icon: '\u2715' },
        },
    }[type]?.[resolvedAction];

    if (!cfg) return null;

    const handleConfirm = async () => {
        setLoading(true);
        try {
            const res = await fetch(cfg.endpoint, {
                method: cfg.method,
                headers: { Authorization: `Bearer ${currentUser.token}` },
            });
            if (res.ok) {
                setDone(true);
                onDone?.();
            }
        } finally {
            setLoading(false);
            setConfirm(false);
        }
    };

    return (
        <>
            <button
                className={`admin-ban-btn${withText ? ' admin-ban-btn--text' : ''}`}
                style={style}
                onClick={(e) => { e.stopPropagation(); setConfirm(true); }}
                disabled={loading}
                title={cfg.text}
            >
                {withText ? cfg.text : cfg.icon}
            </button>
            {confirm && (
                <ConfirmModal
                    message={cfg.msg}
                    confirmLabel={cfg.btnLabel}
                    cancelLabel="Скасувати"
                    onConfirm={handleConfirm}
                    onCancel={() => setConfirm(false)}
                />
            )}
        </>
    );
};

export default AdminBanButton;

