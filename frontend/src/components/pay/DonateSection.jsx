import { useState } from 'react';
import WayForPayForm from './WayForPayForm.jsx';

const DonateSection = ({
    type            = 'DONATION',
    paymentPayload  = {},
    wrapperClass    = 'donate-section',
    btnStartClass   = 'donate-start-wrapper',
    btnInputClass   = 'donate-input-wrapper',
    inputGroupClass = 'donate-input-group',
    startBtnClass   = 'donate-button-start',
    cancelBtnClass  = 'donate-cancel-button',
    inputClass      = 'donate-input',
    confirmClass    = 'donate-button-confirm',
    confirmLabel    = '✓',
    placeholder     = 'Сума',
}) => {
    const [isDonating, setIsDonating] = useState(false);
    const [amount, setAmount]         = useState('');

    const handleCancel = () => {
        setIsDonating(false);
        setAmount('');
    };

    return (
        <div className={`${wrapperClass} ${isDonating ? 'active' : ''}`}>

            <div className={btnStartClass}>
                <button type="button" className={startBtnClass} onClick={() => setIsDonating(true)}>
                    Задонатити
                </button>
            </div>

            <div className={btnInputClass}>
                <div className={inputGroupClass}>
                    <button
                        type="button"
                        className={cancelBtnClass}
                        onClick={handleCancel}
                        title="Скасувати"
                    >
                        ✕
                    </button>
                    <input
                        type="number"
                        className={inputClass}
                        placeholder={placeholder}
                        value={amount}
                        min="1"
                        onChange={(e) => setAmount(e.target.value)}
                    />
                    <WayForPayForm
                        amount={parseFloat(amount) || 0}
                        type={type}
                        paymentPayload={paymentPayload}
                        confirmClass={confirmClass}
                        label={confirmLabel}
                    />
                </div>
            </div>

        </div>
    );
};

export default DonateSection;
