import React, { useState } from 'react';
import AuthService from './AuthService.jsx';

const LoginUser = () => {
    const [data, setData] = useState({ username: '', password: '' });
    const [error, setError] = useState('');

    const handleLogin = (e) => {
        e.preventDefault();
        setError('');
        AuthService.login(data.username, data.password).then(
            () => {
                window.location.reload();
            },
            (error) => {
                console.log(error);
                setError('Невірний логін або пароль');
            }
        );
    };

    return (
        <form className="auth-form" onSubmit={handleLogin}>
            {error && <div style={{color: 'red', fontSize: '14px'}}>{error}</div>}
            <div className="form-group">
                <label className="form-label">Username</label>
                <input
                    type="text"
                    className="form-input"
                    placeholder="Введіть ваше ім'я"
                    value={data.username}
                    onChange={e => setData({...data, username: e.target.value})}
                    required
                />
            </div>
            <div className="form-group">
                <label className="form-label">Password</label>
                <input
                    type="password"
                    className="form-input"
                    placeholder="Введіть ваш пароль"
                    value={data.password}
                    onChange={e => setData({...data, password: e.target.value})}
                    required
                />
            </div>
            <button type="submit" className="btn-submit">Увійти</button>
        </form>
    );
};

export default LoginUser;