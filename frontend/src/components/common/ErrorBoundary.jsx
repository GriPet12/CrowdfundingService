import { Component } from 'react';

class ErrorBoundary extends Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }

    componentDidCatch(error, info) {
        console.error('ErrorBoundary caught:', error, info);
    }

    render() {
        if (this.state.hasError) {
            return (
                <div style={{
                    display: 'flex', flexDirection: 'column', alignItems: 'center',
                    justifyContent: 'center', padding: '60px 24px', color: '#555', textAlign: 'center',
                }}>
                    <h2 style={{ margin: '0 0 8px', fontSize: '1.2rem', color: '#333' }}>
                        Щось пішло не так
                    </h2>
                    <p style={{ margin: '0 0 20px', fontSize: '0.95rem' }}>
                        Виникла непередбачена помилка. Спробуйте перезавантажити сторінку.
                    </p>
                    <button
                        onClick={() => this.setState({ hasError: false, error: null })}
                        style={{
                            padding: '8px 20px', borderRadius: '8px', border: 'none',
                            background: '#4f46e5', color: '#fff', cursor: 'pointer', fontSize: '0.95rem',
                        }}
                    >
                        Спробувати ще раз
                    </button>
                </div>
            );
        }
        return this.props.children;
    }
}

export default ErrorBoundary;

