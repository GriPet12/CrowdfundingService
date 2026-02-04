import axios from 'axios';

const API_URL = '/api/api/auth/';

const register = (username, password) => {
    return axios.post(API_URL + 'register', { username, password });
};

const login = async (username, password) => {
    const response = await axios.post(API_URL + 'login', { username, password });
    if (response.data.token) {
        localStorage.setItem('user', JSON.stringify(response.data));
    }
    return response.data;
};

export default { register, login };