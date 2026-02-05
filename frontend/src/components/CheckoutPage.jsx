import WayForPayForm from './WayForPayForm';

function CheckoutPage() {
    // В реальному проєкті ці дані прийдуть із кошика або стану додатка
    const orderInfo = {
        id: "ORDER_" + Date.now(), // Унікальний ID для тесту
        total: 150.00
    };

    return (
        <div>
            <h1>Оформлення внеску</h1>
            <p>Сума до сплати: {orderInfo.total} грн</p>

            <WayForPayForm
                orderId={orderInfo.id}
                amount={orderInfo.total}
            />
        </div>
    );
}

export default CheckoutPage;