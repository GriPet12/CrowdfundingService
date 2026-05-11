import AuthService from '../components/user/AuthService.jsx';

const log = (path, requireAuth = true) => {
    const user = AuthService.getCurrentUser();
    if (requireAuth && !user?.token) return;
    fetch(path, {
        method: 'POST',
        headers: user?.token ? { Authorization: `Bearer ${user.token}` } : {},
    }).catch(() => {});
};

const analyticsService = {
    
    
    projectClick:     (projectId)  => log(`/api/analytics/project/${projectId}/click`, false),
    
    projectView:      (projectId)  => log(`/api/analytics/project/${projectId}/view`,  false),
    
    projectDonate:    (projectId)  => log(`/api/analytics/project/${projectId}/donate`),
    
    projectFollow:    (projectId)  => log(`/api/analytics/project/${projectId}/follow`),

    
    
    creatorClick:     (creatorId)  => log(`/api/analytics/creator/${creatorId}/click`,  false),
    
    creatorView:      (creatorId)  => log(`/api/analytics/creator/${creatorId}/view`,   false),
    
    creatorSubscribe: (creatorId)  => log(`/api/analytics/creator/${creatorId}/subscribe`),
    
    creatorDonate:    (creatorId)  => log(`/api/analytics/creator/${creatorId}/donate`),
    
    creatorFollow:    (creatorId)  => log(`/api/analytics/creator/${creatorId}/follow`),
};

export default analyticsService;
