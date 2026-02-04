const ProjectItem = ({ project }) => {
    return (
        <div style={{ border: '1px solid #ccc', padding: '15px', margin: '10px 0', borderRadius: '8px' }}>
            <img src={`/api/images/${project.mainImage}`} alt={project.title} style={{ width: '100%' }}/>
            <h3>{project.title}</h3>
            <p>Ціль: <strong>${project.goalAmount}</strong>
            Зібрано: <strong>${project.collectedAmount}</strong></p>

            {/* Простий прогрес-бар */}
            <div style={{ background: '#eee', height: '10px', width: '100%' }}>
                <div style={{
                    background: 'green',
                    height: '10px',
                    width: `${(project.collectedAmount / project.goalAmount) * 100}%`
                }} />
            </div>
        </div>
    );
};

export default ProjectItem;