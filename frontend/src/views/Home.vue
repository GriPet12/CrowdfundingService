<template>
  <HeaderApp />

  <div class="home">

    <h1 class="tag-name">Projects</h1>

    <div class="horizontal-slider">
      <div v-if="projectError" class="error">
        {{ projectError }}
      </div>

      <div class="projects-container">
        <div v-for="project in projects" :key="project.id" class="project-card">
          <h3>{{ project.title }}</h3>
          <p><strong>Author ID:</strong> {{ project.author }}</p>
        </div>

        <div v-if="loadingProjects" class="project-card loading-card">
          <div class="loader"></div>
          <p>Loading more projects...</p>
        </div>

        <div
            v-if="!loadingProjects && hasMoreProjects"
            @click="loadMoreProjects"
            class="project-card load-more-card">
          <div class="load-more-content">
            <span class="plus-icon">+</span>
            <p>Load More Projects</p>
          </div>
        </div>
      </div>
    </div>

    <h1 class="tag-name">Creators</h1>

    <div class="horizontal-slider">
      <div v-if="error" class="error">
        {{ error }}
      </div>

      <div class="users-container">
        <div v-for="user in users" :key="user.id" class="user-card">
          <h3>{{ user.username }}</h3>
          <p><strong>Email:</strong> {{ user.email }}</p>
          <p><strong>Role:</strong> {{ user.role }}</p>
        </div>

        <div v-if="loading" class="user-card loading-card">
          <div class="loader"></div>
          <p>Loading more users...</p>
        </div>

        <div
            v-if="!loading && hasMoreUsers"
            @click="loadMoreUsers"
            class="user-card load-more-card">
          <div class="load-more-content">
            <span class="plus-icon">+</span>
            <p>Load More Users</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import HeaderApp from '@/components/AppHeader.vue'

export default {
  name: 'HomeView',
  components: {
    HeaderApp
  },
  data() {
    return {
      users: [],
      loading: false,
      error: null,
      currentPage: 0,
      pageSize: 10,
      totalPages: 0,
      totalElements: 0,
      hasMoreUsers: true,

      projects: [],
      loadingProjects: false,
      projectError: null,
      projectCurrentPage: 0,
      projectPageSize: 8,
      projectTotalPages: 0,
      projectTotalElements: 0,
      hasMoreProjects: true
    }
  },
  created() {
    this.fetchProjects()
    this.fetchUsers()
  },
  methods: {
    async fetchProjects() {
      this.loadingProjects = true
      this.projectError = null

      try {
        const response = await axios.get('http://localhost:8081/projects', {
          params: {
            page: this.projectCurrentPage,
            size: this.projectPageSize
          }
        });

        this.projects = [...this.projects, ...response.data.content]
        this.projectTotalPages = response.data.totalPages
        this.projectTotalElements = response.data.totalElements

        this.hasMoreProjects = this.projectCurrentPage < response.data.totalPages - 1

      } catch (err) {
        this.projectError = 'Failed to fetch projects: ' + (err.response?.data || err.message)
      } finally {
        this.loadingProjects = false
      }
    },

    loadMoreProjects() {
      this.projectCurrentPage += 1
      this.fetchProjects()
    },

    async fetchUsers() {
      this.loading = true
      this.error = null

      try {
        const response = await axios.get('http://localhost:8081/creators', {
          params: {
            page: this.currentPage,
            size: this.pageSize
          }
        });

        this.users = [...this.users, ...response.data.content]
        this.totalPages = response.data.totalPages
        this.totalElements = response.data.totalElements

        this.hasMoreUsers = this.currentPage < response.data.totalPages - 1

      } catch (err) {
        this.error = 'Failed to fetch users: ' + (err.response?.data || err.message)
      } finally {
        this.loading = false
      }
    },

    loadMoreUsers() {
      this.currentPage += 1
      this.fetchUsers()
    }
  }
}
</script>

<style scoped>
.horizontal-slider {
  width: 100%;
  padding: 20px 0;
  border: 1px solid #eee;
  border-radius: 8px;
  margin: 20px auto;
  max-width: 90%;
  overflow-x: auto;
}

.projects-container {
  display: grid;
  grid-template-rows: repeat(2, 1fr);
  grid-auto-flow: column;
  gap: 20px;
  padding: 0 20px;
}

.project-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 15px;
  min-width: 300px;
  height: 200px;
  flex: 0 0 200px;
  text-align: center;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.users-container {
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  gap: 20px;
  padding: 0 20px;
}

.user-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 15px;
  min-width: 200px;
  height: 200px;
  flex: 0 0 200px;
  text-align: center;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.loading-card {
  justify-content: center;
  align-items: center;
  color: #666;
}

.load-more-card {
  cursor: pointer;
  background-color: #f9f9f9;
  transition: background-color 0.3s;
}

.load-more-card:hover {
  background-color: #eaeaea;
}

.load-more-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.plus-icon {
  font-size: 36px;
  margin-bottom: 10px;
  color: #4caf50;
}

.loader {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

.tag-name {
  width: 100%;
  padding: 20px 0;
  margin: 20px auto;
  max-width: 90%;
  text-align: left;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error {
  color: #d32f2f;
  background-color: #ffebee;
  border: 1px solid #ffcdd2;
  border-radius: 4px;
  padding: 10px 15px;
  margin: 0 0 20px 0;
  width: 100%;
  text-align: left;
  font-size: 14px;
}
</style>