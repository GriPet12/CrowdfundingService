import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthService from '../user/AuthService.jsx';
import '../../styles/createProject.css';

const MAX_FILE_SIZE_MB = 15;

const CreateProjectPage = () => {
    const navigate = useNavigate();
    const currentUser = AuthService.getCurrentUser();
    const mainImageRef = useRef();
    const mediaRef = useRef();

    const [form, setForm] = useState({
        title: '',
        description: '',
        goalAmount: '',
        categories: [],
    });

    const [mainImageFile, setMainImageFile] = useState(null);  
    const [mainImagePreview, setMainImagePreview] = useState(null);
    const [mediaFiles, setMediaFiles] = useState([]);           
    const [categories, setCategories] = useState([]);
    const [submitting, setSubmitting] = useState(false);
    const [uploadProgress, setUploadProgress] = useState('');
    const [error, setError] = useState('');

    
    useEffect(() => {
        if (!currentUser) navigate('/');
    }, []); 

    
    useEffect(() => {
        fetch('/api/categories', {
            headers: { Authorization: `Bearer ${currentUser?.token}` },
        })
            .then(r => r.ok ? r.json() : [])
            .then(setCategories)
            .catch(() => {});
    }, []); 

    const handleField = (e) => {
        const { name, value } = e.target;
        setForm(p => ({ ...p, [name]: value }));
    };

    const toggleCategory = (name) => {
        setForm(p => ({
            ...p,
            categories: p.categories.includes(name)
                ? p.categories.filter(c => c !== name)
                : [...p.categories, name],
        }));
    };

    const handleMainImage = (e) => {
        const file = e.target.files[0];
        if (!file) return;
        if (!file.type.startsWith('image/')) { setError('Головне зображення має бути картинкою'); return; }
        setMainImageFile(file);
        setMainImagePreview(URL.createObjectURL(file));
        setError('');
    };

    const handleMediaFiles = (e) => {
        const files = Array.from(e.target.files);
        const valid = files.filter(f => {
            if (f.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
                setError(`Файл "${f.name}" перевищує ${MAX_FILE_SIZE_MB}МБ`);
                return false;
            }
            return true;
        });
        const newItems = valid.map(f => ({
            file: f,
            preview: f.type.startsWith('image/') || f.type.startsWith('video/')
                ? URL.createObjectURL(f) : null,
            type: f.type,
            name: f.name,
        }));
        setMediaFiles(p => [...p, ...newItems]);
        e.target.value = '';
    };

    const removeMedia = (idx) => {
        setMediaFiles(p => p.filter((_, i) => i !== idx));
    };

    const uploadFile = async (file) => {
        const fd = new FormData();
        fd.append('file', file);
        const res = await fetch('/api/files/upload', {
            method: 'POST',
            headers: { Authorization: `Bearer ${currentUser.token}` },
            body: fd,
        });
        if (!res.ok) throw new Error('Помилка завантаження файлу');
        const data = await res.json();
        return data.id;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (!form.title.trim()) { setError('Введіть назву проекту'); return; }
        if (!form.goalAmount || Number(form.goalAmount) <= 0) { setError('Введіть суму збору > 0'); return; }
        if (!mainImageFile) { setError('Додайте головне зображення'); return; }

        setSubmitting(true);
        try {
            
            setUploadProgress('Завантаження головного зображення…');
            const mainImageId = await uploadFile(mainImageFile);

            
            const mediaIds = [];
            for (let i = 0; i < mediaFiles.length; i++) {
                setUploadProgress(`Завантаження файлів… ${i + 1} / ${mediaFiles.length}`);
                const id = await uploadFile(mediaFiles[i].file);
                mediaIds.push(id);
            }

            
            setUploadProgress('Створення проекту…');
            const payload = {
                title: form.title.trim(),
                description: form.description.trim() || null,
                goalAmount: Number(form.goalAmount),
                mainImage: mainImageId,
                mediaIds: mediaIds,
                categories: form.categories,
            };

            const res = await fetch('/api/projects', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${currentUser.token}`,
                },
                body: JSON.stringify(payload),
            });

            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || 'Помилка створення проекту');
            }

            const created = await res.json();
            navigate(`/project/${created.projectId}`);
        } catch (err) {
            setError(err.message);
            setUploadProgress('');
        } finally {
            setSubmitting(false);
        }
    };

    const fileIcon = (type) => {
        if (type.startsWith('image/')) return 'IMG';
        if (type.startsWith('video/')) return 'VID';
        if (type.startsWith('audio/')) return 'AUD';
        return 'FILE';
    };

    if (!currentUser) return null;

    return (
        <div className="cp-page">
            <div className="cp-back">
                <button className="cp-back-btn" onClick={() => navigate(-1)}>← Назад</button>
            </div>

            <div className="cp-container">
                <h1 className="cp-title">Новий проект</h1>

                <form className="cp-form" onSubmit={handleSubmit}>

                    
                    <section className="cp-section">
                        <h2 className="cp-section-title">Основна інформація</h2>

                        <div className="cp-field">
                            <label className="cp-label">Назва проекту *</label>
                            <input
                                className="cp-input"
                                type="text"
                                name="title"
                                maxLength={120}
                                placeholder="Коротка і зрозуміла назва"
                                value={form.title}
                                onChange={handleField}
                            />
                        </div>

                        <div className="cp-field">
                            <label className="cp-label">Опис</label>
                            <textarea
                                className="cp-textarea"
                                name="description"
                                rows={6}
                                placeholder="Розкажіть про свій проект — навіщо він, що зробите з коштами, хто ви"
                                value={form.description}
                                onChange={handleField}
                            />
                        </div>

                        <div className="cp-field cp-field--half">
                            <label className="cp-label">Сума збору (₴) *</label>
                            <input
                                className="cp-input"
                                type="number"
                                name="goalAmount"
                                min="1"
                                placeholder="10000"
                                value={form.goalAmount}
                                onChange={handleField}
                            />
                        </div>
                    </section>

                    
                    <section className="cp-section">
                        <h2 className="cp-section-title">Головне зображення *</h2>
                        <p className="cp-section-hint">Зображення що буде показуватися на картці проекту</p>

                        <div
                            className={`cp-main-image-drop ${mainImagePreview ? 'cp-main-image-drop--filled' : ''}`}
                            onClick={() => mainImageRef.current.click()}
                        >
                            {mainImagePreview ? (
                                <img src={mainImagePreview} alt="preview" className="cp-main-image-preview" />
                            ) : (
                                <div className="cp-main-image-placeholder">
                                    <span className="cp-upload-icon">
                                        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                                    </span>
                                    <p>Натисніть щоб вибрати зображення</p>
                                    <p className="cp-upload-hint">JPG, PNG, WEBP до {MAX_FILE_SIZE_MB}МБ</p>
                                </div>
                            )}
                        </div>
                        {mainImagePreview && (
                            <button type="button" className="cp-remove-main-img" onClick={() => {
                                setMainImageFile(null);
                                setMainImagePreview(null);
                            }}>✕ Видалити зображення</button>
                        )}
                        <input ref={mainImageRef} type="file" accept="image/*" style={{ display: 'none' }} onChange={handleMainImage} />
                    </section>

                    
                    <section className="cp-section">
                        <h2 className="cp-section-title">Медіа файли</h2>
                        <p className="cp-section-hint">Фото, відео, документи — все що допоможе розповісти про проект</p>

                        <div className="cp-media-grid">
                            {mediaFiles.map((item, idx) => (
                                <div key={idx} className="cp-media-card">
                                    {item.type.startsWith('image/') && item.preview ? (
                                        <img src={item.preview} alt={item.name} className="cp-media-thumb" />
                                    ) : item.type.startsWith('video/') && item.preview ? (
                                        <video src={item.preview} className="cp-media-thumb" muted />
                                    ) : (
                                        <div className="cp-media-icon">{fileIcon(item.type)}</div>
                                    )}
                                    <p className="cp-media-name">{item.name}</p>
                                    <button type="button" className="cp-media-remove" onClick={() => removeMedia(idx)}>✕</button>
                                </div>
                            ))}

                            <div className="cp-media-add" onClick={() => mediaRef.current.click()}>
                                <span className="cp-upload-icon">＋</span>
                                <p>Додати файл</p>
                            </div>
                        </div>
                        <input
                            ref={mediaRef}
                            type="file"
                            multiple
                            accept="image/*,video/*,audio/*,.pdf,.doc,.docx"
                            style={{ display: 'none' }}
                            onChange={handleMediaFiles}
                        />
                    </section>

                    
                    {categories.length > 0 && (
                        <section className="cp-section">
                            <h2 className="cp-section-title">Категорії</h2>
                            <div className="cp-categories">
                                {categories.map(cat => (
                                    <button
                                        key={cat.categoryId}
                                        type="button"
                                        className={`cp-category-btn ${form.categories.includes(cat.categoryName) ? 'cp-category-btn--active' : ''}`}
                                        onClick={() => toggleCategory(cat.categoryName)}
                                    >
                                        {cat.categoryName}
                                    </button>
                                ))}
                            </div>
                        </section>
                    )}

                    
                    {error && <p className="cp-error">{error}</p>}
                    {submitting && uploadProgress && (
                        <p className="cp-progress">{uploadProgress}</p>
                    )}

                    <div className="cp-submit-row">
                        <button
                            type="button"
                            className="cp-cancel-btn"
                            onClick={() => navigate(-1)}
                            disabled={submitting}
                        >
                            Скасувати
                        </button>
                        <button
                            type="submit"
                            className="cp-submit-btn"
                            disabled={submitting}
                        >
                            {submitting ? uploadProgress || 'Публікація…' : 'Опублікувати проект'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default CreateProjectPage;

