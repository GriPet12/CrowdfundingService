<template>
  <div class="home">
    <h1>Users from Backend</h1>

    <div v-if="loading" class="loading">
      Loading...
    </div>

    <div v-if="error" class="error">
      {{ error }}
    </div>

    <div v-if="!loading && !error">
      <div class="users-container">
        <div v-for="user in users" :key="user.id" class="user-card">
          <h3>{{ user.username }}</h3>
          <p><strong>Email:</strong> {{ user.email }}</p>
          <p><strong>Role:</strong> {{ user.role }}</p>
        </div>
      </div>

      <div class="pagination">
        <button
            :disabled="currentPage === 0"
            @click="changePage(currentPage - 1)"
            class="page-btn"
        >
          Previous
        </button>
        <span class="page-info">Page {{ currentPage + 1 }} of {{ totalPages }}</span>
        <button
            :disabled="currentPage >= totalPages - 1"
            @click="changePage(currentPage + 1)"
            class="page-btn"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'HomeView',
  data() {
    return {
      users: [],
      loading: false,
      error: null,
      currentPage: 0,
      pageSize: 10,
      totalPages: 0,
      totalElements: 0
    }
  },
  created() {
    this.fetchUsers()
  },
  methods: {
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

        this.users = response.data.content
        this.totalPages = response.data.totalPages
        this.totalElements = response.data.totalElements
        this.currentPage = response.data.currentPage
      } catch (err) {
        this.error = 'Failed to fetch users: ' + (err.response?.data || err.message)
      } finally {
        this.loading = false
      }
    },
    changePage(page) {
      this.currentPage = page
      this.fetchUsers()
    }
  }
}
</script>

<style scoped>
.loading, .error {
  margin: 20px;
  padding: 10px;
}

.error {
  color: red;
  border: 1px solid red;
  border-radius: 4px;
}

.users-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  margin-bottom: 20px;
}

.user-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 15px;
  margin: 10px;
  width: 250px;
  text-align: left;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 20px 0;
}

.page-btn {
  padding: 8px 16px;
  margin: 0 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: #f8f8f8;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  margin: 0 16px;
}
</style>