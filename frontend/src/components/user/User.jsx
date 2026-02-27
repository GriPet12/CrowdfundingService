import React from 'react';

const User = ({user}) => {
    return (
        <div>
            <div className="user_image"><img src={"/api/files/" + user.image} alt={user.name} /></div>
            <div className="user_name">{user.name}</div>
        </div>
    )
}

export default User;
