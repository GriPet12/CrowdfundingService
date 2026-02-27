import React, { useState } from 'react';
import AuthService from './AuthService';

const RegisterUser = ({ onSuccess }) => {
    const [data, setData] = useState({ username: '', email: '', password: '', confirmPassword: '' });
    const [error, setError] = useState('');

    const handleRegister = (e) => {
        e.preventDefault();
        setError('');

        if (data.password !== data.confirmPassword) {
            setError('Паролі не співпадають');
            return;
        }

        AuthService.register(data.username, data.email, data.password).then(
            () => {
                window.location.reload();
            },
            (error) => {
                console.log(error);
                setError('Помилка реєстрації. Можливо, користувач вже існує.');
            }
        );
    };

    return (
        <form className="auth-form" onSubmit={handleRegister}>
            {error && <div style={{color: 'red', fontSize: '14px'}}>{error}</div>}

            <div className="form-group">
                <label className="form-label">Username</label>
                <input
                    type="text"
                    className="form-input"
                    placeholder="Введіть ім'я користувача"
                    value={data.username}
                    onChange={e => setData({...data, username: e.target.value})}
                    required
                />
            </div>

            <div className="form-group">
                <label className="form-label">Email</label>
                <input
                    type="email"
                    className="form-input"
                    placeholder="Введіть ваш email"
                    value={data.email}
                    onChange={e => setData({...data, email: e.target.value})}
                    required
                />
            </div>

            <div className="form-group">
                <label className="form-label">Password</label>
                <input
                    type="password"
                    className="form-input"
                    placeholder="Введіть пароль"
                    value={data.password}
                    onChange={e => setData({...data, password: e.target.value})}
                    required
                />
            </div>

            <div className="form-group">
                <label className="form-label">Confirm Password</label>
                <input
                    type="password"
                    className="form-input"
                    placeholder="Підтвердіть пароль"
                    value={data.confirmPassword}
                    onChange={e => setData({...data, confirmPassword: e.target.value})}
                    required
                />
            </div>

            <button type="submit" className="btn-submit">Зареєструватися</button>
        </form>
    );
};

export default RegisterUser;
