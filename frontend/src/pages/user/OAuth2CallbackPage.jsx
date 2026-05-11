import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

const OAuth2CallbackPage = () => {
    const [params] = useSearchParams();
    const navigate = useNavigate();

    useEffect(() => {
        const token    = params.get('token');
        const id       = params.get('id');
        const username = params.get('username');
        const role     = params.get('role');

        if (token && username) {
            const user = { token, id: Number(id), username, role, roles: [role === 'ADMIN' ? 'ROLE_ADMIN' : 'ROLE_USER'] };
            localStorage.setItem('user', JSON.stringify(user));
        }
        navigate('/', { replace: true });
        window.location.reload();
    }, []); 

    return (
        <div style={{ textAlign: 'center', padding: '80px', color: '#888' }}>
            Авторизація через соціальну мережу…
        </div>
    );
};

export default OAuth2CallbackPage;

