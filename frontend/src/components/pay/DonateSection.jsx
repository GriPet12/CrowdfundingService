import { useState, useEffect } from 'react';
import WayForPayForm from './WayForPayForm.jsx';
import AuthService from '../user/AuthService.jsx';

const DonateSection = ({
    type            = 'DONATION',
    paymentPayload  = {},
    projectId       = null,
    disabled        = false,
    fundraisingClosed = false,
    closedLabel     = 'Збір закрито',
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
    onDonate        = null,
}) => {
    const [isDonating, setIsDonating] = useState(false);
    const [amount, setAmount]         = useState('');
    const [rewards, setRewards]       = useState([]);
    const [selectedReward, setSelectedReward] = useState(null);
    const [rewardWarning, setRewardWarning]   = useState(false);

    const currentUser = AuthService.getCurrentUser();
    const isClosed = fundraisingClosed;
    const isBlocked = disabled || isClosed;

    useEffect(() => {
        if (!projectId || isClosed) return;
        fetch(`/api/rewards/${projectId}`)
            .then(r => r.ok ? r.json() : [])
            .then(data => setRewards(Array.isArray(data) ? data : []))
            .catch(() => {});
    }, [projectId, isClosed]);

    const handleCancel = () => {
        setIsDonating(false);
        setAmount('');
        setSelectedReward(null);
        setRewardWarning(false);
    };

    const handleRewardSelect = (reward) => {
        if (!currentUser) {
            setRewardWarning(true);
            setSelectedReward(null);
            return;
        }
        setRewardWarning(false);
        if (selectedReward?.rewardId === reward.rewardId) {
            setSelectedReward(null);
        } else {
            setSelectedReward(reward);
            if (!amount || parseFloat(amount) < parseFloat(reward.minimalAmount)) {
                setAmount(String(reward.minimalAmount));
            }
        }
    };

    const finalPayload = {
        ...paymentPayload,
        reward: selectedReward ? selectedReward.rewardId : (paymentPayload.reward ?? 0),
    };

    const buttonLabel = isClosed ? closedLabel : 'Задонатити';
    const buttonTitle = isClosed
        ? 'Збір коштів для цього проєкту закрито'
        : disabled
            ? 'Ваш акаунт заблоковано'
            : undefined;

    return (
        <div className={`${wrapperClass} ${isDonating ? 'active' : ''}${isClosed ? ' donate-section--closed' : ''}`}>
            <div className={btnStartClass}>
                {isBlocked ? (
                    <button
                        type="button"
                        className={`${startBtnClass}${isClosed ? ' donate-button-closed' : ''}`}
                        disabled
                        title={buttonTitle}
                    >
                        {buttonLabel}
                    </button>
                ) : (
                    <button type="button" className={startBtnClass} onClick={() => setIsDonating(true)}>
                        Задонатити
                    </button>
                )}
            </div>

            {!isBlocked && (
            <div className={btnInputClass}>
                {isDonating && rewards.length > 0 && (
                    <div className="donate-rewards-list">
                        <p className="donate-rewards-title">Оберіть винагороду (необов'язково):</p>
                        {rewards.map(r => {
                            const soldOut = r.isHaveQuantity && r.quantityClaimed >= r.quantityAvailable;
                            return (
                                <button
                                    key={r.rewardId}
                                    type="button"
                                    className={`donate-reward-item ${selectedReward?.rewardId === r.rewardId ? 'donate-reward-item--active' : ''} ${soldOut ? 'donate-reward-item--soldout' : ''}`}
                                    onClick={() => !soldOut && handleRewardSelect(r)}
                                    disabled={soldOut}
                                    title={soldOut ? 'Вичерпано' : ''}
                                >
                                    <span className="donate-reward-name">{r.rewardName}</span>
                                    <span className="donate-reward-amount">від ₴{r.minimalAmount}</span>
                                    {soldOut && <span className="donate-reward-soldout">Вичерпано</span>}
                                </button>
                            );
                        })}
                        {rewardWarning && (
                            <p className="donate-reward-warning">
                                Винагороди доступні лише для авторизованих користувачів. Ви можете зробити донат без винагороди.
                            </p>
                        )}
                    </div>
                )}

                <div className={inputGroupClass}>
                    <button type="button" className={cancelBtnClass} onClick={handleCancel} title="Скасувати">✕</button>
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
                        paymentPayload={finalPayload}
                        confirmClass={confirmClass}
                        label={confirmLabel}
                        onBeforeSubmit={onDonate}
                    />
                </div>
            </div>
            )}
        </div>
    );
};

export default DonateSection;
