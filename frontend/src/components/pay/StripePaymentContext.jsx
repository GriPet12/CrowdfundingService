import { createContext, useContext, useState, useCallback } from 'react';

export const StripePaymentContext = createContext(null);

export const StripePaymentProvider = ({ children }) => {
    const [paymentState, setPaymentState] = useState(null);

    const openPayment = useCallback((opts) => {
        setPaymentState(opts);
    }, []);

    const closePayment = useCallback(() => {
        setPaymentState(null);
    }, []);

    return (
        <StripePaymentContext.Provider value={{ openPayment, closePayment, paymentState }}>
            {children}
        </StripePaymentContext.Provider>
    );
};

// eslint-disable-next-line react-refresh/only-export-components
export const useStripePayment = () => useContext(StripePaymentContext);
