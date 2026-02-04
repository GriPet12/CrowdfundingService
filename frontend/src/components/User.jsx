import React from 'react';

const User = ({user}) => {
    return (
        <div>
            <div className="user_image"><img src={"http://localhost:8080/" + user.image} /></div>
            <div className="user_name">{user.name}</div>
        </div>
    )
}

export default User;
