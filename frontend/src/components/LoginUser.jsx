import React, { useState } from 'react';
import AuthService from './AuthService.jsx';

const LoginUser = () => {
    const [data, setData] = useState({ username: '', password: '' });

    const handleLogin = (e) => {
        e.preventDefault();
        AuthService.login(data.username, data.password).then(
            () => {
                alert("Успішний вхід!");
                window.location.reload(); // або редирект через useNavigate
            },
            (error) => console.log(error)
        );
    };

    return (
        <form onSubmit={handleLogin}>
            <input type="text" placeholder="Username" onChange={e => setData({...data, username: e.target.value})} />
            <input type="password" placeholder="Password" onChange={e => setData({...data, password: e.target.value})} />
            <button type="submit">Увійти</button>
        </form>
    );
};

export default LoginUser;