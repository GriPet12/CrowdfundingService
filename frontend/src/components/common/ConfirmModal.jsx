import { useEffect } from 'react';
import '../../styles/confirmModal.css';

const ConfirmModal = ({ message, confirmLabel = 'Видалити', cancelLabel = 'Скасувати', onConfirm, onCancel }) => {

    
    useEffect(() => {
        const onKey = (e) => { if (e.key === 'Escape') onCancel(); };
        document.addEventListener('keydown', onKey);
        return () => document.removeEventListener('keydown', onKey);
    }, [onCancel]);

    return (
        <div className="cm-overlay" onClick={onCancel}>
            <div className="cm-dialog" onClick={(e) => e.stopPropagation()}>
                <div className="cm-icon">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <polyline points="3 6 5 6 21 6"/>
                        <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                        <path d="M10 11v6M14 11v6"/>
                        <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
                    </svg>
                </div>
                <p className="cm-message">{message}</p>
                <div className="cm-actions">
                    <button className="cm-btn cm-btn--cancel" onClick={onCancel}>{cancelLabel}</button>
                    <button className="cm-btn cm-btn--confirm" onClick={onConfirm}>{confirmLabel}</button>
                </div>
            </div>
        </div>
    );
};

export default ConfirmModal;

